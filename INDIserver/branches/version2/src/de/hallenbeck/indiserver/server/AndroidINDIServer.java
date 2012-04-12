package de.hallenbeck.indiserver.server;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import de.hallenbeck.indiserver.device_drivers.lx200autostar;
import de.hallenbeck.indiserver.device_drivers.lx200basic;

import laazotea.indi.INDIException;
import laazotea.indi.server.DefaultINDIServer;

public class AndroidINDIServer extends DefaultINDIServer {

	
	/**
	 * Just loads the available driver.
	 */
	public AndroidINDIServer(Context context) {
		super();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		String DeviceDriver = settings.getString("device_driver", null);
		String ComDriver = settings.getString("com_driver", null);
		String Device = settings.getString("device", null);
		boolean autoconnect = settings.getBoolean("autoconnect", false);
		// Loads the Java Driver. Please note that this must be in the classpath.
		try {
			loadAndroidDriver(lx200autostar.class, ComDriver, Device);
			loadAndroidDriver(lx200basic.class, ComDriver, Device);
		} catch (INDIException e) {
			e.printStackTrace();
			//notifyUser("INDIserver", "ERROR loading drivers", false);
		}
	}
	
	public void stopServer() {
		super.stopServer();
		destroyJavaDriver(lx200autostar.class);
		destroyJavaDriver(lx200basic.class);
	}
	
	protected synchronized void loadAndroidDriver(Class cls, String ComDriver, String Device) throws INDIException {
	    INDIAndroidDevice newDevice = new INDIAndroidDevice(this, cls, "class+-+" + cls.getName(), ComDriver, Device);
	    addDevice(newDevice);
	  }
	  @Override
	  protected void onClientConnected(String address) {
	  }
	  
	  @Override
	  protected void onClientDisconnected(String address) {
	  }
}
