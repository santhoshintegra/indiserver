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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Serial communication driver for devices with /dev/ttyS or /dev/ttyUSB support
 * Base class for all other communication drivers as they all use serial communication
 * @author atuschen
 *
 */

public class serial implements communication_driver_interface {

	protected InputStream InStream;
	protected OutputStream OutStream;
	protected InputStreamReader InReader;
	protected BufferedReader BufReader;
    private int timeout = 100; 
    
 

    
    /**
     * Class Constructor
     */
    public serial() {
        
    }

	@Override
    public void connect(String device) throws IOException {
		// TODO Open serial device and construct Readers;
	}

	@Override
	public void disconnect() {
		Timer.stopTread();
	}
	
	/**
	 * Send a String to the device
	 * @param command: String
	 */
	@Override
	public synchronized void sendCommand(String command) throws IOException {
		byte[] buffer=command.getBytes();
		OutStream.write(buffer);
		
	}

	@Override
	public void set_timeout(int timeout_ms) {
		timeout = timeout_ms;
	}

	@Override
	public void write(String command) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void write(byte command) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public synchronized String read(char stopchar) throws IOException {
		char c = (char) 255 ;
		char[] chararray = new char[255];
		String ret = null;
		
		//Try to read until stopchar is detected or a timeout occurs
		
		int pos = 0;
		
		long endTimeMillis = System.currentTimeMillis() + timeout;
		
		while ((c != stopchar) && (System.currentTimeMillis()<endTimeMillis)) {
			int b=0;
			if (BufReader.ready()) b = BufReader.read(chararray, pos, 1);
			if (b == 1) {
				if (chararray[pos]== (char) 65533) chararray[pos]=42; // Workaround for Autostar Degree-sign
				c = chararray[pos];
				pos++;
			} 
		}
		if (c != stopchar) throw new IOException("Timeout");
		
		ret = String.copyValueOf(chararray);

		return ret;
	}

	@Override
	public synchronized String read(int bytes) throws IOException {
		char[] chararray = new char[255];
		String ret = null;
		
		//Try to read num bytes or until a timeout occurs

		int pos = 0;
		long endTimeMillis = System.currentTimeMillis() + timeout;
		
		while ((pos != bytes) && (System.currentTimeMillis()<endTimeMillis)) {
			int b=0;
			if (BufReader.ready()) b = BufReader.read(chararray, pos, 1);
			if (b == 1) pos++;
		}
		if (pos != bytes) throw new IOException("Timeout");
		
		ret = String.copyValueOf(chararray);
		ret = ret.trim();
		
		return ret;
	}
	
	@Override
	public synchronized void emptyBuffer() throws IOException {
		//while (BufReader.ready()) {
	//		int b = BufReader.read();
		//}
			
	}

}
