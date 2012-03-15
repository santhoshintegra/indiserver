package de.hallenbeck.indiserver.telescope_drivers;

import de.hallenbeck.indiserver.communication_drivers.communication_driver;

/**
 * Generic interface definition for telescope drivers 
 * @author atuschen
 *
 */
public interface telescope_driver {
	
	/**
	 * Set the driver for communication with the telecope
	 * @param driver
	 */
	public void set_communication_driver (communication_driver driver);
	
	/**
	 * Interface for sending INDI XML-messages to the driver
	 * @param xmlcommand
	 */
	public void sendINDImsg(String xmlcommand);
	
	/**
	 * Interface for receiving INDI XML-messages from the driver
	 * @return
	 */
	public String recvINDImsg();
}
