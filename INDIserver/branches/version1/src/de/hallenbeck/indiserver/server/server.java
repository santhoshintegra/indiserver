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
package de.hallenbeck.indiserver.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import de.hallenbeck.indiserver.R;
import de.hallenbeck.indiserver.device_drivers.device_driver_interface;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.IBinder;
import android.preference.PreferenceManager;

/**
 * @author Alexander Tuschen <atuschen75 at gmail dot com>
 *
 */
public class server extends Service {

	
	public ServerThread SThread;
	public DriverThread[] DThread;
	public boolean autoconnect = false;
	
	//max number of clients 
	private static final int maxClients = 8;
	
	//max number of devices/drivers
	private static final int maxDevices = 4;
	
	//TODO: Add support for multiple drivers/devices

	/**
	 * Seperate TCP ConnectionThread created by ServerThread on connection of client
	 * Handles all communication with the client.
	 * 
	 * @author atuschen
	 *
	 */
	class ConnectionThread extends Thread {
		private BufferedReader in;
		private BufferedWriter out;
		private char[] buffer;
		private int connectionSlot;
		private boolean connected = false;
		
		/**
		 * ConnectionThread Constructor 
		 * @param sock Socket of Connection
		 * @param slot Connection number
		 */
		public ConnectionThread (Socket sock, int slot) {
			buffer = new char[8192];
			connectionSlot = slot;
			try {
				in  = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
				connected = true;
			} catch (IOException e) {
				notifyUser("INDIserver error",e.getMessage(),false);
			}
			
		}
		
		/**
		 * Main runnable, listens for client messages and sends them to any driver 
		 */
		public void run() {
			SThread.IncreaseConnectionCount();	
			while (connected) {
				try {

					// Blocking: Read data from tcp inputstream (from client) 
					int len = in.read(buffer,0,8192);
					if (len != -1) {
						// Write data to local sock via DriverThread (to driver)
						DThread[0].write(buffer,len);	
					} else {
						//Connection to client lost
						SThread.DecreaseConnectionCount(connectionSlot);
						connected = false;
						
						// If this was the last client stop the service
						if (SThread.getConnectionCount() == 0) {
							// Notify User 
							notifyUser("INDIServer stopped", "All Clients disconnected", false);
							stopSelf();
						}
					}

				} catch (IOException e) {
					notifyUser("INDIserver error",e.getMessage(),false);
					connected = false;
				}

			}
		}
		
		/**
		 * Close the Readers (causes an Exception and returns all blocked read()-operations
		 */
		public void closeSocket() {
			try {
				out.close();
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		/**
		 * Write method, called by the Server-Thread to send messages to the client
		 * @param data chararray with data to send
		 * @param len length of data
		 */
		public synchronized void write(char[] data, int len) {
			try {
				//Write data to tcp outputstream (to client)
				out.write(data, 0, len);
				out.flush();
			} catch (IOException e) {
				notifyUser("INDIserver error",e.getMessage(),false);
				connected = false;
			}
		}
	}
	

	
	/**
	 * Main TCP Server Thread
	 * listens for connections on port 7624 and starts a new 
	 * ConnectionThread if a client connects
	 *  
	 * @author atuschen
	 *
	 */
	class ServerThread extends Thread {
		private ConnectionThread[] ConnectionThreads;
		private int ConnectionCount = 0;
		ServerSocket Sock = null;
		
		/**
		 * ServerThread Constructor
		 */
		public ServerThread(){
			//Create the tcp-socket
			try {
				Sock = new ServerSocket(7624);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		public void run() {
			// array of ConnectionThreads 
			ConnectionThreads = new ConnectionThread[maxClients];
			
			while ((Sock != null) && (!Sock.isClosed())) {
				try {

					// This blocks until a new connection is established
					Socket sock = Sock.accept();

					int i = 0;
					boolean emptySlotFound = false;

					// search for an empty array index and create a ConnectionThread
					while ((i < maxClients) && (!emptySlotFound)) {
						if (ConnectionThreads[i] == null) {
							ConnectionThreads[i] = new ConnectionThread(sock,i);
							ConnectionThreads[i].start();
							emptySlotFound = true;
						}
						i++;
					}

				} catch (IOException e) {
					// Thrown away, Socket closed (by closeSocket())
					
				}

			}

		}

		// Close all ConnectionThreads and the tcp-socket
		public void closeSocket() {
			try {
				int i = 0;
				while (i < maxClients) {
					if (ConnectionThreads[i] != null) ConnectionThreads[i].closeSocket();
					i++;
				}
				// Close the socket 
				Sock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// Get number of connected clients
		public int getConnectionCount() {
			return ConnectionCount;
		}
		
		/**
		 * Write data to ONE specific client
		 * @param slot Connection Number of the client
		 * @param data Data to send
		 * @param len length of data
		 */
		public synchronized void writeToClient(int slot, char[] data, int len) {
			ConnectionThreads[slot].write(data, len);
		}
		
		/**
		 * Write Data to ALL connected clients
		 * @param data Data to send
		 * @param len length of Data
		 */
		public synchronized void writeToAllClients(char[] data, int len) {
			int i = 0;
			while (i < maxClients) {
				if (ConnectionThreads[i] != null) ConnectionThreads[i].write(data, len);
				i++;
			}
		}
		
		/** 
		 * Increase the number of conncted cleints and inform the user
		 */
		public synchronized void IncreaseConnectionCount() {
			ConnectionCount++;
			notifyUser("INDIserver running", ConnectionCount + " Client(s) connected", true);
		}
		
		/** 
		 * Decrease the number of conncted cleints and inform the user
		 */
		public synchronized void DecreaseConnectionCount(int slot) {
			ConnectionCount--;
			ConnectionThreads[slot] = null;
			if (ConnectionCount > 0) notifyUser("INDIserver running", ConnectionCount + " Client(s) connected", true);
		}	
		
	}

	/**
	 * Driver start Thread - called by DriverThread
	 * Creates a new instance of a driver in its own Thread
	 * Driver is listening on local socket named the classname of driver.
	 *   
	 * @author atuschen
	 *
	 */
	public class DriverStartThread extends Thread {
		private String DeviceDriver;
		private String ComDriver;
		private String Device;
		private device_driver_interface devicedriver = null;
		
		public DriverStartThread(String deviceDriver, String comDriver, String device) {
			DeviceDriver = deviceDriver;
			ComDriver = comDriver;
			Device = device;
		}
		
		public void run() {
			try {
				// create an new instance
				devicedriver = (device_driver_interface) Class.forName(DeviceDriver).newInstance();
				// set comm driver
				devicedriver.set_communication_driver(ComDriver);
				// set the devicename to connect to
				devicedriver.set_device(Device);
				
				if (autoconnect) devicedriver.connect();
				
			} catch (ClassNotFoundException e) {
				notifyUser("INDIserver error",e.getMessage(),false); 
				stopSelf();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				notifyUser("INDIserver error",e.getMessage(),false);
				stopSelf();
			}
			
		}
	}
	
	
	
	
	/**
	 * DriverThread - calls the DriverStartThread 
	 * and connects to the driver over the local socket
	 * Handles all communication with driver.
	 * 
	 * @author atuschen
	 *
	 */
	class DriverThread extends Thread {
		private BufferedReader in;
		private BufferedWriter out;
		private char[] buffer;
		private LocalSocket sock = null;
		private DriverStartThread Driver;
		private boolean connected = false;
		
		public DriverThread(String deviceDriver, String comDriver, String device) {
			
			buffer = new char[8192];
				
			
			// Start the Driver 
			Driver = new DriverStartThread(deviceDriver, comDriver, device);
			Driver.start();
			
			// Connect to the driver
			try {
				// let the driver start, so wait a moment
				sleep(1000);
				
				// create the socket address from the driver class name
				LocalSocketAddress address = new LocalSocketAddress(deviceDriver);
				
				// create the local socket
				sock = new LocalSocket();
				
				// connect to the driver 
				sock.connect(address);
				
				// create the readers/writers
				in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
				connected = true;
				
			} catch (IOException e) {
				notifyUser("INDIserver error",e.getMessage(),false);
				stopSelf();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		/** 
		 * DriverThread main runnable
		 * Listens for mesages from drivers and sends them to all connected Clients
		 */
		public void run() {
			int len=0;
			while (connected) {
				try {
					// This is blocking 
					len = in.read(buffer, 0, 8192);
					if (len != -1) {
						
						// Write Driver output to all connected clients 
						SThread.writeToAllClients(buffer, len);
						
					} 

				} catch (IOException e) {
					notifyUser("INDIserver error",e.getMessage(),false);
					stopSelf();
				}

			}
		}
		
		/**
		 * close the local socket, causes an Exception an returns all blocked read() operations
		 */
		public void closeSocket() {
			try {
				// close input/outputstreams (causes thread to terminate)
				sock.shutdownOutput();
				sock.shutdownInput();
				connected = false;
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * Write data to the driver
		 * Called by ConnectionThread 
		 * @param data Data to send
		 * @param len length of Data
		 */
		public synchronized void write(char[] data,int len) {
			try {
				out.write(data,0,len);
				out.flush();
			} catch (IOException e) {
				notifyUser("INDIserver error",e.getMessage(),false);
				stopSelf();
			}

		}
	}
	
	/**
	 * Notify user about running server and connected clients
	 */
	public void notifyUser(String title, String message, boolean ongoing) {
		
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

		Notification.Builder notificationbuilder = new Notification.Builder(getApplicationContext());
		
		notificationbuilder.setContentTitle(title);
		notificationbuilder.setContentText(message);
		notificationbuilder.setTicker(message);
		notificationbuilder.setSmallIcon(R.drawable.ic_launcher);
		notificationbuilder.setOngoing(ongoing);
		Notification notification = notificationbuilder.getNotification();
		
		mNotificationManager.notify(1, notification);
		
	}
	
	/**
	 * Main Code 
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// Get the settings from shared preferences
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String DeviceDriver = settings.getString("device_driver", null);
		String ComDriver = settings.getString("com_driver", null);
		String Device = settings.getString("device", null);
		autoconnect = settings.getBoolean("autoconnect", false);

		// Start the Server thread, that listens for incoming TCP-connections
		SThread = new ServerThread();
		SThread.start();
		
		notifyUser("INDIserver started","Waiting for Clients...",true);
		
		DThread = new DriverThread[maxDevices];
		
		// Start the DriverThread
		DThread[0] = new DriverThread( DeviceDriver,ComDriver,Device );
		DThread[0].start();

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onTrimMemory(int level) {
		// TODO Auto-generated method stub
		super.onTrimMemory(level);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		
		// Close the DriverThread Socket (causes the DriverThread and all Drivers to terminate)
		DThread[0].closeSocket();

		// Close the ServerThread Socket (causes the ServerThread and all ConnectionThreads to terminate)
		SThread.closeSocket();

		notifyUser("INDIServer stopped", "All Clients disconnected", false);
		super.onDestroy();
	}

	@Override
	public void onLowMemory() {
		// TODO Auto-generated method stub
		super.onLowMemory();
	}
}
