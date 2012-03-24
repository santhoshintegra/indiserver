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
 * @author atuschen75@gmail.com
 *
 */
public class lx200basic extends telescope implements device_driver_interface {
	
	private final String DriverName	= "LX200basic";
	public final int majorVersion = 0;
	public final int minorVersion = 0;
	public final int buildVersion = 93; 
	protected final static int LX200_TRACK	= 0;
	protected final static int LX200_SYNC	= 1;
	
	/* INDI Properties */
	
	/**********************************************************************************************/
	/************************************ GROUP: Communication ************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Connection
	*********************************************/
	protected INDISwitchProperty ConnectSP;
	protected INDISwitchElement ConnectS;
	protected INDISwitchElement DisconnectS;

	/********************************************
	 Property: Telescope Alignment Mode
	*********************************************/
	protected INDISwitchProperty AlignmentSP;
	protected INDISwitchElement PolarS;
	protected INDISwitchElement AltAzS;
	protected INDISwitchElement LandS;
	
	/**********************************************************************************************/
	/************************************ GROUP: Main Control *************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Equatorial Coordinates JNow
	 Perm: Transient WO.
	 Timeout: 120 seconds.
	*********************************************/
	protected INDINumberProperty EquatorialCoordsWNP;
	protected INDINumberElement RAWN;
	protected INDINumberElement DECWN;
	
	/********************************************
	 Property: Equatorial Coordinates JNow
	 Perm: RO
	*********************************************/
	protected INDINumberProperty EquatorialCoordsRNP;
	protected INDINumberElement RARN;
	protected INDINumberElement DECRN;
	
	/********************************************
	 Property: On Coord Set
	 Description: This property decides what happens
	             when we receive a new equatorial coord
	             value. We either track, or sync
		     to the new coordinates.
	*********************************************/
	protected INDISwitchProperty OnCoordSetSP;
	protected INDISwitchElement SlewS;
	protected INDISwitchElement SyncS;
	
	/********************************************
	 Property: Abort telescope motion
	*********************************************/
	protected INDISwitchProperty AbortSlewSP;
	protected INDISwitchElement AbortSlewS;
	
	/**********************************************************************************************/
	/************************************** GROUP: Motion *****************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Slew Speed
	*********************************************/
	protected INDISwitchProperty SlewModeSP;
	protected INDISwitchElement MaxS;
	protected INDISwitchElement FindS;
	protected INDISwitchElement CenteringS;
	protected INDISwitchElement GuideS;
	
	/********************************************
	 Property: Tracking Mode
	*********************************************/
	protected INDISwitchProperty TrackModeSP;
	protected INDISwitchElement DefaultModeS;
	protected INDISwitchElement LunarModeS;
	protected INDISwitchElement ManualModeS;
	
	/********************************************
	 Property: Tracking Frequency
	*********************************************/
	protected INDINumberProperty TrackFreqNP;
	protected INDINumberElement TrackFreqN;
	
	/********************************************
	 Property: Movement (Arrow keys on handset). North/South
	*********************************************/
	protected INDISwitchProperty MovementNSSP;
	protected INDISwitchElement MoveNorthS;
	protected INDISwitchElement MoveSouthS;
	
	/********************************************
	 Property: Movement (Arrow keys on handset). West/East
	*********************************************/
	protected INDISwitchProperty MovementWESP;
	protected INDISwitchElement MoveWestS;
	protected INDISwitchElement MoveEastS;

	/********************************************
	 Property: Timed Guide movement. North/South
	*********************************************/
	protected INDINumberProperty GuideNSNP;
	protected INDINumberElement GuideNorthN;
	protected INDINumberElement GuideSouthN;
	
	/********************************************
	 Property: Timed Guide movement. West/East
	*********************************************/
	protected INDINumberProperty GuideWENP;
	protected INDINumberElement GuideWestN;
	protected INDINumberElement GuideEastN;
	
	/********************************************
	 Property: Slew Accuracy
	 Desciption: How close the scope have to be with
		     respect to the requested coords for 
		     the tracking operation to be successull
		     i.e. returns OK
	*********************************************/
	protected INDINumberProperty SlewAccuracyNP;
	protected INDINumberElement SlewAccuracyRAN;
	protected INDINumberElement SlewAccuracyDECN;
	
	/********************************************
	 Property: Use pulse-guide commands
	 Desciption: Set to on if this mount can support
	             pulse guide commands.  There appears to
	             be no way to query this information from
	             the mount
	*********************************************/
	protected INDISwitchProperty UsePulseCommandSP;
	protected INDISwitchElement UsePulseCommandOnS;
	protected INDISwitchElement UsePulseCommandOffS;

	/**********************************************************************************************/
	/************************************** GROUP: Focus ******************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Focus Direction
	*********************************************/
	protected INDISwitchProperty FocusMotionSP;
	protected INDISwitchElement FocusInS;
	protected INDISwitchElement FocusOutS;

	/********************************************
	 Property: Focus Timer
	*********************************************/
	protected INDINumberProperty FocusTimerNP;
	protected INDINumberElement FocusTimerN;
	
	/********************************************
	 Property: Focus Mode
	*********************************************/
	protected INDISwitchProperty FocusModesSP;
	protected INDISwitchElement FocusHaltS;
	protected INDISwitchElement FocusSlowS;
	protected INDISwitchElement FocusFastS;

	/**********************************************************************************************/
	/*********************************** GROUP: Date & Time ***************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: UTC Time
	*********************************************/
	protected INDITextProperty TimeTP;
	protected INDITextElement TimeT;

	/********************************************
	 Property: DST Corrected UTC Offfset
	*********************************************/
	protected INDINumberProperty UTCOffsetNP;
	protected INDINumberElement UTCOffsetN;

	/********************************************
	 Property: Sidereal Time
	*********************************************/
	protected INDINumberProperty SDTimeNP;
	protected INDINumberElement SDTimeN;

	/**********************************************************************************************/
	/************************************* GROUP: Sites *******************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Site Management
	*********************************************/
	protected INDISwitchProperty SitesSP;
	protected INDISwitchElement Sites1S;
	protected INDISwitchElement Sites2S;
	protected INDISwitchElement Sites3S;
	protected INDISwitchElement Sites4S;

	/********************************************
	 Property: Site Name
	*********************************************/
	protected INDITextProperty SiteNameTP;
	protected INDITextElement SiteNameT;

	/********************************************
	 Property: Geographical Location
	*********************************************/
	protected INDINumberProperty GeoNP;
	protected INDINumberElement GeoLatN;
	protected INDINumberElement GeoLongN;
	protected INDINumberElement GeoHeightN;

	/*****************************************************************************************************/
	/**************************************** END PROPERTIES *********************************************/
	/*****************************************************************************************************/

	/**
	 * Constructor with input and outputstream for indi-xml-messages.
	 * TODO: extend with com_driver and device interface string
	 */
	public lx200basic(InputStream inputStream, OutputStream outputStream) {
		super(inputStream, outputStream);

	    addConnectionProperty();
	
	    /*
		 * INDI Properties 
		 * For compatibility reasons names, labels and settings of elements/properties are
		 * the same as in lx200generic.cpp from original indilib. 
		 */
	    
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
		EquatorialCoordsWNP.addElement(RAWN);
		EquatorialCoordsWNP.addElemant(DECWN);
		addProperty(EquatorialCoordsWNP);    
		
		RARN = new INDINumberElement("RA", "RA  H:M:S", 0, 0, 24, 0, "%10.6m");
		DECRN = new INDINumberElement("DEC", "Dec D:M:S", 0, -90, 90, 0, "%10.6m");
		EquatorialCoordsRNP = new INDINumberProperty(this, "EQUATORIAL_EOD_COORD_REQUEST", "Equatorial JNow", BASIC_GROUP, PropertyStates.IDLE, PropertyPermissions.RO, 120);
		EquatorialCoordsRNP.addElement(RARN);
		EquatorialCoordsRNP.addElement(DECRN);
		addProperty(EquatorialCoordsRNP);
		
		SlewS = new INDISwitchElement("SLEW", "Slew", SwitchStatus.ON);
		SyncS = new INDISwitchElement("SYNC", "Sync", SwitchStatus.OFF);
		OnCoordSetSP = new INDISwitchProperty(this, "ON_COORD_SET", "On Set", BASIC_GROUP,PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
		OnCoordSetSP.addElement(SlewS);
		OnCoordSetSP.addElement(SyncS);
		addProperty(OnCoordSetSP);
		
		AbortSlewS = new INDISwitchElement("ABORT", "Abort", SwitchStatus.OFF);
		AbortSlewSP = new INDISwitchProperty(this, "TELESCOPE_ABORT_MOTION", "Abort Slew", BASIC_GROUP,PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
		AbortSlewSP.addElement(AbortSlewS);
		addProperty(AbortSlewSP);
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
	 * INDI Driver methods
	 * @see laazotea.indi.driver.INDIDriver
	 */

	@Override
	public String getName() {
		return DriverName;
	}

	@Override
	public void processNewTextValue(INDITextProperty property, Date timestamp,
			INDITextElementAndValue[] elementsAndValues) {
	}

	@Override
	public void processNewSwitchValue(INDISwitchProperty property,
			Date timestamp, INDISwitchElementAndValue[] elementsAndValues) {
	}

	@Override
	public void processNewNumberValue(INDINumberProperty property,
			Date timestamp, INDINumberElementAndValue[] elementsAndValues) {
	}

	@Override
	public void processNewBLOBValue(INDIBLOBProperty property, Date timestamp,
			INDIBLOBElementAndValue[] elementsAndValues) {
	}

	
	/*
	 * Auxillary functions (LX200 specific)
	 */
	
	/**
	 * Get a converted sexagesimal value from the device 
	 * @param command
	 * @return double
	 */
	protected double getCommandSexa(String command){
		return 0;
	}
	
	/**
	 * Get an integer from the device 
	 * @param command
	 * @return integer 
	 */
	protected int getCommandInt(String command){
		return 0;
	}
	
	/**
	 * Get a string from the device
	 * @param command 
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
	
	

}
