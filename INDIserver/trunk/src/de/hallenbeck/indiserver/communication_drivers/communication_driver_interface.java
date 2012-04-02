/*
 *
 * This file is part of INDIserver.
 *
 * INDIserver is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
	
	/**
	 * set global timeout for read/write operations
	 * @param timeout milliseconds
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
	 * Read until end of stream and throw it away
	 * @throws IOException 
	 */
	public void emptyBuffer() throws IOException;

}
