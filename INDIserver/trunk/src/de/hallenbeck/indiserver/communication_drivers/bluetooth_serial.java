package de.hallenbeck.indiserver.communication_drivers;

/**
 * Bluetooth-Serial-Port-Profile driver for short-range wireless connection to telescope
 * @author atuschen
 *
 */

public class bluetooth_serial extends serial implements communication_driver_interface {


	public int connect(String device) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int disconnect() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void setTimeout(int timeout) {
		// TODO Auto-generated method stub
		
	}

	public int sendCommand(String command) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getAnswerInt() {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getAnswerString() {
		// TODO Auto-generated method stub
		return null;
	}

	public void wait(int delay) {
		// TODO Auto-generated method stub
		
	}

	

}
