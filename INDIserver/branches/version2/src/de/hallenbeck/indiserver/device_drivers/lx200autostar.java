package de.hallenbeck.indiserver.device_drivers;

import java.io.IOException;
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

public class lx200autostar extends lx200basic implements device_driver_interface {
	
	private final static String driverName = "LX200autostar";

	public lx200autostar(InputStream in, OutputStream out) {
		super(in, out);

	}
	
	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.lx200basic#getName()
	 */
	@Override
	public String getName() {
		return driverName;
	}

	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.lx200basic#connect()
	 */
	@Override
	public void connect() throws IOException {
		// TODO Auto-generated method stub
		super.connect();
		
		// EXPERIMENTAL! Works only with Firmware 43Eg (Display messages are localized!)
		// Navigate Handbox to main menu after power-on, skipping all data entry prompts,
		// because Handbox does NOT save any parameters as long as it is NOT in main menu.
		if (FirmwareVersionT.getValue().compareTo("43Eg")==0) {

			if (getDisplayMessage().compareTo("*Press 0 to Alignor MODE for Menu")==0) {
				int i=0;
				while (i < 7) {
					// "Press" the MODE-Key 7 times
					sendCommand("#:EK9#");
					i++;
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.lx200basic#processNewTextValue(laazotea.indi.driver.INDITextProperty, java.util.Date, laazotea.indi.driver.INDITextElementAndValue[])
	 */
	@Override
	public void processNewTextValue(INDITextProperty property, Date timestamp,
			INDITextElementAndValue[] elementsAndValues) {
		// TODO Auto-generated method stub
		super.processNewTextValue(property, timestamp, elementsAndValues);
	}

	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.lx200basic#processNewSwitchValue(laazotea.indi.driver.INDISwitchProperty, java.util.Date, laazotea.indi.driver.INDISwitchElementAndValue[])
	 */
	@Override
	public void processNewSwitchValue(INDISwitchProperty property,
			Date timestamp, INDISwitchElementAndValue[] elementsAndValues) {
		// TODO Auto-generated method stub
		super.processNewSwitchValue(property, timestamp, elementsAndValues);
	}

	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.lx200basic#processNewNumberValue(laazotea.indi.driver.INDINumberProperty, java.util.Date, laazotea.indi.driver.INDINumberElementAndValue[])
	 */
	@Override
	public void processNewNumberValue(INDINumberProperty property,
			Date timestamp, INDINumberElementAndValue[] elementsAndValues) {
		// TODO Auto-generated method stub
		super.processNewNumberValue(property, timestamp, elementsAndValues);
	}

	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.lx200basic#processNewBLOBValue(laazotea.indi.driver.INDIBLOBProperty, java.util.Date, laazotea.indi.driver.INDIBLOBElementAndValue[])
	 */
	@Override
	public void processNewBLOBValue(INDIBLOBProperty property, Date timestamp,
			INDIBLOBElementAndValue[] elementsAndValues) {
		// TODO Auto-generated method stub
		super.processNewBLOBValue(property, timestamp, elementsAndValues);
	}

	/* (non-Javadoc)
	 * @see de.hallenbeck.indiserver.device_drivers.lx200basic#disconnect()
	 */
	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		super.disconnect();
	}

}
