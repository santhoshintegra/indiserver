package de.hallenbeck.indiserver.device_drivers;

import android.os.Handler;

/**
 * Driver for LX200 compatible telescopes, only covering the basic commandset.
 * Extended LX200-protocols should be derived from this class. 
 * @author atuschen
 *
 */
public class lx200generic extends telescope implements generic_device_driver {
	
	public lx200generic(String driver, Handler mHandler) {
		super(driver, mHandler, false);
		// TODO Auto-generated constructor stub
	}

	private final int delay=200;
	private boolean HighPrecision=false;
	private boolean connected=false;
	
	public int connect() {
		com_driver.setTimeout(500);
		super.connect();
		get_firmware_info();
		get_current_position();
		return 0;
	}

	public int get_firmware_info() {
		com_driver.sendCommand(":GVP#");
		com_driver.wait(delay);
		com_driver.getAnswerString();
		return 0;
	}

	public int site_coords(int site) {
		return 0;
	}

	public int get_current_position() {
		return 0;
	}

	public int set_datetime(int datetime) {
		return 0;
	}

	public int set_utc_offset(int offset) {
		return 0;
	}

	public int set_site_coords(int site, float longitude, float latitude,
			String name) {
		return 0;
	}

	public int set_slew_speed(int speed) {
		return 0;
	}

	public void move_north() {
	}

	public void move_east() {
	}

	public void move_south() {
	}

	public void move_west() {
	}

	public void cancel_all() {
	}

	public void cancel_north() {
	}

	public void cancel_east() {
	}

	public void cancel_south() {
	}

	public void cancel_west() {
	}

	public int move_to_target(int radec) {
		return 0;
	}

	public void sendINDImsg(String xmlcommand) {
		// TODO Auto-generated method stub
		
	}

	public String recvINDImsg() {
		// TODO Auto-generated method stub
		return null;
	}

	public void set_msg_handler(Handler mHandler) {
		// TODO Auto-generated method stub
		
	}

}
