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
    private int timeout = 0;
 
    public serial() {
    	
    }
    
	@Override
    public void connect(String device) throws IOException {
		// TODO Open serial device and construct Readers;
		
	}

	@Override
	public void disconnect() {
		// TODO Close serial device and in/outstreams
	}

	/**
	 * Send a String to the device
	 * @param command: String
	 */
	@Override
	public synchronized void sendCommand(String command) throws IOException {
		byte[] buffer=command.getBytes();
		if (OutStream != null) {
			OutStream.write(buffer); 
		} else {
			throw new IOException("Serial [sendCommand]: OutputStream closed");
		}
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

		// Set timeout 
		long endTimeMillis = System.currentTimeMillis() + timeout;
		
		int pos = 0;
		
		//Try to read until stopchar is detected or a timeout occurs
		
		while ((System.currentTimeMillis()<= endTimeMillis) && (c != stopchar)) {
			//FIXME: ready() does consume way too much CPU time
			if	(BufReader.ready()) {
				int b = BufReader.read();
				if (b != -1) {
					if (b==65533) b=42; // Workaround for Autostar Degree-sign
					chararray[pos]=(char) b;
					pos++;
					c = (char) b;
				} else throw new IOException ("Serial [read(char stopchar)]: InputStream closed");
			}
			
		}
		
		// Catch timeout and throw execption
		if ((pos==0) || (chararray[pos-1] != stopchar)) {
			throw new IOException ("Serial [read(char stopchar)]: timeout");
			
		} else {
			// Construct String from chararray
			ret = String.copyValueOf(chararray);
		}

		return ret;
	}

	@Override
	public synchronized String read(int bytes) throws IOException {
		char[] chararray = new char[255];
		String ret = null;

		// Set timeout 
		long endTimeMillis = System.currentTimeMillis() + timeout;
		
		int pos = 0;
		
		//Try to read num bytes or until a timeout occurs

		while ((pos != bytes) && (System.currentTimeMillis()<= endTimeMillis)) { 
			//FIXME: ready() does consume way too much CPU time
			if	(BufReader.ready()) {
				int b = BufReader.read();
				if (b != -1) {
					chararray[pos]=(char) b;
					pos++;
				} else throw new IOException ("Serial [read(int bytes)]: InputStream closed");
			}
		}

		// Catch timeout and throw execption
		if ((pos==0) || (pos < bytes)) {
			throw new IOException ("Serial [read(int bytes)]: timeout");
		} else {
			// Construct String from chararray
			ret = String.copyValueOf(chararray);
			ret = ret.trim();
		}

		return ret;
	}

	@Override
	public synchronized String read() throws IOException {
		char[] chararray = new char[255];
		String ret = null;

		// Set timeout 
		long endTimeMillis = System.currentTimeMillis() + timeout;

		int pos = 0;

		//Try to read until timeout occurs
		while ((System.currentTimeMillis()<= endTimeMillis)) {
			//FIXME: ready() does consume way too much CPU time
			if	(BufReader.ready()) {
				int b = BufReader.read();
				if (b != -1) {
					chararray[pos]=(char) b;
					pos++;

				} else throw new IOException ("Serial [read()]: InputStream closed");
			}

		}
		
		// Catch empty String
		if (pos==0) throw new IOException ("Serial [read()]: Reader not ready");
		
		// Construct String from chararray
		ret = String.copyValueOf(chararray);

		return ret;
	}
	
	
	@Override
	public synchronized void emptyBuffer() throws IOException {
		//FIXME: ready() does consume way too much CPU time
		while (BufReader.ready()) {
			int b=0;
			b = BufReader.read();
		}
			
	}

}
