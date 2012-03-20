package de.hallenbeck.indiserver.device_drivers;

import java.io.InputStream;
import java.io.OutputStream;


/**
 * Driver for LX200GPS devices, derived from lx200generic
 * @author atuschen
 *
 */
public class lx200gps extends lx200generic implements device_driver_interface {

	protected lx200gps(InputStream inputStream, OutputStream outputStream) {
		super(inputStream, outputStream);
		// TODO Auto-generated constructor stub
	}

	/*public lx200gps(String driver, Handler mHandler) {
		super(driver, mHandler);
		// TODO Auto-generated constructor stub
	}*/

}
