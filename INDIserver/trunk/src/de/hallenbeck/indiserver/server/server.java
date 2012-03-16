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
 * Theoretical procedure:
 * 
 * 0. Start server manually via main-activity or automatically via intent by another android-app 
 * 1. Get telescope- and communications-driver setting from preferences set by main-activity
 * 2. Create a new instance of the appropriate telescope-driver class via reflection 
 * 3. Set the appropriate communication-driver (either via constructor of the telescope-driver
 *    or via public method telescope_driver.set_communication_driver(drivername) )
 * 4. call the telescope_driver.connect() method (may be omited if using constructor?)
 * 5. give the driver a callback-handler for receiving messages from the telescope 
 *    (also via constructor or telescope_driver.set_msg_handler(handler) method )
 * 5. open a listening socket on port tcp/7624 for clients to connect (one thread for each client)
 * 6. Notify the user of running background service and number of connected clients via notification-manager 
 * 7. when clients connect keep an array of connected clients
 * 8. relay messages between clients and driver in both directions
 * 9. when last client disconnects stop/destroy service and clear notification of running service 
 *  
 * Exception-handling:
 * 
 * 1. Connection to telescope lost (raised by the driver)
 * 2. Connection to client(s) lost (raised by the server)
 * 
 * In the far future:
 * Support other devices like focusers/imagers/cameras etc.
 * 
 * @author atuschen
 *
 */

public class server {

}
