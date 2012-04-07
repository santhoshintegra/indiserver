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
import java.util.Date;

import laazotea.indi.INDISexagesimalFormatter;
import laazotea.indi.Constants.PropertyPermissions;
import laazotea.indi.Constants.PropertyStates;
import laazotea.indi.Constants.SwitchRules;
import laazotea.indi.Constants.SwitchStatus;
import laazotea.indi.driver.INDIBLOBElementAndValue;
import laazotea.indi.driver.INDIBLOBProperty;
import laazotea.indi.driver.INDIDriver;
import laazotea.indi.driver.INDINumberElement;
import laazotea.indi.driver.INDINumberElementAndValue;
import laazotea.indi.driver.INDINumberProperty;
import laazotea.indi.driver.INDISwitchElement;
import laazotea.indi.driver.INDISwitchElementAndValue;
import laazotea.indi.driver.INDISwitchProperty;
import laazotea.indi.driver.INDITextElement;
import laazotea.indi.driver.INDITextElementAndValue;
import laazotea.indi.driver.INDITextProperty;
import de.hallenbeck.indiserver.communication_drivers.communication_driver_interface;

/**
 * Abstract telescope-class with basic functions and basic INDI interface
 * 
 * @author atuschen
 *
 */
public abstract class telescope extends INDIDriver implements device_driver_interface {
	
	protected final static String COMM_GROUP = "Communication";
	protected final static String BASIC_GROUP = "Main Control";
	protected final static String MOTION_GROUP	= "Motion Control";
	protected final static String DATETIME_GROUP = "Date/Time";
	protected final static String SITE_GROUP = "Site Management";
	protected static INDISexagesimalFormatter sexa = new INDISexagesimalFormatter("%10.6m");
	protected static communication_driver_interface com_driver=null;
	private static String device=null;
	private static boolean connected=false;

	/* Only the very basic INDI-Properties, that every telescope should have */
	
	/**********************************************************************************************/
	/************************************ GROUP: Communication ************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Connection
	*********************************************/
	protected INDISwitchProperty ConnectSP;// = new INDISwitchProperty(this, "CONNECTION", "Connection", COMM_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);			// suffix SP = SwitchProperty
	protected INDISwitchElement ConnectS;// = new INDISwitchElement(ConnectSP, "CONNECT" , "Connect" , SwitchStatus.OFF);			
	protected INDISwitchElement DisconnectS;// = new INDISwitchElement(ConnectSP, "DISCONNECT" , "Disconnect" , SwitchStatus.ON);		
	
	/**********************************************************************************************/
	/************************************ GROUP: Main Control *************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Equatorial Coordinates JNow
	 Perm: Transient WO.
	 Timeout: 120 seconds.
	*********************************************/
	protected INDINumberProperty EquatorialCoordsWNP;// = new INDINumberProperty(this, "EQUATORIAL_EOD_COORD_REQUEST", "Equatorial JNow", BASIC_GROUP, PropertyStates.IDLE, PropertyPermissions.WO, 120);	// suffix NP = NumberProperty
	protected INDINumberElement RAWN;// = new INDINumberElement(EquatorialCoordsWNP, "RA", "RA  H:M:S", 0, 0, 24, 0, "%10.6m");
	protected INDINumberElement DECWN;// = new INDINumberElement(EquatorialCoordsWNP, "DEC", "Dec D:M:S", 0, -90, 90, 0, "%10.6m");
	
	/********************************************
	 Property: Equatorial Coordinates JNow
	 Perm: RO
	*********************************************/
	protected INDINumberProperty EquatorialCoordsRNP;// = new INDINumberProperty(this, "EQUATORIAL_EOD_COORD", "Equatorial JNow", BASIC_GROUP, PropertyStates.IDLE, PropertyPermissions.RO, 120);
	protected INDINumberElement RARN;// = new INDINumberElement(EquatorialCoordsRNP, "RA", "RA  H:M:S", 0, 0, 24, 0, "%10.6m");
	protected INDINumberElement DECRN;// = new INDINumberElement(EquatorialCoordsRNP, "DEC", "Dec D:M:S", 0, -90, 90, 0, "%10.6m");
	
	/********************************************
	 Property: Abort telescope motion
	*********************************************/
	protected INDISwitchProperty AbortSlewSP;// = new INDISwitchProperty(this, "TELESCOPE_ABORT_MOTION", "Abort Slew", BASIC_GROUP,PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
	protected INDISwitchElement AbortSlewS;// = new INDISwitchElement(AbortSlewSP, "ABORT", "Abort", SwitchStatus.OFF);
	
	/**********************************************************************************************/
	/************************************** GROUP: Motion *****************************************/
	/**********************************************************************************************/
	
	/********************************************
	 Property: Movement (Arrow keys on handset). North/South
	*********************************************/
	protected INDISwitchProperty MovementNSSP;// = new INDISwitchProperty(this,"TELESCOPE_MOTION_NS", "North/South", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
	protected INDISwitchElement MoveNorthS;// = new INDISwitchElement(MovementNSSP, "MOTION_NORTH", "North", SwitchStatus.OFF);
	protected INDISwitchElement MoveSouthS;// = new INDISwitchElement(MovementNSSP, "MOTION_SOUTH", "South", SwitchStatus.OFF);
	
	/********************************************
	 Property: Movement (Arrow keys on handset). West/East
	*********************************************/
	protected INDISwitchProperty MovementWESP;// = new INDISwitchProperty(this,"TELESCOPE_MOTION_WE", "West/East", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
	protected INDISwitchElement MoveWestS;// = new INDISwitchElement(MovementWESP, "MOTION_WEST", "West", SwitchStatus.OFF);
	protected INDISwitchElement MoveEastS;// = new INDISwitchElement(MovementWESP, "MOTION_EAST", "East", SwitchStatus.OFF);
	
	/**********************************************************************************************/
	/*********************************** GROUP: Date & Time ***************************************/
	/**********************************************************************************************/
	
	/********************************************
	 Property: UTC Time
	*********************************************/
	protected INDITextProperty TimeTP;// = new INDITextProperty(this,  "TIME_UTC", "UTC Time", DATETIME_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
	protected INDITextElement TimeT;// = new INDITextElement(TimeTP, "UTC", "UTC", "0");

	/********************************************
	 Property: DST Corrected UTC Offfset
	*********************************************/
	protected INDINumberProperty UTCOffsetNP;// = new INDINumberProperty(this, "TIME_UTC_OFFSET", "UTC Offset", DATETIME_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
	protected INDINumberElement UTCOffsetN;// = new INDINumberElement(UTCOffsetNP, "OFFSET", "Offset", 0, -12, 12, 0.5, "%0.3g");

	/**********************************************************************************************/
	/************************************* GROUP: Sites *******************************************/
	/**********************************************************************************************/
	
	/********************************************
	 Property: Geographical Location
	*********************************************/
	protected INDINumberProperty GeoNP;// = new INDINumberProperty(this,"GEOGRAPHIC_COORD", "Geographic Location", SITE_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
	protected INDINumberElement GeoLatN;// = new INDINumberElement(GeoNP, "LAT",  "Lat.  D:M:S +N", 0, -90, 90, 0, "%10.6m");
	protected INDINumberElement GeoLongN;// = new INDINumberElement(GeoNP, "LONG",  "Long. D:M:S", 0, 0, 360, 0, "%10.6m");
	
	/*****************************************************************************************************/
	/**************************************** END PROPERTIES *********************************************/
	/*****************************************************************************************************/


	protected telescope(InputStream in, OutputStream out) {
		 super(in, out);
		 ConnectSP = new INDISwitchProperty(this, "CONNECTION", "Connection", COMM_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
		 ConnectS = new INDISwitchElement(ConnectSP, "CONNECT" , "Connect" , SwitchStatus.OFF);
		 DisconnectS = new INDISwitchElement(ConnectSP, "DISCONNECT" , "Disconnect" , SwitchStatus.ON);
		 EquatorialCoordsWNP = new INDINumberProperty(this, "EQUATORIAL_EOD_COORD_REQUEST", "Equatorial JNow", BASIC_GROUP, PropertyStates.IDLE, PropertyPermissions.WO, 120);	// suffix NP = NumberProperty
		 RAWN = new INDINumberElement(EquatorialCoordsWNP, "RA", "RA  H:M:S", 0, 0, 24, 0, "%10.6m");
		 DECWN = new INDINumberElement(EquatorialCoordsWNP, "DEC", "Dec D:M:S", 0, -90, 90, 0, "%10.6m");
		 EquatorialCoordsRNP = new INDINumberProperty(this, "EQUATORIAL_EOD_COORD", "Equatorial JNow", BASIC_GROUP, PropertyStates.IDLE, PropertyPermissions.RO, 120);
		 RARN = new INDINumberElement(EquatorialCoordsRNP, "RA", "RA  H:M:S", 0, 0, 24, 0, "%10.6m");
		 DECRN = new INDINumberElement(EquatorialCoordsRNP, "DEC", "Dec D:M:S", 0, -90, 90, 0, "%10.6m");
		 AbortSlewSP = new INDISwitchProperty(this, "TELESCOPE_ABORT_MOTION", "Abort Slew", BASIC_GROUP,PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
		 AbortSlewS = new INDISwitchElement(AbortSlewSP, "ABORT", "Abort", SwitchStatus.OFF);
		 MovementNSSP = new INDISwitchProperty(this,"TELESCOPE_MOTION_NS", "North/South", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
		 MoveNorthS = new INDISwitchElement(MovementNSSP, "MOTION_NORTH", "North", SwitchStatus.OFF);
		 MoveSouthS = new INDISwitchElement(MovementNSSP, "MOTION_SOUTH", "South", SwitchStatus.OFF);
		 MovementWESP = new INDISwitchProperty(this,"TELESCOPE_MOTION_WE", "West/East", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
		 MoveWestS = new INDISwitchElement(MovementWESP, "MOTION_WEST", "West", SwitchStatus.OFF);
		 MoveEastS = new INDISwitchElement(MovementWESP, "MOTION_EAST", "East", SwitchStatus.OFF);
		 TimeTP = new INDITextProperty(this,  "TIME_UTC", "UTC Time", DATETIME_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
		 TimeT = new INDITextElement(TimeTP, "UTC", "UTC", "0");
		 UTCOffsetNP = new INDINumberProperty(this, "TIME_UTC_OFFSET", "UTC Offset", DATETIME_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
		 UTCOffsetN = new INDINumberElement(UTCOffsetNP, "OFFSET", "Offset", 0, -12, 12, 0.5, "%0.3g");
		 GeoNP = new INDINumberProperty(this,"GEOGRAPHIC_COORD", "Geographic Location", SITE_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
		 GeoLatN = new INDINumberElement(GeoNP, "LAT",  "Lat.  D:M:S +N", 0, -90, 90, 0, "%10.6m");
		 GeoLongN = new INDINumberElement(GeoNP, "LONG",  "Long. D:M:S", 0, 0, 360, 0, "%10.6m");
		 this.addProperty(ConnectSP);
	}
	
	
	/**
	 * Set the driver for communication with the telescope
	 * @param driver fully qualified name of driver class
	 */
	public void set_communication_driver(String driver) throws ClassNotFoundException {
		try {
			if (driver != null) com_driver = (communication_driver_interface) Class.forName(driver).newInstance();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
	}
	
	public void set_device(String sdevice) {
		device = sdevice;
	}
	
	/**
	 * Connect to the telescope
	 */
	@Override
	public void connect() throws IOException {
		if ((!connected) && (com_driver != null) && (device != null)) {
			com_driver.connect(device);
			connected=true;
			addProperty(EquatorialCoordsWNP);
		    addProperty(EquatorialCoordsRNP);
		    addProperty(AbortSlewSP);
		    addProperty(MovementNSSP);
		    addProperty(MovementWESP);
		    addProperty(TimeTP);
		    addProperty(UTCOffsetNP);
		    addProperty(GeoNP);
		    updateProperty(ConnectSP);
		} else {
			if (com_driver == null) throw new IOException("Telescope: Communication Driver not set");
			if (device == null) throw new IOException("Telescope: Device not set");
		}
	}
	
	/**
	 * Are we connected?
	 * @return
	 */
	@Override
	public boolean isConnected() {
		return connected;
	}
	
	/**
	 * Disconnect from telescope
	 */
	@Override
	public void disconnect() {
		if ((connected) && (com_driver != null)) {
			com_driver.disconnect();
			connected=false;
			removeProperty(EquatorialCoordsWNP);
			removeProperty(EquatorialCoordsRNP);
			removeProperty(AbortSlewSP);
			removeProperty(MovementNSSP);
			removeProperty(MovementWESP);
			removeProperty(TimeTP);
			removeProperty(UTCOffsetNP);
			removeProperty(GeoNP);
		}
	}


	/* (non-Javadoc)
	 * @see laazotea.indi.driver.INDIDriver#getName()
	 */
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}


	/* (non-Javadoc)
	 * @see laazotea.indi.driver.INDIDriver#processNewTextValue(laazotea.indi.driver.INDITextProperty, java.util.Date, laazotea.indi.driver.INDITextElementAndValue[])
	 */
	@Override
	public void processNewTextValue(INDITextProperty property, Date timestamp,
			INDITextElementAndValue[] elementsAndValues) {
		// TODO Auto-generated method stub
		
	}


	/* (non-Javadoc)
	 * @see laazotea.indi.driver.INDIDriver#processNewSwitchValue(laazotea.indi.driver.INDISwitchProperty, java.util.Date, laazotea.indi.driver.INDISwitchElementAndValue[])
	 */
	@Override
	public void processNewSwitchValue(INDISwitchProperty property,
			Date timestamp, INDISwitchElementAndValue[] elementsAndValues) {
		// TODO Auto-generated method stub
		
	}


	/* (non-Javadoc)
	 * @see laazotea.indi.driver.INDIDriver#processNewNumberValue(laazotea.indi.driver.INDINumberProperty, java.util.Date, laazotea.indi.driver.INDINumberElementAndValue[])
	 */
	@Override
	public void processNewNumberValue(INDINumberProperty property,
			Date timestamp, INDINumberElementAndValue[] elementsAndValues) {
		// TODO Auto-generated method stub
		
	}


	/* (non-Javadoc)
	 * @see laazotea.indi.driver.INDIDriver#processNewBLOBValue(laazotea.indi.driver.INDIBLOBProperty, java.util.Date, laazotea.indi.driver.INDIBLOBElementAndValue[])
	 */
	@Override
	public void processNewBLOBValue(INDIBLOBProperty property, Date timestamp,
			INDIBLOBElementAndValue[] elementsAndValues) {
		// TODO Auto-generated method stub
		
	}

}
