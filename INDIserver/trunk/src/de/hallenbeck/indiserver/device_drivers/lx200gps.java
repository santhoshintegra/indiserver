package de.hallenbeck.indiserver.device_drivers;

import android.os.Handler;

/**
 * Driver for LX200GPS devices, derived from lx200generic
 * @author atuschen
 *
 */
public class lx200gps extends lx200generic implements device_driver_interface {

	public lx200gps(String driver, Handler mHandler) {
		super(driver, mHandler);
		// TODO Auto-generated constructor stub
	}

}
