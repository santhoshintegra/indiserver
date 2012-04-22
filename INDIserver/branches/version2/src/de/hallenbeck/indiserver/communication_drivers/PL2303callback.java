
package de.hallenbeck.indiserver.communication_drivers;

// Callback Interface for pl2303 driver
// called by the driver after permission to access the device was granted by the user

public interface PL2303callback {
	public void onInitSuccess();
	public void onInitFailed(String reason);
	public void onRI();
	public void onDCD();
	public void onDSR();
	public void onCTS();
	
}
