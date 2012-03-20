package de.hallenbeck.indiserver.device_drivers;


import java.io.IOException;

import de.hallenbeck.indiserver.communication_drivers.communication_driver_interface;

/**
 * Generic telescope-class with basic functions and basic INDI interface
 * 
 * @author atuschen
 *
 */
public abstract class telescope /* extends INDIDriver*/ implements device_driver_interface {
	
	protected communication_driver_interface com_driver=null;
	protected boolean connected=false;
	protected telescope_information information;
	protected telescope_pointing pointing;
	
	protected class telescope_information {
		private String Description;
		private String Version;
		private String DateTime;
		/**
		 * @return the description
		 */
		public String getDescription() {
			return Description;
		}
		/**
		 * @param description the description to set
		 */
		public void setDescription(String description) {
			Description = description;
		}
		/**
		 * @return the version
		 */
		public String getVersion() {
			return Version;
		}
		/**
		 * @param version the version to set
		 */
		public void setVersion(String version) {
			Version = version;
		}
		/**
		 * @return the dateTime
		 */
		public String getDateTime() {
			return DateTime;
		}
		/**
		 * @param dateTime the dateTime to set
		 */
		public void setDateTime(String dateTime) {
			DateTime = dateTime;
		}
	}
	
	protected class telescope_pointing {
		private float RA;
		private float DEC;
		/**
		 * @return the rA
		 */
		public float getRA() {
			return RA;
		}
		/**
		 * @param rA the rA to set
		 */
		public void setRA(float rA) {
			RA = rA;
		}
		/**
		 * @return the dEC
		 */
		public float getDEC() {
			return DEC;
		}
		/**
		 * @param dEC the dEC to set
		 */
		public void setDEC(float dEC) {
			DEC = dEC;
		}
	}

	/**
	 * Set the driver for communication with the telescope
	 * @param driver fully qualified name of driver class
	 */
	public void set_communication_driver(String driver) {
		try {
			com_driver = (communication_driver_interface) Class.forName(driver).newInstance();
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Connect to the telescope
	 */
	public void connect(String device) {
		try {
			com_driver.connect(device);
			connected=true;
		} catch (IOException e) {
			e.printStackTrace();
			connected=false;
		}
	}
	
	/**
	 * Are we connected?
	 * @return
	 */
	public boolean isConnected() {
		return connected;
	}
	
	/**
	 * Disconnect from telescope
	 */
	public void disconnect() {
		connected=false;
		com_driver.disconnect();
	}

}
