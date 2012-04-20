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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;

/**
 * PL2303 USB Serial Adapter Driver for
 * devices with android usbhost-support (3.2 upwards)
 * Based on pl2303.c from linux sources:
 * http://lxr.free-electrons.com/source/drivers/usb/serial/pl2303.c
 * 
 * Supports PL2303 and newer PL2303HX-types (both tested)
 * 
 * TODO: support multiple adaptors, implement RTS/CTS FlowControl, add support for DCD/DTR  
 * 
 * @author atuschen75 at gmail dot com
 *
 */
public class usbhost_serial_pl2303 {

	private Context AppContext; 
	private UsbManager mUsbManager;
	private UsbDevice mDevice;
	private UsbDeviceConnection mConnection;
	private UsbInterface intf;
	private UsbEndpoint ep1;
	private UsbEndpoint ep2;
	private ArrayList<UsbDevice> pl2303ArrayList = new ArrayList<UsbDevice>();
	
	// Callback method after permission to device was granted
	private PL2303callback pl2303Callback; 	

	// Type 0 = PL2303, type 1 = PL2303HX
	private int PL2303type = 0; 			

	public enum BaudRate {
		B0, 
		B75,
		B150,
		B300,
		B600,
		B1200,
		B1800,
		B2400,
		B4800,
		B9600,
		B19200,
		B38400,
		B57600,
		B115200,
		B230400,
		B460800,
		B614400,
		B921600,
		B1228800,
		B2457600,
		B3000000,
		B6000000
	};

	public enum DataBits {
		D5,
		D6,
		D7,
		D8
	};

	public enum StopBits {
		S1,
		S2
	};

	public enum Parity {
		NONE,
		ODD,
		EVEN
	};

	// USB control commands
	private static final int SET_LINE_REQUEST_TYPE = 0x21;
	private static final int SET_LINE_REQUEST = 0x20;
	private static final int BREAK_REQUEST_TYPE = 0x21;
	private static final int BREAK_REQUEST = 0x23;	
	private static final int BREAK_OFF = 0x0000;
	private static final int GET_LINE_REQUEST_TYPE = 0xa1;
	private static final int GET_LINE_REQUEST = 0x21;
	private static final int VENDOR_WRITE_REQUEST_TYPE = 0x40;
	private static final int VENDOR_WRITE_REQUEST = 0x01;
	private static final int VENDOR_READ_REQUEST_TYPE = 0xc0;
	private static final int VENDOR_READ_REQUEST = 0x01;

	// Action for PendingIntent
	private static final String ACTION_USB_PERMISSION 	=   "com.android.hardware.USB_PERMISSION";

	// BraodcastReceiver for permission to use USB-Device
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if ((intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) && (device != null)) {
						Log.d("BroadcastReceiver", "Permission granted for device " + device.getDeviceName());
						if (initalize(device)){
							// Callback
							Log.d("initialize", "Device successfully initialized");
							pl2303Callback.pl2303_ConnectSuccess();
						} else {
							Log.d("initialize", "Device initialization failed");
							pl2303Callback.pl2303_ConnectFailed("Device initialization failed");
							close();
						}
					} 
					else {
						mDevice = null;
						Log.d("BroadcastReceiver", "Permission denied for device " + device.getDeviceName());
						pl2303Callback.pl2303_ConnectFailed("Permission denied");
					}
				}
			}
		}
	};

	/**
	 * Constructor
	 * @param context Application Context
	 * @param PL2303callback Object which implements the callback methods
	 */
	public usbhost_serial_pl2303(Context context, PL2303callback callback) {
		Log.d("constructor", "PL2303 driver starting");
		AppContext = context;
		pl2303Callback = callback;
		mUsbManager = (UsbManager) AppContext.getSystemService(Context.USB_SERVICE);

		// Register BroadcastReceiver for Permission Intent
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		AppContext.registerReceiver(mUsbReceiver, filter);
	}
	
	/**
	 * Get a list of pl2303 devices currently connected. Must be called before open().
	 * @return ArrayList<UsbDevice> 
	 */
	public ArrayList<UsbDevice> getDeviceList() {
		pl2303ArrayList.clear();
		
		// Get the USB device list (of all devices)
		HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
				
		// Scan the devices and copy all PL2303-Adaptors into the pl2303ArrayList
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		while(deviceIterator.hasNext()){
			UsbDevice device = deviceIterator.next();
			if ((device.getProductId()==0x2303) && (device.getVendorId()==0x067b)) {
			pl2303ArrayList.add(device); }
		}
		Log.d("getDeviceList", pl2303ArrayList.size()+" device(s) found");
 
		return pl2303ArrayList;
	}
	
	/**
	 * Open connection to a device (after getDeviceList() has been called)
	 * @param UsbDevice
	 * @throws IOException if device is not PL2303 or was not in the original getDeviceList()
	 */
	public void open(UsbDevice device) throws IOException {
		if ((device.getProductId()!=0x2303) && (device.getVendorId()!=0x067b)) throw new IOException("Not a PL2303-device");
		if (!pl2303ArrayList.contains(device)) throw new IOException("Device not in original list"); 
		
		PendingIntent mPermissionIntent;
		mPermissionIntent = PendingIntent.getBroadcast(AppContext, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_ONE_SHOT);

		// Request the permission to use the device
		mUsbManager.requestPermission(device, mPermissionIntent);
		Log.d("open", "Requesting permission to use " + device.getDeviceName());
	}
		
	/**
	 * open the USB-connection and initialize the PL2303
	 * @return true on success
	 */
	private boolean initalize(UsbDevice device) {
		mDevice = device;
		Log.d("initialize", "Device Name: "+mDevice.getDeviceName());
		Log.d("initialize", "VendorID: "+mDevice.getVendorId());
		Log.d("initialize", "ProductID: "+mDevice.getProductId());
		
		intf = mDevice.getInterface(0);
		if (intf == null) return false;
		Log.d("initialize", "Got interface");
		
		ep1 = intf.getEndpoint(1); //endpoint addr 0x2 = output bulk
		if ((ep1.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) || (ep1.getDirection() != UsbConstants.USB_DIR_OUT)) return false;
		Log.d("initialize", "Got output endpoint");
		
		ep2 = intf.getEndpoint(2); //endpoint addr 0x83 = input bulk
		if ((ep2.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) || (ep2.getDirection() != UsbConstants.USB_DIR_IN)) return false;
		Log.d("initialize", "Got input endpoint");
		
		UsbDeviceConnection connection = mUsbManager.openDevice(mDevice);
		if (connection == null) return false;
		Log.d("initialize", "Got connection");
		
		if (!connection.claimInterface(intf, true)) return false;
		Log.d("initialize", "Claimed exclusive interface access");
		
		mConnection = connection;

		if (mConnection.getRawDescriptors()[7] == 64) PL2303type = 1; //Type 1 = PL2303HX
		Log.d("initialize", "PL2303 Type: " +PL2303type+ " detected");		
		
		// Initialization of PL2303 according to linux pl2303.c driver
		byte[] buffer = new byte[1];
		mConnection.controlTransfer(VENDOR_READ_REQUEST_TYPE, VENDOR_READ_REQUEST, 0x8484, 0, buffer, 1, 100);
		mConnection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 0x0404, 0, null, 0, 100);
		mConnection.controlTransfer(VENDOR_READ_REQUEST_TYPE, VENDOR_READ_REQUEST, 0x8484, 0, buffer, 1, 100);
		mConnection.controlTransfer(VENDOR_READ_REQUEST_TYPE, VENDOR_READ_REQUEST, 0x8383, 0, buffer, 1, 100);
		mConnection.controlTransfer(VENDOR_READ_REQUEST_TYPE, VENDOR_READ_REQUEST, 0x8484, 0, buffer, 1, 100);
		mConnection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 0x0404, 1, null, 0, 100);
		mConnection.controlTransfer(VENDOR_READ_REQUEST_TYPE, VENDOR_READ_REQUEST, 0x8484, 0, buffer, 1, 100);
		mConnection.controlTransfer(VENDOR_READ_REQUEST_TYPE, VENDOR_READ_REQUEST, 0x8383, 0, buffer, 1, 100);
		mConnection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 0, 1, null, 0, 100);
		mConnection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 1, 0, null, 0, 100);
		if (PL2303type == 1) mConnection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 2, 0x44, null, 0, 100);
		else mConnection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 2, 0x24, null, 0, 100);

		return true;
	}

	/**
	 *  Close the connection
	 */
	public void close() {
		if (mConnection != null) {
			mConnection.releaseInterface(intf);
			mConnection.close();
			mConnection = null;
			mDevice = null;
			ep1 = null;
			ep2 = null;
			Log.d("close", "Device closed");
		}
	}

	/**
	 * Are we connected?
	 * @return true on connection
	 */
	public boolean isConnected() {
		if (mConnection != null) return true;
		else return false;
	}

	/**
	 *  Setup basic communication parameters according to linux pl2303.c driver 
	 * @param Enum BaudRate 
	 * @param Enum DataBits
	 * @param Enum StopBits
	 * @param Enum Parity
	 * @throws IOException if settings not supported or connection closed
	 */
	public void setup(BaudRate R, DataBits D, StopBits S, Parity P) throws IOException {
		byte[] oldSettings = new byte[7];
		byte[] buffer = new byte[7];

		if (mConnection == null) throw new IOException("Connection closed");

		// Get current settings
		mConnection.controlTransfer(GET_LINE_REQUEST_TYPE, GET_LINE_REQUEST, 0, 0, oldSettings, 7, 100);
		mConnection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 0, 1, null, 0, 100);
		Log.d("setup", "Current serial configuration:" + oldSettings[0] + oldSettings[1] + oldSettings[2] + oldSettings[3] + oldSettings[4] + oldSettings[5] + oldSettings[6]);
		
		buffer = oldSettings;

		// Setup Baudrate
		int baud;
		switch (R) {
		case B0: baud = 0; break;
		case B75: baud = 75; break;
		case B150: baud = 150; break;
		case B300: baud = 300; break;
		case B600: baud = 600; break;
		case B1200: baud = 1200; break;
		case B1800: baud = 1800; break;
		case B2400: baud = 2400; break;
		case B4800: baud = 4800; break;
		case B9600: baud = 9600; break;
		case B19200: baud = 19200; break;
		case B38400: baud = 38400; break;
		case B57600: baud = 57600; break;
		case B115200: baud = 115200; break;
		case B230400: baud = 230400; break;
		case B460800: baud = 460800; break;
		case B614400: baud = 614400; break;
		case B921600: baud = 921600; break;
		case B1228800: baud = 1228800; break;
		// The following rates only for HX-Type of PL2303
		case B2457600: baud = 2457600; break;
		case B3000000: baud = 3000000; break;
		case B6000000: baud = 6000000; break;
		default: throw new IOException("Baudrate not supported");
		}

		if  ((baud > 1228800) && (PL2303type == 0)) throw new IOException("Baudrate not supported"); // Only PL2303HX supports higher baudrates   

		buffer[0]=(byte) (baud & 0xff);
		buffer[1]=(byte) ((baud >> 8) & 0xff);
		buffer[2]=(byte) ((baud >> 16) & 0xff);
		buffer[3]=(byte) ((baud >> 24) & 0xff);

		// Setup Stopbits
		switch (S) {
		case S1: buffer[4] = 0; break;
		case S2: buffer[4] = 2; break;
		default: throw new IOException("Stopbit setting not supported"); 
		}

		// Setup Parity
		switch (P) {
		case NONE: buffer[5] = 0; break;
		case ODD: buffer[5] = 1; break;
		case EVEN: buffer[5] = 2; break;
		default: throw new IOException("Parity setting not supported"); 
		}

		// Setup Databits
		switch (D) {
		case D5: buffer[6] = 5; break;
		case D6: buffer[6] = 6; break;
		case D7: buffer[6] = 7; break;
		case D8: buffer[6] = 8; break;
		default: throw new IOException("Databit setting not supported");
		}

		// Set new configuration on PL2303
		mConnection.controlTransfer(SET_LINE_REQUEST_TYPE, SET_LINE_REQUEST, 0, 0, buffer, 7, 100); 
		Log.d("setup", "New serial configuration:" + buffer[0] + buffer[1] + buffer[2] + buffer[3] + buffer[4] + buffer[5] + buffer[6]);
		
		// Disable BreakControl
		mConnection.controlTransfer(BREAK_REQUEST_TYPE, BREAK_REQUEST, BREAK_OFF, 0, null, 0, 100);

		// Disable FlowControl
		//mConnection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 0, 0, null, 0, 100);

		//TODO: implement RTS/CTS FlowControl
	}

	/** 
	 * Get the InputStream of the connection. 
	 * Supported functions:
	 * 
	 * read() - blocking read (only one byte) 
	 * read(byte[], int, int) - non-blocking read (any size)
	 * 
	 * @return InputStream if connected else null
	 */
	public InputStream getInputStream() {
		if (mConnection != null) {
			InputStream in = new InputStream() {

				// Blocking read
				@Override
				public int read() throws IOException {
					synchronized (this) {
						int retVal= -1;
						if (mConnection == null) throw new IOException("Connection closed");
						ByteBuffer readBuffer = ByteBuffer.allocate(1);
						UsbRequest request = new UsbRequest();
						if (!request.initialize(mConnection, ep2)) throw new IOException("ReadRequest.initailize() failed");
						if (!request.queue(readBuffer, 1)) throw new IOException("ReadRequest.queue() failed");
						UsbRequest retRequest = mConnection.requestWait();
						if (retRequest == null) throw new IOException("ReadRequest failed");
						if (retRequest == request) {
							retVal = readBuffer.get();
						} 
						return retVal;
					}
				}

				// Non-blocking read
				@Override
				public int read(byte[] buffer, int offset, int length) throws IOException, IndexOutOfBoundsException {
					synchronized (this) {
						if ((offset < 0) || (length < 0) || ((offset + length) > buffer.length)) throw new IndexOutOfBoundsException();
						if (mConnection == null) throw new IOException("Connection closed");
						byte [] readBuffer = new byte[length];
						int bytesRead = mConnection.bulkTransfer(ep2, readBuffer, length, 100);
						if (bytesRead > 0) System.arraycopy(readBuffer, 0, buffer, offset, bytesRead);
						return bytesRead;	
					}
				}
			};
			return in;
		} else return null;
	}

	/** 
	 * Get the OutputStream of the connection. 
	 * Supported functions:
	 * 
	 * public void write(int) - blocking write (only one byte) 
	 * public void write(byte[], int, int) - non-blocking write (any size)
	 * 
	 * @return OutputStream if connected else null
	 */
	public OutputStream getOutputStream() {
		if (mConnection != null) {
			OutputStream out = new OutputStream() {

				// Blocking write
				@Override 
				public void write(int oneByte) throws IOException{
					synchronized (this) {
						if (mConnection == null) throw new IOException("Connection closed");
						ByteBuffer writeBuffer = ByteBuffer.allocate(1);
						UsbRequest request = new UsbRequest();
						if (!request.initialize(mConnection, ep1)) throw new IOException("WriteRequest.initailize() failed");
						if (!request.queue(writeBuffer, 1)) throw new IOException("WriteRequest.queue() failed");
						UsbRequest retRequest = mConnection.requestWait();
						if (retRequest == null) throw new IOException("WriteRequest failed");
					}
				}

				// Non-blocking write
				@Override
				public void write (byte[] buffer, int offset, int count) throws IOException, IndexOutOfBoundsException {
					synchronized (this) {
						if ((offset < 0) || (count < 0) || ((offset + count) > buffer.length)) throw new IndexOutOfBoundsException();
						if (mConnection == null) throw new IOException("Connection closed");
						byte [] writeBuffer = new byte[count];
						System.arraycopy(buffer, offset, writeBuffer, 0, count);
						int bytesWritten = mConnection.bulkTransfer(ep1, writeBuffer, count, 100);
						if (bytesWritten != count) throw new IOException ("BulkWrite failed - count: "+count+" written: "+bytesWritten);
					}
				}
			};
			return out;
		} else return null;
	} 
}
