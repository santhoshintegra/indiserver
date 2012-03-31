package de.hallenbeck.indiserver.device_drivers;


import java.io.IOException;
import laazotea.indi.driver.INDIDriver;
import de.hallenbeck.indiserver.communication_drivers.communication_driver_interface;

/**
 * Abstract telescope-class with basic functions and basic INDI interface
 * 
 * @author atuschen
 *
 */
public abstract class telescope extends INDIDriver implements device_driver_interface {
	
	protected final static String COMM_GROUP = "Communication";
	protected final static String BASIC_GROUP = "Main Control";
	protected final static String MOTION_GROUP	= "Motion Control";
	protected final static String DATETIME_GROUP = "Date/Time";
	protected final static String SITE_GROUP = "Site Management";
	protected final static String FOCUS_GROUP = "Focus Control";
	protected static communication_driver_interface com_driver=null;
	protected static String device=null;
	protected static boolean connected=false;

	protected telescope() {
		 super();
	}

	
	/**
	 * Set the driver for communication with the telescope
	 * @param driver fully qualified name of driver class
	 */
	public void set_communication_driver(String driver) {
		try {
			com_driver = (communication_driver_interface) Class.forName(driver).newInstance();
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
	}
	
	public void set_device(String sdevice) {
		device = sdevice;
	}
	
	/**
	 * Connect to the telescope
	 */
	public void connect() {
		if (!connected) {
		try {
			com_driver.connect(device);
			connected=true;
		} catch (IOException e) {
			e.printStackTrace();
			connected=false;
		}
		}
	}
	
	/**
	 * Are we connected?
	 * @return
	 */
	public boolean isConnected() {
		return connected;
	}
	
	/**
	 * Disconnect from telescope
	 */
	public void disconnect() {
		if (connected) {
		connected=false;
		com_driver.disconnect();
		}
	}

}
