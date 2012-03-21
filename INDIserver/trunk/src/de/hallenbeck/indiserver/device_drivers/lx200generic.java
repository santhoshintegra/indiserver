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
import laazotea.indi.driver.INDINumberElement;
import laazotea.indi.driver.INDINumberElementAndValue;
import laazotea.indi.driver.INDINumberProperty;
import laazotea.indi.driver.INDISwitchElement;
import laazotea.indi.driver.INDISwitchElementAndValue;
import laazotea.indi.driver.INDISwitchProperty;
import laazotea.indi.driver.INDITextElement;
import laazotea.indi.driver.INDITextElementAndValue;
import laazotea.indi.driver.INDITextProperty;

import android.os.Handler;

/**
 * Driver for LX200 compatible telescopes, only covering the basic commandset.
 * Extended LX200-protocols should be derived from this class. 
 * There are some errors in the official Meade LX200 protocol sheet.
 * i.e. some answer-strings are localized (command ":P#" gives "HOCH PRAEZISION" or 
 * "NIEDER PRAEZISION" on german Firmware 42Gg)
 * 
 * This class is based on my own tests with Autostar #497 Firmware 43Eg (english)
 * No guarantee that this will work with the newer Autostar #497-EP models or 
 * any other firmware version than 43Eg!
 *   
 * @author atuschen
 *
 */
public class lx200generic extends telescope implements device_driver_interface {
	
	protected final static int LX200_TRACK	= 0;
	protected final static int LX200_SYNC	= 1;
	
	/*
	 * INDI Properties 
	 */

	/**********************************************************************************************/
	/************************************ GROUP: Communication ************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Connection
	*********************************************/
	private INDISwitchProperty ConnectSP;
	private INDISwitchElement ConnectS;
	private INDISwitchElement DisconnectS;

	/********************************************
	 Property: Telescope Alignment Mode
	*********************************************/
	private INDISwitchProperty AlignmentSP;
	private INDISwitchElement PolarS;
	private INDISwitchElement AltAzS;
	private INDISwitchElement LandS;
	
	/**********************************************************************************************/
	/************************************ GROUP: Main Control *************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Equatorial Coordinates JNow
	 Perm: Transient WO.
	 Timeout: 120 seconds.
	*********************************************/
	private INDINumberProperty EquatorialCoordsWNP;
	private INDINumberElement RAWN;
	private INDINumberElement DECWN;
	
	/********************************************
	 Property: Equatorial Coordinates JNow
	 Perm: RO
	*********************************************/
	private INDINumberProperty EquatorialCoordsRNP;
	private INDINumberElement RARN;
	private INDINumberElement DECRN;
	
	/********************************************
	 Property: On Coord Set
	 Description: This property decides what happens
	             when we receive a new equatorial coord
	             value. We either track, or sync
		     to the new coordinates.
	*********************************************/
	private INDISwitchProperty OnCoordSetSP;
	private INDISwitchElement OnCoordSetS;
	
	/********************************************
	 Property: Abort telescope motion
	*********************************************/
	private INDISwitchProperty AbortSlewSP;
	private INDISwitchElement AbortSlewS;
	
	/**********************************************************************************************/
	/************************************** GROUP: Motion *****************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Slew Speed
	*********************************************/
	private INDISwitchProperty SlewModeSP;
	private INDISwitchElement SlewModeS;
	
	/********************************************
	 Property: Tracking Mode
	*********************************************/
	private INDISwitchProperty TrackModeSP;
	private INDISwitchElement TrackModeS;
	
	/********************************************
	 Property: Tracking Frequency
	*********************************************/
	private INDINumberProperty TrackFreqNP;
	private INDINumberElement TrackFreqN;
	
	/********************************************
	 Property: Movement (Arrow keys on handset). North/South
	*********************************************/
	private INDISwitchProperty MovementNSSP;
	private INDISwitchElement MovementS;
	
	/********************************************
	 Property: Movement (Arrow keys on handset). West/East
	*********************************************/
	private INDISwitchProperty MovementWESP;
	private INDISwitchElement MovementWES;

	/********************************************
	 Property: Timed Guide movement. North/South
	*********************************************/
	private INDINumberProperty GuideNSNP;
	private INDINumberElement GuideNSN;
	
	/********************************************
	 Property: Timed Guide movement. West/East
	*********************************************/
	private INDINumberProperty GuideWENP;
	private INDINumberElement GuideWEN;
	
	/********************************************
	 Property: Slew Accuracy
	 Desciption: How close the scope have to be with
		     respect to the requested coords for 
		     the tracking operation to be successull
		     i.e. returns OK
	*********************************************/
	private INDINumberProperty SlewAccuracyNP;
	private INDINumberElement SlewAccuracyN;
	
	/********************************************
	 Property: Use pulse-guide commands
	 Desciption: Set to on if this mount can support
	             pulse guide commands.  There appears to
	             be no way to query this information from
	             the mount
	*********************************************/
	private INDISwitchProperty UsePulseCommandSP;
	private INDISwitchElement UsePulseCommandS;

	/**********************************************************************************************/
	/************************************** GROUP: Focus ******************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Focus Direction
	*********************************************/
	private INDISwitchProperty FocusMotionSP;
	private INDISwitchElement FocusMotionS;

	/********************************************
	 Property: Focus Timer
	*********************************************/
	private INDINumberProperty FocusTimerNP;
	private INDINumberElement FocusTimerN;
	
	/********************************************
	 Property: Focus Mode
	*********************************************/
	private INDISwitchProperty FocusModesSP;
	private INDISwitchElement FocusModesS;

	/**********************************************************************************************/
	/*********************************** GROUP: Date & Time ***************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: UTC Time
	*********************************************/
	private INDITextProperty TimeTP;
	private INDITextElement TimeT;

	/********************************************
	 Property: DST Corrected UTC Offfset
	*********************************************/
	private INDINumberProperty UTCOffsetNP;
	private INDINumberElement UTCOffsetN;

	/********************************************
	 Property: Sidereal Time
	*********************************************/
	private INDINumberProperty SDTimeNP;
	private INDINumberElement SDTimeN;

	/**********************************************************************************************/
	/************************************* GROUP: Sites *******************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Site Management
	*********************************************/
	private INDISwitchProperty SitesSP;
	private INDISwitchElement SitesS;

	/********************************************
	 Property: Site Name
	*********************************************/
	private INDITextProperty SiteNameTP;
	private INDITextElement SiteNameT;

	/********************************************
	 Property: Geographical Location
	*********************************************/
	private INDINumberProperty GeoNP;
	private INDINumberElement GeoN;

	/*****************************************************************************************************/
	/**************************************** END PROPERTIES *********************************************/
	/*****************************************************************************************************/

	/*
	 * Constructor with input and outputstream for indi-xml-messages.
	 * TODO: extend with com_driver and device interface string
	 */
	
	public lx200generic(InputStream inputStream, OutputStream outputStream) {
		super(inputStream, outputStream);

	    addConnectionProperty();
	    
	    ConnectS = new INDISwitchElement("CONNECT" , "Connect" , SwitchStatus.OFF);
	    DisconnectS = new INDISwitchElement("DISCONNECT" , "Disconnect" , SwitchStatus.ON);
	    ConnectSP = new INDISwitchProperty(this, "CONNECTION", "Connection", COMM_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
	    ConnectSP.addElement(ConnectS);
	    ConnectSP.addElement(DisconnectS);
	    addProperty(ConnectSP);
	    
	    PolarS = new INDISwitchElement("POLAR" , "Polar" , SwitchStatus.ON);
	    AltAzS = new INDISwitchElement("ALTAZ" , "AltAz" , SwitchStatus.OFF);
	    LandS = new INDISwitchElement("LAND" , "Land" , SwitchStatus.OFF);
	    AlignmentSP = new INDISwitchProperty(this, "ALIGNMENT", "Alignment", COMM_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
	    AlignmentSP.addElement(PolarS);
	    AlignmentSP.addElement(AltAzS);
	    AlignmentSP.addElement(LandS);
	    addProperty(AlignmentSP);
	    
	    RAWN = new INDINumberElement("RA", "RA  H:M:S", 0, 0, 24, 0, "%10.6m");
		DECWN = new INDINumberElement("DEC", "Dec D:M:S", 0, -90, 90, 0, "%10.6m");
		EquatorialCoordsWNP = new INDINumberProperty(this, "EQUATORIAL_EOD_COORD_REQUEST", "Equatorial JNow", BASIC_GROUP, PropertyStates.IDLE, PropertyPermissions.WO, 120);
		addProperty(EquatorialCoordsWNP);    
	    
	}

	/*
	 * Public interface methods 
	 */
	
	
	/**
	 * Connect to telescope 
	 * @param device: driver specific device address
	 */
	public void connect(String device) {
		// Set delay-before-read to 200ms 
		// After some testing I found this a reliable value 
		com_driver.set_delay(200); 
		super.connect(device);
	}

	/*
	 * Internal methods for LX200 and derived classes
	 */
	

	
	
	
	/*
	 * Auxillary functions (LX200 specific)
	 * 
	 */
	
	/**
	 * Get a converted sexagesimal value from the device 
	 * @param command
	 * @return double
	 */
	protected double getCommandSexa(String command){
		String tmpStr;
		double value;
		
		
		
	}
	
	/**
	 * Get an integer from the device 
	 * @param command
	 * @return integer 
	 */
	protected int getCommandInt(String command){
		
	}
	
	/**
	 * Get a string from the device
	 * @param command command string
	 * @return string 
	 */
	protected String getCommandString(String command) {
		String tmp=null;
		try {
			com_driver.sendCommand(command);
			tmp = com_driver.getAnswerString();
			tmp = tmp.substring(0, tmp.indexOf("#")-1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tmp;
	}
	
	/**
	 * Just send a command to the device 
	 * for some commands there is no return (i.e. movement)
	 * @param command
	 */
	protected void sendCommand(String command) {
		try {
			com_driver.sendCommand(command);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void processNewTextValue(INDITextProperty property, Date timestamp,
			INDITextElementAndValue[] elementsAndValues) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processNewSwitchValue(INDISwitchProperty property,
			Date timestamp, INDISwitchElementAndValue[] elementsAndValues) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processNewNumberValue(INDINumberProperty property,
			Date timestamp, INDINumberElementAndValue[] elementsAndValues) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processNewBLOBValue(INDIBLOBProperty property, Date timestamp,
			INDIBLOBElementAndValue[] elementsAndValues) {
		// TODO Auto-generated method stub
		
	}

}
