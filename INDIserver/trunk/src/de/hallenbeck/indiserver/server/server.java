package de.hallenbeck.indiserver.server;

/**
 * The server only acts as a hub between the clients and the devices.
 * There is no parsing of xml-messages. Parsing is done by the drivers itself. 
 * The server should run as service in background and take its settings from 
 * preferences set by the main activity.
 * 
 * The server-service should be able to be started by intent if the first 
 * android client connects and destroy itself after the last client disconnected. 
 * 
 * For network connections (without android-clients) the server should be started
 * by the main activity.
 * 
 * The user should be notified of a running server via notification-manager.
 *   
 * @author atuschen
 *
 */
public class server {

}
