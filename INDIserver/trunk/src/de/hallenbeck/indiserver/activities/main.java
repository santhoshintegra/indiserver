package de.hallenbeck.indiserver.activities;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import de.hallenbeck.indiserver.R;
import de.hallenbeck.indiserver.device_drivers.device_driver_interface;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

/**
 * Main activity. This is where you could control the server, i.e. set the drivers.
 * Setting should be saved in preferences.
 * 
 * 
 *  
 * @author atuschen
 *
 */
public class main extends Activity {

	public static ServerThread SThread;
	private static final int maxClients = 8;
	public DriverThread DThread;

	private Handler mHandler;
	static TextView nClients;
	
	
	/**
	 * TCP ConnectionThread 
	 * @author atuschen
	 *
	 */
	class ConnectionThread extends Thread {
		private BufferedReader in;
		private BufferedWriter out;
		private char[] buffer;
		
		public ConnectionThread (Socket sock) {
			buffer = new char[8192];
			try {
				in  = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		public void run() {
			boolean connected = true;
			SThread.IncreaseConnectionCount();	
			while (connected) {
				try {
						int len = in.read(buffer,0,8192);
						if (len != -1) {
							DThread.write(buffer,len);	
						} else {
							SThread.DecreaseConnectionCount();
							connected = false;
						}
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
		
		public synchronized void write(char[] data, int len) {
			try {
				out.write(data, 0, len);
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * TCP Server Thread
	 * @author atuschen
	 *
	 */
	class ServerThread extends Thread {
		private Thread[] ConnectionThreads;
		private int ConnectionCount = 0;

		public void run() {
			ConnectionThreads = new Thread[maxClients];
			while (true) {
				try {
					ServerSocket Sock = new ServerSocket(7624);
					Socket sock = Sock.accept();
					
					int connections = getConnectionCount();
					
					if ( connections < maxClients) {
						ConnectionThreads[connections] = new ConnectionThread(sock);
						ConnectionThreads[connections].start();
					}
				
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}	

		}
		
		public int getConnectionCount() {
			return ConnectionCount;
		}
		
		
		public synchronized void IncreaseConnectionCount() {
			ConnectionCount++;
			mHandler.post(new Runnable() { 
			     public void run() { updateClients(); } 
			});
			
		}
		
		public synchronized void DecreaseConnectionCount() {
			ConnectionCount--;
			mHandler.post(new Runnable() { 
			     public void run() { updateClients(); } 
			});
			
		}	
		
	}
	
	/**
	 * Driver start Thread
	 * @author atuschen
	 *
	 */
	public class StartThread extends Thread {
		private String DeviceDriver;
		private String ComDriver;
		private String Device;
		private device_driver_interface devicedriver = null;
		
		public StartThread(String deviceDriver, String comDriver, String device) {
			DeviceDriver = deviceDriver;
			ComDriver = comDriver;
			Device = device;
		}
		
		public void run() {
			try {
				devicedriver = (device_driver_interface) Class.forName(DeviceDriver).newInstance();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			}
			devicedriver.set_communication_driver(ComDriver);
			devicedriver.set_device(Device);	
		}
	}
	
	
	/**
	 * Driver Connection Thread
	 * @author atuschen
	 *
	 */
	class DriverThread extends Thread {
		private BufferedReader in;
		private BufferedWriter out;
		private char[] buffer;
		private StartThread Driver;
		
		public DriverThread(String deviceDriver, String comDriver, String device) {
			
			buffer = new char[8192];
			LocalSocket sock = new LocalSocket();
			
			// Start the Driver 
			Driver = new StartThread(deviceDriver, comDriver, device);
			Driver.start();
			
			// Connect to the driver
			try {
				sleep(1000); //let the driver start, so wait a moment 
				LocalSocketAddress address = new LocalSocketAddress(deviceDriver);
				sock = new LocalSocket();
				sock.connect(address);
				in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void run() {
			int len=0;
			while(true) {
				try {
						len = in.read(buffer, 0, 8192);
						if (len != -1) {
							int i=0;
							while (i < SThread.getConnectionCount()) {
								((ConnectionThread) SThread.ConnectionThreads[i]).write(buffer,len);
								i++;
							}
						}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
					
			}
		}
		
		public synchronized void write(char[] data,int len) {
			try {
				out.write(data,0,len);
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
	
	
	
	

	public void updateClients() {
		int num = SThread.getConnectionCount();
		nClients.setText(String.valueOf(num));
		
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

		Notification.Builder notificationbuilder = new Notification.Builder(getApplicationContext());
		
		notificationbuilder.setContentTitle("INDIserver running");
		notificationbuilder.setContentText(String.format("%d Clients connected",num));
		notificationbuilder.setTicker(String.format("%d Clients connected",num));
		notificationbuilder.setSmallIcon(R.drawable.ic_launcher);
		notificationbuilder.setOngoing(true);

		Notification not = notificationbuilder.getNotification();
		
		mNotificationManager.notify(2, not);
		
	}
	
	/**
	 * Main Program
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHandler = new Handler();
		
		// Start the Server thread, that listens for incoming TCP-connections
		SThread = new ServerThread();
		SThread.start();
		
		// Start the DriverThread
		DThread = new DriverThread( "de.hallenbeck.indiserver.device_drivers.lx200basic",
									"de.hallenbeck.indiserver.communication_drivers.bluetooth_serial",
									"00:80:37:14:9F:E7" );
		DThread.start();
		
		
		setContentView(R.layout.main);
		nClients = (TextView)findViewById(R.id.text);
        nClients.setText("0"); 
 
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}
	
	
	
	
	
	
}




























