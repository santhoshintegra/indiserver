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
import java.io.PrintWriter;
import java.util.ArrayList;

import de.hallenbeck.indiserver.communication_drivers.PL2303driver.BaudRate;
import de.hallenbeck.indiserver.communication_drivers.PL2303driver.DataBits;
import de.hallenbeck.indiserver.communication_drivers.PL2303driver.FlowControl;
import de.hallenbeck.indiserver.communication_drivers.PL2303driver.Parity;
import de.hallenbeck.indiserver.communication_drivers.PL2303driver.StopBits;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.util.Log;


/**
 * Example of a class representing a PL2303 Serial-to-USB converter
 * 
 * @author atuschen
 *
 */
public class PL2303device implements PL2303callback, Runnable {
	private PL2303driver pl2303;
	private ArrayList<UsbDevice> deviceList;
	private BufferedReader reader;
	private PrintWriter writer;
	private boolean connected = false;
	
	PL2303device(Context context) {
		pl2303 = new PL2303driver(context, this);
		deviceList = pl2303.getDeviceList();
		
		try {
			pl2303.open(deviceList.get(0));
		} catch (PL2303Exception e) {
			Log.e("PL2303device","Error: " + e.getMessage());
		}
	}
	
	@Override
	public void onInitSuccess(String devicename) {
		try {
			pl2303.setup(BaudRate.B9600, DataBits.D8, StopBits.S1, Parity.NONE, FlowControl.RTSCTS);
		} catch (PL2303Exception e) {
			Log.e("PL2303device","Error: " + e.getMessage());
		}
	}

	@Override
	public void onInitFailed(String reason) {
		Log.e("PL2303device","Error: " + reason);
	}

	@Override
	public void onRI(boolean state) {
		if (state) {
			Log.i("PL2303device","RING detected");
	 
		}
	}

	@Override
	public void onDCD(boolean state) {
		if (state) {
			Log.i("PL2303device","Carrier detected");
			
		} else {
			Log.i("PL2303device","Carrier lost");
	
		}
	}

	@Override
	public void onDSR(boolean state) {
		if (state) {
			Log.i("PL2303device","Modem ready");
			if (!connected) {
				connected = true;
				reader = new BufferedReader( new InputStreamReader( pl2303.getInputStream() ) );
				writer = new PrintWriter( pl2303.getOutputStream() );
				Thread t = new Thread(this);
				t.start();
			}
		} else {
			Log.i("PL2303device","Modem off");
			if (connected) {
				connected = false;
				writer.close();
				try {
					reader.close();
				} catch (IOException e) {
				}

			}
		}
	}

	@Override
	public void onCTS(boolean state) {
		if (state) Log.i("PL2303device","We are clear to send");
		else Log.i("PL2303device","Stop sending data!");
	}

	
	@Override
	public void run() {
		while (connected) {
			try {
				reader.read();
			} catch (IOException e) {
			}
		}
		
	}

	@Override
	public void onDeviceDetached(String devicename) {
		// TODO Auto-generated method stub
		
	}
	
	

}
