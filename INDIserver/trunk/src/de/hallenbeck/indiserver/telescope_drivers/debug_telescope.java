package de.hallenbeck.indiserver.telescope_drivers;

/**
 * Dummy driver for debugging purposes only
 * @author atuschen
 *
 */
public class debug_telescope extends telescope implements telescope_driver {

	public int get_firmware_info() {
		// TODO Auto-generated method stub
		return 0;
	}

	public coords get_site_coords(int site) {
		// TODO Auto-generated method stub
		return null;
	}

	public RADec get_current_position() {
		// TODO Auto-generated method stub
		return null;
	}

	public int set_datetime(int datetime) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int set_utc_offset(int offset) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int set_site_coords(int site, float longitude, float latitude,
			String name) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int set_slew_speed(int speed) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void cancel_all() {
		// TODO Auto-generated method stub

	}

	public void cancel_north() {
		// TODO Auto-generated method stub

	}

	public void cancel_east() {
		// TODO Auto-generated method stub

	}

	public void cancel_south() {
		// TODO Auto-generated method stub

	}

	public void cancel_west() {
		// TODO Auto-generated method stub

	}

	public void move_north() {
		// TODO Auto-generated method stub

	}

	public void move_east() {
		// TODO Auto-generated method stub

	}

	public void move_south() {
		// TODO Auto-generated method stub

	}

	public void move_west() {
		// TODO Auto-generated method stub

	}

	public int move_to_target(RADec radec) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	int StringToInt(String s) {
		// TODO Auto-generated method stub
		return 0;
	}

}
