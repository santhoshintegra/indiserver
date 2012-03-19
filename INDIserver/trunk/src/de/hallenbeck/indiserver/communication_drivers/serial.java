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

	public InputStream InStream;
    public OutputStream OutStream;
    private int sleeptime=0;
    
	public int connect(String device) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void disconnect() {
		// TODO Auto-generated method stub
	}

	/**
	 * Send a String to the device
	 * @param command: String
	 */
	public int sendCommand(String command) {
		byte[] buffer=command.getBytes();
		try {
			OutStream.write(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public int getAnswerInt() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Read from the device
	 * @return String
	 */
	public String getAnswerString() {
		Thread rThread = new recvThread();
		String tmp = ((recvThread) rThread).read();
		return tmp;
	}
	
	/**
	 * Receive-Thread only necessary because some devices (i.e. Autostar) need time to answer
	 * Therefore the sleep() instruction.
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
	        String tmp=new String(rcvbuffer,0,len);
			return tmp;
		}
	}


	public void set_delay(int delay) {
		sleeptime = delay;
		
	}

}
