package de.hallenbeck.indiserver.communication_drivers;

import java.io.IOException;

public abstract class communication_driver {
	
	protected int Timeout;
	
	public void connect(String device, int timeout) throws IOException{
		Timeout=timeout;
		onConnect(device);
	}

	public void disconnect() {
		onDisconnect();
	}
	
	/**
	 * Try to write a string to the device
	 * @param command string to send
	 * @throws IOException timeout
	 */
	public void write(String data) throws IOException {
		onWrite(data);
	}
	
	/**
	 * Try to write a byte to the device
	 * @param command byte to send
	 * @throws IOException timeout
	 */
	public void write(byte data) throws IOException {
		onWrite(data);
	}

	/**
	 * Try to read from device until stopchar is detected
	 * @param stopchar 
	 * @return String
	 * @throws IOException timeout
	 */
	public String read(char stopchar) throws IOException {
		return onRead(stopchar);
	}
	
	/**
	 * Try to read at least num bytes from device
	 * @param bytes number of bytes to read
	 * @return String 
	 * @throws IOException timeout
	 */
	public String read(int len) throws IOException {
		return onRead(len);
	}
	
	protected void onConnect(String device) throws IOException {
	}
	
	protected void onDisconnect() {
	}

	protected void onWrite(String data) throws IOException {
	}
	
	protected void onWrite(byte data) throws IOException {
	}
	
	protected String onRead(char stopchar) throws IOException {
		return null;
	}
	
	protected String onRead(int len) throws IOException {
		return null;
	}
}
