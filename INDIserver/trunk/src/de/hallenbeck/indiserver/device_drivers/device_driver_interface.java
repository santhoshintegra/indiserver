package de.hallenbeck.indiserver.device_drivers;

import android.os.Handler;


/**
 * Generic interface definition for device drivers 
 * @author atuschen
 *
 */
public interface device_driver_interface {
	
	/**
	 * Set the driver for communication with the telecope
	 * @param driver
	 */
	public void set_communication_driver (String driver);
}
