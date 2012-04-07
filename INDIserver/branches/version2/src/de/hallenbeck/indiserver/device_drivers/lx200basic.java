/*
 *
 * This file is part of INDIserver.
 *
 * INDIserver is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * INDIserver is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with INDIserver.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2012 Alexander Tuschen <atuschen75 at gmail dot com>
 *
 */

package de.hallenbeck.indiserver.device_drivers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import laazotea.indi.INDISexagesimalFormatter;
import laazotea.indi.INDIDateFormat;
import laazotea.indi.Constants.PropertyPermissions;
import laazotea.indi.Constants.PropertyStates;
import laazotea.indi.Constants.SwitchRules;
import laazotea.indi.Constants.SwitchStatus;
import laazotea.indi.driver.INDIBLOBElementAndValue;
import laazotea.indi.driver.INDIBLOBProperty;
import laazotea.indi.driver.INDINumberElement;
import laazotea.indi.driver.INDINumberElementAndValue;
import laazotea.indi.driver.INDINumberProperty;
import laazotea.indi.driver.INDISwitchElement;
import laazotea.indi.driver.INDISwitchElementAndValue;
import laazotea.indi.driver.INDISwitchProperty;
import laazotea.indi.driver.INDITextElement;
import laazotea.indi.driver.INDITextElementAndValue;
import laazotea.indi.driver.INDITextProperty;

import de.hallenbeck.indiserver.device_drivers.lx200commands;

/**
 * Driver for LX200 compatible telescopes, only covering the basic commandset.
 * Extended LX200-protocols should be derived from this class. 
 * There are some errors in the official Meade LX200 protocol sheet.
 * i.e. some answer-strings are localized (command ":P#" gives "HOCH PRAEZISION" or 
 * "NIEDER PRAEZISION" on german Firmware 42Gg)
 * 
 * This class is based on lx200generic.cpp of indilib and my own tests with 
 * Autostar #497 Firmware 43Eg (english)
 * No guarantee that this will work with the newer Autostar #497-EP models or 
 * any other firmware version than 43Eg!
 *   
 * @author atuschen75 at gmail.com
 *
 *
 */

public class lx200basic extends telescope implements device_driver_interface {
	
	protected static lx200commands lx200  = new lx200commands();
	
	protected static boolean AbortSlew = false; 
	private final static String driverName = "LX200basic";
	private final static int majorVersion = 0;
	private final static int minorVersion = 1;	
	protected final static String FOCUS_GROUP = "Focus Control";
	protected final static String FIRMWARE_GROUP = "Firmware Information";
	
	/**
	 * Seperate Thread for slewing and continually updating equatorial coordinates
	 * @author atuschen
	 *
	 */
	
	protected class SlewThread extends Thread {

		public void run() {

			AbortSlew = false;

			// get return of MoveToTarget Command (starts slewing immediately, if possible)
			int err = getCommandInt(lx200.MoveToTargetCmd); 
			if (err !=0) {
				// if Error exit with message
				EquatorialCoordsWNP.setState(PropertyStates.ALERT);
				if (err==1) updateProperty(EquatorialCoordsWNP, "Slew not possible: Target object below horizon");
				if (err==2) updateProperty(EquatorialCoordsWNP, "Slew not possible: Target object not reachable");

			} else { 

				EquatorialCoordsRNP.setState(PropertyStates.BUSY);
				updateProperty(EquatorialCoordsRNP,"Slewing...");

				// Loop until slewing completed or aborted
				while ((!AbortSlew) && (getCommandString(lx200.DistanceBarsCmd).length()==1)) {

					//Continually update equatorial coordinates without updating the property-state
					getEqCoords(false);

				}

				if (AbortSlew) {

					sendCommand(lx200.StopAllMovementCmd);
					updateProperty(EquatorialCoordsWNP,"Slew aborted");

				} else {

					updateProperty(EquatorialCoordsWNP,"Slew complete");
				}

			}
			getEqCoords(true); // Update equatorial coordinates and property
		}
	}
			
	/* INDI Properties for LX200 compatible telescopes */
	
	/**********************************************************************************************/
	/************************************ GROUP: Communication ************************************/
	/**********************************************************************************************/

	/**
	 * The device-interface property isn't implemented.
	 * It's up to the server-app to set the right interface for the device, not the remote client(s).
	 */
	
	/********************************************
	 Property: Telescope Alignment Mode
	*********************************************/
	protected INDISwitchProperty AlignmentSP  = new INDISwitchProperty(this, "ALIGNMENT", "Alignment", COMM_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
	protected INDISwitchElement PolarS = new INDISwitchElement(AlignmentSP, "POLAR" , "Polar" , SwitchStatus.ON);
	protected INDISwitchElement AltAzS = new INDISwitchElement(AlignmentSP, "ALTAZ" , "AltAz" , SwitchStatus.OFF);
	protected INDISwitchElement LandS = new INDISwitchElement(AlignmentSP, "LAND" , "Land" , SwitchStatus.OFF);
	
	/**********************************************************************************************/
	/************************************ GROUP: Main Control *************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: On Coord Set
	 Description: This property decides what happens
	             when we receive a new equatorial coord
	             value. We either track, or sync
		     to the new coordinates.
	*********************************************/
	protected INDISwitchProperty OnCoordSetSP = new INDISwitchProperty(this, "ON_COORD_SET", "On Set", BASIC_GROUP,PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
	protected INDISwitchElement SlewS = new INDISwitchElement(OnCoordSetSP, "SLEW", "Slew", SwitchStatus.ON);
	protected INDISwitchElement SyncS = new INDISwitchElement(OnCoordSetSP, "SYNC", "Sync", SwitchStatus.OFF);
	
	/**********************************************************************************************/
	/************************************** GROUP: Motion *****************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Slew Speed
	*********************************************/
	protected INDISwitchProperty SlewModeSP = new INDISwitchProperty(this,"SLEW_RATE","Slew rate", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
	protected INDISwitchElement MaxS = new INDISwitchElement(SlewModeSP, "MAX", "Max", SwitchStatus.ON);
	protected INDISwitchElement FindS = new INDISwitchElement(SlewModeSP, "FIND", "Find", SwitchStatus.OFF);
	protected INDISwitchElement CenteringS = new INDISwitchElement(SlewModeSP, "CENTERING", "Centering", SwitchStatus.OFF);
	protected INDISwitchElement GuideS = new INDISwitchElement(SlewModeSP, "GUIDE", "Guide", SwitchStatus.OFF);
	
	/********************************************
	 Property: Tracking Mode
	*********************************************/
	protected INDISwitchProperty TrackModeSP = new INDISwitchProperty(this,"TRACKING_MODE", "Tracking Mode", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
	protected INDISwitchElement DefaultModeS = new INDISwitchElement(TrackModeSP, "DEFAULT", "Default", SwitchStatus.ON);
	protected INDISwitchElement LunarModeS = new INDISwitchElement(TrackModeSP, "LUNAR", "Lunar", SwitchStatus.OFF);
	protected INDISwitchElement ManualModeS = new INDISwitchElement(TrackModeSP, "MANUAL", "Manual", SwitchStatus.OFF);
	
	/********************************************
	 Property: Tracking Frequency
	*********************************************/
	protected INDINumberProperty TrackFreqNP = new INDINumberProperty(this,"TRACKING_FREQ", "Tracking Frequency", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
	protected INDINumberElement TrackFreqN = new INDINumberElement(TrackFreqNP, "TRACK_FREQ", "Freq", 60.1, 56.4, 60.1, 0.1, "%g");
	
	/********************************************
	 Property: Timed Guide movement. North/South
	*********************************************/
	protected INDINumberProperty GuideNSNP = new INDINumberProperty(this,"TELESCOPE_TIMED_GUIDE_NS", "Guide North/South", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
	protected INDINumberElement GuideNorthN = new INDINumberElement(GuideNSNP, "TIMED_GUIDE_N", "North (sec)", 0, 0, 10, 0.001, "%g");
	protected INDINumberElement GuideSouthN = new INDINumberElement(GuideNSNP, "TIMED_GUIDE_S", "South (sec)", 0, 0, 10, 0.001, "%g");
	
	/********************************************
	 Property: Timed Guide movement. West/East
	*********************************************/
	protected INDINumberProperty GuideWENP = new INDINumberProperty(this,"TELESCOPE_TIMED_GUIDE_WE", "Guide West/East", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
	protected INDINumberElement GuideWestN = new INDINumberElement(GuideWENP, "TIMED_GUIDE_W", "West (sec)", 0, 0, 10, 0.001, "%g");
	protected INDINumberElement GuideEastN = new INDINumberElement(GuideWENP, "TIMED_GUIDE_E", "East (sec)", 0, 0, 10, 0.001, "%g");
	
	/********************************************
	 Property: Slew Accuracy
	 Desciption: How close the scope have to be with
		     respect to the requested coords for 
		     the tracking operation to be successull
		     i.e. returns OK
	*********************************************/
	protected INDINumberProperty SlewAccuracyNP = new INDINumberProperty(this,"SLEW_ACCURACY", "Slew Accuracy", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
	protected INDINumberElement SlewAccuracyRAN = new INDINumberElement(SlewAccuracyNP, "SLEW_RA",  "RA (arcmin)", 3, 0, 60, 1, "%g");
	protected INDINumberElement SlewAccuracyDECN = new INDINumberElement(SlewAccuracyNP, "SLEW_DEC", "Dec (arcmin)", 3, 0, 60, 1, "%g");
	
	/********************************************
	 Property: Use pulse-guide commands
	 Desciption: Set to on if this mount can support
	             pulse guide commands.  There appears to
	             be no way to query this information from
	             the mount
	*********************************************/
	protected INDISwitchProperty UsePulseCommandSP = new INDISwitchProperty(this,"USE_PULSE_CMD", "Use PulseCMd", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
	protected INDISwitchElement UsePulseCommandOnS = new INDISwitchElement(UsePulseCommandSP, "PULSE_ON", "On", SwitchStatus.OFF);
	protected INDISwitchElement UsePulseCommandOffS = new INDISwitchElement(UsePulseCommandSP, "PULSE_OFF", "Off", SwitchStatus.ON);

	/**********************************************************************************************/
	/************************************** GROUP: Focus ******************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Focus Direction
	*********************************************/
	protected INDISwitchProperty FocusMotionSP = new INDISwitchProperty(this,"FOCUS_MOTION", "Motion", FOCUS_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
	protected INDISwitchElement FocusInS = new INDISwitchElement(FocusMotionSP, "IN", "Focus in", SwitchStatus.OFF);
	protected INDISwitchElement FocusOutS = new INDISwitchElement(FocusMotionSP, "OUT", "Focus out", SwitchStatus.OFF);

	/********************************************
	 Property: Focus Timer
	*********************************************/
	protected INDINumberProperty FocusTimerNP = new INDINumberProperty(this,"FOCUS_TIMER", "Focus Timer", FOCUS_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
	protected INDINumberElement FocusTimerN = new INDINumberElement(FocusTimerNP, "TIMER", "Timer (ms)", 50, 0, 10000, 1000, "%g");
	
	/********************************************
	 Property: Focus Mode
	*********************************************/
	protected INDISwitchProperty FocusModesSP = new INDISwitchProperty(this,"FOCUS_MODE", "Mode", FOCUS_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
	protected INDISwitchElement FocusHaltS = new INDISwitchElement(FocusModesSP, "FOCUS_HALT", "Halt", SwitchStatus.ON);
	protected INDISwitchElement FocusSlowS = new INDISwitchElement(FocusModesSP, "FOCUS_SLOW", "Slow", SwitchStatus.OFF);
	protected INDISwitchElement FocusFastS = new INDISwitchElement(FocusModesSP, "FOCUS_FAST", "Fast", SwitchStatus.OFF);

	/**********************************************************************************************/
	/*********************************** GROUP: Date & Time ***************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Sidereal Time
	*********************************************/
	protected INDINumberProperty SDTimeNP = new INDINumberProperty(this,"TIME_LST", "Sidereal Time", DATETIME_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
	protected INDINumberElement SDTimeN = new INDINumberElement(SDTimeNP, "LST", "Sidereal time", 0, 0, 24, 0, "%10.6m");

	/**********************************************************************************************/
	/************************************* GROUP: Sites *******************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Site Management
	*********************************************/
	protected INDISwitchProperty SitesSP = new INDISwitchProperty(this,"SITES", "Sites", SITE_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
	protected INDISwitchElement Sites1S = new INDISwitchElement(SitesSP, "SITE1", "Site 1", SwitchStatus.ON);
	protected INDISwitchElement Sites2S = new INDISwitchElement(SitesSP, "SITE2", "Site 2", SwitchStatus.OFF);
	protected INDISwitchElement Sites3S = new INDISwitchElement(SitesSP, "SITE3", "Site 3", SwitchStatus.OFF);
	protected INDISwitchElement Sites4S = new INDISwitchElement(SitesSP, "SITE4", "Site 4", SwitchStatus.OFF);

	/********************************************
	 Property: Site Name
	*********************************************/
	protected INDITextProperty SiteNameTP = new INDITextProperty(this,  "SITE NAME", "Site Name", SITE_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
	protected INDITextElement SiteNameT = new INDITextElement(SiteNameTP, "NAME", "Name", "");

	/**********************************************************************************************/
	/************************************* GROUP: Firmware*****************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Site Name
	*********************************************/
	protected INDITextProperty ProductNameTP = new INDITextProperty(this,  "PRODUCT_NAME", "Product Name", FIRMWARE_GROUP, PropertyStates.IDLE, PropertyPermissions.RO, 0);
	protected INDITextElement ProductNameT = new INDITextElement(ProductNameTP, "FWNAME", "Name", "");

	/********************************************
	 Property: Site Name
	*********************************************/
	protected INDITextProperty FirmwareVersionTP = new INDITextProperty(this,  "FIRMWARE_VERSION", "Firmware Version", FIRMWARE_GROUP, PropertyStates.IDLE, PropertyPermissions.RO, 0);
	protected INDITextElement FirmwareVersionT = new INDITextElement(FirmwareVersionTP, "FWVERSION", "Version", "");

	/********************************************
	 Property: Site Name
	*********************************************/
	protected INDITextProperty FirmwareDateTimeTP = new INDITextProperty(this,  "FIRMWARE_DATETIME", "Firmware Date & Time", FIRMWARE_GROUP, PropertyStates.IDLE, PropertyPermissions.RO, 0);
	protected INDITextElement FirmwareDateTimeT = new INDITextElement(FirmwareDateTimeTP, "FWDATETIME", "Date & Time", "");

	
	/*****************************************************************************************************/
	/**************************************** END PROPERTIES *********************************************/
	/*****************************************************************************************************/

	
	
	/**
	 * Constructor with input and outputstream for indi-xml-messages.
	 * TODO: extend with com_driver and device interface string
	 */
	public lx200basic(InputStream in, OutputStream out) {
		super(in,out);
	}

	
	/*
	 * Public interface methods 
	 */
		
	/**
	 * Connect to telescope and update INDI-Properties
	 */ 
	@Override
	public void connect() throws IOException{
		try {
			set_communication_driver("de.hallenbeck.indiserver.communication_drivers.bluetooth_serial");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		set_device("00:80:37:14:9F:E7");
		
		if (!isConnected()) {
			super.connect();
			// Test serial connection
			// by getting Alignment information
			getAlignmentMode();
		} 
		
		if (!isConnected()) {
			
			throw new IOException("No serial connection");
			
		} else {
			this.addProperty(AlignmentSP);
		    this.addProperty(OnCoordSetSP);
		    this.addProperty(SlewModeSP);
		    this.addProperty(TrackModeSP);
		    this.addProperty(TrackFreqNP);
		    this.addProperty(GuideNSNP);
		    this.addProperty(GuideWENP);
		    this.addProperty(SlewAccuracyNP);
		    this.addProperty(UsePulseCommandSP);
		    this.addProperty(FocusMotionSP);
		    this.addProperty(FocusTimerNP);
		    this.addProperty(FocusModesSP);
		    this.addProperty(SDTimeNP);
		    this.addProperty(SitesSP);
		    this.addProperty(SiteNameTP);
		    this.addProperty(ProductNameTP);
		    this.addProperty(FirmwareVersionTP);
		    this.addProperty(FirmwareDateTimeTP);
		    
			getFirmwareInformation();
			
			getAlignmentStatus();
			
			// Always use high-precision coords
			if (getCommandString(lx200.getCurrentRACmd).length() == 7) {
				sendCommand(lx200.PrecisionToggleCmd);
				updateProperty(ConnectSP,"Setting high precision coords");
			}
			
			// Get data for Site #1
			getSiteName(1);
			
			getGeolocation();
			
			// Get current equatorial coords the scope is pointing at
			getEqCoords(true);
			
			ConnectS.setValue(SwitchStatus.ON);
			DisconnectS.setValue(SwitchStatus.OFF);
			ConnectSP.setState(PropertyStates.OK);
			updateProperty(ConnectSP,"Telescope ready, awaiting commmands...");
		}
		
	}

	/**
	 * Disconnect from telescope and update INDI-Properties
	 */
	@Override
	public void disconnect() {
			
		if (isConnected()) {
			super.disconnect();
			this.removeProperty(AlignmentSP);
		    this.removeProperty(OnCoordSetSP);
		    this.removeProperty(SlewModeSP);
		    this.removeProperty(TrackModeSP);
		    this.removeProperty(TrackFreqNP);
		    this.removeProperty(GuideNSNP);
		    this.removeProperty(GuideWENP);
		    this.removeProperty(SlewAccuracyNP);
		    this.removeProperty(UsePulseCommandSP);
		    this.removeProperty(FocusMotionSP);
		    this.removeProperty(FocusTimerNP);
		    this.removeProperty(FocusModesSP);
		    this.removeProperty(SDTimeNP);
		    this.removeProperty(SitesSP);
		    this.removeProperty(SiteNameTP);
		    this.removeProperty(ProductNameTP);
		    this.removeProperty(FirmwareVersionTP);
		    this.removeProperty(FirmwareDateTimeTP);
		    
		    
			AbortSlew=true;
			ConnectS.setValue(SwitchStatus.OFF);
			DisconnectS.setValue(SwitchStatus.ON);
			ConnectSP.setState(PropertyStates.IDLE);
			updateProperty(ConnectSP,"Disconnected from telescope");
		}
	}
	
	
	/*
	 * INDI Driver methods
	 * @see laazotea.indi.driver.INDIDriver
	 */

	/**
	 * return the DriverName
	 */
	@Override
	public String getName() {
		return driverName;
	}

	/**
	 * Set new text-values received from clients
	 */
	@Override
	public void processNewTextValue(INDITextProperty property, Date timestamp,
			INDITextElementAndValue[] elementsAndValues) {


		/**
		 * UTC Time Property
		 */
		if (property==TimeTP) {

			Date date = INDIDateFormat.parseTimestamp(elementsAndValues[0].getValue());

			// Autostar expects local time, but INDI clients send UTC!
			// We have to add the UTC-Offset to get the local time
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.add(Calendar.HOUR, UTCOffsetN.getValue().intValue());
			cal.add(Calendar.MINUTE, (int) ((UTCOffsetN.getValue()%1)*60));
			date = cal.getTime();

			// assemble Autostar-format date/time 
			String dateStr = new SimpleDateFormat("MM/dd/yy").format(date);
			String timeStr = new SimpleDateFormat("kk:mm:ss").format(date);
			String DateCmd = String.format(lx200.setDateCmd, dateStr);
			String TimeCmd = String.format(lx200.setTimeCmd, timeStr);

			// send to Autostar
			getCommandChar(TimeCmd);
			getCommandChar(DateCmd);
			try {
				com_driver.read('#'); // Return String "Updating planetary data... #"
				com_driver.read('#'); // Return String "                           #"
			} catch (IOException e) {
				e.printStackTrace();
			}

			// Verify by read and update Properties
			getDateTime();
		}

		/**
		 * Geolocation Property
		 */
		
		super.processNewTextValue(property, timestamp, elementsAndValues);

	}


	/**
	 * Set new switch-values received from clients
	 */
	@Override
	public void processNewSwitchValue(INDISwitchProperty property,
			Date timestamp, INDISwitchElementAndValue[] elementsAndValues) {

		// Get the Element
		INDISwitchElement elem = elementsAndValues[0].getElement();

		/**
		 * Connect Property always available
		 */
		if (property==ConnectSP) {
			if (elem == DisconnectS) disconnect();
			if (elem == ConnectS) {
				try {
					connect();
				} catch (IOException e) {
					property.setState(PropertyStates.ALERT);
					updateProperty(property,e.getMessage());
				}
			}
		}


		/**
		 * Alignment Property
		 */
		if (property==AlignmentSP) {
			if (elem==AltAzS) sendCommand(lx200.AlignmentAltAzCmd);
			if (elem==PolarS) sendCommand(lx200.AlignmentPolarCmd);
			if (elem==LandS) sendCommand(lx200.AlignmentLandCmd);
			getAlignmentMode();
		}

		/**
		 * SiteSwitch Property
		 */
		if (property==SitesSP) {
			int site=1;

			if (elem==Sites1S) {
				site=1;
				Sites1S.setValue(SwitchStatus.ON);
				Sites2S.setValue(SwitchStatus.OFF);
				Sites3S.setValue(SwitchStatus.OFF);
				Sites4S.setValue(SwitchStatus.OFF);
			}
			if (elem==Sites2S) {
				site=2;
				Sites1S.setValue(SwitchStatus.OFF);
				Sites2S.setValue(SwitchStatus.ON);
				Sites3S.setValue(SwitchStatus.OFF);
				Sites4S.setValue(SwitchStatus.OFF);
			}
			if (elem==Sites3S){
				site=3;
				Sites1S.setValue(SwitchStatus.OFF);
				Sites2S.setValue(SwitchStatus.OFF);
				Sites3S.setValue(SwitchStatus.ON);
				Sites4S.setValue(SwitchStatus.OFF);
			}
			if (elem==Sites4S){
				site=4;
				Sites1S.setValue(SwitchStatus.OFF);
				Sites2S.setValue(SwitchStatus.OFF);
				Sites3S.setValue(SwitchStatus.OFF);
				Sites4S.setValue(SwitchStatus.ON);
			}
			// Set current site  
			sendCommand(String.format(lx200.SiteSelectCmd,site));

			// Get the name of the selected site
			getSiteName(site);
			// Get the geolocation of the selected site
			getGeolocation();

			SitesSP.setState(PropertyStates.OK);
			updateProperty(SitesSP,"Selected Site #"+site);
		}

		/**
		 * Switch between slew and sync on new equatorial coords 
		 */
		if (property==OnCoordSetSP) {
			if (elem==SlewS) {
				SlewS.setValue(SwitchStatus.ON);
				SyncS.setValue(SwitchStatus.OFF);
			}
			if (elem==SyncS) {
				SlewS.setValue(SwitchStatus.OFF);
				SyncS.setValue(SwitchStatus.ON);
			}
			OnCoordSetSP.setState(PropertyStates.OK);
			updateProperty(OnCoordSetSP);

		}

		/**
		 * Abort all current slewing
		 */
		if (property==AbortSlewSP) {
			AbortSlew=true;
			updateProperty(AbortSlewSP);
		}
		super.processNewSwitchValue(property, timestamp, elementsAndValues);
	}

	/**
	 * Set new number-values received from clients 
	 */
	@Override
	public void processNewNumberValue(INDINumberProperty property,
			Date timestamp, INDINumberElementAndValue[] elementsAndValues) {


		/**
		 * UTC-Offset Property
		 */
		if (property==UTCOffsetNP) {

			if (elementsAndValues.length>0) {
				// Get the value
				double val = elementsAndValues[0].getValue();
				// Standard "String.format" doesn't work, we need a string like "+02.0"
				// Additionally we have to change +/-, because Autostar needs a value to YIELD UTC
				// KStars sends the Offset (+02.0) but Autostar needs (-02.0) to get the right time.
				// The Handbox only displays the correct timezone +02.0 if we send -02.0 to it.  

				String sign = "-";
				if (val<0) {
					sign = "+";
					val = val * -1;
				}
				String tmp = String.format("%s%02d.%01d", sign, (int) val, (int) (val % 1));
				String UTCHoursCmd = String.format(lx200.setUTCHoursCmd, tmp);
				getCommandInt(UTCHoursCmd);	
				UTCOffsetN.setValue(val);
				UTCOffsetNP.setState(PropertyStates.OK);
				updateProperty(property, "Local Time to UTC difference: "+tmp+"h");
			}
		}

		/**
		 * Geolocation Property
		 */
		if (property==GeoNP) {
			int i = 0;
			while (i < elementsAndValues.length) {
				if (elementsAndValues[i].getElement() ==  GeoLatN) {
					double geolat = elementsAndValues[0].getValue();
					// Assemble an Autostar Latitude format
					// Positive = North, Negative = South  Example: "+50*01"
					String sign = "+";
					if (geolat<0) {
						sign ="-";
						geolat = geolat * -1;
					}
					// TODO: Instead of truncating doubles with (int) we should round them 
					String tmp = String.format("%s%02d*%02d", sign, (int) geolat, (int) ((geolat % 1)*60) );
					String GeolatCmd = String.format(lx200.setSiteLatCmd, tmp);
					updateProperty(property,"Latitude sent:" + tmp);

					// Set latitude
					getCommandChar(GeolatCmd);
				}
				if (elementsAndValues[i].getElement() == GeoLongN) {
					double geolong = 360-elementsAndValues[1].getValue();
					// Assemble an Autostar longitude format
					// TODO: Instead of truncating doubles with (int) we should round them 
					String tmp = String.format("%03d*%02d", (int) geolong, (int) ((geolong % 1)*60) ); 
					String GeolongCmd = String.format(lx200.setSiteLongCmd, tmp);
					updateProperty(property,"Longitude sent:" + tmp);
					// Set longitude
					getCommandChar(GeolongCmd);
				}
				i++; 
			}
			//Verify by read and update Properties
			getGeolocation();
		}

		/**
		 * New Equatorial Coords
		 */
		if (property == EquatorialCoordsWNP) {

			// Get the coords
			int i=0;
			while (i < elementsAndValues.length) {
				if (elementsAndValues[i].getElement() ==  RAWN) {
					double RA = elementsAndValues[i].getValue();
					String RAStr = sexa.format(RA);
					String RACmd = String.format(lx200.setTargetRACmd,RAStr);
					if (getCommandInt(RACmd)==1) updateProperty(property,"Target RA set: "+RAStr);

				}
				if (elementsAndValues[i].getElement() == DECWN) {
					double DEC = elementsAndValues[i].getValue();
					String DECStr = sexa.format(DEC);
					String DECCmd = String.format(lx200.setTargetDECCmd,DECStr);
					//if (getCommandChar(DECCmd)==1) updateProperty(property,"Target DEC set: "+DECStr);
					getCommandChar(DECCmd);
					updateProperty(property,"Target DEC set: "+DECStr);
				}
				i++;
			}

			// Verify target coords by read 
			getTargetCoords();

			// "Slew on new coord" is set
			if (SlewS.getValue()==SwitchStatus.ON) {

				// Abort all current slewing
				AbortSlew = true;

				// Wait a moment for a thread to terminate
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				// Start new thread for slewing
				SlewThread slewThread = new SlewThread();
				slewThread.start();
			}

			// "Sync on new coord" is set
			if (SyncS.getValue()==SwitchStatus.ON) {
				getCommandString(lx200.SyncToTargetCmd);
				getEqCoords(true);
				updateProperty(property,"Synced telescope to coordinates");
			}

		}
		super.processNewNumberValue(property, timestamp, elementsAndValues);
	}

	/**
	 * Not needed, telescope doesn't use BLOBs
	 */
	@Override
	public void processNewBLOBValue(INDIBLOBProperty property, Date timestamp,
			INDIBLOBElementAndValue[] elementsAndValues) {
		// Leave empty here, not needed. 
		super.processNewBLOBValue(property, timestamp, elementsAndValues);
	}

	/**
	 * Get the Alignment-Mode from the telescope
	 */
	protected void getAlignmentMode() {

		// Send command 2 times!
		getCommandChar(lx200.AlignmentModeCmd);

		String stmp=null;

		switch (getCommandChar(lx200.AlignmentModeCmd)) {
		case 'A': 			
			AltAzS.setValue(SwitchStatus.ON);
			LandS.setValue(SwitchStatus.OFF);
			PolarS.setValue(SwitchStatus.OFF);
			AlignmentSP.setState(PropertyStates.OK);
			stmp="AltAz alignment";
			break;
		case 'D': 
			// I doubt that sending data to the
			// Handbox while it's in download-mode is very healthy...
			// Confirmed: It isn't! I had to reflash the Unit. 
			stmp="WARNING: DOWNLOADER ACTIVE! DISCONNECTING...";
			disconnect();
			AlignmentSP.setState(PropertyStates.ALERT);
			break;
		case 'L':
			AltAzS.setValue(SwitchStatus.OFF);
			LandS.setValue(SwitchStatus.ON);
			PolarS.setValue(SwitchStatus.OFF);
			AlignmentSP.setState(PropertyStates.OK);
			stmp="Land alignment";
			break;	
		case 'P': 
			AltAzS.setValue(SwitchStatus.OFF);
			LandS.setValue(SwitchStatus.OFF);
			PolarS.setValue(SwitchStatus.ON);
			AlignmentSP.setState(PropertyStates.OK);
			stmp="Polar alignment";
			break;
		}
		updateProperty(AlignmentSP,stmp);
	}

	/**
	 * Get alignment status		
	 */
	protected void getAlignmentStatus() {
		String tmp = getCommandString(lx200.getAlignmentStatusCmd,3);
		String stmp = null;
		switch (tmp.charAt(0)) {
		case 'A':
			stmp = "AzEl mount";
			break;
		case 'P':
			stmp = "Eq mount";
			break;
		case 'G':
			stmp = "German Eq mount";
			break;
		}

		switch (tmp.charAt(1)) {
		case 'T':
			stmp = stmp + ", Tracking";
			break;
		case 'N':
			stmp = stmp + ", not Tracking";
			break;
		}

		//TODO: Display some warning about not aligned telescope, slewing may be inaccurate
		switch (tmp.charAt(2)) {
		case '0':
			stmp = stmp + ", NOT aligned!";
			break;
		case '1':
			stmp = stmp + ", one-star aligned";
			break;
		case '2':
			stmp = stmp + ", two-star aligned";
			break;
		case '3':
			stmp = stmp + ", three-star aligned";
			break;
		}
		updateProperty(AlignmentSP, stmp);
	}

	/**
	 * Get Firmware info
	 */
	protected void getFirmwareInformation() {
		ProductNameT.setValue(getCommandString(lx200.getProductNameCmd));
		updateProperty(ProductNameTP);
		FirmwareVersionT.setValue(getCommandString(lx200.getFirmwareNumberCmd));
		updateProperty(FirmwareVersionTP);
		FirmwareDateTimeT.setValue(getCommandString(lx200.getFirmwareDateCmd)+" "+getCommandString(lx200.getFirmwareTimeCmd));
		updateProperty(FirmwareDateTimeTP);
	}

	/**
	 * Get the current Date/Time from telescope
	 */
	protected void getDateTime() {
		String dateStr = getCommandString(lx200.getTimeCmd)+" "+getCommandString(lx200.getDateCmd);
		try {
			Date date = new SimpleDateFormat("kk:mm:ss MM/dd/yy").parse(dateStr);
			TimeT.setValue(INDIDateFormat.formatTimestamp(date));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TimeTP.setState(PropertyStates.OK);
		updateProperty(TimeTP, "Local time:"+dateStr);
	}

	/**
	 * Get the current Geolocation from telescope
	 */
	protected void getGeolocation() {
		GeoLatN.setValue(getCommandSexa(lx200.getSiteLatCmd));
		GeoLongN.setValue(360-getCommandSexa(lx200.getSiteLongCmd));
		GeoNP.setState(PropertyStates.OK);
		updateProperty(GeoNP, "Geolocation Lat: "+GeoLatN+" Long: "+GeoLongN);
	}

	/** 
	 * Get Name of Site site# from telescope
	 */
	protected void getSiteName(int site) {
		switch (site) {
		case 1: 
			SiteNameT.setValue(getCommandString(lx200.getSite1NameCmd));
			break;
		case 2: 
			SiteNameT.setValue(getCommandString(lx200.getSite2NameCmd));
			break;
		case 3: 
			SiteNameT.setValue(getCommandString(lx200.getSite3NameCmd));
			break;
		case 4: 
			SiteNameT.setValue(getCommandString(lx200.getSite4NameCmd));
			break;
		}
		SiteNameTP.setState(PropertyStates.OK);
		updateProperty(SiteNameTP,"Site Name: "+SiteNameT.getValue());
	}


	protected String getDisplayMessage() {
		return getCommandString(lx200.getDisplayMsgCmd);
	}

	/**
	 * Get the current equatorial coords the scope is pointing at
	 * TODO: some warning if telescope is not aligned, coords may be inaccurate  
	 */
	protected synchronized void getEqCoords(boolean updateState) {

		try {
			double RA = sexa.parseSexagesimal(getCommandString(lx200.getCurrentRACmd));
			double DEC = sexa.parseSexagesimal(getCommandString(lx200.getCurrentDECCmd));
			RARN.setValue(RA);
			DECRN.setValue(DEC);
			if (updateState) EquatorialCoordsRNP.setState(PropertyStates.OK);
			updateProperty(EquatorialCoordsRNP); //,"Current coords RA: "+RARN.getValueAsString()+" DEC: "+DECRN.getValueAsString());
		} catch (IllegalArgumentException e) {

		}

	}

	protected void getTargetCoords() {

		String tmp = getCommandString(lx200.getTargetRACmd);
		double RA = sexa.parseSexagesimal(tmp);
		String tmp2 = getCommandString(lx200.getTargetDECCmd);
		double DEC = sexa.parseSexagesimal(tmp2);
		RAWN.setValue(RA);
		DECWN.setValue(DEC);
		EquatorialCoordsWNP.setState(PropertyStates.OK);
		updateProperty(EquatorialCoordsWNP,"Target Object RA: "+RAWN.getValueAsString()+" DEC: "+DECWN.getValueAsString());

	}
	
	
	/*
	 * Auxillary functions (LX200 specific)
	 * As communication with Autostar is synchronous, it will only respond on commands.
	 * It never sends anything on it's own. There are some inconsistencies in the command
	 * protocol: Most returned strings end with a # character, but sadly not all.
	 * Most replys of a 1 indicate success and a 0 indicates a failure, but there are some 
	 * commands where it's vice-versa (at least according to the protocol-sheet). 
	 */
	
	/**
	 * Get a converted sexagesimal value from the device 
	 * @param command
	 * @return double
	 */
	protected synchronized double getCommandSexa(String command){
		double tmp = sexa.parseSexagesimal(getCommandString(command));
		return tmp;
	}
	
	/**
	 * Get an integer from the device 
	 * @param command
	 * @return integer 
	 */
	protected synchronized int getCommandInt(String command){
		return Integer.parseInt(getCommandString(command,1));
		
	}
	
	/**
	 * Get a char from the device
	 * @param command
	 * @return char
	 */
	protected synchronized char getCommandChar(String command) {
		char tmp='-';
		if (command!=null){
			com_driver.set_timeout(1000);

			try {
				//com_driver.emptyBuffer();
				com_driver.sendCommand(command);
				tmp = com_driver.read(1).charAt(0);
				
			} catch (IOException e) {
				updateProperty(ConnectSP,e.getMessage());
				disconnect();
			}
		}
		return tmp;
	}
	
	/**
	 * Get a string from the device without the #-suffix and < >
	 * @param command 
	 * @return string 
	 */
	protected synchronized String getCommandString(String command) {
		String tmp="";
		try {
			//com_driver.emptyBuffer();
			com_driver.sendCommand(command);
			com_driver.set_timeout(1000);
			tmp = com_driver.read('#');
			tmp = tmp.replaceAll("#", "");
			tmp = tmp.replaceAll("<", "");
			tmp = tmp.replaceAll(">", "");
			tmp = tmp.trim();
			
		} catch (IOException e) {
			updateProperty(ConnectSP,e.getMessage());
			disconnect();
		}
		return tmp;
	}
	
	protected synchronized String getCommandString(String command, int bytes) {
		String tmp="";
		try {
			//com_driver.emptyBuffer();
			com_driver.sendCommand(command);
			com_driver.set_timeout(1000);
			tmp = com_driver.read(bytes);
			
		} catch (IOException e) {
			updateProperty(ConnectSP,e.getMessage());
			disconnect();
		}
		return tmp;
	}
	
	/**
	 * Just send a command to the device 
	 * for some commands there is no return (i.e. movement)
	 * @param command
	 */
	protected synchronized void sendCommand(String command) {
		try {
			//com_driver.emptyBuffer();
			com_driver.sendCommand(command);
			
		} catch (IOException e) {
			updateProperty(ConnectSP,e.getMessage());
			disconnect();
		}
	}
	
}
