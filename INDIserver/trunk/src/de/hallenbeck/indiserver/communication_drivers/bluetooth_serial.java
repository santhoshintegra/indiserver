package de.hallenbeck.indiserver.communication_drivers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

/**
 * Bluetooth-Serial-Port-Profile driver for short-range wireless connection to telescope
 * @author atuschen
 *
 */

public class bluetooth_serial extends serial implements communication_driver_interface {

	private BluetoothAdapter btAdapter;
	private BluetoothSocket btSocket;
    private BluetoothDevice btDevice;
    private InputStream btInStream;
    private OutputStream btOutStream;
    
    /**
     * Connect to a bluetooth-device
     * @param device: String containing the the device-address 
     */
	public int connect(String device) {
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		btDevice = btAdapter.getRemoteDevice(device);
        
        
        // Get a BluetoothSocket for a connection with the
        // given BluetoothDevice
        try {
        	// WORKAROUND, since no connection was possible on my Archos-Devices
        	// with device.createRfcommSocketToServiceRecord(MY_UUID);
        	
        	Method m = btDevice.getClass().getMethod("createRfcommSocket",
               new Class[] { int.class });
            btSocket = (BluetoothSocket)m.invoke(btDevice, Integer.valueOf(1));
            
           // Always cancel discovery because it will slow down a connection
            btAdapter.cancelDiscovery();

            // This is a blocking call and will only return on a
            // successful connection or an exception
            btSocket.connect();
            // Get the BluetoothSocket input and output streams
            btInStream = btSocket.getInputStream();
            btOutStream = btSocket.getOutputStream();
  
        
        } catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
        
        
        
  
		return 0;
	}

	/**
	 * Disconnect from bluetooth-device
	 */
	public int disconnect() {
		try {
			btInStream.close();
			btOutStream.close();
			btSocket.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		return 0;
	}
	
	/**
	 * Send a String to the device
	 * @param command: String
	 */
	public int sendCommand(String command) {
		byte[] buffer=command.getBytes();
		try {
			btOutStream.write(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
				
		return 0;
	}
	
	/**
	 * Read from the device
	 * @return String
	 */
	public String getAnswerString() {
		int len=0;
       	byte[] rcvbuffer = new byte[1024];
       	
        try {
			len=btInStream.read(rcvbuffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
        String tmp=new String(rcvbuffer,0,len);
		return tmp;
	}


}
