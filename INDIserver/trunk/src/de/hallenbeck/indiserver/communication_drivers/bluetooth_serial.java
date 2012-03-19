package de.hallenbeck.indiserver.communication_drivers;

import java.io.IOException;
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
    
    
    /**
     * Connect to a bluetooth-device
     * @param device: String containing the device-address 
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
            InStream = btSocket.getInputStream();
            OutStream = btSocket.getOutputStream();
            
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
	public void disconnect() {
		try {
			InStream.close();
			OutStream.close();
			btSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
