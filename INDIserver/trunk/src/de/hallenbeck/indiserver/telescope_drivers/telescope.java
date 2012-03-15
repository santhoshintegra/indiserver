package de.hallenbeck.indiserver.telescope_drivers;

import de.hallenbeck.indiserver.communication_drivers.communication_driver;

/**
 * Generic telescope-class with basic functions   
 * @author atuschen
 *
 */
public abstract class telescope {
	
	public communication_driver com_driver=null;
	public boolean connected=false;
	
	abstract int StringToInt(String s);

	/**
	 * Set the driver for communication with the telescope
	 * @param driver
	 */
	public void set_communication_driver(communication_driver driver) {
		// TODO Auto-generated method stub
		com_driver=driver;
	}
	
	/**
	 * Connect to the telescope
	 * @return
	 */
	public int connect() {
		com_driver.connect("/dev/ttyS0");
		connected=true;
		return 0;
	}
	
	/**
	 * Are we connected?
	 * @return
	 */
	public boolean isConnected() {
		return connected;
	}
	
	/**
	 * Disconnect from telescope
	 * @return
	 */
	public int disconnect() {
		connected=false;
		com_driver.disconnect();
		return 0;
	}

}
