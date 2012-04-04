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
import java.io.InputStreamReader;
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
    @Override
	public void connect(String device) throws IOException {

		btAdapter = BluetoothAdapter.getDefaultAdapter();

		if (btAdapter == null) throw new IOException("Bluetooth not available"); 

		btDevice = btAdapter.getRemoteDevice(device);

		try {
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
		super.connect(device);
	}

	/**
	 * Disconnect from bluetooth-device
	 */
    @Override
	public void disconnect() {
		try {
			btSocket.close();
			super.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
