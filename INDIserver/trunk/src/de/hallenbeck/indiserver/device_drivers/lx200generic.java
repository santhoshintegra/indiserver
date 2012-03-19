package de.hallenbeck.indiserver.device_drivers;

import java.io.IOException;

import android.os.Handler;

/**
 * Driver for LX200 compatible telescopes, only covering the basic commandset.
 * Extended LX200-protocols should be derived from this class. 
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
		//get_current_position();
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
			com_driver.sendCommand(":GVP#");
			com_driver.getAnswerString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	protected int site_coords(int site) {
		return 0;
	}

	protected int get_current_position() {
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

}
