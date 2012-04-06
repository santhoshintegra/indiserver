/*
 *  This file is part of INDI for Java Server.
 * 
 *  INDI for Java Server is free software: you can redistribute it
 *  and/or modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation, either version 3 of 
 *  the License, or (at your option) any later version.
 * 
 *  INDI for Java Server is distributed in the hope that it will be
 *  useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 *  of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with INDI for Java Server.  If not, see 
 *  <http://www.gnu.org/licenses/>.
 */
package laazotea.indi.server;

import java.util.ArrayList;
import laazotea.indi.XMLToString;
import org.w3c.dom.Element;

/**
 * A class that represents a listener to devices. It is used to include both
 * usual Clients and Devices, as Drivers can also snoop Properties from other
 * Devices according to the INDI protocol.
 *
 * @author S. Alonso (Zerjillo) [zerjio at zerjio.com]
 * @version 1.21, April 4, 2012
 */
public abstract class INDIDeviceListener {

  /**
   * Determines if the object listens to all devices.
   */
  private boolean listenToAllDevices;
  /**
   * A list of devices that are listened.
   */
  private ArrayList<String> devicesToListen;
  /**
   * A list of properties that are listened. It consists on pairs of device,property names, separated by &|&
   */
  private ArrayList<String> propertiesToListen; 

  /**
   * Constructs a new <code>INDIDeviceListener</code>.
   */
  protected INDIDeviceListener() {
    listenToAllDevices = false;

    devicesToListen = new ArrayList<String>();
    propertiesToListen = new ArrayList<String>();
  }

  /**
   * 
   * @return <code>true</code> if the listener listens to all the devices. <code>false</code> otherwise.
   */
  public boolean listensToAllDevices() {
    return listenToAllDevices;
  }

  /**
   * Adds a new Device to be listened.
   * @param deviceName The Device name to be listened.
   */
  protected void addDeviceToListen(String deviceName) {
    devicesToListen.add(deviceName);
  }

  /**
   * Adds a new Property to be listened.
   * @param deviceName The Device name owner of the Property
   * @param propertyName The Property name to be listened.
   */
  protected void addPropertyToListen(String deviceName, String propertyName) {
    propertiesToListen.add(deviceName + "&|&" + propertyName);
  }

  /**
   * Sets the listenToAllDevices flag.
   * @param listenToAllDevices The new value of the flag.
   */
  protected void setListenToAllDevices(boolean listenToAllDevices) {
    this.listenToAllDevices = listenToAllDevices;
  }

  /**
   * Determines if the listener listens to a Device.
   * @param deviceName The Device name to check.
   * @return <code>true</code> if the listener listens to the Device. <code>false</code> otherwise.
   */
  protected boolean listensToDevice(String deviceName) {
    if (listenToAllDevices) {
      return true;
    }

    if (devicesToListen.contains(deviceName)) {
      return true;
    }

    return false;
  }

  
  /**
   * Determines if the listener listens to a Property.
   * @param deviceName The Device name to which the Property belongs.
   * @param propertyName The Property name to check.
   * @return <code>true</code> if the listener listens to the Property. <code>false</code> otherwise.
   */
  protected boolean listensToProperty(String deviceName, String propertyName) {
    if (listensToDevice(deviceName)) {
      return true;
    }

    if (propertiesToListen.contains(deviceName + "&|&" + propertyName)) {
      return true;
    }

    return false;
  }

  /**
   * Determines if the listener listens to specifically one Property of a Device.
   * @param deviceName The Device name to check.
   * @return <code>true</code> if the listener listens specifically to any Property of the Device. <code>false</code> otherwise.
   */
  protected boolean listensToSingleProperty(String deviceName) {
    for (int i = 0; i < propertiesToListen.size(); i++) {
      if (propertiesToListen.get(i).startsWith(deviceName + "&|&")) {
        return true;
      }
    }

    return false;
  }

  /**
   * Sends a XML message to the listener.
   * @param xml The message to be sent.
   */
  public void sendXMLMessage(Element xml) {
    String message = XMLToString.transform(xml);

    sendXMLMessage(message);
  }

  /**
   * Sends a String (usually containing some XML) to the listener.
   * @param xml The string to be sent.
   */
  protected abstract void sendXMLMessage(String xml);
}
