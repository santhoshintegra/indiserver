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

import laazotea.indi.INDIDateFormat;
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
import de.hallenbeck.indiserver.communication_drivers.communication_driver;


/**
 * Abstract telescope-class with basic functions and basic INDI interface
 * 
 * @author atuschen
 *
 */
public abstract class telescope extends INDIDriver {
	
	protected final static String COMM_GROUP = "Communication";
	protected final static String BASIC_GROUP = "Main Control";
	protected final static String MOTION_GROUP	= "Motion Control";
	protected final static String DATETIME_GROUP = "Date/Time";
	protected final static String SITE_GROUP = "Site Management";
	
	protected static INDISexagesimalFormatter sexa = new INDISexagesimalFormatter("%10.6m");
	protected static communication_driver com_driver=null;
	protected static String propertyUpdateInfo = null;
	private static String device=null;
	private static boolean connected=false;

	/* Only the very basic INDI-Properties, that every telescope should have */
	
	/**********************************************************************************************/
	/************************************ GROUP: Communication ************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Connection
	*********************************************/
	protected INDISwitchProperty ConnectSP = new INDISwitchProperty(this, "CONNECTION", "Connection", COMM_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);			// suffix SP = SwitchProperty
	protected INDISwitchElement ConnectS = new INDISwitchElement(ConnectSP, "CONNECT" , "Connect" , SwitchStatus.OFF);			
	protected INDISwitchElement DisconnectS = new INDISwitchElement(ConnectSP, "DISCONNECT" , "Disconnect" , SwitchStatus.ON);		
	
	/**********************************************************************************************/
	/************************************ GROUP: Main Control *************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Equatorial Coordinates JNow
	 Perm: Transient WO.
	 Timeout: 120 seconds.
	*********************************************/
	protected INDINumberProperty EquatorialCoordsWNP = new INDINumberProperty(this, "EQUATORIAL_EOD_COORD_REQUEST", "Equatorial JNow", BASIC_GROUP, PropertyStates.IDLE, PropertyPermissions.WO, 120);	// suffix NP = NumberProperty
	protected INDINumberElement RAWN = new INDINumberElement(EquatorialCoordsWNP, "RA", "RA  H:M:S", 0, 0, 24, 0, "%10.6m");
	protected INDINumberElement DECWN = new INDINumberElement(EquatorialCoordsWNP, "DEC", "Dec D:M:S", 0, -90, 90, 0, "%10.6m");
	
	/********************************************
	 Property: Equatorial Coordinates JNow
	 Perm: RO
	*********************************************/
	protected INDINumberProperty EquatorialCoordsRNP = new INDINumberProperty(this, "EQUATORIAL_EOD_COORD", "Equatorial JNow", BASIC_GROUP, PropertyStates.IDLE, PropertyPermissions.RO, 120);
	protected INDINumberElement RARN = new INDINumberElement(EquatorialCoordsRNP, "RA", "RA  H:M:S", 0, 0, 24, 0, "%10.6m");
	protected INDINumberElement DECRN = new INDINumberElement(EquatorialCoordsRNP, "DEC", "Dec D:M:S", 0, -90, 90, 0, "%10.6m");
	
	/********************************************
	 Property: Abort telescope motion
	*********************************************/
	protected INDISwitchProperty AbortSlewSP = new INDISwitchProperty(this, "TELESCOPE_ABORT_MOTION", "Abort Slew", BASIC_GROUP,PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
	protected INDISwitchElement AbortSlewS = new INDISwitchElement(AbortSlewSP, "ABORT", "Abort", SwitchStatus.OFF);
	
	/**********************************************************************************************/
	/************************************** GROUP: Motion *****************************************/
	/**********************************************************************************************/
	
	/********************************************
	 Property: Movement (Arrow keys on handset). North/South
	*********************************************/
	protected INDISwitchProperty MovementNSSP = new INDISwitchProperty(this,"TELESCOPE_MOTION_NS", "North/South", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
	protected INDISwitchElement MoveNorthS = new INDISwitchElement(MovementNSSP, "MOTION_NORTH", "North", SwitchStatus.OFF);
	protected INDISwitchElement MoveSouthS = new INDISwitchElement(MovementNSSP, "MOTION_SOUTH", "South", SwitchStatus.OFF);
	
	/********************************************
	 Property: Movement (Arrow keys on handset). West/East
	*********************************************/
	protected INDISwitchProperty MovementWESP = new INDISwitchProperty(this,"TELESCOPE_MOTION_WE", "West/East", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
	protected INDISwitchElement MoveWestS = new INDISwitchElement(MovementWESP, "MOTION_WEST", "West", SwitchStatus.OFF);
	protected INDISwitchElement MoveEastS = new INDISwitchElement(MovementWESP, "MOTION_EAST", "East", SwitchStatus.OFF);
	
	/**********************************************************************************************/
	/*********************************** GROUP: Date & Time ***************************************/
	/**********************************************************************************************/
	
	/********************************************
	 Property: UTC Time
	*********************************************/
	protected INDITextProperty TimeTP = new INDITextProperty(this,  "TIME_UTC", "UTC Time", DATETIME_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
	protected INDITextElement TimeT = new INDITextElement(TimeTP, "UTC", "UTC", "0");

	/********************************************
	 Property: DST Corrected UTC Offfset
	*********************************************/
	protected INDINumberProperty UTCOffsetNP = new INDINumberProperty(this, "TIME_UTC_OFFSET", "UTC Offset", DATETIME_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
	protected INDINumberElement UTCOffsetN = new INDINumberElement(UTCOffsetNP, "OFFSET", "Offset", 0, -12, 12, 0.5, "%2.1g");

	/**********************************************************************************************/
	/************************************* GROUP: Sites *******************************************/
	/**********************************************************************************************/
	
	/********************************************
	 Property: Geographical Location
	*********************************************/
	protected INDINumberProperty GeoNP = new INDINumberProperty(this,"GEOGRAPHIC_COORD", "Geographic Location", SITE_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
	protected INDINumberElement GeoLatN = new INDINumberElement(GeoNP, "LAT",  "Lat.  D:M:S +N", 0, -90, 90, 0, "%10.6m");
	protected INDINumberElement GeoLongN = new INDINumberElement(GeoNP, "LONG",  "Long. D:M:S", 0, 0, 360, 0, "%10.6m");
	
	/*****************************************************************************************************/
	/**************************************** END PROPERTIES *********************************************/
	/*****************************************************************************************************/


	/**
	 * Constructor
	 * @param in
	 * @param out
	 */
	protected telescope(InputStream in, OutputStream out, String driver, String device) {
		super(in, out);
		try {
			setCommunicationDriver(driver);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setCommunicationDevice(device);
		this.addProperty(ConnectSP);
		updateProperty(ConnectSP,"INDI4Java Driver "+getName()+ "started");

	}
	
	
	/**
	 * Set the driver for communication with the telescope
	 * @param driver fully qualified name of driver class
	 */
	public void setCommunicationDriver(String driver) throws ClassNotFoundException {
		try {
			if (driver != null) com_driver = (communication_driver) Class.forName(driver).newInstance();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Set com device/interface
	 */
	public void setCommunicationDevice(String sdevice) {
		device = sdevice;
	}
	
	/**
	 * Connect to the telescope
	 */
	public boolean connect() {
		boolean ret=false;
		if ((!connected) && (com_driver != null) && (device != null)) {

			try {
				com_driver.connect(device,3000);
				connected=true;
				ConnectS.setValue(SwitchStatus.ON);
				ConnectSP.setState(PropertyStates.OK);
				updateProperty(ConnectSP,"Connected to "+device);
				onConnect();
				ret=true;
			} catch (IOException e) {
				connected=false;
				DisconnectS.setValue(SwitchStatus.ON);
				ConnectSP.setState(PropertyStates.ALERT);
				updateProperty(ConnectSP,"Error connecting "+device+" "+e.getMessage());
			}
			
		}	
		return ret;

	}
	
	/**
	 * Disconnect from telescope
	 */
	public boolean disconnect() {
		if ((connected) && (com_driver != null)) {
			com_driver.disconnect();
			connected=false;
			onDisconnect();
			DisconnectS.setValue(SwitchStatus.ON);
			ConnectSP.setState(PropertyStates.IDLE);
			updateProperty(ConnectSP,"Disconnected from "+device);
	
		}
		return true;
	}


	/* (non-Javadoc)
	 * @see laazotea.indi.driver.INDIDriver#processNewTextValue(laazotea.indi.driver.INDITextProperty, java.util.Date, laazotea.indi.driver.INDITextElementAndValue[])
	 */
	@Override
	public void processNewTextValue(INDITextProperty property, Date timestamp,
			INDITextElementAndValue[] elementsAndValues) {
		
		boolean ret=false;
		
		/**
		 * UTC Time Property
		 */
		if (property==TimeTP) {
			Date date = INDIDateFormat.parseTimestamp(elementsAndValues[0].getValue());
			ret = setDateTime(date);
			if (ret) property.setState(PropertyStates.OK);
			else property.setState(PropertyStates.ALERT); 
			updateProperty(property, propertyUpdateInfo);
		}
		
		
		
	}


	/* (non-Javadoc)
	 * @see laazotea.indi.driver.INDIDriver#processNewSwitchValue(laazotea.indi.driver.INDISwitchProperty, java.util.Date, laazotea.indi.driver.INDISwitchElementAndValue[])
	 */
	@Override
	public void processNewSwitchValue(INDISwitchProperty property,
			Date timestamp, INDISwitchElementAndValue[] elementsAndValues) {
	
		boolean ret = false;
		// Get the Element
		INDISwitchElement elem = elementsAndValues[0].getElement();
	
		/**
		 * Connect Property always available
		 */
		if (property==ConnectSP) {
			if (elem == DisconnectS) ret = disconnect();
			if (elem == ConnectS) ret = connect();
			if (ret) property.setState(PropertyStates.OK);
			else property.setState(PropertyStates.ALERT); 
			updateProperty(property, propertyUpdateInfo);
		}
	
		/**
		 * Abort all current slewing
		 */
		if (property==AbortSlewSP) {
			ret = onAbortSlew();
			if (ret) property.setState(PropertyStates.OK);
			else property.setState(PropertyStates.ALERT); 
			updateProperty(property, propertyUpdateInfo);
		}
	
		/**
		 * Move North/South
		 */
		if (property==MovementNSSP) {
			if (elem == MoveNorthS) ret = onMovementNS('N');
			if (elem == MoveSouthS) ret = onMovementNS('S');
			if (ret) property.setState(PropertyStates.OK);
			else property.setState(PropertyStates.ALERT); 
			updateProperty(property, propertyUpdateInfo);
		}
	
		/**
		 * Move West/East
		 */
		if (property==MovementWESP) {
			if (elem == MoveWestS) ret = onMovementNS('W');
			if (elem == MoveEastS) ret = onMovementNS('E');
			if (ret) property.setState(PropertyStates.OK);
			else property.setState(PropertyStates.ALERT); 
			updateProperty(property, propertyUpdateInfo);
		}
		
		propertyUpdateInfo = null;
	
	}


	/* (non-Javadoc)
	 * @see laazotea.indi.driver.INDIDriver#processNewNumberValue(laazotea.indi.driver.INDINumberProperty, java.util.Date, laazotea.indi.driver.INDINumberElementAndValue[])
	 */
	@Override
	public void processNewNumberValue(INDINumberProperty property,
			Date timestamp, INDINumberElementAndValue[] elementsAndValues) {
		
		boolean ret=false;
		
		/**
		 * UTC-Offset Property
		 */
		if (property==UTCOffsetNP) {
			if (elementsAndValues.length>0) ret = setUTCOffset(elementsAndValues[0].getValue());
			if (!ret) propertyUpdateInfo="Error setting new UTC offset";
			if (ret) property.setState(PropertyStates.OK); else property.setState(PropertyStates.ALERT);
			updateProperty(property, propertyUpdateInfo);
		}
	
		/**
		 * Geolocation Property
		 */
		if (property==GeoNP) {
			ret = true;
			for (int i=0; i < elementsAndValues.length; i++) {
				if (elementsAndValues[i].getElement() ==  GeoLatN) ret = ret && setLatitude(elementsAndValues[i].getValue());
				if (elementsAndValues[i].getElement() == GeoLongN) ret = ret && setLongitude(elementsAndValues[i].getValue());
			}
			if (!ret) propertyUpdateInfo="Error setting new geolocation";
			if (ret) property.setState(PropertyStates.OK); else property.setState(PropertyStates.ALERT);
			updateProperty(property, propertyUpdateInfo);
		}
	
		/**
		 * New Equatorial Coords
		 */
		if (property == EquatorialCoordsWNP) {
			ret = true;
			for (int i=0; i < elementsAndValues.length; i++) {
				if (elementsAndValues[i].getElement() ==  RAWN) ret = ret && setTargetRA(elementsAndValues[i].getValue()); 
				if (elementsAndValues[i].getElement() == DECWN) ret = ret && setTargetDEC(elementsAndValues[i].getValue());
			}
			if (ret) {
				getTargetCoords();
				ret = onNewEquatorialCoords(); 
			} else propertyUpdateInfo="Error setting new target coords";
			if (ret) property.setState(PropertyStates.OK); else property.setState(PropertyStates.ALERT);
			updateProperty(property, propertyUpdateInfo);
		}
		
	}


	/* (non-Javadoc)
	 * @see laazotea.indi.driver.INDIDriver#processNewBLOBValue(laazotea.indi.driver.INDIBLOBProperty, java.util.Date, laazotea.indi.driver.INDIBLOBElementAndValue[])
	 */
	@Override
	public void processNewBLOBValue(INDIBLOBProperty property, Date timestamp,
			INDIBLOBElementAndValue[] elementsAndValues) {
		
		boolean ret=false;
		
		if (ret) property.setState(PropertyStates.OK);
		else property.setState(PropertyStates.ALERT);
		updateProperty(property);
		propertyUpdateInfo = null;
		
	}


	/**
	 * Called from connect(), after Connection has been established
	 */
	protected void onConnect() {
		this.addProperty(EquatorialCoordsWNP);
	    this.addProperty(EquatorialCoordsRNP);
	    this.addProperty(AbortSlewSP);
	    this.addProperty(MovementNSSP);
	    this.addProperty(MovementWESP);
	    this.addProperty(TimeTP);
	    this.addProperty(UTCOffsetNP);
	    this.addProperty(GeoNP);
	}
	
	/**
	 * Called from disconnect(), after Connection is down
	 */
	protected void onDisconnect() {
		this.removeProperty(EquatorialCoordsWNP);
		this.removeProperty(EquatorialCoordsRNP);
		this.removeProperty(AbortSlewSP);
		this.removeProperty(MovementNSSP);
		this.removeProperty(MovementWESP);
		this.removeProperty(TimeTP);
		this.removeProperty(UTCOffsetNP);
		this.removeProperty(GeoNP);
	}
	
	/**
	 * Called when new equatorial target coords have been succesfully set
	 * @return true if OK, false on Error
	 */
	protected boolean onNewEquatorialCoords() {
		return false;
	}


	/**
	 * Called when Abort button is clicked
	 * @return true if OK, false on Error
	 */
	protected boolean onAbortSlew() {
		return false;
	}


	/**
	 * Called when Move North/South buttons are clicked
	 * @param direction N=North, S=South
	 * @return true if OK, false on Error
	 */
	protected boolean onMovementNS(char direction) {
		return false;
	}


	/**
	 * Called when Move West/East buttons are clicked
	 * @param direction W=West, E=East
	 * @return true if OK, false on Error
	 */
	protected boolean onMovementWE(char direction) {
		return false;
	}


	/**
	 * Called when a new Date/Time is set by the client
	 * @param date
	 * @return true if OK, false on Error
	 */
	protected boolean setDateTime(Date date) {
		return false;
	}
	
	/**
	 * Called when a new UTC Offset value is set by the client
	 * @param offset
	 * @return true if OK, false on Error
	 */
	protected boolean setUTCOffset(double offset) {
		return false;
	}
	
	/**
	 * Called when client sets a new Geolocation Latitude value
	 * @param latitude
	 * @return true if OK, false on Error
	 */
	protected boolean setLatitude(double latitude) {
		return false;
	}
	
	/**
	 * Called when client sets a new Geolocation Longitude value
	 * @param longitude
	 * @return true if OK, false on Error
	 */
	protected boolean setLongitude(double longitude) {
		return false;
	}
	
	/**
	 * Called when client sets a new Target RA value
	 * @param RA
	 * @return true if OK, false on Error
	 */
	protected boolean setTargetRA(double RA) {
		return false;
	}
	
	/**
	 * Called when client sets a new Target declination
	 * @param DEC
	 * @return true if OK, false on Error
	 */
	protected boolean setTargetDEC(double DEC) {
		return false;
	}
	
	
	/**
	 * Get the Driver name
	 */
	@Override
	public String getName() {
		return null;
	}
	
	/**
	 * Called to update the Target Coords from the Telescope
	 */
	protected void getTargetCoords() {
		
	}

}
