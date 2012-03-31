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

	public static ServerThread SThread;
	public DriverThread DThread;
	public static boolean autoconnect = false;
	
	//max number of clients 
	private static final int maxClients = 8;
	
	//max number of devices/drivers
	private static final int maxDevices = 4;
	
	

	/**
	 * TCP ConnectionThread created by ServerThread on connection of client
	 * Handles all communication with the client.
	 * 
	 * @author atuschen
	 *
	 */
	class ConnectionThread extends Thread {
		private BufferedReader in;
		private BufferedWriter out;
		private char[] buffer;
		
		/**
		 * Seperate thread for each tcp-connection 
		 * @param sock Connection Socket 
		 */
		public ConnectionThread (Socket sock) {
			buffer = new char[8192];
			try {
				in  = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		public void run() {
			boolean connected = true;
			SThread.IncreaseConnectionCount();	
			while (connected) {
				try {
						//Read data from tcp inputstream (from client)
						int len = in.read(buffer,0,8192);
						if (len != -1) {
							// Write data to local sock via DriverThread (to driver)
							DThread.write(buffer,len);	
						} else {
							//Connection to client lost
							SThread.DecreaseConnectionCount();
							connected = false;
						}
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
		
		public synchronized void write(char[] data, int len) {
			try {
				//Write data to tcp outputstream (to client)
				out.write(data, 0, len);
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
		private Thread[] ConnectionThreads;
		private int ConnectionCount = 0;

		public void run() {
			ConnectionThreads = new Thread[maxClients];
			while (true) {
				try {
					ServerSocket Sock = new ServerSocket(7624);
					Socket sock = Sock.accept();
					
					int connections = getConnectionCount();
					
					if ( connections < maxClients) {
						ConnectionThreads[connections] = new ConnectionThread(sock);
						ConnectionThreads[connections].start();
					}
				
				} catch (IOException e) {

					e.printStackTrace();
				}

			}	

		}
		
		public int getConnectionCount() {
			return ConnectionCount;
		}
		
		public synchronized void IncreaseConnectionCount() {
			ConnectionCount++;
			notifyUser();
		}
		
		public synchronized void DecreaseConnectionCount() {
			ConnectionCount--;
			notifyUser();
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
	public class StartThread extends Thread {
		private String DeviceDriver;
		private String ComDriver;
		private String Device;
		private device_driver_interface devicedriver = null;
		
		public StartThread(String deviceDriver, String comDriver, String device) {
			DeviceDriver = deviceDriver;
			ComDriver = comDriver;
			Device = device;
		}
		
		public void run() {
			try {
				devicedriver = (device_driver_interface) Class.forName(DeviceDriver).newInstance();
				devicedriver.set_communication_driver(ComDriver);
				devicedriver.set_device(Device);
				if (autoconnect) devicedriver.connect();
				
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
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
		private StartThread Driver;
		
		public DriverThread(String deviceDriver, String comDriver, String device) {
			
			buffer = new char[8192];
			LocalSocket sock = new LocalSocket();
			
			// Start the Driver 
			Driver = new StartThread(deviceDriver, comDriver, device);
			Driver.start();
			
			// Connect to the driver
			try {
				sleep(1000); //let the driver start, so wait a moment 
				LocalSocketAddress address = new LocalSocketAddress(deviceDriver);
				sock = new LocalSocket();
				sock.connect(address);
				in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void run() {
			int len=0;
			while(true) {
				try {
						len = in.read(buffer, 0, 8192);
						if (len != -1) {
							int i=0;
							while (i < SThread.getConnectionCount()) {
								((ConnectionThread) SThread.ConnectionThreads[i]).write(buffer,len);
								i++;
							}
						}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
					
			}
		}
		
		public synchronized void write(char[] data,int len) {
			try {
				out.write(data,0,len);
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
	
	/**
	 * Notify user about running server and connected clients
	 */
	public void notifyUser() {
		int nClients = SThread.getConnectionCount();
		
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

		Notification.Builder notificationbuilder = new Notification.Builder(getApplicationContext());
		
		notificationbuilder.setContentTitle("INDIserver running");
		notificationbuilder.setContentText(String.format("%d Client(s) connected",nClients));
		notificationbuilder.setTicker(String.format("%d Client(s) connected",nClients));
		notificationbuilder.setSmallIcon(R.drawable.ic_launcher);
		notificationbuilder.setOngoing(true);

		Notification notification = notificationbuilder.getNotification();
		
		mNotificationManager.notify(2, notification);
		
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

		// Start the DriverThread
		DThread = new DriverThread( DeviceDriver,ComDriver,Device );
		DThread.start();

		notifyUser();

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
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	public void onLowMemory() {
		// TODO Auto-generated method stub
		super.onLowMemory();
	}
}
