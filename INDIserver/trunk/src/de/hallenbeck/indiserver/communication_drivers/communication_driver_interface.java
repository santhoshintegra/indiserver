package de.hallenbeck.indiserver.communication_drivers;

/**
 * Generic interface definition for communication drivers
 * @author atuschen
 *
 */
public interface communication_driver_interface {
	
	public int OK=0;
	
	public int ERR_DEVICE=1;
	
	public int ERR_TIMEOUT=2;

	/**
	 * Set communication timeout 
	 * @param timeout in ms
	 */
	public void setTimeout(int timeout);
	
	/**
	 * Connect to device
	 * @param device
	 * @return 0 success, -1 failed 
	 */
	public int connect(String device);
	
	/**
	 * Disconnect from device
	 * @return 0
	 */
	public int disconnect();
	
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
	public void wait(int delay);
	
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
