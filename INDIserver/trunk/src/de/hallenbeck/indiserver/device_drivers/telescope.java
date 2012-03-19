package de.hallenbeck.indiserver.device_drivers;


import java.io.IOException;

import de.hallenbeck.indiserver.communication_drivers.communication_driver_interface;

/**
 * Generic telescope-class with basic functions   
 * @author atuschen
 *
 */
public abstract class telescope implements device_driver_interface {
	
	protected static communication_driver_interface com_driver=null;
	protected static boolean connected=false;

	/**
	 * Set the driver for communication with the telescope
	 * @param driver fully qualified name of driver class
	 */
	public void set_communication_driver(String driver) {
		try {
			com_driver = (communication_driver_interface) Class.forName(driver).newInstance();
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Connect to the telescope
	 */
	public void connect(String device) {
		try {
			com_driver.connect(device);
			connected=true;
		} catch (IOException e) {
			e.printStackTrace();
			connected=false;
		}
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
	 */
	public void disconnect() {
		connected=false;
		com_driver.disconnect();
	}

}
