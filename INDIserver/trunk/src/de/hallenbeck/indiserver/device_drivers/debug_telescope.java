package de.hallenbeck.indiserver.device_drivers;

import de.hallenbeck.indiserver.communication_drivers.communication_driver_interface;
import android.os.Handler;


/**
 * Dummy driver for debugging purposes only
 * @author atuschen
 *
 */
public class debug_telescope extends telescope implements device_driver_interface {

	/*public debug_telescope(String driver, Handler mHandler) {
		super(driver, mHandler, false);
		
	}*/

	
	public void sendINDImsg(String xmlcommand) {
		// TODO Auto-generated method stub
		
	}

	public String recvINDImsg() {
		// TODO Auto-generated method stub
		return null;
	}

	public void set_msg_handler(Handler mHandler) {
		// TODO Auto-generated method stub
		
	}

	public void set_communication_driver(communication_driver_interface driver) {
		// TODO Auto-generated method stub
		
	}
}
