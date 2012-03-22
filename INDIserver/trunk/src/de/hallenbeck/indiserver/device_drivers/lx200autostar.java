package de.hallenbeck.indiserver.device_drivers;

import java.io.InputStream;
import java.io.OutputStream;



/**
 * Driver for Autostar-driven telescopes, derived from lx200generic
 * @author atuschen
 *
 */
public class lx200autostar extends lx200basic implements device_driver_interface {

	protected lx200autostar(InputStream inputStream, OutputStream outputStream) {
		super(inputStream, outputStream);
		// TODO Auto-generated constructor stub
	}

	/*public lx200autostar(String driver, Handler mHandler) {
		super(driver, mHandler);
		// TODO Auto-generated constructor stub
	}*/

}
