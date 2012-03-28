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
	 * @deprecated
	 */
	public void set_delay(int delay);
	
	/**
	 * set global timeout for read/write operations
	 * @param timeout
	 */
	public void set_timeout(int timeout);
	
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
	 * @deprecated
	 */
	public void sendCommand(String command) throws IOException;
	
	/**
	 * Try to write a string to the device
	 * @param command string to send
	 * @throws IOException timeout
	 */
	public void write(String command) throws IOException;
	
	/**
	 * Try to write a byte to the device
	 * @param command byte to send
	 * @throws IOException timeout
	 */
	public void write(byte command) throws IOException;
	
	/**
	 * Read integer value from device
	 * @return int
	 * @deprecated
	 */
	public int getAnswerInt() throws IOException;
	
	/**
	 * Read string from device 
	 * @return string
	 * @deprecated
	 */
	public String getAnswerString() throws IOException;
	
	/**
	 * Try to read from device until stopchar is detected
	 * @param stopchar 
	 * @return String
	 * @throws IOException timeout
	 */
	public String read(char stopchar) throws IOException;
	
	/**
	 * Try to read at least num bytes from device
	 * @param bytes number of bytes to read
	 * @return String 
	 * @throws IOException timeout
	 */
	public String read(int bytes) throws IOException;
	
	/**
	 * Try to read from device until end of stream or timeout 
	 * @return String
	 * @throws IOException timeout
	 */
	public String read() throws IOException;

}
