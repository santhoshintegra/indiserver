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

import laazotea.indi.INDIException;
import laazotea.indi.server.DefaultINDIServer;
import laazotea.indi.server.INDIDevice;
import de.hallenbeck.indiserver.R;
import de.hallenbeck.indiserver.activities.main;
import de.hallenbeck.indiserver.device_drivers.lx200autostar;
import de.hallenbeck.indiserver.device_drivers.lx200basic;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

/**
 * Android Background-Service for INDI-Server
 * @author Alexander Tuschen <atuschen75 at gmail dot com>
 *
 */
public class INDIservice extends Service {
	
	private boolean autoconnect = false;
	private INDIServer server;
			

	/**
	 * Seperate Server-Class
	 * @author atuschen
	 *
	 */
	public class INDIServer extends DefaultINDIServer {

		/**
		 * Just loads the available driver.
		 */
		public INDIServer() {
			super();

			// Loads the Java Driver. Please note that this must be in the classpath.
			try {
				loadJavaDriver(lx200autostar.class);
				loadJavaDriver(lx200basic.class);
			} catch (INDIException e) {
				e.printStackTrace();
				notifyUser("INDIserver", "ERROR loading drivers", false);
				//System.exit(-1);
			}
		}

		
		public void stopServer() {
			super.stopServer();
			destroyJavaDriver(lx200autostar.class);
			destroyJavaDriver(lx200basic.class);
		
		}
		/**
		 * Just creates one instance of this server.
		 * @param args 
		 */
		public void main(String[] args) {
			INDIServer s = new INDIServer();  
		}

		@Override
		protected void onClientConnected(String address) {
			notifyUser("INDIserver running", getNumDevices()+" Devices, "+getNumClients()+" Clients", true);
		}
		
		@Override
		protected void onClientDisconnected(String address) {
			notifyUser("INDIserver running", getNumDevices()+" Devices, "+getNumClients()+" Clients", true);
		}
		
	}

	/**
	 * Notify user about running server and connected clients
	 */
	public synchronized void notifyUser(String title, String message, boolean ongoing) {
		
		PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(),main.class) , 0);
		
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

		NotificationCompat.Builder notificationbuilder = new NotificationCompat.Builder(getApplicationContext());
		
		notificationbuilder.setContentTitle(title);
		notificationbuilder.setContentText(message);
		notificationbuilder.setTicker(message);
		notificationbuilder.setSmallIcon(R.drawable.ic_launcher);
		notificationbuilder.setContentIntent(intent);
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
		// This does NOT work at the moment! 
		// TODO: Communication Driver and Device are hardcoded at the time
		
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String DeviceDriver = settings.getString("device_driver", null);
		String ComDriver = settings.getString("com_driver", null);
		String Device = settings.getString("device", null);
		autoconnect = settings.getBoolean("autoconnect", false);
		
		// just start the server, no parameters are given at the moment
		server = new INDIServer();
		
		notifyUser("INDIserver started","Waiting for Clients...",true);
		
		return super.onStartCommand(intent, flags, startId);
	}

	// Only in Android 4.0
	  
	/*@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);
	} */

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		server.stopServer();
		notifyUser("INDIServer stopped", "All Clients disconnected", false);
		super.onDestroy();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}
}
