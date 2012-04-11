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
	protected static INDISexagesimalFormatter sexaGeo = new INDISexagesimalFormatter("%6.3m");
	protected static communication_driver com_driver=null;
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
	 * @param InputStream object
	 * @param OutputStream object
	 * @param Driver class
	 * @param Device to connect to
	 */
	protected telescope(InputStream in, OutputStream out, String Driver, String Device) {
		super(in, out);
		if (Driver != null)
			try {
				com_driver = (communication_driver) Class.forName(Driver).newInstance();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		device = Device;
		
		this.addProperty(ConnectSP);
		updateProperty(ConnectSP,"INDI4Java Driver "+getName()+ "started");

	}
	
	/**
	 * Connect to the telescope
	 */
	public void connect() {
		if ((!connected) && (com_driver != null) && (device != null)) {
			try {
				com_driver.connect(device,3000);
				connected=true;
				ConnectS.setValue(SwitchStatus.ON);
				ConnectSP.setState(PropertyStates.OK);
				updateProperty(ConnectSP,"Connected to "+device);
				this.addProperty(EquatorialCoordsWNP);
			    this.addProperty(EquatorialCoordsRNP);
			    this.addProperty(AbortSlewSP);
			    this.addProperty(MovementNSSP);
			    this.addProperty(MovementWESP);
			    this.addProperty(TimeTP);
			    this.addProperty(UTCOffsetNP);
			    this.addProperty(GeoNP);
				onConnect();
			} catch (IOException e) {
				connected=false;
				DisconnectS.setValue(SwitchStatus.ON);
				ConnectSP.setState(PropertyStates.ALERT);
				updateProperty(ConnectSP,"Error connecting "+device+" "+e.getMessage());
			}
			
		}	

	}
	
	/**
	 * Disconnect from telescope
	 */
	public void disconnect() {
		if ((connected) && (com_driver != null)) {
			com_driver.disconnect();
			connected=false;
			onDisconnect();
			this.removeProperty(EquatorialCoordsWNP);
			this.removeProperty(EquatorialCoordsRNP);
			this.removeProperty(AbortSlewSP);
			this.removeProperty(MovementNSSP);
			this.removeProperty(MovementWESP);
			this.removeProperty(TimeTP);
			this.removeProperty(UTCOffsetNP);
			this.removeProperty(GeoNP);
			DisconnectS.setValue(SwitchStatus.ON);
			ConnectSP.setState(PropertyStates.IDLE);
			updateProperty(ConnectSP,"Disconnected from "+device);
	
		}
		
	}


	@Override
	public void processNewTextValue(INDITextProperty property, Date timestamp,
			INDITextElementAndValue[] elementsAndValues) {
		
		/**
		 * UTC Time Property
		 */
		if (property==TimeTP) {
			if (setDateTime(INDIDateFormat.parseTimestamp(elementsAndValues[0].getValue()))) {
				getDateTime();
			} else {
				property.setState(PropertyStates.ALERT); 
				updateProperty(property, "Error setting new Date/Time");
			}
		}
	}

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
			if (elem == ConnectS) connect();
		}
	
		/**
		 * Abort all current slewing
		 */
		if (property==AbortSlewSP) {
			onAbortSlew();
			property.setState(PropertyStates.OK);
			updateProperty(property);
		}
	
		/**
		 * Move North/South
		 */
		if (property==MovementNSSP) {
			if (elem == MoveNorthS) onMovementNS('N');
			if (elem == MoveSouthS) onMovementNS('S');
			property.setState(PropertyStates.OK); 
			updateProperty(property);
		}
	
		/**
		 * Move West/East
		 */
		if (property==MovementWESP) {
			if (elem == MoveWestS) onMovementNS('W');
			if (elem == MoveEastS) onMovementNS('E');
			property.setState(PropertyStates.OK);
			updateProperty(property);
		}
		
	}

	@Override
	public void processNewNumberValue(INDINumberProperty property,
			Date timestamp, INDINumberElementAndValue[] elementsAndValues) {
		
		boolean ret=true;
		
		/**
		 * UTC-Offset Property
		 */
		if (property==UTCOffsetNP) {
			if ((elementsAndValues.length>0) && setUTCOffset(elementsAndValues[0].getValue())) {
				getUTCOffset();
			}
			else {
				property.setState(PropertyStates.ALERT);
				updateProperty(property, "Error setting new UTC offset");
			}
		}
	
		/**
		 * Geolocation Property
		 */
		if (property==GeoNP) {
			for (int i=0; i < elementsAndValues.length; i++) {
				if (elementsAndValues[i].getElement() ==  GeoLatN) ret = ret && setLatitude(elementsAndValues[i].getValue());
				if (elementsAndValues[i].getElement() == GeoLongN) ret = ret && setLongitude(elementsAndValues[i].getValue());
			}
			
			if (ret) {
				getGeolocation();
			}
			else {
				property.setState(PropertyStates.ALERT);
				updateProperty(property, "Error setting new geolocation");
			}
			
		}
	
		/**
		 * New Target Coords
		 */
		if (property == EquatorialCoordsWNP) {
			for (int i=0; i < elementsAndValues.length; i++) {
				if (elementsAndValues[i].getElement() ==  RAWN) ret = ret && setTargetRA(elementsAndValues[i].getValue()); 
				if (elementsAndValues[i].getElement() == DECWN) ret = ret && setTargetDEC(elementsAndValues[i].getValue());
			}
			
			if (ret) {
				onNewTargetCoords();
				getTargetCoords();
			}
			else { 
				property.setState(PropertyStates.ALERT);
				updateProperty(property, "Error setting new target coords");
			}
			
		}
		
	}


	@Override
	public void processNewBLOBValue(INDIBLOBProperty property, Date timestamp,
			INDIBLOBElementAndValue[] elementsAndValues) {
	}


	/**
	 * Called from connect(), after Connection has been established
	 */
	protected abstract void onConnect();
	
	
	/**
	 * Called from disconnect(), after Connection is down
	 */
	protected abstract void onDisconnect();
	
	
	/**
	 * Called when new equatorial target coords have been succesfully set
	 */
	protected abstract void onNewTargetCoords();


	/**
	 * Called when Abort button is clicked
	 */
	protected abstract void onAbortSlew();
	
	
	/**
	 * Called when Move North/South buttons are clicked
	 * @param direction N=North, S=South
	 */
	protected abstract void onMovementNS(char direction);


	/**
	 * Called when Move West/East buttons are clicked
	 * @param direction W=West, E=East
	 */
	protected abstract void onMovementWE(char direction);


	/**
	 * Called when a new Date/Time is set by the client
	 * @param date
	 * @return true if OK, false on Error
	 */
	protected abstract boolean setDateTime(Date date);
	
	/**
	 * Called when a new UTC Offset value is set by the client
	 * @param offset
	 * @return true if OK, false on Error
	 */
	protected abstract boolean setUTCOffset(double offset);
	
	/**
	 * Called when client sets a new Geolocation Latitude value
	 * @param latitude
	 * @return true if OK, false on Error
	 */
	protected abstract boolean setLatitude(double latitude);
	
	/**
	 * Called when client sets a new Geolocation Longitude value
	 * @param longitude
	 * @return true if OK, false on Error
	 */
	protected abstract boolean setLongitude(double longitude);
	
	/**
	 * Called when client sets a new Target RA value
	 * @param RA
	 * @return true if OK, false on Error
	 */
	protected abstract boolean setTargetRA(double RA);
	
	/**
	 * Called when client sets a new Target declination
	 * @param DEC
	 * @return true if OK, false on Error
	 */
	protected abstract boolean setTargetDEC(double DEC);
	
	/**
	 * Called to update the Target Coords from the Telescope
	 */
	protected abstract void getTargetCoords();
	
	/**
	 * Called to update the geolocation
	 */
	protected abstract void getGeolocation();
	
	protected abstract void getUTCOffset();
	
	protected abstract void getDateTime();
	
	/**
	 * Get the Driver name
	 */
	@Override
	public String getName() {
		return null;
	}
}
