package de.hallenbeck.indiserver.device_drivers;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import laazotea.indi.driver.INDIDriver;

import de.hallenbeck.indiserver.communication_drivers.communication_driver_interface;

/**
 * Generic telescope-class with basic functions and basic INDI interface
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

	/* Simulation Parameters */
	protected final static int	SLEWRATE = 1;		/* slew rate, degrees/s */
	protected final static double SIDRATE =	0.004178;	/* sidereal rate, degrees/s */

	/* Handy Macros 
	#define currentRA	EquatorialCoordsRN[0].value
	#define currentDEC	EquatorialCoordsRN[1].value
	#define targetRA	EquatorialCoordsWN[0].value
	#define targetDEC	EquatorialCoordsWN[1].value
	*/
	
	protected telescope(InputStream inputStream, OutputStream outputStream) {
		super(inputStream, outputStream);
	}

	protected communication_driver_interface com_driver=null;
	protected boolean connected=false;

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
	
	/**
	 * Connect to the telescope
	 */
	public void connect(String device) {
		try {
			com_driver.connect(device);
			connected=true;
		} catch (IOException e) {
			e.printStackTrace();
			connected=false;
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
		connected=false;
		com_driver.disconnect();
	}

}
