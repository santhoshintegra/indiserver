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
import de.hallenbeck.indiserver.device_drivers.lx200basic;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.IBinder;
import android.preference.PreferenceManager;

/**
 * @author Alexander Tuschen <atuschen75 at gmail dot com>
 *
 */
public class INDIservice extends Service {
	
	public class INDIServer extends DefaultINDIServer {

		  /**
		   * Just loads the available driver.
		   */
		  public INDIServer() {
		    super();

		    // Loads the Java Driver. Please note that this must be in the classpath.
		    try {
		      loadJavaDriver(lx200basic.class);
		    } catch (INDIException e) {
		      e.printStackTrace();
		      
		      System.exit(-1);
		    }
		  }

		 

		  /* (non-Javadoc)
		 * @see laazotea.indi.server.DefaultINDIServer#getDevice(java.lang.String)
		 */
		@Override
		protected INDIDevice getDevice(String deviceName) {
			// TODO Auto-generated method stub
			return super.getDevice(deviceName);
		}



		/**
		   * Just creates one instance of this server.
		   * @param args 
		   */
		  public void main(String[] args) {
		    INDIServer s = new INDIServer();  
		  }
		}

	
	public boolean autoconnect = false;
	
	private INDIServer server;
	//max number of clients 
	private static final int maxClients = 8;
	
	//max number of devices/drivers
	private static final int maxDevices = 4;
	
		
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
		server = new INDIServer();
		//INDIDevice d = server.getDevice("LX200basic");
		
		notifyUser("INDIserver started","Waiting for Clients...",true);
		
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
		
		

		notifyUser("INDIServer stopped", "All Clients disconnected", false);
		super.onDestroy();
	}

	@Override
	public void onLowMemory() {
		// TODO Auto-generated method stub
		super.onLowMemory();
	}
}
