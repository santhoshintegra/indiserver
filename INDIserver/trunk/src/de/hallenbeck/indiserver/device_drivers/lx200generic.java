package de.hallenbeck.indiserver.device_drivers;

import java.io.IOException;

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
	 */
	public void sendINDImsg(String xmlcommand) {
	}

	/** 
	 * Callback-Handler for INDI xml-messages from device to server (receive)
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
		information.setDescription(getDataString(":GVN#"));
		// Get firmware date
		information.setDescription(getDataString(":GVD#"));
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

}
