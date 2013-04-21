/*
 *
 * This file is part of INDIserver.
 *
 * INDIserver is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * INDIserver is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with INDIserver.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2012 Alexander Tuschen <atuschen75 at gmail dot com>
 *
 */

package de.hallenbeck.indiserver.communication_drivers;

import java.io.IOException;

/**
 * Abstract base class for communication drivers
 * @author atuschen
 */

public abstract class communication_driver {
	
	protected int Timeout;
	public boolean connected=false;
	
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
	 * Try to read at least len bytes from device
	 * @param bytes number of bytes to read
	 * @return String 
	 * @throws IOException timeout
	 */
	public String read(int len) throws IOException {
		return onRead(len);
	}
	
	public abstract String getVersion();
	
	public abstract String getName();
	
	protected abstract void onConnect(String device) throws IOException;
	
	protected abstract void onDisconnect();

	protected abstract void onWrite(String data) throws IOException;
	
	protected abstract void onWrite(byte data) throws IOException;
	
	protected abstract String onRead(char stopchar) throws IOException;
	
	protected abstract String onRead(int len) throws IOException;
}
