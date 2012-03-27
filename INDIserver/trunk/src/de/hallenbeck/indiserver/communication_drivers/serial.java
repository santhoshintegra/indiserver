package de.hallenbeck.indiserver.communication_drivers;

import java.io.IOException;
import java.io.InputStream;
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
    private int sleeptime=0;
 
	public void set_delay(int delay) {
		sleeptime = delay;
	}
    
    public void connect(String device) throws IOException {
		// TODO Open serial device
		
	}

	public void disconnect() {
		// TODO Close serial device and in/outstreams
	}

	/**
	 * Send a String to the device
	 * @param command: String
	 */
	public void sendCommand(String command) throws IOException {
		byte[] buffer=command.getBytes();
		if (OutStream != null) {
			OutStream.write(buffer); 
		} else {
			throw new IOException("Not connected");
		}
	}

	public int getAnswerInt() {
		return 0;
	}

	/**
	 * Read from the device
	 * @return String
	 */
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

}
