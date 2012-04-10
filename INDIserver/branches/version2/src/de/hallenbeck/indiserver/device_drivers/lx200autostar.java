package de.hallenbeck.indiserver.device_drivers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import laazotea.indi.INDIDateFormat;
import laazotea.indi.Constants.PropertyStates;
import laazotea.indi.Constants.SwitchStatus;
import laazotea.indi.driver.INDIBLOBElementAndValue;
import laazotea.indi.driver.INDIBLOBProperty;
import laazotea.indi.driver.INDINumberElementAndValue;
import laazotea.indi.driver.INDINumberProperty;
import laazotea.indi.driver.INDISwitchElement;
import laazotea.indi.driver.INDISwitchElementAndValue;
import laazotea.indi.driver.INDISwitchProperty;
import laazotea.indi.driver.INDITextElementAndValue;
import laazotea.indi.driver.INDITextProperty;

/**
 * Extended Driver for Autostar#497 compatible telescopes, only covering the basic commandset.
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
public class lx200autostar extends lx200basic {

	private final static String driverName = "LX200autostar";

	public lx200autostar(InputStream in, OutputStream out) {
		super(in, out);
	}
	
	// Additional Slew speed elements (Speed 3-7)
	protected INDISwitchElement SlewSpeed3S = new INDISwitchElement(SlewModeSP, "8X", "8x", SwitchStatus.OFF);
	protected INDISwitchElement SlewSpeed4S = new INDISwitchElement(SlewModeSP, "16X", "16x", SwitchStatus.OFF);
	protected INDISwitchElement SlewSpeed5S = new INDISwitchElement(SlewModeSP, "64X", "64x", SwitchStatus.OFF);
	protected INDISwitchElement SlewSpeed6S = new INDISwitchElement(SlewModeSP, "05D", "0.5 deg/sec", SwitchStatus.OFF);
	protected INDISwitchElement SlewSpeed7S = new INDISwitchElement(SlewModeSP, "10D", "1.0 deg/sec", SwitchStatus.OFF);


	@Override
	public void processNewSwitchValue(INDISwitchProperty property,
			Date timestamp, INDISwitchElementAndValue[] elementsAndValues) {

		INDISwitchElement elem = elementsAndValues[0].getElement();
		
		if (property==SlewModeSP) {
			if (elem==SlewSpeed3S) setSlewMode(3);
			if (elem==SlewSpeed4S) setSlewMode(4);
			if (elem==SlewSpeed5S) setSlewMode(5);
			if (elem==SlewSpeed6S) setSlewMode(6);
			if (elem==SlewSpeed7S) setSlewMode(7);
			super.processNewSwitchValue(property, timestamp, elementsAndValues);
		}
	}

	@Override
	public void onConnect() {
		super.onConnect();
		
		// EXPERIMENTAL! Works only with Firmware 43Eg (Display messages are localized!)
		// Navigate Handbox to main menu after power-on, skipping all data entry prompts,
		// because Handbox does NOT save any parameters as long as it is NOT in main menu.
		
		if (FirmwareVersionT.getValue().compareTo("43Eg")==0) {
	
			if (getDisplayMessage().compareTo("*Press 0 to Alignor MODE for Menu")==0) {
				int i=0;
				while (i < 7) {
					// "Press" the MODE-Key 7 times
					sendCommand("#:EK9#");
					i++;
				}
			}
		}
	}


	@Override
	protected void getUTCOffset() {
		String tmp = getCommandString(lx200.getUTCHoursCmd);
		double offset = Double.parseDouble(tmp)*-1;
		UTCOffsetN.setValue(offset);
		UTCOffsetNP.setState(PropertyStates.OK);
		updateProperty(UTCOffsetNP, "Local time to UTC difference: "+tmp+"h");
	}
	
	/**
	 * Get the current Date/Time from telescope
	 */
	@Override
	protected void getDateTime() {
		//Subtract UTC-Offset to get UTC-Time from Local Time
		
		String dateStr = getCommandString(lx200.getTimeCmd)+" "+getCommandString(lx200.getDateCmd);
		try {
			//This is in local Time!
			Date date = new SimpleDateFormat("kk:mm:ss MM/dd/yy").parse(dateStr);
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.add(Calendar.HOUR, UTCOffsetN.getValue().intValue()*-1);
			cal.add(Calendar.MINUTE, (int) ((UTCOffsetN.getValue()%1)*60)*-1);
			date = cal.getTime();
			TimeT.setValue(INDIDateFormat.formatTimestamp(date));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TimeTP.setState(PropertyStates.OK);
		updateProperty(TimeTP, "Local time:"+dateStr);
	}
	
	@Override
	protected boolean setUTCOffset(double offset) {
		// Standard "String.format" doesn't work, we need a string like "+02.0"
        // Additionally we have to change +/-, because Autostar#497 needs a value to yield UTC from local Time.
        // KStars sends the Offset (+02.0) but Autostar needs (-02.0) to get the right time.
        // The Handbox only displays the correct timezone +02.0 if we send -02.0 to it.  
        
        String sign = "-";
        if (offset<0) {
                sign = "+";
                offset = offset * -1;
        }
        String tmp = String.format("%s%02d.%01d", sign, (int) offset, (int) (offset % 1));
        String UTCHoursCmd = String.format(lx200.setUTCHoursCmd, tmp);
        getCommandInt(UTCHoursCmd);     
        getUTCOffset();
		return true;
	}

	@Override
	protected boolean setDateTime(Date date) {
		// Autostar#497 expects local time, but INDI clients send UTC!
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
	
		// send Time first and at last the Date
		// Telescope is calculating planetary objects after a new date is set
		if ((getCommandInt(TimeCmd)==1) && (getCommandInt(DateCmd)==1)) {

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
	
	@Override
	protected void setSlewMode(int mode) {
		super.setSlewMode(mode);
		
		if (mode==3) { sendCommand("#:EK 51#"); SlewSpeed3S.setValue(SwitchStatus.ON); }
		if (mode==4) { sendCommand("#:EK 52#"); SlewSpeed4S.setValue(SwitchStatus.ON); }
		if (mode==5) { sendCommand("#:EK 53#"); SlewSpeed5S.setValue(SwitchStatus.ON); }
		if (mode==6) { sendCommand("#:EK 54#"); SlewSpeed6S.setValue(SwitchStatus.ON); }
		if (mode==7) { sendCommand("#:EK 55#"); SlewSpeed7S.setValue(SwitchStatus.ON); }
		
	}
}
