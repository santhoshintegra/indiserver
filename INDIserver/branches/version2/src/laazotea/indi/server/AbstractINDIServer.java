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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import laazotea.indi.INDIException;
import laazotea.indi.driver.INDIDriver;
import org.w3c.dom.Element;

/**
 * A class representing a INDI Server. It is in charge of dealing with several
 * Drivers and Clients which interexchange messages. Check the INDI
 * documentation to better understand its pourpose.
 *
 * It has the appropriate methods to load / unload several kinds of Drivers:
 * Java Drivers (created with the INDI for Java Driver library, Native Drivers
 * (probably created with the original INDI Library and launched as external
 * processes) and Network Devices (which consist on other INDI Servers).
 *
 * New INDI Servers that implement additional functionality may inherit from
 * this class.
 *
 * @author S. Alonso (Zerjillo) [zerjio at zerjio.com]
 * @version 1.21, April 4, 2012
 */
public abstract class AbstractINDIServer implements Runnable {

  /**
   * A list of Devices loaded by the server.
   */
  private ArrayList<INDIDevice> devices;
  /**
   * A list of clients (and devices if they are snooping) connected to the
   * server.
   */
  private ArrayList<INDIDeviceListener> clients;
  /**
   * The port to which the Server listens.
   */
  private int listeningPort;
  /**
   * The socket to which the Server listens.
   */
  private ServerSocket socket;

  private boolean running = true;
  
  /**
   * Constructs a new Server. The Server begins to listen to the default port.
   */
  protected AbstractINDIServer() {
    listeningPort = 7624;

    initServer();
  }

  /**
   * Constructs a new Server. The Server begins to listen to a particular port.
   *
   * @param listeningPort The port to which the Server will listen.
   */
  protected AbstractINDIServer(int listeningPort) {
    this.listeningPort = listeningPort;

    initServer();
  }

  /**
   * Inits the Server and launches the listening thread.
   */
  private void initServer() {
    devices = new ArrayList<INDIDevice>();
    clients = new ArrayList<INDIDeviceListener>();

    Thread serverThread = new Thread(this);
    serverThread.start();
  }

  /**
   * Gets the port to which the Server listens.
   *
   * @return The port to which the Server listens.
   */
  protected int getListeningPort() {
    return listeningPort;
  }

  /**
   * The thread listens to the server socket and when a client connects, it is
   * added to the list of clients.
   */
  @Override
  public void run() {
    try {
      socket = new ServerSocket(listeningPort);
    } catch (IOException e) {
      System.err.println("Could not listen on port: " + listeningPort + " (maybe busy)");
      System.exit(-1);
    }

    System.err.println("Listening to port " + listeningPort);
    System.err.flush();

    while (running) {
      Socket clientSocket = null;

      try {
        clientSocket = socket.accept();
      } catch (IOException e) {
        System.err.println("Accept failed: " + listeningPort);
        //System.exit(-1);
      }

      if (clientSocket != null) {
        if (acceptClient(clientSocket)) {
          INDIClient client = new INDIClient(clientSocket, this);

          clients.add(client);
          
          onClientConnected(client.getInetAddress());
          
          System.err.println("Client " + client.getInetAddress() + " connected");
        } else {
          try {
            clientSocket.close();
          } catch (IOException e) {
          }
          
          System.err.println("Client " + clientSocket.getInetAddress() + " rejected");
        }
      }
    }
  }

  public void stopServer() {
	  running = false;
	  try {
		socket.close();
	} catch (IOException e) {
		e.printStackTrace();
	}
  }
  
  /**
   * Loads all INDI for Java Drivers in a JAR file.
   *
   * @param jarFileName The jar file from which to load Drivers.
   * @throws INDIException if there is a problem with the JAR file
   */
  protected synchronized void loadJavaDriversFromJAR(String jarFileName) throws INDIException {
    if (isAlreadyLoaded(jarFileName)) {
      throw new INDIException("JAR file already loaded.");
    }

    ArrayList<String> list = getClassesInJAR(jarFileName);

    File file = new File(jarFileName);
    URL url;

    try {
      url = file.toURL();
    } catch (MalformedURLException e) {
      throw new INDIException("Error reading JAR file for Class Loader.");
    }

    URL[] urls = new URL[]{url};
    ClassLoader cl = new URLClassLoader(urls);

    for (int i = 0 ; i < list.size() ; i++) {
      String className = list.get(i);
      Class cls;

      try {
        cls = cl.loadClass(className);
      } catch (ClassNotFoundException e) {
        throw new INDIException("Error loading class " + className);
      }

      if (isINDIDriver(cls)) {
        print("Loading Driver " + className + " from " + jarFileName);

        loadJavaDriver(cls, jarFileName);
      }
    }
  }

  /**
   * Gets the list of loaded Devices.
   *
   * @return The list of loaded Devices.
   */
  protected ArrayList<INDIDevice> getDevices() {
    return devices;
  }

  /**
   * Loads a particular Java Driver.
   *
   * @param cls The Class of the driver to load.
   * @param identifier A UNIQUE identifier. Please note that if there are
   * repeated identifiers strange things may happen. MUST BE CHECKED IN THE
   * FUTURE.
   * @throws INDIException If there is any problem instantiating the Driver.
   */
  private synchronized void loadJavaDriver(Class cls, String identifier) throws INDIException {
    INDIJavaDevice newDevice = new INDIJavaDevice(this, cls, identifier);

    addDevice(newDevice);
  }

  /**
   * Loads a particular Java Driver that is already in the classpath.
   *
   * @param cls The Class of the driver to load.
   * @throws INDIException If there is any problem instantiating the Driver.
   */
  protected synchronized void loadJavaDriver(Class cls) throws INDIException {
    loadJavaDriver(cls, "class+-+" + cls.getName());
  }

  /**
   * Loads a Native Driver.
   *
   * @param driverPath The Driver path name. It will be executed in a separate
   * process.
   * @throws INDIException if there is any problem executing the Driver.
   */
  protected synchronized void loadNativeDriver(String driverPath) throws INDIException {

    if (isAlreadyLoaded(driverPath)) {
      throw new INDIException("Driver already loaded.");
    }

    print("Loading Native Driver " + driverPath);

    INDINativeDevice newDevice;

    newDevice = new INDINativeDevice(this, driverPath);

    addDevice(newDevice);
  }

  /**
   * Loads a Network Driver.
   *
   * @param host The host of the Network Driver.
   * @param port The port of the Network Driver.
   * @throws INDIException if there is any problem with the connection.
   */
  protected synchronized void loadNetworkDriver(String host, int port) throws INDIException {
    String networkName = host + ":" + port;

    if (isAlreadyLoaded(networkName)) {
      throw new INDIException("Network Driver " + networkName + " already loaded.");
    }

    print("Loading Network Driver " + networkName);

    INDINetworkDevice newDevice;

    newDevice = new INDINetworkDevice(this, host, port);

    addDevice(newDevice);
  }

  /**
   * Checks if a particular Driver is already loaded.
   *
   * @param deviceIdentifier The device identifier.
   * @return
   * <code>true</code> if the Driver identified by
   * <code>deviceIdentifier</code> is already loaded.
   * <code>false</code> otherwise.
   */
  private boolean isAlreadyLoaded(String deviceIdentifier) {
    for (int i = 0 ; i < devices.size() ; i++) {
      INDIDevice d = devices.get(i);


      if (d.isDevice(deviceIdentifier)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Gets a list of devices with a particular identifier. Note that may be more
   * than one in the case of Java Drivers as there might be more than one in a
   * single JAR file.
   *
   * @param deviceIdentifier The device identifier.
   * @return A list of devices with a particular identifier.
   */
  private ArrayList<INDIDevice> getDevicesWithIdentifier(String deviceIdentifier) {
    ArrayList<INDIDevice> found = new ArrayList<INDIDevice>();

    for (int i = 0 ; i < devices.size() ; i++) {
      INDIDevice d = devices.get(i);

      if (d.isDevice(deviceIdentifier)) {
        found.add(d);
      }
    }

    return found;
  }

  /**
   * Adds a Device to the list of devices, starts its reading process and asks
   * for the properties.
   *
   * @param device The device to add.
   */
  private void addDevice(INDIDevice device) {
    devices.add(device);
    clients.add(device);

    device.startReading();

    String message = "<getProperties version=\"1.7\" />";  // Force the device to send its properties for already connected clients

    device.sendXMLMessage(message);
  }

  /**
   * Removes the Java Drivers in a JAR file.
   *
   * @param jarFileName The name of the JAR file.
   */
  protected synchronized void destroyJavaDriversFromJAR(String jarFileName) {
    print("Removing drivers from " + jarFileName);

    destroyIdentifiedDrivers(jarFileName);
  }

  /**
   * Removes a Java Driver by its class.
   *
   * @param cls The class of the Java Driver to remove.
   */
  protected synchronized void destroyJavaDriver(Class cls) {
    print("Removing driver " + cls.getName());

    destroyIdentifiedDrivers("class+-+" + cls.getName());
  }

  /**
   * Destroys the Devices with a particular identifier. Note that the Devices
   * will be removed from the list when their listening thread ends (which may
   * occur in the near future in another Thread).
   *
   * @param deviceIdentifier The device identifier.
   */
  private synchronized void destroyIdentifiedDrivers(String deviceIdentifier) {
    ArrayList<INDIDevice> toRemove = getDevicesWithIdentifier(deviceIdentifier);

    for (int i = 0 ; i < toRemove.size() ; i++) {
      INDIDevice d = toRemove.get(i);

      d.destroy();
    }
  }

  /**
   * Destroys a Native Driver.
   *
   * @param driverPath The path of the Driver to be destroyed.
   */
  protected synchronized void destroyNativeDriver(String driverPath) {
    print("Removing native driver " + driverPath);

    destroyIdentifiedDrivers(driverPath);
  }

  /**
   * Destroys a Network Driver.
   *
   * @param host The host of the Driver.
   * @param port The port of the Driver.
   */
  protected synchronized void destroyNetworkDriver(String host, int port) {
    String networkName = host + ":" + port;

    print("Removing network driver " + networkName);

    destroyIdentifiedDrivers(networkName);
  }

  /**
   * Removes a Device from the list of devices. Called by the Devices to be
   * removed when connection brokes
   *
   * @param device The Device to be removed.
   */
  protected void removeDevice(INDIDevice device) {
    String[] names = device.getNames();

    devices.remove(device);
    clients.remove(device);

    notifyClientsDeviceRemoved(names);
  }

  /**
   * Notifies the listening clients that some particular Devices have been
   * removed by sending
   * <code>delProperty</code> messages.
   *
   * @param deviceNames The names of the Devices that have been removed.
   */
  private void notifyClientsDeviceRemoved(String[] deviceNames) {
    for (int h = 0 ; h < deviceNames.length ; h++) {
      String deviceName = deviceNames[h];

      String message = "<delProperty device=\"" + deviceName + "\" />";

      ArrayList<INDIDeviceListener> list = this.getClientsListeningToDevice(deviceName);

      for (int i = 0 ; i < list.size() ; i++) {
        INDIDeviceListener c = list.get(i);

        c.sendXMLMessage(message);
      }

      ArrayList<INDIDeviceListener> list2 = this.getClientsListeningToSingleProperties(deviceName);

      for (int i = 0 ; i < list2.size() ; i++) {
        INDIDeviceListener c = list2.get(i);

        c.sendXMLMessage(message);
      }
    }
  }

  /**
   * Removes a Client from the List of clients. Called by the clients when the
   * connection is broken.
   *
   * @param client The Client to remove.
   */
  protected void removeClient(INDIClient client) {
    clients.remove(client);
    onClientDisconnected(client.getInetAddress());
  }

  /**
   * Checks if a Class is subclass of INDIDriver.
   *
   * @param c The class to check
   * @return
   * <code>true</code> if the Class inherits from INDIDriver.
   * <code>false</code> otherwise.
   */
  private boolean isINDIDriver(Class c) {
    Class s = c.getSuperclass();

    while (s != null) {
      if (s == INDIDriver.class) {
        return true;
      }

      s = s.getSuperclass();
    }

    return false;
  }

  /**
   * Gets a list of Strings with the name of the classes in a JAR file.
   *
   * @param file The JAR file.
   * @return A list with all the names of the classes in the JAR file.
   * @throws INDIException if there is any problem accessing to the JAR file.
   */
  private ArrayList<String> getClassesInJAR(String file) throws INDIException {

    ArrayList<String> list = new ArrayList<String>();

    try {
      JarInputStream jarFile = new JarInputStream(new FileInputStream(file));
      JarEntry jarEntry = jarFile.getNextJarEntry();
      while (jarEntry != null) {
        if (jarEntry.getName().endsWith(".class")) {
          String n = jarEntry.getName().replace("/", ".");

          n = n.substring(0, n.length() - 6);

          list.add(n);
        }

        jarEntry = jarFile.getNextJarEntry();
      }
    } catch (IOException e) {
      throw new INDIException("Error loading JAR file contents.");
    }

    return list;
  }

  /**
   * Prints a message to the standard output.
   *
   * @param message The message to print in the standard output.
   */
  private void print(String message) {
    System.out.println(message);
    System.out.flush();
  }

  /**
   * Gets a Device given its name.
   *
   * @param deviceName The name of the Device to get.
   * @return The Device with name
   * <code>deviceName</code>.
   */
  protected INDIDevice getDevice(String deviceName) {
    for (int i = 0 ; i < devices.size() ; i++) {
      if (devices.get(i).hasName(deviceName)) {
        return devices.get(i);
      }
    }

    return null;
  }

  /**
   * Sends a XML message to all the Devices.
   *
   * @param xml The message to send.
   */
  protected void sendXMLMessageToAllDevices(Element xml) {
    for (int i = 0 ; i < devices.size() ; i++) {
      INDIDevice d = devices.get(i);

      d.sendXMLMessage(xml);
    }
  }

  /**
   * Sends a XML message to all the Clients.
   *
   * @param xml The message to send.
   */
  protected void sendXMLMessageToAllClients(Element xml) {
    for (int i = 0 ; i < clients.size() ; i++) {
      INDIDeviceListener c = clients.get(i);

      if (c instanceof INDIClient) {
        c.sendXMLMessage(xml);
      }
    }
  }

  /**
   * Gets a list of Clients that listen to a Property.
   *
   * @param deviceName The name of the Device of the Property.
   * @param propertyName The name of the Property.
   * @return A list of Clients that listen to a Property.
   */
  protected ArrayList<INDIDeviceListener> getClientsListeningToProperty(String deviceName, String propertyName) {
    ArrayList<INDIDeviceListener> list = new ArrayList<INDIDeviceListener>();

    for (int i = 0 ; i < clients.size() ; i++) {
      INDIDeviceListener c = clients.get(i);

      if (c.listensToProperty(deviceName, propertyName)) {
        list.add(c);
      }
    }

    return list;
  }

  /**
   * Gets a list of Clients that specifically listen to a Property of a Device.
   *
   * @param deviceName The name of the Device.
   * @return A list of Clients that specifically listen to a Property of a
   * Device.
   */
  protected ArrayList<INDIDeviceListener> getClientsListeningToSingleProperties(String deviceName) {
    ArrayList<INDIDeviceListener> list = new ArrayList<INDIDeviceListener>();

    for (int i = 0 ; i < clients.size() ; i++) {
      INDIDeviceListener c = clients.get(i);

      if (c.listensToSingleProperty(deviceName)) {
        list.add(c);
      }
    }

    return list;
  }

  /**
   * Gets a list of Clients that listen to a Device.
   *
   * @param deviceName The name of the Device.
   * @return A list of Clients that specifically listen to a Device.
   */
  protected ArrayList<INDIDeviceListener> getClientsListeningToDevice(String deviceName) {
    ArrayList<INDIDeviceListener> list = new ArrayList<INDIDeviceListener>();

    for (int i = 0 ; i < clients.size() ; i++) {
      INDIDeviceListener c = clients.get(i);

      if (c.listensToDevice(deviceName)) {
        list.add(c);
      }
    }

    return list;
  }

  protected int getNumClients() {
	  return clients.size() - devices.size();
  }
  
  protected int getNumDevices() {
	  return devices.size();
  }
  
  protected abstract void onClientConnected(String address);
  
  protected abstract void onClientDisconnected(String address);
  
  /**
   * Must return
   * <code>true</code> is the Client that stablished this connection must be
   * allowed in the Server. Otherwise the connection will be closed.
   *
   * @param socket The socket created with a possible client.
   * @return
   * <code>true</code> if the Client is allowed to connect to the server.
   * <code>false</code> otherwise.
   */
  protected abstract boolean acceptClient(Socket socket);

  /**
   * Notifies Clients of a
   * <code>defXXXVector</code> message.
   *
   * @param device The Device sending the message.
   * @param xml The message.
   */
  protected abstract void notifyDeviceListenersDefXXXVector(INDIDevice device, Element xml);

  /**
   * Notifies Clients of a
   * <code>setXXXVector</code> message.
   *
   * @param device The Device sending the message.
   * @param xml The message.
   */
  protected abstract void notifyDeviceListenersSetXXXVector(INDIDevice device, Element xml);

  /**
   * Notifies Clients of a
   * <code>message</code> message.
   *
   * @param device The Device sending the message.
   * @param xml The message.
   */
  protected abstract void notifyDeviceListenersMessage(INDIDevice device, Element xml);

  /**
   * Notifies Clients of a
   * <code>delProperty</code> message.
   *
   * @param device The Device sending the message.
   * @param xml The message.
   */
  protected abstract void notifyDeviceListenersDelProperty(INDIDevice device, Element xml);

  /**
   * Notifies Devices of a
   * <code>getProperties</code> message.
   *
   * @param client The Client sending the message.
   * @param xml The message.
   */
  protected abstract void notifyClientListenersGetProperties(INDIDeviceListener client, Element xml);

  /**
   * Notifies Devices of a
   * <code>newXXXVector</code> message.
   *
   * @param client The Client sending the message.
   * @param xml The message.
   */
  protected abstract void notifyClientListenersNewXXXVector(INDIClient client, Element xml);

  /**
   * Notifies Devices of a
   * <code>enableBLOB</code> message.
   *
   * @param client The Client sending the message.
   * @param xml The message.
   */
  protected abstract void notifyClientListenersEnableBLOB(INDIClient client, Element xml);
}
