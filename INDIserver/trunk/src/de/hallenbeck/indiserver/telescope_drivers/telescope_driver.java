package de.hallenbeck.indiserver.telescope_drivers;

import de.hallenbeck.indiserver.communication_drivers.communication_driver;

/**
 * Generic interface definition for telescope drivers 
 * @author atuschen
 *
 */
public interface telescope_driver {
	
	public static class coords {
		private int site=0;
		private float longitude=0;
		private float latitude=0;
		private String name=null;
		
		public int get_site() {
			return site;
		}
		
		public float get_longitude() {
			return longitude;
		}
		
		public float get_latitude() {
			return latitude;
		}
		
		public String get_name() {
			return name;
		}
		
	}
	
	public static class RADec {
		private float RA=0;
		private float DEC=0;
		private int Precision=0;
		
		public float getRA() {
			return RA;
		}
		
		public float getDEC() {
			return DEC;
		}
		
		public float getPrecision() {
			return Precision;
		}
		public void setRA(float ra) {
			RA=ra;
		}
		
		public void setDEC(float dec) {
			DEC=dec;
		}
		
		public void setPrecision(int precision) {
			Precision=precision;
		}
	}
	
	/**
	 * Basic commands
	 */
	
	/**
	 * Connect to the telescope
	 * @return 0 success, -1 failed
	 */
	abstract int connect();
	
	/**
	 * Are we connected?
	 * @return true or false
	 */
	abstract boolean isConnected();
	
	/**
	 * Disconnect from telescope
	 * @return 0 success, -1 failed
	 */
	abstract int disconnect();
	
	
	
	/**
	 * Get commands
	 */
	
	/**
	 * Get information about the telescope
	 * @return 0 success, -1 failed
	 */
	abstract int get_firmware_info();
	
	/**
	 * Get long/lat/name of site
	 * @param site
	 * @return coords
	 */
	abstract coords get_site_coords(int site);
	
	/**
	 * Get the current position the telescope is pointing at
	 * @return RADec
	 */
	abstract RADec get_current_position();
	
	
	/**
	 * Set commands
	 */
	
	/**
	 * Set the driver for communication with the telecope
	 * @param driver
	 */
	public void set_communication_driver (communication_driver driver);
	
	/**
	 * Set date and time
	 * @param datetime
	 * @return 0 success, -1 failed
	 */
	abstract int set_datetime(int datetime);
	
	/**
	 * Set UTC Offset
	 * @param offset
	 * @return 0 success, -1 failed
	 */
	abstract int set_utc_offset(int offset);
	
	/**
	 * Set long/lat/name for location site  
	 * @param site 
	 * @param longitude
	 * @param latitude
	 * @param name
	 * @return 0 success, -1 failed
	 */
	abstract int set_site_coords(int site, float longitude, float latitude, String name);
	
	/**
	 * Set slew speed
	 * @param speed
	 * @return 0 success, -1 failed
	 */
	abstract int set_slew_speed(int speed);
	
	/**
	 * Movement commands
	 */
	
	/**
	 * Cancel all movements
	 */
	abstract void cancel_all();
	
	/**
	 * Cancel northwards movements
	 */
	abstract void cancel_north();
	
	/**
	 * Cancel eastwards movements
	 */
	abstract void cancel_east();
	
	/**
	 * Cancel southwards movements
	 */
	abstract void cancel_south();
	
	/**
	 * Cancel westwards movements
	 */
	abstract void cancel_west();
	
	/**
	 * Move north with actual slew speed
	 */
	abstract void move_north();
	
	/**
	 * Move east with actual slew speed
	 */
	abstract void move_east();
	
	/**
	 * Move south with actual slew speed
	 */
	abstract void move_south();
	
	/**
	 * Move west with actual slew speed
	 */
	abstract void move_west();
	
	/**
	 * Move to target defined by RA and DEC
	 * @param RADec
	 * @return 0 success, -1 failed
	 */
	abstract int move_to_target(RADec radec);
	
	
}
