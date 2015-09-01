# Introduction #

Communication drivers are used to physicaly connect to the hardware


# Details #

Communication drivers act as a link layer. They connect over a medium like bluetooth, usb, serial or network to the devices and present a standardized interface to the actual device drivers.

The following drivers should be available:

**serial** for direct communication via /dev/ttyS or /dev/ttyUSB

**bluetooth\_serial** for communication via Bluetooth-SPP

**usbhost\_serial\_pl2303** for USB-to-serial adapters with pl2303 chipset using the usbhost feature of Android 3.2 and higher.

In the far future there can also be

**usbhost\_lpi** for Meade LunarPlanetaryImager

**usbhost\_dsi** for Meade DeepSkyImager

**usbhost\_canon\_dslr** for Canon DSLRs

... and much more!