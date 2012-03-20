package de.hallenbeck.indiserver.device_drivers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import laazotea.indi.Constants.PropertyPermissions;
import laazotea.indi.Constants.PropertyStates;
import laazotea.indi.driver.INDIBLOBElementAndValue;
import laazotea.indi.driver.INDIBLOBProperty;
import laazotea.indi.driver.INDINumberElementAndValue;
import laazotea.indi.driver.INDINumberProperty;
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
	
	private INDITextProperty telescopeInfoProp;
	private INDITextElement telescopeInfoElem;
	
	/*
	 * Constructor with input and outputstream for indi-xml-messages.
	 * TODO: extend with com_driver and device interface string
	 */
	
	public lx200generic(InputStream inputStream, OutputStream outputStream) {
		super(inputStream, outputStream);
	
		// We add the default CONNECTION Property
	    addConnectionProperty();
	    
	    // We create the Text Property for telescope info 
	    telescopeInfoElem = new INDITextElement("TELESCOPE_INFO", "Telescope Info", "");
	    telescopeInfoProp = new INDITextProperty(this, "TELESCOPE_INFO", "Telescope Info", "Firmware Information", PropertyStates.IDLE, PropertyPermissions.RO, 3);
	    telescopeInfoProp.addElement(telescopeInfoElem);

	    addProperty(telescopeInfoProp);
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
		get_firmware_info();
		get_current_position();
	}

	/**
	 * Interface for INDI xml-messages from server to device (send)
	 * TODO: OBSOLETE! 
	 */
	public void sendINDImsg(String xmlcommand) {
	}

	/** 
	 * Callback-Handler for INDI xml-messages from device to server (receive)
	 * TODO: OBSOLETE!
	 */
	public void set_msg_handler(Handler mHandler) {
	}
	
	/*
	 * Internal methods for LX200 and derived classes
	 */
	
	/**
	 * Get some information about the telescope
	 * 
	 */
	protected void get_firmware_info() {
		information = new telescope_information();
		// Get product name
		information.setDescription(getDataString(":GVP#"));
		// Get version
		information.setVersion(getDataString(":GVN#"));
		// Get firmware date
		information.setDateTime(getDataString(":GVD#"));
		
		INDITextProperty pn = (INDITextProperty) getProperty("TELESCOPE_INFO");
	    INDITextElement en = (INDITextElement) pn.getElement("TELESCOPE_INFO");
	    en.setValue(information.getDescription());
	    telescopeInfoProp.setState(PropertyStates.OK);
        updateProperty(telescopeInfoProp);
	    
		
	}

	/**
	 * Get the long/lat of site # stored in telescope
	 * @param site site#
	 * @return 
	 */
	protected int get_site_coords(int site) {
		return 0;
	}

	/**
	 * Get the current RA/DEC the telescope is pointing at 
	 * @return
	 */
	protected int get_current_position() {
		pointing = new telescope_pointing();
		// Get RA
		pointing.setRA(RAtoFloat(getDataString(":GR#")));
		// Get DEC
		pointing.setDEC(DECtoFloat(getDataString(":GD#")));
		return 0;
	}

	/**
	 * Set the date and time
	 * @param datetime
	 * @return
	 */
	protected int set_datetime(int datetime) {
		return 0;
	}

	/**
	 * Set UTC offset
	 * @param offset
	 * @return
	 */
	protected int set_utc_offset(int offset) {
		return 0;
	}

	/**
	 * Set long/lat/name of site#
	 * @param site
	 * @param longitude
	 * @param latitude
	 * @param name
	 * @return
	 */
	protected int set_site_coords(int site, float longitude, float latitude,
			String name) {
		return 0;
	}

	/**
	 * Set slewing speed
	 * @param speed
	 * @return
	 */
	protected int set_slew_speed(int speed) {
		return 0;
	}

	/* 
	 * Simple movement commands for manual positioning
	 * (scope returns nothing on these) 
	 */
	
	protected void move_north() {
		send(":Mn#");
	}

	protected void move_east() {
		send(":Me#");
	}

	protected void move_south() {
		send(":Ms#");
	}

	protected void move_west() {
		send(":Mw#");
	}

	protected void cancel_all() {
		send(":Q#");
	}

	protected void cancel_north() {
		send(":Qn#");
	}

	protected void cancel_east() {
		send(":Qe#");
	}

	protected void cancel_south() {
		send(":Qs#");
	}

	protected void cancel_west() {
		send(":Qw#");
	}

	/**
	 * Move scope to target RA/DEC
	 * @param radec
	 * @return
	 */
	protected int move_to_target(int radec) {
		return 0;
	}
	
	
	
	/*
	 * Auxillary functions (LX200 specific)
	 * 
	 */
	
	
	
	/**
	 * Get a data string from the device as answer to a command string
	 * @param command command string
	 * @return string 
	 */
	protected String getDataString(String command) {
		String tmp=null;
		try {
			com_driver.sendCommand(command);
			tmp = com_driver.getAnswerString();
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
	protected void send(String command) {
		try {
			com_driver.sendCommand(command);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 *  Strip the # from end of strings
	 * @param str
	 * @return
	 */
	protected String stripChar(String str) {
		return null;
	}
	
	/**
	 * Convert RA string to float value
	 * NOTE: Scope is always reporting "High precision" coords!
	 * Example: 13:17:12#
	 * @param RA
	 * @return
	 */
	protected float RAtoFloat(String RA) {
		return 0;
	}
	
	/**
	 * Convert DEC string to float value
	 * NOTE: Scope is always reporting "High precision" coords! 
	 * I get a ° (0xDF) instead of * as told by protocol-sheet
	 * Example: +89°59:59#
	 * @param DEC
	 * @return
	 */
	protected float DECtoFloat(String DEC) {
		return 0;
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
