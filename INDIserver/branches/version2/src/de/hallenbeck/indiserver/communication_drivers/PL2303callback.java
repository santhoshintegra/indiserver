
package de.hallenbeck.indiserver.communication_drivers;

// Callback Interface for pl2303 driver
// called by the driver after permission to access the device was granted by the user

public interface PL2303callback {
	public void onInitSuccess();
	public void onInitFailed(String reason);
	public void onRI(boolean state);
	public void onDCD(boolean state);
	public void onDSR(boolean state);
	public void onCTS(boolean state);
	
}
