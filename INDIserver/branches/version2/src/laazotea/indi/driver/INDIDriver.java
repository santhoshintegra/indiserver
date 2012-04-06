/*
 *  This file is part of INDI Driver for Java.
 * 
 *  INDI Driver for Java is free software: you can redistribute it
 *  and/or modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation, either version 3 of 
 *  the License, or (at your option) any later version.
 * 
 *  INDI Driver for Java is distributed in the hope that it will be
 *  useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 *  of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with INDI Driver for Java.  If not, see 
 *  <http://www.gnu.org/licenses/>.
 */
package laazotea.indi.driver;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import laazotea.indi.Constants.PropertyPermissions;
import laazotea.indi.Constants.PropertyStates;
import laazotea.indi.Constants.SwitchRules;
import laazotea.indi.Constants.SwitchStatus;
import laazotea.indi.INDIBLOBValue;
import laazotea.indi.INDIDateFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.net.LocalServerSocket;
import android.net.LocalSocket;


/**
 * A class representing a Driver in the INDI Protocol. INDI Drivers should extend this class. It is in charge of stablishing the connection to the clients and parsing / formating any incoming / leaving messages.
 *
 * @author S. Alonso (Zerjillo) [zerjio at zerjio.com]
 * @version 1.10, March 19, 2012
 */
public abstract class INDIDriver {

  private BufferedReader in;
  private PrintWriter out;

  /**
   * A list of Properties for this Driver
   */
  private LinkedHashMap<String, INDIProperty> properties;


  /**
   * Constructs a INDIDriver with a particular <code>inputStream<code> from which to read the incoming messages (from clients) and a <code>outputStream</code> to write the messages to the clients.
   * @param inputStream The stream from which to read messages.
   * @param outputStream The stream to which to write the messages.
   */
  protected INDIDriver(InputStream inputStream, OutputStream outputStream) {
    this.out = new PrintWriter(outputStream);
    in = new BufferedReader(new InputStreamReader(inputStream));

    properties = new LinkedHashMap<String, INDIProperty>();
  }
  
  protected INDIDriver() {
	  	LocalServerSocket serversock;
		try {
			serversock = new LocalServerSocket(getName());
			LocalSocket sock = serversock.accept();
		    this.out = new PrintWriter(sock.getOutputStream());
		    in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  	
	    properties = new LinkedHashMap<String, INDIProperty>();
	    startListening();
	  }

  /**
   * Gets the name of the Driver.
   * @return The name of the Driver.
   */
  public abstract String getName();

  /**
   * Starts listening to inputStream. It creates a new Thread to make the readings. Thus, the normal execution of the code is not stopped. This method is not usually called by the Driver itself but the encapsulating class (for example <code>INDIDriverRunner</code>.
   * @see INDIDriverRunner
   */
  public void startListening() {
    INDIDriverThread t = new INDIDriverThread(this);

    t.start();
  }

  /**
   * Gets the input <code>BufferedReader</code>.
   * @return The input <code>BufferedReader</code>.
   */
  protected BufferedReader getIn() {
    return in;
  }

  /**
   * Gets the output <code>PrintWriter</code>.
   * @return The output <code>PrintWriter</code>.
   */
  protected PrintWriter getOut() {
    return out;
  }
  
  /**
   * Adds a <code>INDISwitchProperty</code> called "CONNECTION" with two Elements called "CONNECT" and "DISCONNECT". The DISCONNECT Element is ON, while the CONNECT Element is OFF. It is a Read / write property with "one of many" rule (thus, one option is always selected).
   */
  protected void addConnectionProperty() {
    INDISwitchElement s1 = new INDISwitchElement("CONNECT", "Connect", SwitchStatus.OFF);
    INDISwitchElement s2 = new INDISwitchElement("DISCONNECT", "Disconnect", SwitchStatus.ON);
    INDISwitchProperty switch1 = new INDISwitchProperty(this, "CONNECTION", "Connection", "Main Control", PropertyStates.IDLE, PropertyPermissions.RW, 10, SwitchRules.ONE_OF_MANY);
    switch1.addElement(s1);
    switch1.addElement(s2);
    
    addProperty(switch1);     
  }


  /**
   * Parses the XML messages. Should not be called by particular Drivers.
   *
   * @param doc the messages to be parsed.
   */
  protected void parseXML(Document doc) {
    Element el = doc.getDocumentElement();

    if (el.getNodeName().compareTo("INDI") != 0) {
      return;
    }

    NodeList nodes = el.getChildNodes();

    for (int i = 0; i < nodes.getLength(); i++) {
      Node n = nodes.item(i);

      if (n instanceof Element) {
        Element child = (Element) n;

        String name = child.getNodeName();

        if (name.equals("getProperties")) {
          processGetProperties(child);
        } else if (name.equals("newTextVector")) {
          processNewTextVector(child);
        } else if (name.equals("newSwitchVector")) {
          processNewSwitchVector(child);
        } else if (name.equals("newNumberVector")) {
          processNewNumberVector(child);
        } else if (name.equals("newBLOBVector")) {
          processNewBLOBVector(child);
        }
      }
    }
  }

  /**
   * Parses a &lt;newTextVector&gt; XML message.
   * @param xml The &lt;newTextVector&gt; XML message to be parsed.
   */
  private void processNewTextVector(Element xml) {
    INDIProperty prop = processNewXXXVector(xml);

    if (prop == null) {
      return;
    }

    if (!(prop instanceof INDITextProperty)) {
      return;
    }

    INDIElementAndValue[] evs = processINDIElements(prop, xml);

    Date timestamp = INDIDateFormat.parseTimestamp(xml.getAttribute("timestamp"));

    INDITextElementAndValue[] newEvs = Arrays.copyOf(evs, evs.length, INDITextElementAndValue[].class);

    processNewTextValue((INDITextProperty) prop, timestamp, newEvs);
  }

  /**
   * Called when a new Text Vector message has been received from a Client. Must be implemented in Drivers to take care of the new values sent by clients. It will be called with correct Properties and Elements. Any incorrect Text Message received will be discarded and this method will not be called.
   * @param property The Text Property asked to change.
   * @param timestamp The timestamp of the received message
   * @param elementsAndValues An array of pairs of Text Elements and its requested values to be parsed.
   */
  public abstract void processNewTextValue(INDITextProperty property, Date timestamp, INDITextElementAndValue[] elementsAndValues);

  /**
   * Parses a &lt;newSwitchVector&gt; XML message.
   * @param xml The &lt;newSwitchVector&gt; XML message to be parsed.
   */  
  private void processNewSwitchVector(Element xml) {
    INDIProperty prop = processNewXXXVector(xml);

    if (prop == null) {
      return;
    }

    if (!(prop instanceof INDISwitchProperty)) {
      return;
    }

    INDIElementAndValue[] evs = processINDIElements(prop, xml);

    Date timestamp = INDIDateFormat.parseTimestamp(xml.getAttribute("timestamp"));

        INDISwitchElementAndValue[] newEvs = Arrays.copyOf(evs, evs.length, INDISwitchElementAndValue[].class);
        
    processNewSwitchValue((INDISwitchProperty) prop, timestamp, newEvs);
  }

  /**
   * Called when a new Switch Vector message has been received from a Client. Must be implemented in Drivers to take care of the new values sent by clients. It will be called with correct Properties and Elements. Any incorrect Switch Message received will be discarded and this method will not be called.
   * @param property The Switch Property asked to change.
   * @param timestamp The timestamp of the received message
   * @param elementsAndValues An array of pairs of Switch Elements and its requested values to be parsed.
   */  
  public abstract void processNewSwitchValue(INDISwitchProperty property, Date timestamp, INDISwitchElementAndValue[] elementsAndValues);

  /**
   * Parses a &lt;newNumberVector&gt; XML message.
   * @param xml The &lt;newNumberVector&gt; XML message to be parsed.
   */    
  private void processNewNumberVector(Element xml) {
    INDIProperty prop = processNewXXXVector(xml);

    if (prop == null) {
      return;
    }

    if (!(prop instanceof INDINumberProperty)) {
      return;
    }

    INDIElementAndValue[] evs = processINDIElements(prop, xml);

    Date timestamp = INDIDateFormat.parseTimestamp(xml.getAttribute("timestamp"));

        INDINumberElementAndValue[] newEvs = Arrays.copyOf(evs, evs.length, INDINumberElementAndValue[].class);
    
    processNewNumberValue((INDINumberProperty) prop, timestamp, newEvs);
  }

  /**
   * Called when a new Number Vector message has been received from a Client. Must be implemented in Drivers to take care of the new values sent by clients. It will be called with correct Properties and Elements. Any incorrect Number Message received will be discarded and this method will not be called.
   * @param property The Number Property asked to change.
   * @param timestamp The timestamp of the received message
   * @param elementsAndValues An array of pairs of Number Elements and its requested values to be parsed.
   */    
  public abstract void processNewNumberValue(INDINumberProperty property, Date timestamp, INDINumberElementAndValue[] elementsAndValues);

  /**
   * Parses a &lt;newBLOBVector&gt; XML message.
   * @param xml The &lt;newBLOBVector&gt; XML message to be parsed.
   */   
  private void processNewBLOBVector(Element xml) {
    INDIProperty prop = processNewXXXVector(xml);

    if (prop == null) {
      return;
    }

    if (!(prop instanceof INDIBLOBProperty)) {
      return;
    }

    INDIElementAndValue[] evs = processINDIElements(prop, xml);

    Date timestamp = INDIDateFormat.parseTimestamp(xml.getAttribute("timestamp"));

        INDIBLOBElementAndValue[] newEvs = Arrays.copyOf(evs, evs.length, INDIBLOBElementAndValue[].class);
        
    processNewBLOBValue((INDIBLOBProperty) prop, timestamp, newEvs);
  }

  /**
   * Called when a new BLOB Vector message has been received from a Client. Must be implemented in Drivers to take care of the new values sent by clients. It will be called with correct Properties and Elements. Any incorrect BLOB Message received will be discarded and this method will not be called.
   * @param property The BLOB Property asked to change.
   * @param timestamp The timestamp of the received message
   * @param elementsAndValues An array of pairs of BLOB Elements and its requested values to be parsed.
   */   
  public abstract void processNewBLOBValue(INDIBLOBProperty property, Date timestamp, INDIBLOBElementAndValue[] elementsAndValues);

  /**
   * Returns an array of Elements and its corresponded requested values from a XML message.
   * @param property The property from which to parse the Elements.
   * @param xml The XML message
   * @return An array of Elements and its corresponding requested values
   */
  private INDIElementAndValue[] processINDIElements(INDIProperty property, Element xml) {
        
    String oneType;
    if (property instanceof INDITextProperty) {
      oneType = "oneText";
    } else if (property instanceof INDIBLOBProperty) {
      oneType = "oneBLOB";
    } else if (property instanceof INDINumberProperty) {
      oneType = "oneNumber";
    } else if (property instanceof INDISwitchProperty) {
      oneType = "oneSwitch";
    } else {
      return new INDIElementAndValue[0];
    }

    ArrayList<INDIElementAndValue> list = new ArrayList<INDIElementAndValue>();

    NodeList nodes = xml.getChildNodes();

    for (int i = 0; i < nodes.getLength(); i++) {
      Node n = nodes.item(i);

      if (n instanceof Element) {
        Element child = (Element) n;

        String name = child.getNodeName();

        if (name.equals(oneType)) {
          INDIElementAndValue ev = processOneXXX(property, child);

          if (ev != null) {
            list.add(ev);
          }
        }
      }
    }

    return list.toArray(new INDIElementAndValue[0]);
  }

  /**
   * Processes a XML &lt;oneXXX&gt; message for a property.
   * @param property The property from which to parse the Element.
   * @param xml The &lt;oneXXX&gt; XML message
   * @return A Element and its corresponding requested value
   */
  private INDIElementAndValue processOneXXX(INDIProperty property, Element xml) {
    if (!xml.hasAttribute("name")) {
      return null;
    }

    String elName = xml.getAttribute("name");

    INDIElement el = property.getElement(elName);

    if (el == null) {
      return null;
    }

    Object value;

    try {
      value = el.parseOneValue(xml);
    } catch (IllegalArgumentException e) {
      return null;
    }

    if (el instanceof INDITextElement) {
      return new INDITextElementAndValue((INDITextElement) el, (String) value);
    } else if (el instanceof INDISwitchElement) {
      return new INDISwitchElementAndValue((INDISwitchElement) el, (SwitchStatus) value);
    } else if (el instanceof INDINumberElement) {
      return new INDINumberElementAndValue((INDINumberElement) el, (Double) value);
    } else if (el instanceof INDIBLOBElement) {
      return new INDIBLOBElementAndValue((INDIBLOBElement) el, (INDIBLOBValue) value);
    }

    return null;
  }

  /**
   * Processes a &lt;newXXXVector&gt; message.
   * @param xml The XML message
   * @return The INDI Property to which the <code>xml</code> message refers.
   */
  private INDIProperty processNewXXXVector(Element xml) {
    if ((!xml.hasAttribute("device")) || (!xml.hasAttribute("name"))) {
      return null;
    }

    String devName = xml.getAttribute("device");
    String propName = xml.getAttribute("name");

    if (devName.compareTo(getName()) != 0) {  // If the message is not for this device
      return null;
    }

    INDIProperty prop = getProperty(propName);

    return prop;
  }

 /**
   * Processes a &lt;getProperties&gt; message.
   * @param xml The XML message
   */  
  private void processGetProperties(Element xml) {
    if (!xml.hasAttribute("version")) {
      System.err.println("getProperties: no version specified\n");

      return;
    }

    if (xml.hasAttribute("device")) {
      String deviceName = xml.getAttribute("device").trim();

      if (deviceName.compareTo(deviceName) != 0) {  // not asking for this driver
        return;
      }
    }

    if (xml.hasAttribute("name")) {
      String propertyName = xml.getAttribute("name");
      INDIProperty p = getProperty(propertyName);

      if (p != null) {
        sendDefXXXVectorMessage(p, null);
      }
    } else {  // Send all of them
      ArrayList<INDIProperty> props = getPropertiesAsList();

      for (int i = 0; i < props.size(); i++) {
        sendDefXXXVectorMessage(props.get(i), null);
      }
    }
  }

  /**
   * Disconnects and exits the driver. Usually called when the connection brokes.
   */
  protected void disconnect() {
    System.err.println("Connection closed, exiting.");
    System.exit(0);
  }

  /**
   * Adds a new Property to the Device. A message about it will be send to the clients. Drivers must call this method if they want to define a new Property.
   * @param property The Property to be added.
   */
  protected void addProperty(INDIProperty property) {
    //addProperty(property, null);
	//TODO: not send anything to the clients on initial creation
	//KStars crashes on connect
    if (!properties.containsValue(property)) {
        properties.put(property.getName(), property);
    }
  }

  /**
   * Adds a new Property to the Device with a <code>message</code> to the client. A message about it will be send to the clients. Drivers must call this method if they want to define a new Property.
   * @param property The Property to be added.
   * @param message The message to be sended to the clients with the definition message.
   */  
  protected void addProperty(INDIProperty property, String message) {
    if (!properties.containsValue(property)) {
      properties.put(property.getName(), property);

      sendDefXXXVectorMessage(property, message);
    }
  }

  /**
   * Notifies the clients about the property and its values. Drivres must call this method when the values of the Elements of the property are updated in order to notify the clients.
   * @param property The Property whose values have change and about which the clients must be notified.
   */
  protected void updateProperty(INDIProperty property) {
    updateProperty(property, null);
  }
  
  /**
   * Notifies the clients about the property and its values with an additional <code>message</code>. Drivres must call this method when the values of the Elements of the property are updated in order to notify the clients.
   * @param property The Property whose values have change and about which the clients must be notified.
   * @param message The message to be sended to the clients with the udpate message.
   */
  protected void updateProperty(INDIProperty property, String message) {
    String msg = property.getXMLPropertySet(message);

    sendXML(msg);
  }

  
  /**
   * Notifies the clients about a new property  with a <code>message</code>. The <code>message</code> can be <code>null</code> if there is nothing to special to say.
   * @param property The property that will be notified.
   * @param message
   */
  private void sendDefXXXVectorMessage(INDIProperty property, String message) {
    String msg = property.getXMLPropertyDefinition(message);

    sendXML(msg);
  }

  /**
   * Sends a XML message to the clients.
   * @param XML The message to be sended.
   */
  private void sendXML(String XML) {
    out.print(XML);
    out.flush();
  }
  /**
   * 
   * @param property The Property to be added.
   */
  
  /**
   * Removes a Property from the Device with a <code>message</code>. A XML message about it will be send to the clients. Drivers must call this method if they want to remove a Property.
   * @param property The property to be removed
   * @param message A message that will be included in the XML message to the client.
   */
  protected void removeProperty(INDIProperty property, String message) {
    if (!properties.containsValue(property)) {
      properties.remove(property.getName());

      sendDelPropertyMessage(property, message);
    }
  }
  
  /**
   * Removes the Device from the clients with a <code>message</code>. A XML message about it will be send to the clients. Drivers must call this method if they want to remove the entire device from the clients. It should be used if the Driver is ending.
   * @param message The message to be sended to the clients. It can be <code>null</code> if there is nothing special to say.
   */
  protected void removeDevice(String message) {
    sendDelPropertyMessage(message);
  }


  /**
   * Sends a mesage to the client to remove the entire device.
   * @param message A optional message (can be <code>null</code>).
   */
  private void sendDelPropertyMessage(String message) {
    String mm = "";

    if (message != null) {
      mm = " message=\"" + message + "\"";
    }

    String msg = "<delProperty device=\"" + this.getName() + "\" timestamp=\"" + INDIDateFormat.getCurrentTimestamp() + "\"" + mm + " />";

    sendXML(msg);
  }

  /**
   * Sends a message to the client to remove a Property with a <code>message</code>.
   * @param property The property that is being removed.
   * @param message The optional message (can be <code>null</code>).
   */
  private void sendDelPropertyMessage(INDIProperty property, String message) {
    String mm = "";

    if (message != null) {
      mm = " message=\"" + message + "\"";
    }

    String msg = "<delProperty device=\"" + this.getName() + "\" name=\"" + property.getName() + "\" timestamp=\"" + INDIDateFormat.getCurrentTimestamp() + "\"" + mm + " />";

    sendXML(msg);
  }

  /**
   * Gets a Property of the Driver given its name.
   * @param propertyName The name of the Property to be retrieved.
   * @return The Property with <code>propertyName</code> name. <code>null</code> if there is no property with that name.
   */
  protected INDIProperty getProperty(String propertyName) {
    return properties.get(propertyName);
  }

  /**
   * Gets a list of all the Properties in the Driver.
   * @return A List of all the Properties in the Driver.
   */
  public ArrayList<INDIProperty> getPropertiesAsList() {
    return new ArrayList<INDIProperty>(properties.values());
  }
}
