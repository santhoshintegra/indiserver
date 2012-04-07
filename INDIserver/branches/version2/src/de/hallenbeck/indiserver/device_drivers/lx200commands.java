package de.hallenbeck.indiserver.device_drivers;

/* Meade LX200 Commands according to official protocol sheet */

public class lx200commands {

	/* A - Alignment Commands */
	protected final  String AlignmentModeCmd = String.valueOf((char)6); //Get Alignment Mode; Returns A,P,D or L
	protected final  String AlignmentAltAzCmd = "#:AA#"; //Set Alignment Mode to AltAz
	protected final  String AlignmentPolarCmd = "#:AP#"; //Set Alignment Mode to Polar
	protected final  String AlignmentLandCmd = "#:AL#"; //Set Alignment Mode to Land
	
	/* C - Sync Commands */
	protected final  String SyncToTargetCmd = "#:CM#"; //Sync to target object coords; Returns:  string#
	
	/* D - Get Distance Bars */
	protected final  String DistanceBarsCmd = "#:D#"; //String containing one char until a slew is complete, then a null string is returned
	
	/* F - Focuser Control */
	protected final  String FocuserMoveInward = "#:F+#"; //Start Focuser moving inward (toward objective)
	protected final  String FocuserMoveOutward = "#:F-#"; //Start Focuser moving outward (away from objective)
	protected final  String FocuserHaltMotion = "#:FQ#"; //Halt Focuser Motion
	protected final  String FocuserSpeedFast = "#:FF#"; //Set Focuser speed to fastest setting
	protected final  String FocuserSpeedSlow = "#:FS#"; //Set Focuser speed to slowest setting
	protected final  String FocuserSpeed = "#:F%1d#"; //Set Focuser speed to %1d (1..4)
	
	/* G - Get Telescope Information */
	protected final  String getDateCmd = "#:GC#"; //Get current local Date; Returns: MM/DD/YY#
	protected final  String getCurrentDECCmd = "#:GD#"; //Get current telescope DEC; Returns: sDD*MM# or sDD*MM’SS# 
	protected final  String getTargetDECCmd = "#:Gd#"; //Get Target object DEC; Returns: sDD*MM# or sDD*MM’SS#
	protected final  String getUTCHoursCmd = "#:GG#"; //Get Hours to yield UTC from Local Time; Returns: sHH# or sHH.H#
	protected final  String getSiteLongCmd = "#:Gg#"; //Get Longitude of current site; Returns: sDDD*MM#
	protected final  String getTimeCmd = "#:GL#"; //Get current local Time; Returns: HH:MM:SS#
	protected final  String getSite1NameCmd = "#:GM#"; //Get Name of Site 1
	protected final  String getSite2NameCmd = "#:GN#"; //Get Name of Site 2
	protected final  String getSite3NameCmd = "#:GO#"; //Get Name of Site 3
	protected final  String getSite4NameCmd = "#:GP#"; //Get Name of Site 4
	protected final  String getTargetRACmd = "#:Gr#"; //Get Target object RA; Returns: HH:MM.T# or HH:MM:SS#
	protected final  String getCurrentRACmd = "#:GR#"; //Get current telescope RA; Returns: HH:MM.T# or HH:MM:SS#
	protected final  String getSiderealTimeCmd = "#:GS#"; //Get the Sidereal Time; Returns: HH:MM:SS#
	protected final  String getTrackingRateCmd = "#:GT#"; //Get tracking rate; Returns: TT.T#
	protected final  String getSiteLatCmd = "#:Gt#"; //Get Latitude of current site; Returns: sDD*MM# (Positive=North)
	protected final  String getFirmwareDateCmd = "#:GVD#"; //Get Telescope Firmware Date;	Returns: mmm dd yyyy#
	protected final  String getFirmwareNumberCmd = "#:GVN#"; //Get Telescope Firmware Number; Returns: <string>#
	protected final  String getProductNameCmd = "#:GVP#"; //Get Telescope Product Name; Returns: <string>#
	protected final  String getFirmwareTimeCmd = "#:GVT#"; //Get Telescope Firmware Time;	returns: HH:MM:SS#
	protected final  String getAlignmentStatusCmd = "#:GW#"; //Get Scope Alignment Status; Returns: <mount><tracking><alignment>#

	/* H - Home Position Commands */
	protected final  String HomePositionSeekCmd = "#:hF#"; //Seek Home 
	protected final  String HomePositionParkCmd = "#:hP#"; //Park Scope
	protected final  String HomePostitionStatusCmd = "#:h?#"; //Query Home Status
	
	/* M - Movement Commands */
	protected final  String MoveEastCmd = "#:Me#"; //Move Telescope East at current slew rate until ":Qe#" or ":Q#" is send
	protected final  String MoveNorthCmd = "#:Mn#"; //Move Telescope North at current slew rate until ":Qn#" or ":Q#" is send
	protected final  String MoveSouthCmd = "#:Ms#"; //Move Telescope South at current slew rate until ":Qs#" or ":Q#" is send
	protected final  String MoveWestCmd = "#:Mw#"; //Move Telescope West at current slew rate until ":Qw#" or ":Q#" is send
	protected final  String MoveToTargetCmd = "#:MS#"; //Move Telescope to target object; Returns: 0 Slew is Possible, 1<string># Object Below Horizon, 2<string># Object Below Higher
	
	/* Q - Stop Movement Commands */
	protected final  String StopAllMovementCmd = "#:Q#"; 
	protected final  String StopEastMovementCmd = "#:Qe#";
	protected final  String StopNorthMovementCmd = "#:Qn#";
	protected final  String StopSouthMovementCmd = "#:Qs#";
	protected final  String StopWestMovementCmd = "#:Qe#";
	
	/* R - Slew Rate Commands */
	protected final  String SlewRateCenteringCmd = "#:RC#"; //Centering Rate (2nd slowest)
	protected final  String SlewRateGuidingCmd = "#:RG#"; //Guiding Rate (slowest)
	protected final  String SlewRateFindCmd = "#:RM#"; //Find Rate (2nd fastest)
	protected final  String SlewRateMaxCmd = "#:RS#"; //Max Rate (fastest)
	
	/* S - Telescope Set Commands */
	protected final  String setDateCmd = "#:SC%s#"; //Set Local Date to %s (MM/DD/YY)
	protected final  String setTimeCmd = "#:SL%s#"; //Set Local Time to %s (HH:MM:SS)
	protected final  String setUTCHoursCmd = "#:SG%s#"; //Set Hours to yield UTC from Local Time
	protected final  String setSite1NameCmd = "#:SM%s#"; //Set Name of Site 1 to %s
	protected final  String setSite2NameCmd = "#:SN%s#"; //Set Name of Site 2 to %s
	protected final  String setSite3NameCmd = "#:SO%s#"; //Set Name of Site 3 to %s 
	protected final  String setSite4NameCmd = "#:SP%s#"; //Set Name of Site 4 to %s
	protected final  String setSiteLatCmd = "#:St%s#"; //Set Latitude of selected site to %s (sDD*MM) North=positive
	protected final  String setSiteLongCmd = "#:Sg%s#"; //Set Longitude of selected site to %s (DDD*MM) 
	protected final  String setTargetDECCmd = "#:Sd%s#"; //Set target object DEC to %s (sDD*MM or sDD*MM:SS depending on precision setting); Returns:	1 - DEC Accepted 0 - DEC invalid
	protected final  String setTargetRACmd = "#:Sr%s#"; //Set target object RA to %s (HH:MM.T or HH:MM:SS depending on precision setting); Returns:	1 - RA Accepted 0 - RA invalid
	protected final  String setSiderealTimeCmd ="#:SS%s#"; //Sets the local sidereal time to %s (HH:MM:SS); Returns: 0 - Invalid 1 - Valid

	/* U - Precision Toggle */
	protected final  String PrecisionToggleCmd = "#:U#"; //Toggle between low/hi precision in DEC/RA

	/* W - Site select */
	protected final  String SiteSelectCmd = "#:W%1d#"; //Set current site to %1d (1..4)
	
	/* Undocumented commands, use with caution! */
	/* :ED# 	Get current display message (localized)
	 * :EK9# 	MODE Key
	 * :EK13#	ENTER Key
	 * :EK 71#	GOTO Key
	 * :EK 48#  0 Key
	 * :EK 49#  1 Key
	 * :EK 50#	2 Key
	 * :EK 51#  3 Key ... and so on until
	 * :EK 57#  9 Key
	 * EK9 and EK13 without space! 
	 * Before using any key, check the actual display message! 
	 * One can accidentally navigate to the Download-Function, which can result in a need to reflash the Autostar  
	 */
	protected final  String getDisplayMsgCmd ="#:ED#"; 
	
	protected final  String pressModeKey = "#:EK13#";
	


}
