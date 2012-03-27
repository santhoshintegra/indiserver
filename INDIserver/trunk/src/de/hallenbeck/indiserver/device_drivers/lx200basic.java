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
 */
public class lx200basic extends telescope implements device_driver_interface {
	
	private final String DriverName	= "LX200basic";
	private final int majorVersion = 0;
	private final int minorVersion = 1;	
	
	/* Meade LX200 Commands */
	
	protected final static String getAlignmentCmd = String.valueOf((char)6); //Get Alignment
	protected final static String getAlignmentStatusCmd = "#:GW#"; //Get Alignment Status
	protected final static String setAlignmentAltAzCmd = "#:AA#"; //Set Alignment to AltAz
	protected final static String setAlignmentPolarCmd = "#:AP#"; //Set Alignment to Polar
	protected final static String setAlignmentLandCmd = "#:AL#"; //Set Alignment to Land
	
	protected final static String getDateCmd = "#:GC#"; //Get Local Date
	protected final static String getTimeCmd = "#:GL#"; //Get Local Time
	protected final static String getUTCHoursCmd = "#:GG#"; //Get Hours to yield UTC from Local Time
	protected final static String setDateCmd = "#:SC%s#"; //Set Local Date to %s (MM/DD/YY)
	protected final static String setTimeCmd = "#:SL%s#"; //Set Local Time to %s (HH:MM:SS)
	protected final static String setUTCHoursCmd = "#:SG%s#"; //Set Hours to yield UTC from Local Time
	
	protected final static String getSite1NameCmd = "#:GM#"; //Get Name of Site 1
	protected final static String getSite2NameCmd = "#:GN#"; //Get Name of Site 2
	protected final static String getSite3NameCmd = "#:GO#"; //Get Name of Site 3
	protected final static String getSite4NameCmd = "#:GP#"; //Get Name of Site 4
	protected final static String getSiteLatCmd = "#:Gt#"; //Get Latitude of selected site
	protected final static String getSiteLongCmd = "#:Gg#"; //Get Longitude of selected site 
	protected final static String setSite1NameCmd = "#:SM%s#"; //Set Name of Site 1 to %s
	protected final static String setSite2NameCmd = "#:SN%s#"; //Set Name of Site 2 to %s
	protected final static String setSite3NameCmd = "#:SO%s#"; //Set Name of Site 3 to %s 
	protected final static String setSite4NameCmd = "#:SP%s#"; //Set Name of Site 4 to %s
	protected final static String setSiteLatCmd = "#:St%s#"; //Set Latitude of selected site to %s (sDD*MM) North=positive
	protected final static String setSiteLongCmd = "#:Sg%s#"; //Set Longitude of selected site to %s (sDDD*MM) West=positive 
	protected final static String setCurrentSiteCmd = "#:W%1d#;"; // Set current site to %1d (1..4) 
			
	protected final static String getCurrentRACmd = "#:GR#"; //Get current RA
	protected final static String getCurrentDECCmd = "#:GD#"; //Get current DEC
	
	/* INDI Properties */
	
	/**********************************************************************************************/
	/************************************ GROUP: Communication ************************************/
	/**********************************************************************************************/

	/********************************************
	 Property: Connection
	*********************************************/
	protected INDISwitchProperty ConnectSP;			// suffix SP = SwitchProperty
	protected INDISwitchElement ConnectS;			// suffix S = SwitchElement
	protected INDISwitchElement DisconnectS;		

	/**
	 * The device-interface property isn't implemented.
	 * It's up to the server-app to set right interface for the device, not the remote client(s).
	 *   
	 */
	
	
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
	protected INDINumberProperty EquatorialCoordsWNP;	// suffix NP = NumberProperty
	protected INDINumberElement RAWN;					// suffix N = NumberElement
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
	protected INDITextProperty TimeTP;				// suffix TP = TextProperty
	protected INDITextElement TimeT;				// suffix T = TextElement

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
	// Not used // protected INDINumberElement GeoHeightN;

	/*****************************************************************************************************/
	/**************************************** END PROPERTIES *********************************************/
	/*****************************************************************************************************/

	/**
	 * Constructor with input and outputstream for indi-xml-messages.
	 * TODO: extend with com_driver and device interface string
	 */
	public lx200basic(InputStream inputStream, OutputStream outputStream) {
		super(inputStream, outputStream);

	    /*
		 * INDI Properties 
		 * For compatibility reasons all names, labels and settings of elements/properties are
		 * the same as in lx200generic.cpp from the original indilib.
		 * TODO: localize labels with a string-ressource 
		 */
	    
	    /**********************************************************************************************/
		/************************************ GROUP: Communication ************************************/
		/**********************************************************************************************/

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
	    
	    /**********************************************************************************************/
		/************************************ GROUP: Main Control *************************************/
		/**********************************************************************************************/
	    
	    RAWN = new INDINumberElement("RA", "RA  H:M:S", 0, 0, 24, 0, "%10.6m");
		DECWN = new INDINumberElement("DEC", "Dec D:M:S", 0, -90, 90, 0, "%10.6m");
		EquatorialCoordsWNP = new INDINumberProperty(this, "EQUATORIAL_EOD_COORD_REQUEST", "Equatorial JNow", BASIC_GROUP, PropertyStates.IDLE, PropertyPermissions.WO, 120);
		EquatorialCoordsWNP.addElement(RAWN);
		EquatorialCoordsWNP.addElement(DECWN);
		addProperty(EquatorialCoordsWNP);    
		
		RARN = new INDINumberElement("RA", "RA  H:M:S", 0, 0, 24, 0, "%10.6m");
		DECRN = new INDINumberElement("DEC", "Dec D:M:S", 0, -90, 90, 0, "%10.6m");
		EquatorialCoordsRNP = new INDINumberProperty(this, "EQUATORIAL_EOD_COORD", "Equatorial JNow", BASIC_GROUP, PropertyStates.IDLE, PropertyPermissions.RO, 120);
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
		
		/**********************************************************************************************/
		/************************************** GROUP: Motion *****************************************/
		/**********************************************************************************************/
		
		MaxS = new INDISwitchElement("MAX", "Max", SwitchStatus.ON);
		FindS = new INDISwitchElement("FIND", "Find", SwitchStatus.OFF);
		CenteringS = new INDISwitchElement("CENTERING", "Centering", SwitchStatus.OFF);
		GuideS = new INDISwitchElement("GUIDE", "Guide", SwitchStatus.OFF);
		SlewModeSP = new INDISwitchProperty(this,"SLEW_RATE","Slew rate", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
		SlewModeSP.addElement(MaxS);
		SlewModeSP.addElement(FindS);
		SlewModeSP.addElement(CenteringS);
		SlewModeSP.addElement(GuideS);
		addProperty(SlewModeSP);
		
		DefaultModeS = new INDISwitchElement("DEFAULT", "Default", SwitchStatus.ON);
		LunarModeS = new INDISwitchElement("LUNAR", "Lunar", SwitchStatus.OFF);
		ManualModeS = new INDISwitchElement("MANUAL", "Manual", SwitchStatus.OFF);
		TrackModeSP = new INDISwitchProperty(this,"TRACKING_MODE", "Tracking Mode", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
		TrackModeSP.addElement(DefaultModeS);
		TrackModeSP.addElement(LunarModeS);
		TrackModeSP.addElement(ManualModeS);
		addProperty(TrackModeSP);
		
		TrackFreqN = new INDINumberElement("TRACK_FREQ", "Freq", 60.1, 56.4, 60.1, 0.1, "%g");
		TrackFreqNP = new INDINumberProperty(this,"TRACKING_FREQ", "Tracking Frequency", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
		TrackFreqNP.addElement(TrackFreqN);
		addProperty(TrackFreqNP);
		
		MoveNorthS = new INDISwitchElement("MOTION_NORTH", "North", SwitchStatus.OFF);
		MoveSouthS = new INDISwitchElement("MOTION_SOUTH", "South", SwitchStatus.OFF);
		MovementNSSP = new INDISwitchProperty(this,"TELESCOPE_MOTION_NS", "North/South", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
		MovementNSSP.addElement(MoveNorthS);
		MovementNSSP.addElement(MoveSouthS);
		addProperty(MovementNSSP);
		
		MoveWestS = new INDISwitchElement("MOTION_WEST", "West", SwitchStatus.OFF);
		MoveEastS = new INDISwitchElement("MOTION_EAST", "East", SwitchStatus.OFF);
		MovementWESP = new INDISwitchProperty(this,"TELESCOPE_MOTION_WE", "West/East", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
		MovementWESP.addElement(MoveWestS);
		MovementWESP.addElement(MoveEastS);
		addProperty(MovementWESP);
		
		GuideNorthN = new INDINumberElement("TIMED_GUIDE_N", "North (sec)", 0, 0, 10, 0.001, "%g");
		GuideSouthN = new INDINumberElement("TIMED_GUIDE_S", "South (sec)", 0, 0, 10, 0.001, "%g");
		GuideNSNP = new INDINumberProperty(this,"TELESCOPE_TIMED_GUIDE_NS", "Guide North/South", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
		GuideNSNP.addElement(GuideNorthN);
		GuideNSNP.addElement(GuideSouthN);
		addProperty(GuideNSNP);
		
		GuideWestN = new INDINumberElement("TIMED_GUIDE_W", "West (sec)", 0, 0, 10, 0.001, "%g");
		GuideEastN = new INDINumberElement("TIMED_GUIDE_E", "East (sec)", 0, 0, 10, 0.001, "%g");
		GuideWENP = new INDINumberProperty(this,"TELESCOPE_TIMED_GUIDE_WE", "Guide West/East", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
		GuideWENP.addElement(GuideWestN);
		GuideWENP.addElement(GuideEastN);
		addProperty(GuideWENP);
		
		SlewAccuracyRAN = new INDINumberElement("SLEW_RA",  "RA (arcmin)", 3, 0, 60, 1, "%g");
		SlewAccuracyDECN = new INDINumberElement("SLEW_DEC", "Dec (arcmin)", 3, 0, 60, 1, "%g");
		SlewAccuracyNP = new INDINumberProperty(this,"SLEW_ACCURACY", "Slew Accuracy", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
		SlewAccuracyNP.addElement(SlewAccuracyRAN);
		SlewAccuracyNP.addElement(SlewAccuracyDECN);
		addProperty(SlewAccuracyNP);
		
		UsePulseCommandOnS = new INDISwitchElement("PULSE_ON", "On", SwitchStatus.OFF);
		UsePulseCommandOffS = new INDISwitchElement("PULSE_OFF", "Off", SwitchStatus.ON);
		UsePulseCommandSP = new INDISwitchProperty(this,"USE_PULSE_CMD", "Use PulseCMd", MOTION_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
		UsePulseCommandSP.addElement(UsePulseCommandOnS);
		UsePulseCommandSP.addElement(UsePulseCommandOffS);
		addProperty(UsePulseCommandSP);
		
		/**********************************************************************************************/
		/************************************** GROUP: Focus ******************************************/
		/**********************************************************************************************/
		
		FocusInS = new INDISwitchElement("IN", "Focus in", SwitchStatus.OFF);
		FocusOutS = new INDISwitchElement("OUT", "Focus out", SwitchStatus.OFF);
		FocusMotionSP = new INDISwitchProperty(this,"FOCUS_MOTION", "Motion", FOCUS_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
		
		FocusTimerN = new INDINumberElement("TIMER", "Timer (ms)", 50, 0, 10000, 1000, "%g");
		FocusTimerNP = new INDINumberProperty(this,"FOCUS_TIMER", "Focus Timer", FOCUS_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
		
		FocusHaltS = new INDISwitchElement("FOCUS_HALT", "Halt", SwitchStatus.ON);
		FocusSlowS = new INDISwitchElement("FOCUS_SLOW", "Slow", SwitchStatus.OFF);
		FocusFastS = new INDISwitchElement("FOCUS_FAST", "Fast", SwitchStatus.OFF);
		FocusModesSP = new INDISwitchProperty(this,"FOCUS_MODE", "Mode", FOCUS_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
		
		/**********************************************************************************************/
		/*********************************** GROUP: Date & Time ***************************************/
		/**********************************************************************************************/
		
		TimeT = new INDITextElement("UTC", "UTC", "0");
		TimeTP = new INDITextProperty(this,  "TIME_UTC", "UTC Time", DATETIME_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
		TimeTP.addElement(TimeT);
		addProperty(TimeTP);

		UTCOffsetN = new INDINumberElement("OFFSET", "Offset", 0, -12, 12, 0.5, "%0.3g");
		UTCOffsetNP = new INDINumberProperty(this, "TIME_UTC_OFFSET", "UTC Offset", DATETIME_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
		UTCOffsetNP.addElement(UTCOffsetN);
		addProperty(UTCOffsetNP);

		SDTimeN = new INDINumberElement("LST", "Sidereal time", 0, 0, 24, 0, "%10.6m");
		SDTimeNP = new INDINumberProperty(this,"TIME_LST", "Sidereal Time", DATETIME_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
		SDTimeNP.addElement(SDTimeN);
		addProperty(SDTimeNP);
		
		/**********************************************************************************************/
		/************************************* GROUP: Sites *******************************************/
		/**********************************************************************************************/
		
		Sites1S = new INDISwitchElement("SITE1", "Site 1", SwitchStatus.ON);
		Sites2S = new INDISwitchElement("SITE2", "Site 2", SwitchStatus.OFF);
		Sites3S = new INDISwitchElement("SITE3", "Site 3", SwitchStatus.OFF);
		Sites4S = new INDISwitchElement("SITE4", "Site 4", SwitchStatus.OFF);
		SitesSP = new INDISwitchProperty(this,"SITES", "Sites", SITE_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.ONE_OF_MANY);
		SitesSP.addElement(Sites1S);
		SitesSP.addElement(Sites2S);
		SitesSP.addElement(Sites3S);
		SitesSP.addElement(Sites4S);
		addProperty(SitesSP);

		SiteNameT = new INDITextElement("NAME", "Name", "");
		SiteNameTP = new INDITextProperty(this,  "SITE_NAME", "Site Name", SITE_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
		SiteNameTP.addElement(SiteNameT);
		addProperty(SiteNameTP);

		GeoLatN = new INDINumberElement("LAT",  "Lat.  D:M:S +N", 0, -90, 90, 0, "%10.6m");
		GeoLongN = new INDINumberElement("LONG",  "Long. D:M:S", 0, 0, 360, 0, "%10.6m");
		// Not used // GeoHeightN  = new INDINumberElement("HEIGHT",  "Height m", 610, -300, 6000, 0, "%10.2f");
		GeoNP = new INDINumberProperty(this,"GEOGRAPHIC_COORD", "Geographic Location", SITE_GROUP, PropertyStates.IDLE, PropertyPermissions.RW, 0);
		GeoNP.addElement(GeoLatN);
		GeoNP.addElement(GeoLongN);
		// Not used // GeoNP.addElement(GeoHeightN);
		addProperty(GeoNP);
		
	}

	/*
	 * Public interface methods 
	 */
		
	/**
	 * Connect to telescope and update INDI-Properties
	 */ 
	public void connect() {
		// Set delay-before-read to 200ms 
		// After some testing I found this a reliable value 
		// I'm not sure if it relies on the bluetooth connection, because
		// after looking into lx200generic.cpp it seems that on direct-serial
		// connections this delay isn't necessary.
		com_driver.set_delay(200); 
		
		super.connect();
		
		if (isConnected()) {
			ConnectS.setValue(SwitchStatus.ON);
			DisconnectS.setValue(SwitchStatus.OFF);
			ConnectSP.setState(PropertyStates.OK);
			updateProperty(ConnectSP,"Connected to telescope");
			
			// Get Alignment information
			getAlignment();
			
			// Get data for Site #1
			getSiteName(1);
			
			// Get current equatorial coords the scope is pointing at
			getEqCoords();
		}
		
	}

	/**
	 * Disconnect from telescope and update INDI-Properties
	 */
	public void disconnect() {
		
		super.disconnect();
		
		if (!isConnected()) {
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
		
		return DriverName;
		
	}

	/**
	 * Set new text-values received from clients
	 */
	@Override
	public void processNewTextValue(INDITextProperty property, Date timestamp,
			INDITextElementAndValue[] elementsAndValues) {
		
		if (isConnected()) {
		
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
				
				// send to Autostar 
				String DateCmd = String.format(setDateCmd, dateStr);
				String TimeCmd = String.format(setTimeCmd, timeStr);

				// After setting Date & Time Autostar is recomputing planetary objects, just wait a little longer 
				com_driver.set_delay(300);  
				getCommandInt(TimeCmd);
				getCommandInt(DateCmd);

				getDateTime();
				com_driver.set_delay(200);
			}

			/**
			 * Geolocation Property
			 */

		} else {
			updateProperty(property,"Not connected");
		}
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
			if (elem == ConnectS) connect();
		}
		
		// All other Properties are not available if not connected to telescope!
		
		if (isConnected()) {
			
			/**
			 * Alignment Property
			 */
			if (property==AlignmentSP) {
				if (elem==AltAzS) sendCommand(setAlignmentAltAzCmd);
				if (elem==PolarS) sendCommand(setAlignmentPolarCmd);
				if (elem==LandS) sendCommand(setAlignmentLandCmd);
				getAlignment();
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
				sendCommand(String.format(setCurrentSiteCmd,site));
				
				// Get the name of the selected site
				getSiteName(site);
				// Get the geolocation of the selected site
				getGeolocation();
				
				SitesSP.setState(PropertyStates.OK);
				updateProperty(SitesSP,"Selected Site #"+site);
			}
		} else {
			updateProperty(property,"Not connected");
		}
	}

	/**
	 * Set new number-values received from clients 
	 */
	@Override
	public void processNewNumberValue(INDINumberProperty property,
			Date timestamp, INDINumberElementAndValue[] elementsAndValues) {
		
		if (isConnected()) {
			
			/**
			 * UTC-Offset Property
			 */
			if (property==UTCOffsetNP) {
				// Get the value
				double val = elementsAndValues[0].getValue();
				// Standard "String.format" doesn't work, we need a string like "+02.0"
				// Additionally we have to change +/-, because Autostar needs a value to YIELD UTC
				// KStars sends the Offset (+02.0) but Autostar needs (-02.0) to get the right time.
				// The Handbox only displays the correct timezone +02.0 if we send -02.0 to it.  
				String sign = "-";
				if (val<0) sign = "+";
				String tmp = String.format("%s%02d.%01d", sign, (int) val, (int) (val % 1));
				String UTCHoursCmd = String.format(setUTCHoursCmd, tmp);
				if (getCommandInt(UTCHoursCmd) == 1) UTCOffsetNP.setState(PropertyStates.OK);	
				UTCOffsetN.setValue(val);
				updateProperty(property, "Local Time to UTC difference: "+tmp+"h");
			}

			/**
			 * Geolocation Property
			 */
			if (property==GeoNP) {
				double geolat = elementsAndValues[0].getValue();
				// Assemble a Autostar Latitude format
				// Positive = North, Negative = South  Example: "+50*01"
				String sign = "+";
				if (geolat<0) sign ="-";
				// TODO: Instead of truncating doubles with (int) we should round them 
				String tmp = String.format("%s%02d*%02d", sign, (int) geolat, (int) ((geolat % 1)*60) );
				
				String GeolatCmd = String.format(setSiteLatCmd, tmp);
				
				// Set latitude
				getCommandInt(GeolatCmd);

				double geolong = elementsAndValues[1].getValue();
				// Assemble an Autostar longitude format
				// Positive= West, Negative = East  Example: "-008*34" 
				sign = "-"; 
				if (geolong<0) sign ="+";
				// TODO: Instead of truncating doubles with (int) we should round them 
				tmp = String.format("%s%03d*%02d", sign, (int) geolong, (int) ((geolong % 1)*60) ); 
				String GeolongCmd = String.format(setSiteLongCmd, tmp);
				
				// Set longitude
				getCommandInt(GeolongCmd);

				getGeolocation();
			}
		
		} else {
			updateProperty(property,"Not connected");
		}
	}

	/**
	 * Not needed, telescope doesn't use BLOBs
	 */
	@Override
	public void processNewBLOBValue(INDIBLOBProperty property, Date timestamp,
			INDIBLOBElementAndValue[] elementsAndValues) {
		// Leave empty here, not needed. 
	}

	/**
	 * Get the Alignment-Mode and status from the telescope
	 */
	protected void getAlignment() {
		if (isConnected()) {
			String stmp=null;
			switch (getCommandChar(getAlignmentCmd)) {
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
			
			// For information only
			switch (getCommandString(getAlignmentStatusCmd).charAt(0)) {
			case 'A':
				stmp = stmp + ", AzEl mount";
				break;
			case 'P':
				stmp = stmp + ", Eq mount";
				break;
			case 'G':
				stmp = stmp + ", German Eq mount";
				break;
			}
			
			// For information only
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
			
			updateProperty(AlignmentSP,stmp);
		}
	}

	/**
	 * Get the current Date/Time from telescope
	 */
	protected void getDateTime() {
		String dateStr = getCommandString(getTimeCmd)+" "+getCommandString(getDateCmd);
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
		GeoLatN.setValue(getCommandSexa(getSiteLatCmd));
		GeoLongN.setValue(360-getCommandSexa(getSiteLongCmd));
		GeoNP.setState(PropertyStates.OK);
		updateProperty(GeoNP, "Set Geolocation to Lat: "+GeoLatN.getValueAsString()+" Long: "+GeoLongN.getValueAsString());
	}
	
	/** 
	 * Get Name of Site site# from telescope
	 */
	protected void getSiteName(int site) {
		com_driver.set_delay(300); //Again Autostar is too slow 
		switch (site) {
		case 1: 
			SiteNameT.setValue(getCommandString(getSite1NameCmd));
			break;
		case 2: 
			SiteNameT.setValue(getCommandString(getSite2NameCmd));
			break;
		case 3: 
			SiteNameT.setValue(getCommandString(getSite3NameCmd));
			break;
		case 4: 
			SiteNameT.setValue(getCommandString(getSite4NameCmd));
			break;
		}
		com_driver.set_delay(200);
		SiteNameTP.setState(PropertyStates.OK);
		updateProperty(SiteNameTP,"Site Name: "+SiteNameT.getValue());
	}
	
	/**
	 * Get the current equatorial coords the scope is pointing at
	 * TODO: some warning if telescope is not aligned, coords may be inaccurate  
	 */
	protected void getEqCoords() {
		INDISexagesimalFormatter sexa = new INDISexagesimalFormatter("%10.6m");
		double RA = sexa.parseSexagesimal(getCommandString(getCurrentRACmd));
		double DEC = sexa.parseSexagesimal(getCommandString(getCurrentDECCmd));
		RARN.setValue(RA);
		DECRN.setValue(DEC);
		EquatorialCoordsRNP.setState(PropertyStates.OK);
		updateProperty(EquatorialCoordsRNP,"Current coords RA: "+RARN.getValueAsString()+" DEC: "+DECRN.getValueAsString());
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
	protected double getCommandSexa(String command){
		INDISexagesimalFormatter sexa = new INDISexagesimalFormatter("%10.6m");
		double tmp = sexa.parseSexagesimal(getCommandString(command));
		return tmp;
	}
	
	/**
	 * Get an integer from the device 
	 * @param command
	 * @return integer 
	 */
	protected int getCommandInt(String command){
		return Integer.parseInt(getCommandString(command).substring(0, 1));
		
	}
	
	/**
	 * Get a char from the device
	 * @param command
	 * @return char
	 */
	protected char getCommandChar(String command) {
		return getCommandString(command).charAt(0);
	}
	
	/**
	 * Get a string from the device without the #-suffix and < >
	 * @param command 
	 * @return string 
	 */
	protected String getCommandString(String command) {
		String tmp=null;
		try {
			com_driver.sendCommand(command);
			tmp = com_driver.getAnswerString();
			tmp = tmp.replaceAll("#", "");
			tmp = tmp.replaceAll("<", "");
			tmp = tmp.replaceAll(">", "");
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
