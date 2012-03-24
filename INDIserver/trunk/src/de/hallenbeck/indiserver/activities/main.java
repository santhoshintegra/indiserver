package de.hallenbeck.indiserver.activities;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;

import de.hallenbeck.indiserver.device_drivers.lx200basic;
import android.app.Activity;
import android.os.Bundle;

/**
 * Main activity. This is where you could control the server, i.e. set the drivers.
 * Setting should be saved in preferences.
 * 
 * For the moment this activity is for testing purposes only!
 *  
 * @author atuschen
 *
 */
public class main extends Activity {

	public lx200basic telescope;
	public OutputStream OutStream;
	public InputStream InStream;
	public ServerSocket Sock;
	public Socket test;
	public ServerThread SThread;

	/** TESTING ONLY **/
	class ServerThread extends Thread {
		public void run() {
			try {
				Sock = new ServerSocket(7624);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				test = Sock.accept();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				InStream  = test.getInputStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				OutStream = test.getOutputStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			telescope = new lx200basic(InStream,OutStream);
			telescope.set_communication_driver("de.hallenbeck.indiserver.communication_drivers.bluetooth_serial");
			telescope.connect("00:80:37:14:9F:E7");
				
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		SThread = new ServerThread();
		SThread.start();
		
	}

}
