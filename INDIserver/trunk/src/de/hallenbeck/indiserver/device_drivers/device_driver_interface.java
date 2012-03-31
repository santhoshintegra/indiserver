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


/**
 * Generic interface definition for device drivers 
 * @author atuschen
 *
 */
public interface device_driver_interface {

	/**
	 * Set the driver for communication with the telecope
	 * @param driver
	 */
	public void set_communication_driver (String driver);
	
	// TODO: add connect/disconnect/isConnected here.
	
	public void set_device(String sdevice);
	
	public void connect();
	
	public boolean isConnected();
	
	public void disconnect();
}
