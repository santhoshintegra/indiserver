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
import java.util.Date;
import java.util.Locale;

import laazotea.indi.Constants.LightStates;
import laazotea.indi.INDIDateFormat;
import laazotea.indi.Constants.PropertyPermissions;
import laazotea.indi.Constants.PropertyStates;
import laazotea.indi.Constants.SwitchRules;
import laazotea.indi.Constants.SwitchStatus;
import laazotea.indi.driver.INDIBLOBElementAndValue;
import laazotea.indi.driver.INDIBLOBProperty;
import laazotea.indi.driver.INDILightElement;
import laazotea.indi.driver.INDILightProperty;
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

public class lx200basic extends telescope {
	
	protected static lx200commands lx200  = new lx200commands();
	
	protected static boolean AbortSlew = false; 
	private final static String driverName = "LX200basic";
	//private final static int majorVersion = 0;
	//private final static int minorVersion = 1;	
	protected final static String FOCUS_GROUP = "Focus Control";
	protected final static String INFO_GROUP = "Information";
	
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
	/************************************* GROUP: Information *************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Site Name
	*********************************************/

	protected INDITextProperty ProductNameTP = new INDITextProperty(this,  "PRODUCT_NAME", "Product Name", INFO_GROUP, PropertyStates.IDLE, PropertyPermissions.RO, 0);
	protected INDITextElement ProductNameT = new INDITextElement(ProductNameTP, "FWNAME", "Name", "");

	/********************************************
	 Property: Site Name
	*********************************************/

	protected INDITextProperty FirmwareVersionTP = new INDITextProperty(this,  "FIRMWARE_VERSION", "Firmware Version", INFO_GROUP, PropertyStates.IDLE, PropertyPermissions.RO, 0);
	protected INDITextElement FirmwareVersionT = new INDITextElement(FirmwareVersionTP, "FWVERSION", "Version", "");

	/********************************************
	 Property: Site Name
	*********************************************/

	protected INDITextProperty FirmwareDateTimeTP = new INDITextProperty(this,  "FIRMWARE_DATETIME", "Firmware DateTime", INFO_GROUP, PropertyStates.IDLE, PropertyPermissions.RO, 0);
	protected INDITextElement FirmwareDateTimeT = new INDITextElement(FirmwareDateTimeTP, "FWDATETIME", "DateTime", "");

	protected INDILightProperty MountLP = new INDILightProperty(this, "MOUNT_TYPE", "Mount Type", INFO_GROUP, PropertyStates.IDLE);
	protected INDILightElement AzElL = new INDILightElement(MountLP, "MOUNT_AZEL", "AzEl Mount", LightStates.IDLE);
	protected INDILightElement EqL = new INDILightElement(MountLP, "MOUNT_EQ", "EQ Mount", LightStates.IDLE);
	protected INDILightElement GermanEqL = new INDILightElement(MountLP, "MOUNT_GEQ", "German EQ Mount", LightStates.IDLE);
	
	protected INDILightProperty AlignmentLP = new INDILightProperty(this, "ALIGNMENT_STATUS", "Alignment Status", INFO_GROUP, PropertyStates.IDLE);
	protected INDILightElement NotAlignedL = new INDILightElement(AlignmentLP, "NOT_ALIGNED", "Not aligned", LightStates.IDLE);
	protected INDILightElement OneStarL = new INDILightElement(AlignmentLP, "ONE_STAR_ALIGNED", "One-star aligned", LightStates.IDLE);
	protected INDILightElement TwoStarL = new INDILightElement(AlignmentLP, "TWO_STAR_ALIGNED", "Two-star aligned", LightStates.IDLE);
	protected INDILightElement ThreeStarL = new INDILightElement(AlignmentLP, "THREE_STAR_ALIGNED", "Three-star aligned", LightStates.IDLE);
	
	protected INDILightProperty TrackingLP = new INDILightProperty(this, "TRACKING_STATUS", "Tracking Status", INFO_GROUP, PropertyStates.IDLE);
	protected INDILightElement NotTrackingL = new INDILightElement(TrackingLP, "NOT_TRACKING", "Not Tracking", LightStates.IDLE);
	protected INDILightElement  TrackingL= new INDILightElement(TrackingLP, "TRACKING", "Tracking", LightStates.IDLE);
	
	
	/*****************************************************************************************************/
	/**************************************** END PROPERTIES *********************************************/
	/*****************************************************************************************************/

	
	
	/**
	 * Constructor with input and outputstream for indi-xml-messages.
	 * TODO: extend with com_driver and device interface string
	 */

	public lx200basic(InputStream in, OutputStream out) {
		super(in,out, "de.hallenbeck.indiserver.communication_drivers.bluetooth_serial", "00:80:37:14:9F:E7");

	}

	


	

	/**
	 * Set new text-values received from clients
	 */
	@Override
	public void processNewTextValue(INDITextProperty property, Date timestamp,
			INDITextElementAndValue[] elementsAndValues) {
		
		super.processNewTextValue(property, timestamp, elementsAndValues);
		

	
	}

	/**
	 * Set new switch-values received from clients
	 */
	@Override
	public void processNewSwitchValue(INDISwitchProperty property,
			Date timestamp, INDISwitchElementAndValue[] elementsAndValues) {
	
		super.processNewSwitchValue(property, timestamp, elementsAndValues);
		
		// Get the Element
		INDISwitchElement elem = elementsAndValues[0].getElement();
		
		boolean ret = false; 
	
		/**
		 * Alignment Property
		 */
		if (property==AlignmentSP) {
			if (elem==AltAzS) ret = setAlignmentMode('A');
			if (elem==PolarS) ret = setAlignmentMode('P');
			if (elem==LandS) ret = setAlignmentMode('L');
			if (ret) property.setState(PropertyStates.OK);
			else property.setState(PropertyStates.ALERT); 
			updateProperty(property);
		}
	
		/**
		 * SiteSwitch Property
		 */
		if (property==SitesSP) {
			if (elem==Sites1S) ret = setSite(1);
			if (elem==Sites2S) ret = setSite(2);
			if (elem==Sites3S) ret = setSite(3);
			if (elem==Sites4S) ret = setSite(4);
			if (ret) property.setState(PropertyStates.OK);
			else property.setState(PropertyStates.ALERT); 
			updateProperty(property);
		}
	
		/**
		 * Switch between slew and sync on new equatorial coords 
		 */
		if (property==OnCoordSetSP) {
			if (elem==SlewS) SlewS.setValue(SwitchStatus.ON);				
			if (elem==SyncS) SyncS.setValue(SwitchStatus.ON);
			property.setState(PropertyStates.OK);
			updateProperty(property);
		}
		
		if (property==SlewModeSP) {
			if (elem==GuideS) ret = setSlewMode(1);
			if (elem==CenteringS) ret = setSlewMode(2);
			if (elem==FindS) ret = setSlewMode(8);
			if (elem==MaxS) ret = setSlewMode(9);
			if (ret) property.setState(PropertyStates.OK);
			else property.setState(PropertyStates.ALERT); 
			updateProperty(property);
		}
	
		if (property==TrackModeSP) {
			if (elem==DefaultModeS) ret = setTrackMode(1);
			if (elem==LunarModeS) ret = setTrackMode(2);
			if (elem==ManualModeS) ret = setTrackMode(3);
			if (ret) property.setState(PropertyStates.OK);
			else property.setState(PropertyStates.ALERT); 
			updateProperty(property);
		}
		
		if (property==UsePulseCommandSP) {
			if (elem==UsePulseCommandOnS) ret = setPulseCommand(true);
			if (elem==UsePulseCommandOffS) ret = setPulseCommand(false);
			if (ret) property.setState(PropertyStates.OK);
			else property.setState(PropertyStates.ALERT); 
			updateProperty(property);
		}
		
		if (property==FocusMotionSP) {
			if (elem==FocusInS) ret = setFocusMotion(1);
			if (elem==FocusOutS) ret = setFocusMotion(2);
			if (ret) property.setState(PropertyStates.OK);
			else property.setState(PropertyStates.ALERT); 
			updateProperty(property);
		}
		
		if (property==FocusModesSP) {
			if (elem==FocusHaltS) ret = setFocusMode(0);
			if (elem==FocusSlowS) ret = setFocusMode(1);
			if (elem==FocusFastS) ret = setFocusMode(2);
			if (ret) property.setState(PropertyStates.OK);
			else property.setState(PropertyStates.ALERT); 
			updateProperty(property);
		}
		
	}

	/**
	 * Set new number-values received from clients 
	 */
	@Override
	public void processNewNumberValue(INDINumberProperty property,
			Date timestamp, INDINumberElementAndValue[] elementsAndValues) {
		
		super.processNewNumberValue(property, timestamp, elementsAndValues);
		
		boolean ret = false;
		
		if (property==TrackFreqNP) {
			if (elementsAndValues.length>0) ret = setTrackRate(elementsAndValues[0].getValue());
			if (!ret) propertyUpdateInfo="Error setting new Tracking Frequency";
			if (ret) property.setState(PropertyStates.OK); else property.setState(PropertyStates.ALERT);
			updateProperty(property, propertyUpdateInfo);
		}
	

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

	
	
	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.telescope#onConnect()
	 */
	@Override
	protected void onConnect() {
		super.onConnect();
		
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
		this.addProperty(MountLP);
		this.addProperty(AlignmentLP);
		this.addProperty(TrackingLP);
		
		getAlignmentMode();
		getFirmwareInformation();
		getAlignmentStatus();
		getTrackRate();

		// Always use high-precision coords
		if (getCommandString(lx200.getCurrentRACmd).length() == 7) {
			sendCommand(lx200.PrecisionToggleCmd);
			updateProperty(ConnectSP,"Setting high precision coords");
		}

		// Get data for Site #1
		setSite(1);
		getUTCOffset();
		getDateTime();

		// Get current equatorial coords the scope is pointing at
		getEqCoords(true);
		getTargetCoords();

		ConnectS.setValue(SwitchStatus.ON);
		ConnectSP.setState(PropertyStates.OK);
		updateProperty(ConnectSP,"Telescope ready, awaiting commmands...");
	}


	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.telescope#onDisconnect()
	 */
	@Override
	protected void onDisconnect() {
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
	    this.removeProperty(AlignmentLP);
		this.removeProperty(MountLP);
		this.removeProperty(TrackingLP);
		
		AbortSlew=true;
		super.onDisconnect();
	}


	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.telescope#onMovementNS(char)
	 */
	@Override
	protected boolean onMovementNS(char direction) {
		return super.onMovementNS(direction);
	}

	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.telescope#onMovementWE(char)
	 */
	@Override
	protected boolean onMovementWE(char direction) {
		return super.onMovementWE(direction);
	}

	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.telescope#onNewEquatorialCoords()
	 */
	@Override
	protected boolean onNewEquatorialCoords() {
		if (SlewS.getValue()==SwitchStatus.ON) {
			onAbortSlew();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				
			}
			SlewThread s = new SlewThread();
			s.start();
		}
		
		if (SyncS.getValue()==SwitchStatus.ON) {
			getCommandString(lx200.SyncToTargetCmd);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				
			}
			getEqCoords(true);
		}
		return true;
	}

	protected boolean onStartSlew() {
		return false;
	}

	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.telescope#onAbortSlew()
	 */
	@Override
	protected boolean onAbortSlew() {
		AbortSlew = true;
		return true;
	}

	/**
	 * return the DriverName
	 */
	@Override
	public String getName() {
		return driverName;
	}

	/**
	 * Get the Alignment-Mode from the telescope
	 */
	protected void getAlignmentMode() {
	
		// Send command 2 times!
		getCommandChar(lx200.AlignmentModeCmd);
	
		String error = null;
		AlignmentSP.setState(PropertyStates.OK);
		
		switch (getCommandChar(lx200.AlignmentModeCmd)) {
		case 'A': 			
			AltAzS.setValue(SwitchStatus.ON);
			LandS.setValue(SwitchStatus.OFF);
			PolarS.setValue(SwitchStatus.OFF);
			break;
		case 'D': 
			// I doubt that sending data to the
			// Handbox while it's in download-mode is very healthy...
			// Confirmed: It isn't! I had to reflash the Unit. 
			error = "WARNING: DOWNLOADER ACTIVE! DISCONNECTING...";
			disconnect();
			AlignmentSP.setState(PropertyStates.ALERT);
			break;
		case 'L':
			AltAzS.setValue(SwitchStatus.OFF);
			LandS.setValue(SwitchStatus.ON);
			PolarS.setValue(SwitchStatus.OFF);
			break;	
		case 'P': 
			AltAzS.setValue(SwitchStatus.OFF);
			LandS.setValue(SwitchStatus.OFF);
			PolarS.setValue(SwitchStatus.ON);
			break;
		}
		updateProperty(AlignmentSP, error);
	}

	/**
	 * Get alignment status		
	 */
	protected void getAlignmentStatus() {
		
		String tmp = getCommandString(lx200.getAlignmentStatusCmd,3);
		
		AzElL.setValue(LightStates.IDLE);
		EqL.setValue(LightStates.IDLE);
		GermanEqL.setValue(LightStates.IDLE);
		switch (tmp.charAt(0)) {
		case 'A':
			AzElL.setValue(LightStates.OK);
			break;
		case 'P':
			EqL.setValue(LightStates.OK);
			break;
		case 'G':
			GermanEqL.setValue(LightStates.OK);
			break;
		}
		MountLP.setState(PropertyStates.OK);
		updateProperty(MountLP);
		
		String warning = null;
		TrackingLP.setState(PropertyStates.OK);
		TrackingL.setValue(LightStates.IDLE);
		NotTrackingL.setValue(LightStates.IDLE);
		switch (tmp.charAt(1)) {
		case 'T':
			TrackingL.setValue(LightStates.OK);
			break;
		case 'N':
			NotTrackingL.setValue(LightStates.ALERT);
			TrackingLP.setState(PropertyStates.ALERT);
			warning = "WARNING: Telescope not tracking - Objects will move out of view!";
			break;
		}
		updateProperty(TrackingLP, warning);
		
		warning = null;
		AlignmentLP.setState(PropertyStates.OK);
		NotAlignedL.setValue(LightStates.IDLE);
		OneStarL.setValue(LightStates.IDLE);
		TwoStarL.setValue(LightStates.IDLE);
		ThreeStarL.setValue(LightStates.IDLE);
		switch (tmp.charAt(2)) {
		case '0':
			NotAlignedL.setValue(LightStates.ALERT);
			AlignmentLP.setState(PropertyStates.ALERT);
			warning = "WARNING: Telescope not aligned - Coordinates may be inaccurate!";
			break;
		case '1':
			OneStarL.setValue(LightStates.OK);
			break;
		case '2':
			TwoStarL.setValue(LightStates.OK);
			break;
		case '3':
			ThreeStarL.setValue(LightStates.OK);	
			break;
		}
		updateProperty(AlignmentLP, warning);
	}

	/**
	 * Get Firmware info
	 */
	protected void getFirmwareInformation() {
		ProductNameT.setValue(getCommandString(lx200.getProductNameCmd));
		ProductNameTP.setState(PropertyStates.OK);
		updateProperty(ProductNameTP);
		FirmwareVersionT.setValue(getCommandString(lx200.getFirmwareNumberCmd));
		FirmwareVersionTP.setState(PropertyStates.OK);
		updateProperty(FirmwareVersionTP);
		FirmwareDateTimeT.setValue(getCommandString(lx200.getFirmwareDateCmd)+" "+getCommandString(lx200.getFirmwareTimeCmd));
		FirmwareDateTimeTP.setState(PropertyStates.OK);
		updateProperty(FirmwareDateTimeTP);

	}

	protected void getUTCOffset() {
		String tmp = getCommandString(lx200.getUTCHoursCmd);
		double offset = Double.parseDouble(tmp);
		UTCOffsetN.setValue(offset);
		UTCOffsetNP.setState(PropertyStates.OK);
		updateProperty(UTCOffsetNP, "UTC offset: "+tmp+"h");
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
		updateProperty(TimeTP, "UTC time:"+dateStr);
	}

	/**
	 * Get the current Geolocation from telescope
	 */
	protected void getGeolocation() {
		GeoLatN.setValue(getCommandSexa(lx200.getSiteLatCmd));
		GeoLongN.setValue(getCommandSexa(lx200.getSiteLongCmd));
		GeoNP.setState(PropertyStates.OK);
		updateProperty(GeoNP, "Geolocation Lat: "+GeoLatN.getValueAsString()+" Long: "+GeoLongN.getValueAsString());
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
			if (updateState) {
				EquatorialCoordsRNP.setState(PropertyStates.OK);
				if (NotAlignedL.getValue()==LightStates.ALERT) {
					EquatorialCoordsRNP.setState(PropertyStates.ALERT);
					updateProperty(EquatorialCoordsRNP,"WARNING: Telescope not aligned - Coordinates may be inaccurate!");
				}
				updateProperty(EquatorialCoordsRNP,"Current coords RA: "+RARN.getValueAsString()+" DEC: "+DECRN.getValueAsString());
			} else updateProperty(EquatorialCoordsRNP);
			
		} catch (IllegalArgumentException e) {
	
		}
	
	}
	
	@Override
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
	
	protected void getTrackRate() {
		TrackFreqN.setValue(Double.parseDouble(getCommandString(lx200.getTrackingRateCmd)));
		TrackFreqNP.setState(PropertyStates.OK);
		updateProperty(TrackFreqNP);
	}

	protected boolean setAlignmentMode(char Mode) {
		return false;
	}

	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.telescope#setUTCOffset(double)
	 */
	@Override
	protected boolean setUTCOffset(double offset) {
		String sign = "+";
		if (offset<0) {
			sign = "-";
			offset = offset * -1;
		}
		String tmp = String.format("%s%02d.%01d", sign, (int) offset, (int) (offset % 1));
		String UTCHoursCmd = String.format(lx200.setUTCHoursCmd, tmp);
		if (getCommandInt(UTCHoursCmd)==1) {     
			getUTCOffset();
			return true;
		} else return false;
	}


	@Override
	protected boolean setDateTime(Date date) {
		// This is UTC-Time!
		// assemble date/time 
		String dateStr = new SimpleDateFormat("MM/dd/yy").format(date);
		String timeStr = new SimpleDateFormat("kk:mm:ss").format(date);
		String DateCmd = String.format(lx200.setDateCmd, dateStr);
		String TimeCmd = String.format(lx200.setTimeCmd, timeStr);
	
		// send to Autostar
		if ((getCommandInt(TimeCmd)==1) && (getCommandInt(DateCmd)==1)) {
			try {
				com_driver.read('#'); // Return String "Updating planetary data... #"
				com_driver.read('#'); // Return String "                           #"
			} catch (IOException e) {
				e.printStackTrace();
			}
			getDateTime();
			return true;
		} else return false;
	}

	protected boolean setSite(int site) {
		sendCommand(String.format(lx200.SiteSelectCmd,site));
		if (site==1) Sites1S.setValue(SwitchStatus.ON);
		if (site==2) Sites2S.setValue(SwitchStatus.ON);
		if (site==3) Sites3S.setValue(SwitchStatus.ON);
		if (site==4) Sites4S.setValue(SwitchStatus.ON);
		SitesSP.setState(PropertyStates.OK);
		updateProperty(SitesSP);
		getSiteName(site);
		getGeolocation();
		return true;
	}

	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.telescope#setLatitude(double)
	 */
	@Override
	protected boolean setLatitude(double latitude) {
		// Assemble an Autostar Latitude format
		// Positive = North, Negative = South  Example: "+50*01"
		String sign = "+";
		if (latitude<0) {
			sign ="-";
			latitude = latitude * -1;
		}
		// TODO: Instead of truncating doubles with (int) we should round them 
		String tmp = String.format("%s%02d*%02d", sign, (int) latitude, (int) ((latitude % 1)*60) );
		String GeolatCmd = String.format(lx200.setSiteLatCmd, tmp);

		// Set latitude
		if (getCommandChar(GeolatCmd)=='1') return true; else return false;
	}


	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.telescope#setLongitude(double)
	 */
	@Override
	protected boolean setLongitude(double longitude) {
		// Assemble an Autostar longitude format
		// TODO: Instead of truncating doubles with (int) we should round them 
		String tmp = String.format("%03d*%02d", (int) longitude, (int) ((longitude % 1)*60) ); 
		String GeolongCmd = String.format(lx200.setSiteLongCmd, tmp);
		//updateProperty(property,"Longitude sent:" + tmp);
		// Set longitude
		if (getCommandChar(GeolongCmd)=='1') return true; else return false;
	}


	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.telescope#setRA(double)
	 */
	@Override
	protected boolean setTargetRA(double RA) {
		String RAStr = sexa.format(RA);
		String RACmd = String.format(lx200.setTargetRACmd,RAStr);
		if (getCommandChar(RACmd)=='1') return true; else return false;
	}


	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.telescope#setDEC(double)
	 */
	@Override
	protected boolean setTargetDEC(double DEC) {
		String DECStr = sexa.format(DEC);
		String DECCmd = String.format(lx200.setTargetDECCmd,DECStr);
		if (getCommandChar(DECCmd)=='1') return true; else return false;
	}

	protected boolean setSlewMode(int mode) {
		if (mode==1) { sendCommand(lx200.SlewRateGuidingCmd); GuideS.setValue(SwitchStatus.ON); }
		if (mode==2) { sendCommand(lx200.SlewRateCenteringCmd); CenteringS.setValue(SwitchStatus.ON); }
		if (mode==8) { sendCommand(lx200.SlewRateFindCmd); FindS.setValue(SwitchStatus.ON); }
		if (mode==9) { sendCommand(lx200.SlewRateMaxCmd); MaxS.setValue(SwitchStatus.ON); }
		return true;
	}
	
	protected boolean setTrackMode(int mode) {
		if (mode==1) { sendCommand(lx200.TrackRateSiderealCmd); DefaultModeS.setValue(SwitchStatus.ON); }
		if (mode==2) { sendCommand(lx200.TrackRateLunarCmd); LunarModeS.setValue(SwitchStatus.ON); }
		if (mode==3) { sendCommand(lx200.TrackRateManualCmd); ManualModeS.setValue(SwitchStatus.ON); }
		getTrackRate();
		return true;
	}
	
	protected boolean setTrackRate(double rate) {
		String tmp = String.format(Locale.US,lx200.setTrackingRateCmd,rate);
		if (getCommandChar(tmp)=='1') {
			getTrackRate();
			ManualModeS.setValue(SwitchStatus.ON);
			TrackModeSP.setState(PropertyStates.OK);
			updateProperty(TrackModeSP);
			return true; 
		}
		else return false;
	}
	
	protected boolean setPulseCommand(boolean state) {
		return true;
	}
	
	protected boolean setFocusMotion(int direction) {
		return true;
	}
	
	protected boolean setFocusMode(int mode) {
		return true;
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
			try {
				com_driver.write(command);
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
			com_driver.write(command);
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
			com_driver.write(command);
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
			com_driver.write(command);
			
		} catch (IOException e) {
			updateProperty(ConnectSP,e.getMessage());
			disconnect();
		}
	}
	
}
