# Introduction #

Device drivers are used to send/receive messages to/from the hardware via specified Communication Drivers.


# Details #

INDI device drivers handle all xml-messages coming from the server and translate them into device-specific commands. The other way round they take data from the devices and translate it to xml-messages, which are then send to the server.

Almost all logic has to be implemented in device drivers. They keep a list of device properties (descriptions of actors and sensors) they provide. This list is fetched by the clients through the server. It's then up to the clients, which of the provided functions they will use.

Device drivers utilize the communicaton drivers to connect to the hardware over a specified medium like serial-port/bluetooth/network etc.
This is important, as i.e. serial connections (used for telescopes) can also use USB- or Bluetooth-SPP-Adaptors. Other specific USB devices like cameras and imagers can be connected to USB-WiFi-Adaptors and then be accessed over network.

So the seperation of device-driver and communication-driver is logical.