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

public class lx200autostar extends lx200basic {
	
	
	
	private final static String driverName = "LX200autostar";

	public lx200autostar(InputStream in, OutputStream out) {
		
		super(in, out);
	}
	
	
		protected INDISwitchElement SlewSpeed3S = new INDISwitchElement(SlewModeSP, "8X", "8x", SwitchStatus.OFF);
		protected INDISwitchElement SlewSpeed4S = new INDISwitchElement(SlewModeSP, "16X", "16x", SwitchStatus.OFF);
		protected INDISwitchElement SlewSpeed5S = new INDISwitchElement(SlewModeSP, "64X", "64x", SwitchStatus.OFF);
		protected INDISwitchElement SlewSpeed6S = new INDISwitchElement(SlewModeSP, "05D", "0.5 deg/sec", SwitchStatus.OFF);
		protected INDISwitchElement SlewSpeed7S = new INDISwitchElement(SlewModeSP, "10D", "1.0 deg/sec", SwitchStatus.OFF);
	
	
	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.lx200basic#processNewTextValue(laazotea.indi.driver.INDITextProperty, java.util.Date, laazotea.indi.driver.INDITextElementAndValue[])
	 */
	@Override
	public void processNewTextValue(INDITextProperty property, Date timestamp,
			INDITextElementAndValue[] elementsAndValues) {

		super.processNewTextValue(property, timestamp, elementsAndValues);
	}

	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.lx200basic#processNewSwitchValue(laazotea.indi.driver.INDISwitchProperty, java.util.Date, laazotea.indi.driver.INDISwitchElementAndValue[])
	 */
	@Override
	public void processNewSwitchValue(INDISwitchProperty property,
			Date timestamp, INDISwitchElementAndValue[] elementsAndValues) {
		
		boolean ret=false;
		INDISwitchElement elem = elementsAndValues[0].getElement();
		
		if (property==SlewModeSP) {
			if (elem==SlewSpeed3S) ret = setSlewMode(3);
			if (elem==SlewSpeed4S) ret = setSlewMode(4);
			if (elem==SlewSpeed5S) ret = setSlewMode(5);
			if (elem==SlewSpeed6S) ret = setSlewMode(6);
			if (elem==SlewSpeed7S) ret = setSlewMode(7);
			if (ret) { 
				property.setState(PropertyStates.OK); 
				updateProperty(property); 
			} else 
				super.processNewSwitchValue(property, timestamp, elementsAndValues);
		} else
			super.processNewSwitchValue(property, timestamp, elementsAndValues);
	}

	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.lx200basic#processNewNumberValue(laazotea.indi.driver.INDINumberProperty, java.util.Date, laazotea.indi.driver.INDINumberElementAndValue[])
	 */
	@Override
	public void processNewNumberValue(INDINumberProperty property,
			Date timestamp, INDINumberElementAndValue[] elementsAndValues) {
		// TODO Auto-generated method stub
		super.processNewNumberValue(property, timestamp, elementsAndValues);
	}

	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.lx200basic#processNewBLOBValue(laazotea.indi.driver.INDIBLOBProperty, java.util.Date, laazotea.indi.driver.INDIBLOBElementAndValue[])
	 */
	@Override
	public void processNewBLOBValue(INDIBLOBProperty property, Date timestamp,
			INDIBLOBElementAndValue[] elementsAndValues) {
		// TODO Auto-generated method stub
		super.processNewBLOBValue(property, timestamp, elementsAndValues);
	}

	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.lx200basic#connect()
	 */
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

	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.lx200basic#disconnect()
	 */
	@Override
	public void onDisconnect() {
		// TODO Auto-generated method stub
		super.onDisconnect();
	}

	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.lx200basic#getName()
	 */
	@Override
	public String getName() {
		return driverName;
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
	
	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.lx200basic#setUTCOffset(double)
	 */
	@Override
	protected boolean setUTCOffset(double offset) {
		// Standard "String.format" doesn't work, we need a string like "+02.0"
        // Additionally we have to change +/-, because Autostar needs a value to yield UTC from local Time.
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

	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.lx200basic#setDateTime(java.util.Date)
	 */
	@Override
	protected boolean setDateTime(Date date) {
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
		String DateCmd = String.format(lx200.setDateCmd, dateStr);
		String TimeCmd = String.format(lx200.setTimeCmd, timeStr);
	
		// send to Autostar
		getCommandChar(TimeCmd);
		getCommandChar(DateCmd);
		try {
			com_driver.read('#'); // Return String "Updating planetary data... #"
			com_driver.read('#'); // Return String "                           #"
		} catch (IOException e) {
			e.printStackTrace();
		}
		getDateTime();
		return true;
	}
	
	@Override
	protected boolean setSlewMode(int mode) {
		super.setSlewMode(mode);
		
		if (mode==3) { sendCommand("#:EK 51#"); SlewSpeed3S.setValue(SwitchStatus.ON); }
		if (mode==4) { sendCommand("#:EK 52#"); SlewSpeed4S.setValue(SwitchStatus.ON); }
		if (mode==5) { sendCommand("#:EK 53#"); SlewSpeed5S.setValue(SwitchStatus.ON); }
		if (mode==6) { sendCommand("#:EK 54#"); SlewSpeed6S.setValue(SwitchStatus.ON); }
		if (mode==7) { sendCommand("#:EK 55#"); SlewSpeed7S.setValue(SwitchStatus.ON); }
		
		return true;
	}
}
