
package de.hallenbeck.indiserver.communication_drivers;

// Callback Interface for pl2303 driver
// called by the driver after permission to access the device was granted by the user

public interface PL2303callback {
	public void pl2303_ConnectSuccess();
	public void pl2303_ConnectFailed(String reason);
}
