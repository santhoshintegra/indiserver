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
	private final static int majorVersion = 0;
	private final static int minorVersion = 1;	
	protected final static String FOCUS_GROUP = "Focus Control";
	protected final static String INFO_GROUP = "Information";
	
	/**
	 * Seperate Thread for slewing and continually updating equatorial coordinates
	 * @author atuschen
	 */
	protected class SlewThread extends Thread {

		public void run() {

			AbortSlew = false;

			// get return of MoveToTarget-Command (starts slewing immediately, if possible)
			int err = getCommandInt(lx200.MoveToTargetCmd); 
			if (err !=0) {
				// if Error exit with message
				EquatorialCoordsWNP.setState(PropertyStates.ALERT);
				if (err==1) updateProperty(EquatorialCoordsWNP, "Slew not possible: Target object below horizon");
				if (err==2) updateProperty(EquatorialCoordsWNP, "Slew not possible: Target object not reachable");

			} else { 
				// Set the ReadOnly property to busy 
				// There are 2 EquatorialCoords Properties: RNP is ReadOnly for clients, WNP is WriteOnly
				EquatorialCoordsRNP.setState(PropertyStates.BUSY);
				updateProperty(EquatorialCoordsRNP,"Slewing...");

				// Loop until slewing completed or aborted
				while ((!AbortSlew) && (getCommandString(lx200.DistanceBarsCmd).length()==1)) {

					//Continually update equatorial coordinates (ReadOnly) without updating the property-state
					getEqCoords(false);

				}

				if (AbortSlew) {
					// Stop Movement and update WriteOnly property
					sendCommand(lx200.StopAllMovementCmd);
					updateProperty(EquatorialCoordsWNP,"Slew aborted");

				} else {
					
					updateProperty(EquatorialCoordsWNP,"Slew complete");
				}

			}
			getEqCoords(true); // Update equatorial coordinates (ReadOnly) and property-state
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
			if (elem==Sites1S) setSite(1);
			if (elem==Sites2S) setSite(2);
			if (elem==Sites3S) setSite(3);
			if (elem==Sites4S) setSite(4);
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
		
		/**
		 * Select SlewSpeed
		 */
		if (property==SlewModeSP) {
			if (elem==GuideS) setSlewMode(1);
			if (elem==CenteringS) setSlewMode(2);
			if (elem==FindS) setSlewMode(8);
			if (elem==MaxS) setSlewMode(9);
			property.setState(PropertyStates.OK); 
			updateProperty(property);
		}
	
		/**
		 * Set Tracking rate
		 */
		if (property==TrackModeSP) {
			if (elem==DefaultModeS) setTrackMode(1);
			if (elem==LunarModeS) setTrackMode(2);
			if (elem==ManualModeS) setTrackMode(3);
			property.setState(PropertyStates.OK);
			updateProperty(property);
		}
		
		/**
		 * Use pulse commands for guiding
		 */
		if (property==UsePulseCommandSP) {
			if (elem==UsePulseCommandOnS) setPulseCommand(true);
			if (elem==UsePulseCommandOffS) setPulseCommand(false);
			property.setState(PropertyStates.OK); 
			updateProperty(property);
		}
		
		/**
		 * Move focuser in or out
		 */
		if (property==FocusMotionSP) {
			if (elem==FocusInS) ret = setFocusMotion(1);
			if (elem==FocusOutS) ret = setFocusMotion(2);
			if (ret) property.setState(PropertyStates.OK);
			property.setState(PropertyStates.OK); 
			updateProperty(property);
		}
		
		/**
		 * Set focuser mode
		 */
		if (property==FocusModesSP) {
			if (elem==FocusHaltS) setFocusMode(0);
			if (elem==FocusSlowS) setFocusMode(1);
			if (elem==FocusFastS) setFocusMode(2);
			property.setState(PropertyStates.OK); 
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
		
		if (property==TrackFreqNP) {
			if ((elementsAndValues.length>0) && (setTrackRate(elementsAndValues[0].getValue()))) {
				property.setState(PropertyStates.OK);
				updateProperty(property);
			} else {
				property.setState(PropertyStates.ALERT);
				updateProperty(property, "Error setting new Tracking Frequency");
			}
		}
	}

	/**
	 * Called after successfull connection
	 */
	@Override
	protected void onConnect() {
		
		// Setup lx200 properties
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
		
		// Always call this as first telescope command after connecting
		// It will determine, if the Handbox is in Download-Mode an disconnect immediately
		getAlignmentMode();
		
		// get/set initial values
		getFirmwareInformation();
		getTrackRate();

		// Always use high-precision coords for DA and DEC
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
		
		// update property state 
		updateProperty(ConnectSP,"Telescope ready, awaiting commmands...");
	}

	
	/** 
	 * Called after disconnect from telescope
	 * Clean up here
	 */
	@Override
	protected void onDisconnect() {
		
		// This terminates any running SlewThread, but not
		// Slewing itself, because we're already disconnected
		onAbortSlew();
		
		// remove lx200 properties
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

	}
	
	/**
	 * Called on Movement North/South command
	 */
	@Override
	protected void onMovementNS(char direction) {
		
	}

	/**
	 * Called on Movement North/South command
	 */
	@Override
	protected void onMovementWE(char direction) {
		
	}

	/**
	 * Called after successfully setting new Target Coords
	 */
	@Override
	protected void onNewTargetCoords() {
		
		// We either slew the telescope to the target
		if (SlewS.getValue()==SwitchStatus.ON) {
			
			// Abort all current slewing
			onAbortSlew();	
			
			// Start a new SlewThread
			SlewThread s = new SlewThread();
			s.start();
		}
		
		// or sync the current coords to the target ones
		if (SyncS.getValue()==SwitchStatus.ON) {
			getCommandString(lx200.SyncToTargetCmd);
			
			// Update current coords
			getEqCoords(true);
		}
	}

	/**
	 * Abort all current slewing
	 */
	@Override
	protected void onAbortSlew() {
		AbortSlew = true;
		// Wait a moment for SlewThread to terminate
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}
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
	
		String error = null;
		AlignmentSP.setState(PropertyStates.OK);
		
		switch (getCommandChar(lx200.AlignmentModeCmd)) {
		case 'A': 			
			AltAzS.setValue(SwitchStatus.ON);
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
			LandS.setValue(SwitchStatus.ON);
			break;	
		case 'P': 
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
	
	/**
	 * Get the UTCoffet
	 * NOTE: For LX200-Telescopes this is the UTC-Offset to yield local time
	 * but for Autostar#497 this is the value to yield UTC from local time (see LX200autostar-driver)
	 */
	protected void getUTCOffset() {
		String tmp = getCommandString(lx200.getUTCHoursCmd);
		double offset = Double.parseDouble(tmp);
		UTCOffsetN.setValue(offset);
		UTCOffsetNP.setState(PropertyStates.OK);
		updateProperty(UTCOffsetNP, "UTC offset: "+tmp+"h");
	}
	
	/**
	 * Get the current Date/Time from telescope
	 * NOTE: For LX200-Telescopes this is in UTC
	 * but Autostar#497 uses Local time (see LX200autostar-driver)  
	 */
	protected void getDateTime() {
		String dateStr = getCommandString(lx200.getTimeCmd)+" "+getCommandString(lx200.getDateCmd);
		try {
			Date date = new SimpleDateFormat("kk:mm:ss MM/dd/yy").parse(dateStr);
			TimeT.setValue(INDIDateFormat.formatTimestamp(date));
		} catch (ParseException e) {
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
		GeoLongN.setValue(360-getCommandSexa(lx200.getSiteLongCmd));
		GeoNP.setState(PropertyStates.OK);
		updateProperty(GeoNP, "Geolocation Lat: "+GeoLatN.getValueAsString()+" Long: "+GeoLongN.getValueAsString());
	}

	/** 
	 * Get Name of Site site# from telescope
	 */
	protected void getSiteName(int site) {
		if (site==1) SiteNameT.setValue(getCommandString(lx200.getSite1NameCmd));
		if (site==2) SiteNameT.setValue(getCommandString(lx200.getSite2NameCmd));
		if (site==3) SiteNameT.setValue(getCommandString(lx200.getSite3NameCmd));
		if (site==4) SiteNameT.setValue(getCommandString(lx200.getSite4NameCmd));
		SiteNameTP.setState(PropertyStates.OK);
		updateProperty(SiteNameTP,"Site Name: "+SiteNameT.getValue());
	}
	
	/**
	 * Get the message currently displayed on the Handbox
	 * @return message
	 */
	protected String getDisplayMessage() {
		return getCommandString(lx200.getDisplayMsgCmd);
	}

	/**
	 * Get the current equatorial coords the scope is pointing at  
	 */
	protected synchronized void getEqCoords(boolean updateState) {
		try {
			RARN.setValue(sexa.parseSexagesimal(getCommandString(lx200.getCurrentRACmd)));
			DECRN.setValue(sexa.parseSexagesimal(getCommandString(lx200.getCurrentDECCmd)));
			if (updateState) {
				getAlignmentStatus();
				EquatorialCoordsRNP.setState(PropertyStates.OK);
				if (NotAlignedL.getValue()==LightStates.ALERT) EquatorialCoordsRNP.setState(PropertyStates.ALERT);
				updateProperty(EquatorialCoordsRNP,"Current coords RA: "+RARN.getValueAsString()+" DEC: "+DECRN.getValueAsString());
			} else 
				updateProperty(EquatorialCoordsRNP);

		} catch (IllegalArgumentException e) {

		}

	}
	
	/**
	 *  Get the current target coords from the telescope 
	 */
	@Override
	protected void getTargetCoords() {
		RAWN.setValue(sexa.parseSexagesimal(getCommandString(lx200.getTargetRACmd)));
		DECWN.setValue(sexa.parseSexagesimal(getCommandString(lx200.getTargetDECCmd)));
		EquatorialCoordsWNP.setState(PropertyStates.OK);
		updateProperty(EquatorialCoordsWNP,"Target Object RA: "+RAWN.getValueAsString()+" DEC: "+DECWN.getValueAsString());
	}
	
	/**
	 * Get the current tracking rate from the telescope
	 */
	protected void getTrackRate() {
		TrackFreqN.setValue(Double.parseDouble(getCommandString(lx200.getTrackingRateCmd)));
		TrackFreqNP.setState(PropertyStates.OK);
		updateProperty(TrackFreqNP);
	}

	/**
	 * set the Alignment Mode
	 * @param Mode (A = AltAz, P = Polar, L = Land)
	 * @return true on success, false on error
	 */
	protected boolean setAlignmentMode(char Mode) {
		// TODO: not yet implemented
		return false;
	}

	/**
	 * set the UTC offset 
	 * @param hours to yield local time from UTC
	 * @return true on success, false on error
	 */
	@Override
	protected boolean setUTCOffset(double offset) {
		String sign = "+";
		if (offset<0) {
			sign = "-";
			offset = offset * -1;
		}
		String tmp = String.format("%s%02d.%01d", sign, (int) offset, (int) (offset % 1));
		
		if (getCommandInt(String.format(lx200.setUTCHoursCmd, tmp))==1) 
			return true;
		else 
			return false;
	}

	/**
	 * set current Date/Time in UTC
	 * @param Date
	 * @return true on success, false on error
	 */
	@Override
	protected boolean setDateTime(Date date) {
		
		// assemble date/time lx200-format  
		String dateStr = new SimpleDateFormat("MM/dd/yy").format(date);
		String timeStr = new SimpleDateFormat("kk:mm:ss").format(date);
		
		// send Time first and at last the Date
		// Telescope is calculating planetary objects after a new date is set
		if ((getCommandInt(String.format(lx200.setTimeCmd, timeStr))==1) && 
				(getCommandInt(String.format(lx200.setDateCmd, dateStr))==1)) {
			
			// Read 2 Strings from the Telescope and throw them away
			try {
				com_driver.read('#'); // Return String "Updating planetary data... #"
				com_driver.read('#'); // Return String "                           #"
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			
			return true;
		} else 
			return false;
	}

	/**
	 * set the selected Site number
	 * @param site number
	 * @return always true  
	 */
	protected void setSite(int site) {
		sendCommand(String.format(lx200.SiteSelectCmd,site));
		if (site==1) Sites1S.setValue(SwitchStatus.ON);
		if (site==2) Sites2S.setValue(SwitchStatus.ON);
		if (site==3) Sites3S.setValue(SwitchStatus.ON);
		if (site==4) Sites4S.setValue(SwitchStatus.ON);
		SitesSP.setState(PropertyStates.OK);
		updateProperty(SitesSP);
		getSiteName(site);
		getGeolocation();
	}

	/**
	 * set current site latitude
	 * @param latitude
	 * @return true on success, false on error
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
		String GeolatCmd = String.format(lx200.setSiteLatCmd, String.format("%s%02d*%02d", sign, (int) latitude, (int) ((latitude % 1)*60) ));

		// Set latitude
		if (getCommandInt(GeolatCmd)==1) return true; 
		else return false;
	}

	/**
	 * set current site longitude
	 * @param longitude
	 * @return true on success, false on error
	 */
	@Override
	protected boolean setLongitude(double longitude) {
		// Assemble an Autostar longitude format
		// TODO: Instead of truncating doubles with (int) we should round them  
		String GeolongCmd = String.format(lx200.setSiteLongCmd, String.format("%03d*%02d", (int) (360-longitude), (int) (((360-longitude) % 1)*60) ));
		
		// Set longitude
		if (getCommandInt(GeolongCmd)==1) return true; 
		else return false;
	}

	/**
	 * set target object RA
	 * @param RA
	 * @return true on success, false on error
	 */
	@Override
	protected boolean setTargetRA(double RA) {
		if (getCommandInt(String.format(lx200.setTargetRACmd, sexa.format(RA)))==1) return true; 
		else return false;
	}

	/**
	 * set target object DEC
	 * @param DEC
	 * @return true on success, false on error
	 */
	@Override
	protected boolean setTargetDEC(double DEC) {
		if (getCommandInt(String.format(lx200.setTargetDECCmd, sexa.format(DEC)))==1) return true;
		else return false;
	}

	/**
	 * set slew speed
	 * @param speed 1,2,8,9 (values 3-7 resreved)
	 * @return always true 
	 */
	protected void setSlewMode(int mode) {
		if (mode==1) { sendCommand(lx200.SlewRateGuidingCmd); GuideS.setValue(SwitchStatus.ON); }
		if (mode==2) { sendCommand(lx200.SlewRateCenteringCmd); CenteringS.setValue(SwitchStatus.ON); }
		if (mode==8) { sendCommand(lx200.SlewRateFindCmd); FindS.setValue(SwitchStatus.ON); }
		if (mode==9) { sendCommand(lx200.SlewRateMaxCmd); MaxS.setValue(SwitchStatus.ON); }
	}
	
	/**
	 * set tracking speed
	 * @param speed 1 = sidereal, 2 = lunar , 3 = manual
	 * @return always true
	 */
	protected void setTrackMode(int mode) {
		if (mode==1) { sendCommand(lx200.TrackRateSiderealCmd); DefaultModeS.setValue(SwitchStatus.ON); }
		if (mode==2) { sendCommand(lx200.TrackRateLunarCmd); LunarModeS.setValue(SwitchStatus.ON); }
		if (mode==3) { sendCommand(lx200.TrackRateManualCmd); ManualModeS.setValue(SwitchStatus.ON); }
		getTrackRate();
	}
	
	/**
	 * set manual tracking speed
	 * @param rate
	 * @return true on success, false on error
	 */
	protected boolean setTrackRate(double rate) {
		if (getCommandInt(String.format(Locale.US,lx200.setTrackingRateCmd,rate))==1) {
			getTrackRate();
			ManualModeS.setValue(SwitchStatus.ON);
			TrackModeSP.setState(PropertyStates.OK);
			updateProperty(TrackModeSP);
			return true; 
		}
		else return false;
	}
	
	/**
	 * use pulsed guiding
	 * @param state
	 * @return
	 */
	protected boolean setPulseCommand(boolean state) {
		// TODO: not yet implemented
		return true;
	}
	
	/**
	 * Move focuser in / out
	 * @param direction
	 * @return
	 */
	protected boolean setFocusMotion(int direction) {
		// TODO: not yet implemented
		return true;
	}
	
	/**
	 * Set focuser mode
	 * @param mode
	 * @return
	 */
	protected boolean setFocusMode(int mode) {
		// TODO: not yet implemented
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
		return sexa.parseSexagesimal(getCommandString(command));
	}
	
	/**
	 * Get one digit from the device 
	 * @param command send
	 * @return integer 
	 */
	protected synchronized int getCommandInt(String command){
		return Integer.parseInt(getCommandString(command,1));
	}
	
	/**
	 * Get one char from the device
	 * @param command to send
	 * @return char
	 */
	protected synchronized char getCommandChar(String command) {
		return getCommandString(command,1).charAt(0);
	}
	
	/**
	 * Get a string from the device without the #-suffix and < >
	 * @param command to send 
	 * @return string 
	 */
	protected synchronized String getCommandString(String command) {
		String tmp=null;
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
	
	/**
	 * Get len bytes from the device
	 * @param command to send
	 * @param len 
	 * @return
	 */
	protected synchronized String getCommandString(String command, int len) {
		String tmp=null;
		try {
			com_driver.write(command);
			tmp = com_driver.read(len);
			
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
			com_driver.write(command);
			
		} catch (IOException e) {
			updateProperty(ConnectSP,e.getMessage());
			disconnect();
		}
	}
	
}
