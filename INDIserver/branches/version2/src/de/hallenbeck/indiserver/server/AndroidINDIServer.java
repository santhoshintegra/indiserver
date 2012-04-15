package de.hallenbeck.indiserver.server;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import de.hallenbeck.indiserver.R;
import de.hallenbeck.indiserver.activities.main;
import de.hallenbeck.indiserver.device_drivers.lx200autostar;
import de.hallenbeck.indiserver.device_drivers.lx200basic;

import laazotea.indi.INDIException;
import laazotea.indi.server.DefaultINDIServer;

public class AndroidINDIServer extends DefaultINDIServer {

	private Context AppContext;
	/**
	 * Just loads the available driver.
	 */
	public AndroidINDIServer(Context context) {
		super();
		AppContext = context;
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(AppContext);
		String DeviceDriver = settings.getString("device_driver", null);
		String ComDriver = settings.getString("com_driver", null);
		String Device = settings.getString("device", null);
		boolean autoconnect = settings.getBoolean("autoconnect", false);
		
		
		// Loads the Java Driver. Please note that this must be in the classpath.
		try {
			loadAndroidDriver(lx200autostar.class, ComDriver, Device);
			loadAndroidDriver(lx200basic.class, ComDriver, Device);
			notifyUser("INDIserver started","Waiting for Clients...",true);
		} catch (INDIException e) {
			e.printStackTrace();
			notifyUser("INDIserver stopped", "ERROR loading drivers" + e.getMessage(), false);
		}
	}

	public void stopServer() {
		super.stopServer();
		destroyJavaDriver(lx200autostar.class);
		destroyJavaDriver(lx200basic.class);
		notifyUser("INDIServer stopped", "All Clients disconnected", false);
	}

	protected synchronized void loadAndroidDriver(Class cls, String ComDriver, String Device) throws INDIException {
		INDIAndroidDevice newDevice = new INDIAndroidDevice(this, cls, "class+-+" + cls.getName(), ComDriver, Device);
		addDevice(newDevice);
	}

	/**
	 * Notify user about running server and connected clients
	 */
	public synchronized void notifyUser(String title, String message, boolean ongoing) {

		PendingIntent intent = PendingIntent.getActivity(AppContext, 0, new Intent(AppContext,main.class) , 0);

		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) AppContext.getSystemService(ns);

		NotificationCompat.Builder notificationbuilder = new NotificationCompat.Builder(AppContext);
		notificationbuilder.setContentTitle(title);
		notificationbuilder.setContentText(message);
		notificationbuilder.setTicker(message);
		notificationbuilder.setSmallIcon(R.drawable.ic_launcher);
		notificationbuilder.setContentIntent(intent);
		notificationbuilder.setOngoing(ongoing);
		Notification notification = notificationbuilder.getNotification();
		mNotificationManager.notify(1, notification); 
	}

	@Override
	protected void onClientConnected(String address) {
		notifyUser("INDIServer running", "Client connected "+address, false);
	}

	@Override
	protected void onClientDisconnected(String address) {
		notifyUser("INDIServer running", "Client disconnected "+address, false);
	}
}
