# Introduction #

This project aims to implement an INDI server on Android.
For details about INDI click here: http://indilib.org/

# Details #

I'd like to control my telescope with my Android-devices, specifically with SkyMap. INDI is an open-source hardware-independent control protocol for astronomical hardware. To implement a server it first requires development of device-drivers, as well as communication-drivers.

The device-driver interfaces to the hardware itself via the com-driver.
The INDI-Server then interfaces with the device-driver and listens on a network-port for clients to connect.

The clients (such as SkyMap or SkEye) then send INDI-commands to the server and the server sends them via the drivers to the hardware.

INDI-messages are xml-messages, the server only relays them to the drivers.
Xml-parsing itself must be done by the drivers.

It is also possible to send INDI-messages from apps directly to the drivers, but this way it isn't possible that more than one app/client at the same time can use the driver. Also it then isn't possible to remote control a telescope connected to an android-device via network.

Simple protocol stack (just for explanation/layers not 100% correct)

| **Clients** | Application layer | SkyMap, SkEye, KStars etc. |
|:------------|:------------------|:---------------------------|
|INDI XML-messages over tcp|                   |
| **[Server](Server.md)** | Transport layer   | relays messages to device drivers |
|INDI XML-messages over streams |
| **[DeviceDrivers](DeviceDrivers.md)** | Transport layer   | translate xml-messages to device-specific commands |
| **[CommunicationDrivers](CommunicationDrivers.md)** | Link layer        | Drivers for specific interfaces (Serial/Bluetooth/USB|
| **Devices** | Physical layer    | Telescopes, focusers, imagers etc. |

There are some well-known clients already using INDI:

KStars
http://edu.kde.org/kstars/

SkyChart (a.k.a. Cartes Du Ciel)
http://www.stargazing.net/astropc/

# Status and tasks #

| **Task** | **Priority** | **Status** |
|:---------|:-------------|:-----------|
|XML Parser|very high     |WORKING - Thanks to INDIforJava!|
|DeviceDrivers|high          |Example LX200-Driver (partially) working|
|CommunicationDrivers|high          |Bluetooth-SPP driver working|
|Client interface for SkyMap|mid           |<font color='red'>Will be started when first drivers are ready</font>|
|[Server](Server.md)|low           |started, partially working|

**After posting to the INDI mailing list, the project [INDIforJava](https://sourceforge.net/projects/indiforjava/) was brought to my attention. With it the XML-part of the device drivers should be much easier to make, as well as the client-interface for SkyMap.**

INDIforJava is really great, it simplifies much of the logic. But after I imported it into the project I found one drawback: I had to raise the API level from 8 to 10 which means that the server won't run on devices with Android 2.2. The reason for this was that INDIforJava uses _Arrays.copyOf_ and _Strings.isEmpty_. The latter one is easy to work around, but I need something for _Arrays.copyOf_.

# What is working #

  * My LX200/Autostar driver currently supports the following commands:
    1. Get and set: Local date/time, UTC-Offset, Latitude/Longitude
    1. Get current equatorial coordinates
    1. Set target equatorial coordinates
    1. Slew to target
    1. Sync to target
  * Server can handle multiple clients (currently limited to 8)
  * Server can handle multiple drivers (currently limited to 4, uses only 1)
  * Clients are able to connect to the server and send/receive messages

There are some flaws (mainly with multithreading and high CPU-usage), I'm currently profiling, stabilizing and cleaning up. Call it a feature-freeze :)

# External Ressources #

**The INDI client-server interface**

INDI protocol white paper:
http://www.clearskyinstitute.com/INDI/INDI.pdf

INDI for Java (client+driver only):
https://sourceforge.net/projects/indiforjava/

**Telescope-specific interfaces**

Meade LX200 commandset:
http://www.meade.com/support/TelescopeProtocol_2010-10.pdf

Celestron NexStar commandset:
http://www.nexstarsite.com/download/manuals/NexStarCommunicationProtocolV1.2.zip

# Basic demonstration #
<a href='http://www.youtube.com/watch?feature=player_embedded&v=-aM0vkE_BVc' target='_blank'><img src='http://img.youtube.com/vi/-aM0vkE_BVc/0.jpg' width='425' height=344 /></a>

[AboutMe](AboutMe.md)

My [TestEquipment](TestEquipment.md)