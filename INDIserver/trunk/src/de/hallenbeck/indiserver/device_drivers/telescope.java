package de.hallenbeck.indiserver.device_drivers;

import android.os.Handler;
import de.hallenbeck.indiserver.communication_drivers.communication_driver;

/**
 * Generic telescope-class with basic functions   
 * @author atuschen
 *
 */
public abstract class telescope implements generic_device_driver {
	
	public communication_driver com_driver=null;
	public static boolean connected=false;

	/**
	 * Constructor of base telescope class
	 * @param driver: String of communication_driver class
	 * @param mHandler: Callback Handler for receiving messages
	 * @param connect: Automatically connect to telescope
	 */
	public telescope(String driver, Handler mHandler, boolean connect) {
		set_communication_driver(driver);
		set_msg_handler(mHandler);
		if (connect) {connect();}
	}
	
	/**
	 * Set the driver for communication with the telescope
	 * @param driver
	 */
	public void set_communication_driver(String driver) {
		
	}
	
	/**
	 * Connect to the telescope
	 * @return
	 */
	public int connect() {
		com_driver.connect("/dev/ttyS0");
		connected=true;
		return 0;
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
	 * @return
	 */
	public int disconnect() {
		connected=false;
		com_driver.disconnect();
		return 0;
	}

}
