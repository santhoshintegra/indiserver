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

package de.hallenbeck.indiserver.communication_drivers;

/**
 * PL2303 USB Serial Adapter Driver for
 * devices with android usbhost-support (3.2 upwards)
 * 
 * @author atuschen
 *
 */
public class usbhost_serial_pl2303 extends serial implements communication_driver_interface {

	public void setTimeout(int timeout) {
		// TODO Auto-generated method stub

	}

	public void connect(String device) {
		// TODO Auto-generated method stub
	}

	public void disconnect() {
		// TODO Auto-generated method stub
	}

	public void sendCommand(String command) {
		// TODO Auto-generated method stub
	}

	public void wait(int delay) {
		// TODO Auto-generated method stub

	}

	public int getAnswerInt() {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getAnswerString() {
		// TODO Auto-generated method stub
		return null;
	}

}
