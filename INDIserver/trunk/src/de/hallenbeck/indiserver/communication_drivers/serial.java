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
    private int sleeptime = 0;
    private int timeout = 0;
 
    public serial() {
    	
    }
    
    
	public void set_delay(int delay) {
		sleeptime = delay;
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
	public void sendCommand(String command) throws IOException {
		byte[] buffer=command.getBytes();
		if (OutStream != null) {
			OutStream.write(buffer); 
		} else {
			throw new IOException("Not connected");
		}
	}

	@Override
	public int getAnswerInt() {
		return 0;
	}

	/**
	 * Read from the device
	 * @return String
	 */
	@Override
	public String getAnswerString() throws IOException {
		if (InStream !=null) {
			Thread rThread = new recvThread();
			String tmp = ((recvThread) rThread).read();
			return tmp;
		} else {
			throw new IOException("Not connected");
		}
	}
	
	/**
	 * Receive-Thread only necessary because some devices (i.e. Autostar) need time to answer
	 * Therefore the sleep() instruction.
	 * TODO: complete rewrite! This code is crap! Use InputStreamReader/BufferedReader
	 * @author atuschen
	 *
	 */
	private class recvThread extends Thread {
		
		public recvThread() {
			
		}
		
		public String read() {
			try {
				sleep(sleeptime);
			} catch (InterruptedException e) {
			
				e.printStackTrace();
			}
			int len=0;
	       	byte[] rcvbuffer = new byte[1024];
	       	
	        try {
				len=InStream.read(rcvbuffer);

			} catch (IOException e) {
				e.printStackTrace();
			}
	        
	        //WORKAROUND FOR AUTOSTAR "Degree-Sign" (byte-value: -33)
	        for (int i = 0; i <= len-1; i++) {
	        	if (rcvbuffer[i]==-33) rcvbuffer[i]=42;
	        }
	        
	        String tmp=new String(rcvbuffer,0,len);
			return tmp;
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
	public String read(char stopchar) throws IOException {
		char c = (char) 255 ;
		char[] chararray = new char[255];
		String ret = null;

		// Set timeout 
		long endTimeMillis = System.currentTimeMillis() + timeout;
		
		int pos = 0;
		
		//Try to read until stopchar is detected or a timeout occurs
		//Ignore End of stream!
		while ((System.currentTimeMillis()<= endTimeMillis) && (c != stopchar)) {
			if	(BufReader.ready()) {
				int b = BufReader.read();
				if (b != -1) {
					if (b==65533) b=42;
					chararray[pos]=(char) b;
					pos++;
					c = (char) b;
				}
			}
			
		}

		// Catch timeout and throw execption
		if (chararray[pos-1] != stopchar) {
			throw new IOException ("Read timeout");
		} else {
			// Construct String from chararray
			ret = String.copyValueOf(chararray);
			ret=ret.trim();
		}

		return ret;
	}

	@Override
	public String read(int bytes) throws IOException {
		char[] chararray = new char[255];
		String ret = null;

		// Set timeout 
		long endTimeMillis = System.currentTimeMillis() + timeout;
		
		int pos = 0;
		
		//Try to read until stopchar is detected or a timeout occurs
		//Ignore End of stream!
		while ((pos != bytes) && (System.currentTimeMillis()<= endTimeMillis)) { 
			if	(BufReader.ready()) {
				int b = BufReader.read();
				if (b != -1) {
					chararray[pos]=(char) b;
					pos++;
				}
			}
		}

		// Catch timeout and throw execption
		if (pos < bytes) {
			throw new IOException ("Read timeout");
		} else {
			// Construct String from chararray
			ret = String.copyValueOf(chararray);
		}

		return ret;
	}

	@Override
	public String read() throws IOException {
		char[] chararray = new char[255];
		String ret = null;

		// Set timeout 
		long endTimeMillis = System.currentTimeMillis() + timeout;

		int pos = 0;

		//Try to read until timeout occurs
		//Ignore End of stream!
		while ((System.currentTimeMillis()<= endTimeMillis)) {
			if	(BufReader.ready()) {
				int b = BufReader.read();
				if (b != -1) {
					chararray[pos]=(char) b;
					pos++;

				}
			}

		}

		// Construct String from chararray
		ret = String.copyValueOf(chararray);

		return ret;
	}

}
