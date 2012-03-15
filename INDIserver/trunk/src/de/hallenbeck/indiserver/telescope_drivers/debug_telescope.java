package de.hallenbeck.indiserver.telescope_drivers;

import android.util.Log;

/**
 * Dummy driver for debugging purposes only
 * @author atuschen
 *
 */
public class debug_telescope extends telescope implements telescope_driver {

	private final String TAG="Telescope-Driver";
	
	public int get_firmware_info() {
		Log.d(TAG,"Get Firmware Info");
		return 0;
	}

	public coords get_site_coords(int site) {
		Log.d(TAG,"Get coords for site "+site);
		return null;
	}

	public RADec get_current_position() {
		Log.d(TAG,"Get current position");
		return null;
	}

	public int set_datetime(int datetime) {
		Log.d(TAG,"Set date+time" +datetime);
		return 0;
	}

	public int set_utc_offset(int offset) {
		Log.d(TAG,"Set UTC offset" +offset);
		return 0;
	}

	public int set_site_coords(int site, float longitude, float latitude,
			String name) {
		Log.d(TAG,"Set coords for site " +site);
		return 0;
	}

	public int set_slew_speed(int speed) {
		Log.d(TAG,"Set slew speed" +speed);
		return 0;
	}

	public void cancel_all() {
		Log.d(TAG,"Cancel all movements");

	}

	public void cancel_north() {
		Log.d(TAG,"Cancel northwards movement");

	}

	public void cancel_east() {
		Log.d(TAG,"Cancel eastwards movement");

	}

	public void cancel_south() {
		Log.d(TAG,"Cancel southwards movement");

	}

	public void cancel_west() {
		Log.d(TAG,"Cancel westwards movement");

	}

	public void move_north() {
		Log.d(TAG,"Move north");

	}

	public void move_east() {
		Log.d(TAG,"Move east");

	}

	public void move_south() {
		Log.d(TAG,"Move south");

	}

	public void move_west() {
		Log.d(TAG,"Move west");

	}

	public int move_to_target(RADec radec) {
		Log.d(TAG,"Move to target");
		return 0;
	}

	@Override
	int StringToInt(String s) {
		// TODO Auto-generated method stub
		return 0;
	}

}
