
package de.hallenbeck.indiserver.communication_drivers;

// Callback Interface for pl2303 driver
// called by the driver after permission to access the device was granted by the user

public interface pl2303connect {
	public void onConnect();
}
