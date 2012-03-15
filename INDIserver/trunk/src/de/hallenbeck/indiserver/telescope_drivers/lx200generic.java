package de.hallenbeck.indiserver.telescope_drivers;

/**
 * Driver for LX200 compatible telescopes, only covering the basic commandset.
 * Extended LX200-protocols should be derived from this class. 
 * @author atuschen
 *
 */
public class lx200generic extends telescope implements telescope_driver {
	
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

	private int get_firmware_info() {
		com_driver.sendCommand(":GVP#");
		com_driver.wait(delay);
		com_driver.getAnswerString();
		return 0;
	}

	private int site_coords(int site) {
		return 0;
	}

	private int get_current_position() {
		return 0;
	}

	private int set_datetime(int datetime) {
		return 0;
	}

	private int set_utc_offset(int offset) {
		return 0;
	}

	private int set_site_coords(int site, float longitude, float latitude,
			String name) {
		return 0;
	}

	private int set_slew_speed(int speed) {
		return 0;
	}

	private void move_north() {
	}

	private void move_east() {
	}

	private void move_south() {
	}

	private void move_west() {
	}

	private void cancel_all() {
	}

	private void cancel_north() {
	}

	private void cancel_east() {
	}

	private void cancel_south() {
	}

	private void cancel_west() {
	}

	private int move_to_target(int radec) {
		return 0;
	}

	public void sendINDImsg(String xmlcommand) {
		// TODO Auto-generated method stub
		
	}

	public String recvINDImsg() {
		// TODO Auto-generated method stub
		return null;
	}

}
