package de.hallenbeck.indiserver.communication_drivers;

import android.util.Log;

/**
 * Dummy driver for debugging purposes only
 * @author atuschen
 *
 */
public class debug_driver implements communication_driver_interface {
	
	private final String TAG="Com-Driver";

	public void setTimeout(int timeout) {
		Log.d(TAG,"Set timeout "+timeout);
	}

	public devicelist[] getDeviceList() {
		Log.d(TAG,"Get devicelist");
		return null;
	}

	public int connect(String device) {
		Log.d(TAG,"Connect "+device);
		return 0;
	}

	public int disconnect() {
		Log.d(TAG,"Disconnect");
		return 0;
	}

	public int sendCommand(String command) {
		Log.d(TAG,"Send command "+command);
		return 0;
	}

	public void wait(int delay) {
		Log.d(TAG,"Wait "+delay);
	}

	public int getAnswerInt() {
		Log.d(TAG,"Get Answer Int");
		return 1234;
	}

	public String getAnswerString() {
		Log.d(TAG,"Get Answer String");
		return "dummy";
	}

}
