package de.hallenbeck.indiserver.server;

/**
 * The server only acts as a hub between the clients and the devices.
 * There is no parsing of xml-messages. Parsing is done by the drivers itself. 
 * The server should run as service in background and take its settings from 
 * preferences set by the main activity.
 * 
 * Also the server-service should be started by intent and destroy itself 
 * after the last client disconnected. 
 *   
 * @author atuschen
 *
 */
public class server {

}
