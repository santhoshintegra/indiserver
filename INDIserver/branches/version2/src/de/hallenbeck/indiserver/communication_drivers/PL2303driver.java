/*
 *
 * This file is part of INDIserver.
 *
 * INDIserver is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation
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
 * PL2303 USB Serial Converter Driver for
 * devices with Android usbhost-support (3.2 upwards)
 * Based on pl2303.c from linux sources:
 * http://lxr.free-electrons.com/source/drivers/usb/serial/pl2303.c
 * 
 * Supports PL2303 and newer PL2303HX-types (both tested) 
 * with ProductID 0x2303 and VendorID 0x067b
 * 
 * TODO: support 3rd party USB IDs (see linux driver)
 * 
 * Supports basic RTS/CTS FlowControl 
 * 
 * TODO: add RFR/CTS, DTR/DSR and XON/XOFF FlowControl
 * 
 * @author atuschen75 at gmail dot com
 *
 */
public class PL2303driver implements Runnable {
	
	// ApplicationContext necessary because of UsbManager and PermissionIntent
	private Context mAppContext; 
	
	// All USB Classes
	private UsbManager mUsbManager;
	private UsbDevice mDevice;
	private UsbDeviceConnection mConnection;
	private UsbInterface mUSBIntf;
	private UsbEndpoint mEP0;
	private UsbEndpoint mEP1;
	private UsbEndpoint mEP2;
	
	// ArrayList of all PL2303 converters connected
	private ArrayList<UsbDevice> mPL2303ArrayList = new ArrayList<UsbDevice>();
	
	// Serial Port settings like Baudrate, see setup() 
	private byte[] mPortSetting = new byte[7]; 
	
	// Status of RTC/CTS FlowControl
	private FlowControl mFlow = FlowControl.OFF;
	
	// Status of DTR/RTS Lines
	private int mControlLines = 0;
	
	// Status of DSR/CTS/DCD/RI Lines
	private byte mStatusLines = 0;
	
	// Type 0 = PL2303, Type 1 = PL2303-HX
	private int mPL2303type = 0; 			

	// Callback Class interface
	private PL2303callback mPL2303Callback; 	
	
	public static enum BaudRate {
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

	public static enum DataBits {
		D5,
		D6,
		D7,
		D8
	};

	public static enum StopBits {
		S1,
		S2
	};

	public static enum Parity {
		NONE,
		ODD,
		EVEN
	};
	
	public static enum FlowControl {
		OFF,
		RTSCTS,
		RFRCTS,	// not yet implemented
		DTRDSR,	// not yet implemented
		XONXOFF	// not yet implemented
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
	private static final int SET_CONTROL_REQUEST_TYPE = 0x21;
	private static final int SET_CONTROL_REQUEST = 0x22;
	
	// RS232 Line constants
	private static final int CONTROL_DTR = 0x01;
	private static final int CONTROL_RTS = 0x02;
	private static final int UART_DCD = 0x01;
	private static final int UART_DSR = 0x02;
	private static final int UART_RING = 0x08;
	private static final int UART_CTS = 0x80;

	// Tag for Log.d function
	private static final String TAG = "pl2303";
	
	// Action for PendingIntent
	private static final String ACTION_USB_PERMISSION 	=   "com.android.hardware.USB_PERMISSION";

	/**
	 * BroadcastReceiver for permission to use USB-Device
	 * Called by the System after the user has denied/granted access to a USB-Device
	 */
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if ((intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) && (device != null)) {
						
						Log.d(TAG, "Permission granted for device " + device.getDeviceName());
						
						if (initalize(device)){
							
							Log.d(TAG, "Device successfully initialized");
							mPL2303Callback.onInitSuccess();
							
						} else {
							
							Log.d(TAG, "Device initialization failed");
							mPL2303Callback.onInitFailed("Device initialization failed");
							close();
						
						}
						
					} else {
						
						mDevice = null;
						Log.d(TAG, "Permission denied for device " + device.getDeviceName());
						mPL2303Callback.onInitFailed("Permission denied");
						
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
	public PL2303driver(Context context, PL2303callback callback) {
		Log.d(TAG, "PL2303 driver starting");
		mAppContext = context;
		mPL2303Callback = callback;
		mUsbManager = (UsbManager) mAppContext.getSystemService(Context.USB_SERVICE);

		// Register BroadcastReceiver for Permission Intent
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		mAppContext.registerReceiver(mUsbReceiver, filter);
	}
	
	/**
	 * Get a list of pl2303 devices currently connected. Must be called before open().
	 * @return ArrayList<UsbDevice> 
	 */
	public ArrayList<UsbDevice> getDeviceList() {
		mPL2303ArrayList.clear();
		
		// Get the USB device list (of all devices)
		HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
				
		// Scan the devices and copy all PL2303-Adaptors into the pl2303ArrayList
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		while(deviceIterator.hasNext()){
			UsbDevice device = deviceIterator.next();
			if ((device.getProductId()==0x2303) && (device.getVendorId()==0x067b)) {
			mPL2303ArrayList.add(device); }
		}
		Log.d(TAG, mPL2303ArrayList.size()+" device(s) found");
 
		return mPL2303ArrayList;
	}
	
	/**
	 * Open USB-Connection to a device (after getDeviceList() has been called)
	 * @param UsbDevice
	 * @throws IOException if device is not PL2303 or was not in the original getDeviceList()
	 */
	public void open(UsbDevice device) throws IOException {
		if (mPL2303ArrayList.isEmpty()) throw new IOException ("No devices connected, or getDeviceList() was not called");
		if (!mPL2303ArrayList.contains(device)) throw new IOException("Device not in original list");
		if ((device.getProductId()!=0x2303) && (device.getVendorId()!=0x067b)) throw new IOException("Not a compatible PL2303-device");
		
		PendingIntent mPermissionIntent;
		mPermissionIntent = PendingIntent.getBroadcast(mAppContext, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_ONE_SHOT);

		// Request the permission to use the device from the user
		mUsbManager.requestPermission(device, mPermissionIntent);
		Log.d(TAG, "Requesting permission to use " + device.getDeviceName());
	}
		
	/**
	 * initialize the PL2303 converter
	 * @return true on success
	 */
	private boolean initalize(UsbDevice device) {
		mDevice = device;
		Log.d(TAG, "Device Name: "+mDevice.getDeviceName());
		Log.d(TAG, "VendorID: "+mDevice.getVendorId());
		Log.d(TAG, "ProductID: "+mDevice.getProductId());
		
		mUSBIntf = mDevice.getInterface(0);
		if (mUSBIntf == null) {
			Log.e(TAG, "Getting interface failed!");
			return false;
		}
		
		// endpoint addr 0x81 = input interrupt
		mEP0 = mUSBIntf.getEndpoint(0); 
		if ((mEP0.getType() != UsbConstants.USB_ENDPOINT_XFER_INT) || (mEP0.getDirection() != UsbConstants.USB_DIR_IN)) {
			Log.e(TAG, "Getting endpoint 0 (control) failed!");
			return false;
		}
		
		// endpoint addr 0x2 = output bulk
		mEP1 = mUSBIntf.getEndpoint(1); 
		if ((mEP1.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) || (mEP1.getDirection() != UsbConstants.USB_DIR_OUT)) {
			Log.e(TAG, "Getting endpoint 1 (output) failed!");
			return false;
		}
		
		// endpoint addr 0x83 = input bulk
		mEP2 = mUSBIntf.getEndpoint(2); 
		if ((mEP2.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) || (mEP2.getDirection() != UsbConstants.USB_DIR_IN)) {
			Log.e(TAG, "Getting endpoint 2 (input) failed!");
			return false;
		}
		
		UsbDeviceConnection connection = mUsbManager.openDevice(mDevice);
		if (connection == null) {
			Log.e(TAG, "Getting DeviceConnection failed!");
			return false;
		}
		
		if (!connection.claimInterface(mUSBIntf, true)) {
			Log.e(TAG, "Exclusive interface access failed!");
			return false;
		}
		
		mConnection = connection;

		if (mConnection.getRawDescriptors()[7] == 64) mPL2303type = 1; //Type 1 = PL2303HX
		Log.d(TAG, "PL2303 type " +mPL2303type+ " detected");		
		
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
		
		if (mPL2303type == 1) mConnection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 2, 0x44, null, 0, 100);
		else mConnection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 2, 0x24, null, 0, 100);
		
		// Start control thread for status lines DSR,CTS,DCD and RI
		Thread t = new Thread(this);
		t.start();
		
		return true;
	}

	/**
	 *  Close the connection
	 */
	public void close() {
		if (mConnection != null) {
			// drop DTR/RTS
			setRTS(false);
			setDTR(false);
			
			mConnection.releaseInterface(mUSBIntf);
			mConnection.close();
			mConnection = null;
			mDevice = null;
			mEP0 = null;
			mEP1 = null;
			mEP2 = null;
			Log.d(TAG, "Device closed");
		}
	}

	/**
	 * Are we connected to a pl2303 converter?
	 * This is only the USB connection, not the serial connection.
	 * 
	 * @return true on connection
	 */
	public boolean isConnected() {
		if (mConnection != null) return true;
		else return false;
	}

	/**
	 * Setup basic communication parameters according to linux pl2303.c driver 
	 * @param Enum BaudRate 
	 * @param Enum DataBits
	 * @param Enum StopBits
	 * @param Enum Parity
	 * @param Enum FlowControl
	 * @throws IOException if settings not supported or connection is closed
	 */
	public void setup(BaudRate R, DataBits D, StopBits S, Parity P, FlowControl F) throws IOException {
		
		if (mConnection == null) throw new IOException("Connection closed");

		// Get current settings
		mConnection.controlTransfer(GET_LINE_REQUEST_TYPE, GET_LINE_REQUEST, 0, 0, mPortSetting, 7, 100);
		//mConnection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 0, 1, null, 0, 100);
		Log.d(TAG, "Current serial configuration:" + mPortSetting[0] + "," + mPortSetting[1] + "," + mPortSetting[2] + "," + mPortSetting[3] + "," + mPortSetting[4] + "," + mPortSetting[5] + "," + mPortSetting[6]);

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

		if  ((baud > 1228800) && (mPL2303type == 0)) throw new IOException("Baudrate not supported"); // Only PL2303HX supports the higher baudrates   

		mPortSetting[0]=(byte) (baud & 0xff);
		mPortSetting[1]=(byte) ((baud >> 8) & 0xff);
		mPortSetting[2]=(byte) ((baud >> 16) & 0xff);
		mPortSetting[3]=(byte) ((baud >> 24) & 0xff);

		// Setup Stopbits
		switch (S) {
		case S1: mPortSetting[4] = 0; break;
		case S2: mPortSetting[4] = 2; break;
		default: throw new IOException("Stopbit setting not supported"); 
		}

		// Setup Parity
		switch (P) {
		case NONE: mPortSetting[5] = 0; break;
		case ODD: mPortSetting[5] = 1; break;
		case EVEN: mPortSetting[5] = 2; break;
		default: throw new IOException("Parity setting not supported"); 
		}

		// Setup Databits
		switch (D) {
		case D5: mPortSetting[6] = 5; break;
		case D6: mPortSetting[6] = 6; break;
		case D7: mPortSetting[6] = 7; break;
		case D8: mPortSetting[6] = 8; break;
		default: throw new IOException("Databit setting not supported");
		}

		// Set new configuration on PL2303
		mConnection.controlTransfer(SET_LINE_REQUEST_TYPE, SET_LINE_REQUEST, 0, 0, mPortSetting, 7, 100); 
		Log.d(TAG, "New serial configuration:" + mPortSetting[0] + "," + mPortSetting[1] + "," + mPortSetting[2] + "," + mPortSetting[3] + "," + mPortSetting[4] + "," + mPortSetting[5] + "," + mPortSetting[6]);
		
		// Disable BreakControl
		mConnection.controlTransfer(BREAK_REQUEST_TYPE, BREAK_REQUEST, BREAK_OFF, 0, null, 0, 100);

		// Enable/Disable FlowControl
		switch (F) {
		case OFF:
			mConnection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 0x0, 0x0, null, 0, 100);
			setRTS(false);
			setDTR(false);
			mFlow = F;
			Log.d(TAG, "FlowControl disabled");
			break;
			
		case RTSCTS: 
			if (mPL2303type == 1) mConnection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 0x0, 0x61, null, 0, 100);
			else mConnection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 0x0, 0x41, null, 0, 100);
			setDTR(true);
			setRTS(true);
			mFlow = F;
			Log.d(TAG, "RTS/CTS FlowControl enabled");
			break;
		
		case RFRCTS: break;
		case DTRDSR: break;
		case XONXOFF: break;
		}
		
	}

	/**
	 * Switch DTR on or off
	 * @param state
	 */
	public void setDTR(boolean state) {
		if ((state) && !((mControlLines & CONTROL_DTR)==CONTROL_DTR)) mControlLines = mControlLines + CONTROL_DTR;
		if (!(state) && ((mControlLines & CONTROL_DTR)==CONTROL_DTR)) mControlLines = mControlLines - CONTROL_DTR;
		mConnection.controlTransfer(SET_CONTROL_REQUEST_TYPE, SET_CONTROL_REQUEST, mControlLines , 0, null, 0, 100);
		Log.d(TAG, "DTR set to " + state);
	}
	
	/**
	 * Switch RTS on or off
	 * @param state
	 */
	public void setRTS(boolean state) {
		if ((state) && !((mControlLines & CONTROL_RTS)==CONTROL_RTS)) mControlLines = mControlLines + CONTROL_RTS;
		if (!(state) && ((mControlLines & CONTROL_RTS)==CONTROL_RTS)) mControlLines = mControlLines - CONTROL_RTS;
		mConnection.controlTransfer(SET_CONTROL_REQUEST_TYPE, SET_CONTROL_REQUEST, mControlLines , 0, null, 0, 100);
		Log.d(TAG, "RTS set to " + state);
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

				// Blocking read (Timeout set to 0)
				@Override
				public int read() throws IOException {
					synchronized (this) {
						int retVal= -1;
						if (mConnection == null) throw new IOException("Connection closed");
						
						// If FlowControl: Check DSR before read
						if ((mFlow==FlowControl.RTSCTS) && ((mStatusLines & UART_DSR) != UART_DSR)) throw new IOException ("DSR down");
						
						byte [] readBuffer = new byte[1];
						int bytesRead = mConnection.bulkTransfer(mEP2, readBuffer, 1, 0);
						if (bytesRead > 0) retVal = readBuffer[0];
						return retVal;
					}
				}

				// Non-blocking read (Timeout set to 100ms)
				@Override
				public int read(byte[] buffer, int offset, int length) throws IOException, IndexOutOfBoundsException {
					synchronized (this) {
						int PacketSize = mEP2.getMaxPacketSize();
						int totalBytesRead = 0;
						byte [] readBuffer = new byte[PacketSize];
						
						if ((offset < 0) || (length < 0) || ((offset + length) > buffer.length)) throw new IndexOutOfBoundsException();
						if (mConnection == null) throw new IOException("Connection closed");
						
						// Max Packet Size 64 bytes! Split larger read-requests in multiple bulk-transfers
						int numTransfers = length / PacketSize;
						if (length % PacketSize > 0) numTransfers++;
						
						for (int i = 0 ; i < numTransfers ; i++) {
							// If FlowControl: Check DSR before read
							if ((mFlow == FlowControl.RTSCTS) && ((mStatusLines & UART_DSR) != UART_DSR)) throw new IOException ("DSR down");
							int bytesRead = mConnection.bulkTransfer(mEP2, readBuffer, PacketSize, 100);
							
							if (bytesRead >= 0) {
								System.arraycopy(readBuffer, 0, buffer, offset, bytesRead);
								offset = offset+bytesRead;
								totalBytesRead = totalBytesRead + bytesRead;
							} else break;
							
						}
						return totalBytesRead;	
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
				
				// Blocking write (Timeout set to 0)
				@Override 
				public void write(int oneByte) throws IOException{
					synchronized (this) {
						if (mConnection == null) throw new IOException("Connection closed");
						
						// If FlowControl: Check DSR / CTS before write
						if (mFlow==FlowControl.RTSCTS) {
							if ((mStatusLines & UART_DSR) != UART_DSR) throw new IOException ("DSR down");

							// Wait until CTS is up
							// TODO: this blocks!
							while ((mStatusLines & UART_CTS) != UART_CTS) {
								 try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
								}
							}
						}
						
						byte [] writeBuffer = new byte[1];
						int bytesWritten = mConnection.bulkTransfer(mEP1, writeBuffer, 1, 0);
						if (bytesWritten < 1 ) throw new IOException ("BulkWrite failed - written: "+bytesWritten); 
					}
				}

				// Non-blocking write (Timeout set to 100ms)
				@Override
				public void write (byte[] buffer, int offset, int count) throws IOException, IndexOutOfBoundsException {
					synchronized (this) {
						int PacketSize = mEP1.getMaxPacketSize();
						byte [] writeBuffer = new byte[PacketSize];
						
						
						if ((offset < 0) || (count < 0) || ((offset + count) > buffer.length)) throw new IndexOutOfBoundsException();
						if (mConnection == null) throw new IOException("Connection closed");
						
						// Max Packet Size 64 bytes! Split larger write-requests in multiple bulk-transfers
						int numTransfers = count / PacketSize;
						if (count % PacketSize > 0) numTransfers++;
						
						for (int i=0;i<numTransfers;i++) {

							// If FlowControl: Check DSR /CTS before write
							if (mFlow==FlowControl.RTSCTS) {
								if ((mStatusLines & UART_DSR) != UART_DSR) throw new IOException ("DSR down");

								// Wait until CTS is up
								// TODO: this blocks!
								while ((mStatusLines & UART_CTS) != UART_CTS) {
									 try {
										Thread.sleep(100);
									} catch (InterruptedException e) {
									}
								}
							}
							
							offset = offset + (i * PacketSize);

							// If this is the last part of multiple transfers correct the PacketSize (might be smaller than maxPacketSize) 
							if (i == numTransfers - 1) PacketSize = count - ((numTransfers-1) * PacketSize); 
							
							System.arraycopy(buffer, offset, writeBuffer, 0, PacketSize);
							int bytesWritten = mConnection.bulkTransfer(mEP1, writeBuffer, PacketSize, 100);
							if (bytesWritten != PacketSize) throw new IOException ("BulkWrite failed - count: " + PacketSize + " written: "+bytesWritten);
								
						}
					}
				}
			};
			return out;
		} else return null;
	}

	/**
	 * Runnable for detection of DSR, CTS , DCD and RI
	 * Calls the appropriate Callback-function on status change
	 * UsbRequest on Endpoint zero returns 10 bytes. Byte 9 contains the line status. 
	 */
	@Override
	public void run() {
		ByteBuffer readBuffer = ByteBuffer.allocate(mEP0.getMaxPacketSize());
		UsbRequest request = new UsbRequest();
		
		// Although documentation says that UsbRequest doesn't work on Endpoint 0 it actually works  
		request.initialize(mConnection, mEP0);

		while (mConnection != null) {
			request.queue(readBuffer, mEP0.getMaxPacketSize());
			UsbRequest retRequest = mConnection.requestWait();
			
			// The request returns when any line status has changed
			if (retRequest.getEndpoint()==mEP0) {
				
				if ((readBuffer.get(8) & UART_DSR) != (mStatusLines & UART_DSR)) {
					Log.d(TAG,"Change on DSR detected: "+(readBuffer.get(8) & UART_DSR));
					if ((readBuffer.get(8) & UART_DSR) == UART_DSR) mPL2303Callback.onDSR(true);
					else mPL2303Callback.onDSR(false);
				}
				
				if ((readBuffer.get(8) & UART_CTS) != (mStatusLines & UART_CTS)) {
					Log.d(TAG,"Change on CTS detected: "+(readBuffer.get(8) & UART_CTS));
					if ((readBuffer.get(8) & UART_CTS) == UART_CTS) mPL2303Callback.onCTS(true);
					else mPL2303Callback.onCTS(false);
				}
				
				if ((readBuffer.get(8) & UART_DCD) != (mStatusLines & UART_DCD)) {
					Log.d(TAG,"Change on DCD detected: "+(readBuffer.get(8) & UART_DCD));
					if ((readBuffer.get(8) & UART_DCD) == UART_DCD) mPL2303Callback.onDCD(true);
					else mPL2303Callback.onDCD(false);
				}
				
				if ((readBuffer.get(8) & UART_RING) != (mStatusLines & UART_RING)) {
					Log.d(TAG,"Change on RI detected: "+(readBuffer.get(8) & UART_RING));
					if ((readBuffer.get(8) & UART_RING) == UART_RING) mPL2303Callback.onRI(true);
					else mPL2303Callback.onRI(false);
				}
			}
			
			// Save status
			mStatusLines = readBuffer.get(8);
		}
	} 
	
}
