package de.hallenbeck.indiserver.device_drivers;

import android.os.Handler;
import de.hallenbeck.indiserver.communication_drivers.communication_driver_interface;

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
	public void set_communication_driver (communication_driver_interface driver);
	
	/**
	 * Interface for sending INDI XML-messages to the driver
	 * @param xmlcommand
	 */
	public void sendINDImsg(String xmlcommand);
	
	/**
	 * Interface for receiving INDI XML-messages from the driver
	 * 
	 * a callback message-handler
	 * @return
	 */
	public void set_msg_handler(Handler mHandler);
}