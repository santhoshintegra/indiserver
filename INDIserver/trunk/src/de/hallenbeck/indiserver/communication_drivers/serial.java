package de.hallenbeck.indiserver.communication_drivers;

/**
 * Serial communication driver for devices with /dev/ttyS or /dev/ttyUSB support
 * Base class for all other communication drivers as they all use serial communication
 * @author atuschen
 *
 */

public class serial implements communication_driver {

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
