package de.hallenbeck.indiserver.device_drivers;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import laazotea.indi.driver.INDIBLOBElementAndValue;
import laazotea.indi.driver.INDIBLOBProperty;
import laazotea.indi.driver.INDINumberElementAndValue;
import laazotea.indi.driver.INDINumberProperty;
import laazotea.indi.driver.INDISwitchElementAndValue;
import laazotea.indi.driver.INDISwitchProperty;
import laazotea.indi.driver.INDITextElementAndValue;
import laazotea.indi.driver.INDITextProperty;
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

	
	protected debug_telescope(InputStream inputStream, OutputStream outputStream) {
		super(inputStream, outputStream);
		// TODO Auto-generated constructor stub
	}

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

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void processNewTextValue(INDITextProperty property, Date timestamp,
			INDITextElementAndValue[] elementsAndValues) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processNewSwitchValue(INDISwitchProperty property,
			Date timestamp, INDISwitchElementAndValue[] elementsAndValues) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processNewNumberValue(INDINumberProperty property,
			Date timestamp, INDINumberElementAndValue[] elementsAndValues) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processNewBLOBValue(INDIBLOBProperty property, Date timestamp,
			INDIBLOBElementAndValue[] elementsAndValues) {
		// TODO Auto-generated method stub
		
	}
}
