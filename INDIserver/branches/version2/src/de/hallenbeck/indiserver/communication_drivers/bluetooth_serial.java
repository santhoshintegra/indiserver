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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;



import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Looper;

/**
 * Bluetooth-Serial-Port-Profile driver for short-range wireless connection to telescope
 * @author atuschen
 *
 */

public class bluetooth_serial extends serial {

	private BluetoothAdapter btAdapter;
	private BluetoothSocket btSocket;
    private BluetoothDevice btDevice;
    private static final String DriverName="bluetooth_serial";
	private static final int majorVersion=0;
	private static final int minorVersion=1;
    
    public bluetooth_serial(Context context) {
    	super(context);
    	
    	// Get the default Bluetooth Adapter
    	btAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    
    /**
     * Connect to a bluetooth-device
     * @param device: String containing the device-address 
     */
    @Override
	protected void onConnect(String device) throws IOException {
		
		if (btAdapter == null) throw new IOException("Bluetooth not available"); 

		

		try {
		
			btDevice = btAdapter.getRemoteDevice(device);
			
			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice

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

			// Construct Readers
			InReader = new InputStreamReader(InStream);
			BufReader = new BufferedReader (InReader);

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
		}
		
	}

	/**
	 * Disconnect from bluetooth-device
	 */
    @Override
	protected void onDisconnect() {
		try {
			BufReader.close();
			InReader.close();
			InStream.close();
			OutStream.close();
			btSocket.close();
			super.onDisconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    @Override
	public String getVersion() {
		return majorVersion+"."+minorVersion;
	}

	@Override
	public String getName() {
		return DriverName;
	}
}
