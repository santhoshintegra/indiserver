package de.hallenbeck.indiserver.device_drivers;

import android.os.Handler;
import android.util.Log;

/**
 * Dummy driver for debugging purposes only
 * @author atuschen
 *
 */
public class debug_telescope extends telescope implements generic_device_driver {

	public debug_telescope(String driver, Handler mHandler) {
		super(driver, mHandler, false);
		// TODO Auto-generated constructor stub
	}

	private final String TAG="Telescope-Driver";
	
	private int get_firmware_info() {
		Log.d(TAG,"Get Firmware Info");
		return 0;
	}

	private int get_site_coords(int site) {
		Log.d(TAG,"Get coords for site "+site);
		return 0;
	}

	private int get_current_position() {
		Log.d(TAG,"Get current position");
		return 0;
	}

	private int set_datetime(int datetime) {
		Log.d(TAG,"Set date+time" +datetime);
		return 0;
	}

	private int set_utc_offset(int offset) {
		Log.d(TAG,"Set UTC offset" +offset);
		return 0;
	}

	private int set_site_coords(int site, float longitude, float latitude,
			String name) {
		Log.d(TAG,"Set coords for site " +site);
		return 0;
	}

	private int set_slew_speed(int speed) {
		Log.d(TAG,"Set slew speed" +speed);
		return 0;
	}

	private void cancel_all() {
		Log.d(TAG,"Cancel all movements");

	}

	private void cancel_north() {
		Log.d(TAG,"Cancel northwards movement");

	}

	private void cancel_east() {
		Log.d(TAG,"Cancel eastwards movement");

	}

	private void cancel_south() {
		Log.d(TAG,"Cancel southwards movement");

	}

	private void cancel_west() {
		Log.d(TAG,"Cancel westwards movement");

	}

	private void move_north() {
		Log.d(TAG,"Move north");

	}

	private void move_east() {
		Log.d(TAG,"Move east");

	}

	private void move_south() {
		Log.d(TAG,"Move south");

	}

	private void move_west() {
		Log.d(TAG,"Move west");

	}

	private int move_to_target(int radec) {
		Log.d(TAG,"Move to target");
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
