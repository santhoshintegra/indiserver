package de.hallenbeck.indiserver.communication_drivers;

/** 
 * Communication driver for Meade LPI camera
 * @author atuschen
 *
 */
public class usbhost_lpi implements communication_driver_interface {

	public void setTimeout(int timeout) {
		// TODO Auto-generated method stub

	}

	public devicelist[] getDeviceList() {
		// TODO Auto-generated method stub
		return null;
	}

	public int connect(String device) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int disconnect() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int sendCommand(String command) {
		// TODO Auto-generated method stub
		return 0;
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
