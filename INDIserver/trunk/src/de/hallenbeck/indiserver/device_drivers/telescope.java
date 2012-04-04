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
	public void set_communication_driver(String driver) throws ClassNotFoundException {
		try {
			if (driver != null) com_driver = (communication_driver_interface) Class.forName(driver).newInstance();
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
	public void connect() throws IOException {
		if ((!connected) && (com_driver != null) && (device != null)) {
			com_driver.connect(device);
			connected=true;
		} else {
			if (com_driver == null) throw new IOException("Telescope: Communication Driver not set");
			if (device == null) throw new IOException("Telescope: Device not set");
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
		if ((connected) && (com_driver != null)) {
		com_driver.disconnect();
		connected=false;
		}
	}

}
