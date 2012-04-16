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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;


/**
 * PL2303 USB Serial Adapter Driver for
 * devices with android usbhost-support (3.2 upwards)
 * 
 * @author atuschen
 *
 */
public class usbhost_serial_pl2303 implements Runnable {

	private Context AppContext; 
	private UsbManager mUsbManager;
    //private UsbDevice mDevice;
    private UsbDeviceConnection mConnection;
    private UsbEndpoint ep1;
    private UsbEndpoint ep2;
    
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
    	B460800
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

    private static final int SET_LINE_REQUEST_TYPE		=	0x21;
    private static final int SET_LINE_REQUEST			=	0x20;
    private static final int BREAK_REQUEST_TYPE			=	0x21;
    private static final int BREAK_REQUEST				=	0x23;	
    private static final int BREAK_OFF					=	0x0000;
    private static final int GET_LINE_REQUEST_TYPE		=	0xa1;
    private static final int GET_LINE_REQUEST			=	0x21;
    private static final int VENDOR_WRITE_REQUEST_TYPE	=	0x40;
    private static final int VENDOR_WRITE_REQUEST		=	0x01;
    
    private static final String ACTION_USB_PERMISSION 	=   "com.android.hardware.USB_PERMISSION";

    private ByteBuffer readBuffer = ByteBuffer.allocate(1);
    private boolean BufferReady = false;
    
    // BraodcastReceiver for permission to use USB-Device
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                          setDevice(device);
                       }
                    } 
                    else {
                       //Log.d("permission denied for device " + device);
                    }
                }
            }
        }
    };
    
    // Constructor
    public void usbhost_serial_pl2302(Context context) {
    	AppContext = context;
    	UsbManager mUsbManager = (UsbManager) AppContext.getSystemService(Context.USB_SERVICE);

    	// Register BroadcastReceiver for Permission Intent
    	PendingIntent mPermissionIntent = PendingIntent.getBroadcast(AppContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
    	IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    	AppContext.registerReceiver(mUsbReceiver, filter);
    	
    	// Get the USB device list
    	HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
    	
    	// Scan the devices and find the first PL2303-Adaptor
    	Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
    	while(deviceIterator.hasNext()){
    	    UsbDevice device = deviceIterator.next();
    	    if ((device.getProductId()==0x2303) && (device.getVendorId()==0x067b)) {
    	    	// Request the permission to use the device
    	    	mUsbManager.requestPermission(device, mPermissionIntent);
    	    	break;	
    	    }
    	}
    }
    
    // Open the device and start the runnable
    private void setDevice(UsbDevice device) {
    	UsbInterface intf = device.getInterface(0);
    	ep1 = intf.getEndpoint(1); //endpoint addr 0x2 = output bulk
    	ep2 = intf.getEndpoint(2); //endpoint addr 0x83 = input bulk
    	//mDevice = device;
    	if (device != null) {
    		UsbDeviceConnection connection = mUsbManager.openDevice(device);
    		if (connection != null && connection.claimInterface(intf, true)) {
    			mConnection = connection;
    			Thread thread = new Thread(this);
    			thread.start();
    		} else {
    			mConnection = null;
    		}
    	}
    }
    
    // Setup the communication parameters
    public void setup(BaudRate R, DataBits D, StopBits S, Parity P) {
    	byte[] buffer = new byte[7];
    	
    	for (int i=0;i==7;i++) {
    		buffer[i]=0;
    	}

    	mConnection.controlTransfer(GET_LINE_REQUEST_TYPE, GET_LINE_REQUEST, 0, 0, buffer, 7, 100);
    	mConnection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 0, 1, null, 0, 100);
    	
    	// Setup Baudrate
    	int baud = 0;
    	switch (R) {
    	case B0: baud = 0; break;
    	case B75: baud = 75; break;
    	case B150: baud = 150; break;
    	case B300: baud = 1228800; break; // This is copied fom pl2303.c linux-driver
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
    	}
    	buffer[0]=(byte) (baud & 0xff);
    	buffer[1]=(byte) ((baud >> 8) & 0xff);
    	buffer[2]=(byte) ((baud >> 16) & 0xff);
    	buffer[3]=(byte) ((baud >> 24) & 0xff);
    	
    	// Setup Stopbits
    	switch (S) {
    	case S1: buffer[4] = 0; break;
    	case S2: buffer[4] = 2; break;
    	}
    	
    	// Setup Parity
    	switch (P) {
    	case NONE: buffer[5] = 0; break;
    	case ODD: buffer[5] = 1; break;
    	case EVEN: buffer[5] = 2; break;
    	}
    	
    	// Setup Databits
    	switch (D) {
    	case D5: buffer[6] = 5; break;
    	case D6: buffer[6] = 6; break;
    	case D7: buffer[6] = 7; break;
    	case D8: buffer[6] = 8; break;
    	}
    	
    	// Set configuration on PL2303-Adaptor
    	mConnection.controlTransfer(SET_LINE_REQUEST_TYPE, SET_LINE_REQUEST, 0, 0, buffer, 7, 100);

    	// Disable BreakControl
    	mConnection.controlTransfer(BREAK_REQUEST_TYPE, BREAK_REQUEST, BREAK_OFF, 0, null, 0, 100);
    	
    	//TODO: Setup FlowControl
    }
    
    // Send one byte to the device
    private void sendByte(int data) {
    	synchronized (this) {
            if (mConnection != null) {
                 byte[] buffer = new byte[1];
                 buffer[0] = (byte) data;
                 mConnection.bulkTransfer(ep1, buffer, 1, 0);
             }
         }
    }
    
    // create InputStream
    public InputStream getInputStream() {
    	InputStream in = new InputStream() {
    		@Override
    		public int read() {
    			return readBuffer.get(0);
    		}
    		@Override
    		public int read(byte[] buffer, int offset, int length) {
    			buffer[offset]=readBuffer.get(0);
    			return 1;
    		}
    		@Override
    		public int available() {
    			if (BufferReady) return 1;
    			else return 0;
    		}
    	};
    	return in;
    }
    
    // create OutputStream
    public OutputStream getOutputStream() {
    	OutputStream out = new OutputStream() {
    		@Override 
    		public void write(int oneByte) {
    			sendByte(oneByte);
    		}
    		@Override
    		public void write(byte[] buffer) {
    			for (int i=0;i<buffer.length;i++) {
    				sendByte(buffer[i]);
    			}
    		}
    	};
    	return out;
    }
	
    // The runnable which continually reads from the device 
	@Override
	public void run() {
		
        UsbRequest request = new UsbRequest();
        request.initialize(mConnection, ep2);
    	//setup(BaudRate.B9600, DataBits.D8, StopBits.S1, Parity.NONE);
        while (true) {
        	BufferReady=false;
            request.queue(readBuffer, 1);
            if (mConnection.requestWait() == request) {
                BufferReady=true;
            } else {
            	// Connection lost
                break;
            }
        }
	}
}
