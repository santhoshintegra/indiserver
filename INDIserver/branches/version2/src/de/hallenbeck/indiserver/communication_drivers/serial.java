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
import java.util.ArrayList;

import de.hallenbeck.indiserver.communication_drivers.usbhost_serial_pl2303.BaudRate;
import de.hallenbeck.indiserver.communication_drivers.usbhost_serial_pl2303.DataBits;
import de.hallenbeck.indiserver.communication_drivers.usbhost_serial_pl2303.FlowControl;
import de.hallenbeck.indiserver.communication_drivers.usbhost_serial_pl2303.Parity;
import de.hallenbeck.indiserver.communication_drivers.usbhost_serial_pl2303.StopBits;

import android.content.Context;
import android.hardware.usb.UsbDevice;

/**
 * Serial communication driver for devices with /dev/ttyS or /dev/ttyUSB support
 * Base class for all other communication drivers as they all use serial communication
 * @author atuschen
 *
 */

public class serial extends communication_driver implements PL2303callback {

	protected InputStream InStream;
	protected OutputStream OutStream;
	protected InputStreamReader InReader;
	protected BufferedReader BufReader;
	private static final String DriverName="serial";
	private static final int majorVersion=0;
	private static final int minorVersion=1;
	private usbhost_serial_pl2303 pl2303;
	private Context AppContext;
	//public boolean connected = false;

	public serial(Context context) {
		AppContext = context;
		pl2303 = new usbhost_serial_pl2303(AppContext, this);
	}

	@Override
	protected void onConnect(String device) throws IOException {
		ArrayList<UsbDevice> dev = pl2303.getDeviceList();
		pl2303.open(dev.get(0));
	}

	@Override
	protected void onDisconnect() {
		pl2303.close();
	}


	@Override
	protected void onWrite(String data) throws IOException {
		byte[] buffer=data.getBytes();
		OutStream.write(buffer);
	}

	@Override
	protected void onWrite(byte data) throws IOException {


	}

	@Override
	protected synchronized String onRead(char stopchar) throws IOException {
		char c = (char) 255 ;
		char[] chararray = new char[255];
		String ret = null;

		//Try to read until stopchar is detected or a timeout occurs

		int pos = 0;

		long endTimeMillis = System.currentTimeMillis() + Timeout;

		while ((c != stopchar) && (System.currentTimeMillis()<endTimeMillis)) {
			int b = BufReader.read(chararray, pos, 1);
			if (b == 1) {
				if (chararray[pos]== (char) 65533) chararray[pos]=42; // Workaround for Autostar Degree-sign
				c = chararray[pos];
				pos++;
			} 
		}
		if (c != stopchar) throw new IOException("Timeout");

		ret = String.copyValueOf(chararray);

		return ret;
	}

	@Override
	protected synchronized String onRead(int len) throws IOException {
		char[] chararray = new char[255];
		String ret = null;

		//Try to read num bytes or until a timeout occurs

		int pos = 0;
		long endTimeMillis = System.currentTimeMillis() + Timeout;

		while ((pos != len) && (System.currentTimeMillis()<endTimeMillis)) {
			int b = BufReader.read(chararray, pos, 1);
			if (b == 1) pos++;
		}
		if (pos != len) throw new IOException("Timeout");

		ret = String.copyValueOf(chararray);
		ret = ret.trim();

		return ret;
	}

	@Override
	public String getVersion() {
		return majorVersion+"."+minorVersion;
	}

	@Override
	public String getName() {
		return DriverName;
	}

	@Override
	public void onInitSuccess() {
		try {
			pl2303.setup(BaudRate.B9600,DataBits.D8, StopBits.S1, Parity.NONE, FlowControl.RTSCTS);
			InStream = pl2303.getInputStream();
			OutStream = pl2303.getOutputStream();
			// Construct Readers
			InReader = new InputStreamReader(InStream);
			BufReader = new BufferedReader (InReader);
			connected = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onInitFailed(String reason) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onRI(boolean state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDCD(boolean state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDSR(boolean state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCTS(boolean state) {
		// TODO Auto-generated method stub
		
	}
}