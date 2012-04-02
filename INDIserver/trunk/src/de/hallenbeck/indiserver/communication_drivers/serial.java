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
     * TimerThread for timeout on blocking operations
     * BufferedReader.ready() uses too much CPU power (about 50% on my Archos)
     * Thread uses 50ms steps: A timeout value of 100 yields 5 seconds.
     *
     * FIXME: This Thread _never_ terminates!
     * @author atuschen
     *
     */
    private class TimerThread extends Thread {
    	private boolean StopTimerThread=false;
    	private boolean jobDone;
    	public TimerThread() {
    		start();
    	}
    	public void run() {
    		while (!StopTimerThread) {
    			try {
    				synchronized(this) {
    					wait();
    				}
    				
    				jobDone=false;
    				int i=0;
    				while ((!jobDone) && (i<timeout)) {
    					sleep (50);
    					i++;
    				}
    				
    				if (!jobDone) {
    	    			BufReader.close();
    	    		}
    			} catch (InterruptedException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}	
    		}
    	}
    	
    	public void stoptimer() {
    		jobDone=true;
    	}
    	
    	public void stopTread() {
    		jobDone=true;
    		StopTimerThread=true;
    		synchronized (this) {
    			notify();
    		}
    	}
    }
    
    TimerThread Timer = new TimerThread();
    
    // Start the Timer
    private void TimerStart() {
    	synchronized (Timer) {
    	Timer.notify();
    	}
    }
    
    // Stop the Timer 
    private void TimerStop() {
    	Timer.stoptimer();
    }

    
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
		boolean run = true;
		int pos = 0;
		TimerStart();
		while ((c != stopchar) && (run)) {
			int b = BufReader.read();
			if (b != -1) {
				if (b==65533) b=42; // Workaround for Autostar Degree-sign
				chararray[pos]=(char) b;
				pos++;
				c = (char) b;
			} else run=false;
		}
		TimerStop();
		ret = String.copyValueOf(chararray);

		return ret;
	}

	@Override
	public synchronized String read(int bytes) throws IOException {
		char[] chararray = new char[255];
		String ret = null;
		
		//Try to read num bytes or until a timeout occurs
		boolean run = true;
		int pos = 0;
		TimerStart();
		while ((pos != bytes) && (run)) { 
			int b = BufReader.read();
			if (b != -1) {
				chararray[pos]=(char) b;
				pos++;
			} else run=false;
		}
		TimerStop();
		
		ret = String.copyValueOf(chararray);
		ret = ret.trim();
		
		return ret;
	}
	
	@Override
	public synchronized void emptyBuffer() throws IOException {
		while (BufReader.ready()) {
			int b = BufReader.read();
		}
			
	}

}
