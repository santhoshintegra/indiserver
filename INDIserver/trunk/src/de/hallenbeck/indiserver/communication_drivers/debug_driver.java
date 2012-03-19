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

	public void connect(String device) {
		Log.d(TAG,"Connect "+device);
	}

	public void disconnect() {
		Log.d(TAG,"Disconnect");
	}

	public void sendCommand(String command) {
		Log.d(TAG,"Send command "+command);
	}

	public void set_delay(int delay) {
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
