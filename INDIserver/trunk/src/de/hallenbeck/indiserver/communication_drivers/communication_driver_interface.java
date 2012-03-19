package de.hallenbeck.indiserver.communication_drivers;

/**
 * Generic interface definition for communication drivers
 * @author atuschen
 *
 */
public interface communication_driver_interface {
	
	
	/**
	 * Connect to device
	 * @param device
	 * @return 0 success, -1 failed 
	 */
	public int connect(String device);
	
	/**
	 * Disconnect from device
	 * 
	 */
	public void disconnect();
	
	/**
	 * Send command string to device
	 * @param command
	 * @return 0 success, -1 failed
	 */
	public int sendCommand(String command);
	
	/**
	 * Wait the specified amount of time
	 * @param delay in ms
	 */
	public void set_delay(int delay);
	
	/**
	 * Read integer value from device
	 * @return int
	 */
	public int getAnswerInt();
	
	/**
	 * Read string from device 
	 * @return string
	 */
	public String getAnswerString();

}
