package de.hallenbeck.indiserver.activities;


import de.hallenbeck.indiserver.device_drivers.lx200generic;
import android.app.Activity;
import android.os.Bundle;

/**
 * Main activity. This is where you could control the server, i.e. set the drivers.
 * Setting should be saved in preferences.
 *  
 * @author atuschen
 *
 */
public class main extends Activity {

	public lx200generic telescope;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		telescope = new lx200generic();
		telescope.set_communication_driver("de.hallenbeck.indiserver.communication_drivers.bluetooth_serial");
		telescope.connect("00:80:37:14:9F:E7");
		telescope.disconnect();
		
	}

}
