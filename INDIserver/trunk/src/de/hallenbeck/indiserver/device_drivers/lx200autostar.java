package de.hallenbeck.indiserver.device_drivers;

import android.os.Handler;

/**
 * Driver for Autostar-driven telescopes, derived from lx200generic
 * @author atuschen
 *
 */
public class lx200autostar extends lx200generic implements generic_device_driver {

	public lx200autostar(String driver, Handler mHandler) {
		super(driver, mHandler);
		// TODO Auto-generated constructor stub
	}

}
