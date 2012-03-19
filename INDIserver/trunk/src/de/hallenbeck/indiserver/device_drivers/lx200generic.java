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
		com_driver.set_delay(200);
		super.connect(device);
		get_firmware_info();
		get_current_position();
	}

	/**
	 * Interface for INDI xml-messages (send)
	 */
	public void sendINDImsg(String xmlcommand) {
	}

	/** 
	 * Callback-Handler for INDI xml-messages (receive)
	 */
	public void set_msg_handler(Handler mHandler) {
	}
	
	/*
	 * Internal methods for LX200 and derived classes
	 */
	
	protected int get_firmware_info() {
		try {
			information = new telescope_information();
			// Get product name
			com_driver.sendCommand(":GVP#");
			information.setDescription(com_driver.getAnswerString());
			// Get version
			com_driver.sendCommand(":GVN#");
			information.setVersion(com_driver.getAnswerString());
			// Get firmware date
			com_driver.sendCommand(":GVD#");
			information.setDateTime(com_driver.getAnswerString());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	protected int site_coords(int site) {
		return 0;
	}

	protected int get_current_position() {
		pointing = new telescope_pointing();
		try {
			// Get RA
			com_driver.sendCommand(":GR#");
			pointing.setRA(RAtoFloat(com_driver.getAnswerString()));
			// Get DEC
			com_driver.sendCommand(":GD#");
			pointing.setDEC(DECtoFloat(com_driver.getAnswerString()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	protected int set_datetime(int datetime) {
		return 0;
	}

	protected int set_utc_offset(int offset) {
		return 0;
	}

	protected int set_site_coords(int site, float longitude, float latitude,
			String name) {
		return 0;
	}

	protected int set_slew_speed(int speed) {
		return 0;
	}

	protected void move_north() {
	}

	protected void move_east() {
	}

	protected void move_south() {
	}

	protected void move_west() {
	}

	protected void cancel_all() {
	}

	protected void cancel_north() {
	}

	protected void cancel_east() {
	}

	protected void cancel_south() {
	}

	protected void cancel_west() {
	}

	protected int move_to_target(int radec) {
		return 0;
	}
	
	/*
	 * Auxillary functions (LX200 specific)
	 */
	
	// Strip the # from end of strings
	protected String stripChar(String str) {
		return null;
	}
	
	// Convert RA string to float value
	// NOTE: Scope is always reporting "High precision" coords!
	// Example: 13:17:12#
	protected float RAtoFloat(String RA) {
		return 0;
	}
	
	// Convert DEC string to float value
	// NOTE: Scope is always reporting "High precision" coords! 
	// I get a ° (0xDF) instead of * as told by protocol-sheet
	// Example: +89°59:59#
	protected float DECtoFloat(String DEC) {
		return 0;
	}

}
