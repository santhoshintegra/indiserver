package de.hallenbeck.indiserver.activities;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

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
	public ByteArrayOutputStream OutStream;
	public ByteArrayInputStream InStream;
	public byte[] buf;
	


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		OutStream = new ByteArrayOutputStream();
		InStream = new ByteArrayInputStream(buf);
		
		telescope = new lx200generic(InStream,OutStream);
		telescope.set_communication_driver("de.hallenbeck.indiserver.communication_drivers.bluetooth_serial");
		telescope.connect("00:80:37:14:9F:E7");
		telescope.disconnect();
		
	}

}
