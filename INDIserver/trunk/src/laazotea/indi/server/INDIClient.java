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

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import laazotea.indi.INDIProtocolParser;
import laazotea.indi.INDIProtocolReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A class to represent a Client that connects to the Server.
 *
 * @author S. Alonso (Zerjillo) [zerjio at zerjio.com]
 * @version 1.21, April 4, 2012
 */
public class INDIClient extends INDIDeviceListener implements INDIProtocolParser {

  /**
   * The socket to communicate with the Client.
   */
  private Socket socket;
  /**
   * The Server to which the Client is connected.
   */
  private AbstractINDIServer server;
  /**
   * The reader.
   */
  private INDIProtocolReader reader;

  /**
   * Constructs a new INDIClient that connects to the server and starts listening to it.
   * @param socket The socket to communicate with the Client.
   * @param server The Server to which the Client is connected.
   */
  public INDIClient(Socket socket, AbstractINDIServer server) {
    this.socket = socket;
    this.server = server;

    reader = new INDIProtocolReader(this);
    reader.start();
  }

  // Must be called if when writting to the client there is a communication error. This will make the reading thread to stop
  private void disconnect() {
    if (socket != null) {
      try {
        reader.setStop(true);

        socket.shutdownInput();
        socket.getInputStream().close();
        socket.getOutputStream().close();
        socket.close();

        socket = null;
      } catch (IOException e) {
      }
    }
  }

  @Override
  public void finishReader() {
    server.removeClient(this);

    System.err.println("CLIENT " + getInetAddress() + " finishing");
  }

  /**
   * Gets a String representation of the host and port of the Client.
   * @return A String representation of the host and port of the Client.
   */
  public String getInetAddress() {
    return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
  }

  @Override
  public void parseXML(Document doc) {
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
          processNewXXXVector(child);
        } else if (name.equals("newNumberVector")) {
          processNewXXXVector(child);
        } else if (name.equals("newSwitchVector")) {
          processNewXXXVector(child);
        } else if (name.equals("newBLOBVector")) {
          processNewXXXVector(child);
        } else if (name.equals("enableBLOB")) {
          processEnableBLOB(child);
        }
      }
    }
  }

  private void processEnableBLOB(Element xml) {
    String device = xml.getAttribute("device").trim();
    if (device.length() == 0) {
      return;
    }

    String property = xml.getAttribute("name").trim();
    boolean listens = false;

    if (property.length() == 0) {
      if (this.listensToDevice(device)) {
        listens = true;
      }
    } else {
      if (this.listensToProperty(device, property)) {
        listens = true;
      }
    }

    if (listens) {
      server.notifyClientListenersEnableBLOB(this, xml);
    }
  }

  private void processNewXXXVector(Element xml) {
    String device = xml.getAttribute("device").trim();
    if (device.length() == 0) {
      return;
    }

    String property = xml.getAttribute("name").trim();
    if (property.length() == 0) {
      return;
    }

    if (this.listensToProperty(device, property)) {  // If this client does not listen to the property avoid changing it
      server.notifyClientListenersNewXXXVector(this, xml);
    }
  }

  private void processGetProperties(Element xml) {
    String version = xml.getAttribute("version").trim();

    if (version.length() == 0) { // Some conditions to ignore the messages
      return;
    }

    String device = xml.getAttribute("device").trim();
    String property = xml.getAttribute("name").trim();

    if (device.length() == 0) {
      setListenToAllDevices(true);
    } else {
      if (property.length() == 0) {
        addDeviceToListen(device);
      } else {
        addPropertyToListen(device, property);
      }
    }

    server.notifyClientListenersGetProperties(this, xml);
  }

  protected void sendXMLMessage(String xml) {
    try {
      socket.getOutputStream().write(xml.getBytes());
      socket.getOutputStream().flush();
    } catch (IOException e) {
      disconnect();
    }
  }

  @Override
  public InputStream getInputStream() {
    try {
      return socket.getInputStream();
    } catch (IOException e) {
      return null;
    }
  }
}
