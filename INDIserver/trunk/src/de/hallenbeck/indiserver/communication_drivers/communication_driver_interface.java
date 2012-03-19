package de.hallenbeck.indiserver.communication_drivers;

import java.io.IOException;

/**
 * Generic interface definition for communication drivers
 * @author atuschen
 *
 */
public interface communication_driver_interface {
	
	/**
	 * Wait the specified amount of time before reading from device
	 * @param delay in ms
	 */
	public void set_delay(int delay);
	
	/**
	 * Connect to device
	 * @param device
	 * 
	 */
	public void connect(String device) throws IOException;
	
	/**
	 * Disconnect from device
	 * 
	 */
	public void disconnect();
	
	/**
	 * Send command string to device
	 * @param command
	 */
	public void sendCommand(String command) throws IOException;
	
	/**
	 * Read integer value from device
	 * @return int
	 */
	public int getAnswerInt() throws IOException;
	
	/**
	 * Read string from device 
	 * @return string
	 */
	public String getAnswerString() throws IOException;

}
