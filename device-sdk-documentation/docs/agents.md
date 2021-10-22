### Overview

The Java agent provides an example implementation for many device management features of Cumulocity plus drivers for various device kits. The Java agent supports Windows and Unix-based systems and contains the following functionality:

* Discovery and hardware identification: Automatically registers a device, its supported functionality and its connected sensors and controls.
* Availability management: Determine if a device connects as expected and notify the user if not.
* Software management: Upgrade the software on the device and restart with the new software.
* Modem support: Provides basic modem information and signal statistics  (IMEI, ICCID, cell ID, signal strength, BER; depending on modem type).
* Extensibility through driver concept.
* Ready-made drivers for [Raspberry Pi](/guides/devices/raspberry-pi), [PiFace Digital](/guides/devices/raspberry-pi) and [Tinkerforge](/guides/devices/tinkerforge).

> The agent is provided in open source form as-is without support or warranty. For commercial use, we recommend to use industrial hardware and/or the Cumulocity C++ SDK.

### Prerequisites

An installation of Java SE 7 is required. To verify the availability of Java on your system, type

	$ java -version
	java version "1.7.0_45"

To install Java, please visit http://java.com.


### Installing the agent

Binary packages are available for the Raspberry Pi, the Kontron M2MSSDK, Mac OS X, Linux and Windows.

* On a Linux OS (including Embedded Linux distributions such as Kontron Linux), download the [latest Linux Agent Archive](http://resources.cumulocity.com/examples/cumulocity-linux-agent-latest.tar.gz).
* For the Raspberry Pi and similar Embedded Linux devices, check the [Raspberry Pi documentation](/guides/devices/raspberry-pi) in the Devices guide.
* On a Mac OS X, download the [latest Mac Agent Archive](http://resources.cumulocity.com/examples/cumulocity-mac-agent-latest.tar.gz).
* On a Windows OS, please the [latest Windows Agent Archive](http://resources.cumulocity.com/examples/cumulocity-win-agent-latest.zip).

Unpack your respective package in a new folder. After unpacking, use the script "c8y-agent" to run the agent. If you need more debug information, run the script "c8y-agent-debug" or change the file cfg/logback.xml to use a more fine granular logging.

#### Configuration

All the configuration files are located in the *cfg/* folder.

1. The cumulocity.properties file has the following example structure:

		host=<host>
		bootstrap.tenant = <bootstrap tenant>
		bootstrap.user = <bootstrap  username>
		bootstrap.password = <bootstrap password>

	* The host is the http/https address of your Cumulocity instance. By default it points to http://developer.cumulocity.com/, which is the Cumulocity production environment.
	* The bootstrap.* properties define the bootstrap user for your instance. Their default value is the default Cumulocity bootstrap user.

	If your tenant is "&lt;tenant&gt;.cumulocity.com" you don't need to change anything here.

	On the Raspberry Pi this file is a symbolic link pointing to /etc/cumulocity-agent.properties.

2. The device.properties file contains the credentials the device uses to connect to your tenant. They are automatically filled in, once you register the device in Cumulocity. If you would like to move your device to a different tenant you have to delete the content of this file.

3. The cumulocity-config.properties persists the device specific configuration that can be set in the Cumulocity UI. Here is an example configuration of a device that has one ThinkerForge temperature sensor connected to it:

		#Tue Apr 28 16:03:53 UTC 2015
		c8y_temperature.interval=5000
		c8y.log.eventLevel=INFO
		c8y.log.alarmLevel=ERROR

	* The c8y_temperature.interval property defines the reporting rate of the TinkerForge temperature sensor. It is measured in milliseconds.
	* The c8y.log.* properties defines the severity of log entries to be reported by the device as events and alarms respectively. The possible values are TRACE, DEBUG, INFO, WARN and ERROR.

	This file is automatically created by the device.

4. The logback.xml and logback-debug.xml files define the logging for running the agent normally and in debug mode respectively.

On the Raspberry Pi the agent is implemented as a service. The configuration files are located in `/usr/share/cumulocity-rpi-agent/cfg/*`.


### Running the agent

#### Registering the device

When the agent is successfully started, open Cumulocity in a web browser and navigate to the "Registration" page. Enter the device ID of the device on which the agent is running. The device ID depends on the type of device.

* On a Raspberry Pi, it's the serial number of the Raspberry Pi (see the [Raspberry Pi documentation](/guides/devices/raspberry-pi)) in the Devices guide.
* On a Mac, it's the serial number of the Mac. Click on the Apple logo in the menu, "About This Mac", "More Info" to display the serial number.
* On Windows, it's the serial number of your PC. To get it, run "wmic csproduct get identifyingnumber" from a command prompt.
* On other Linux devices or as a fallback, the MAC address of the first network interface is used. Use "ifconfig" to print the network interfaces and check for the "HWaddr" entry. Keep in mind only the numbers are used, without any colons! Often, the MAC address is also printed on the board.

#### Monitoring the connection

By default, the agent is "always on", i.e. it maintains a connection to Cumulocity to receive operations in real time. Hence, the agent is shown as "Connected" in the user interface. If the connection breaks down, the agent will be shown as "Offline" and an alarm will be sent.

![Availability](/guides/images/devices/javaavailability.png)

#### Restarting the device

To restart the device on which the agent is running, switch to the "Control" tab and click "Restart".

![Restart and configure](/guides/images/devices/javarestartconfig.png)

### Tracing

The agent writes a log to Cumulocity. Low-severity log levels are sent to Cumulocity as events and are visible in the "Events" tab of the device. Higher-severity log levels are recorded as alarms and are visible in the "Alarms" tab of the device. To configure the verbosity of logging, go to the "Control" tab and edit the configuration.

* "c8y.log.eventLevel" is the minimum level of log entries that are sent as events. Default is INFO.
* "c8y.log.alarmLevel" is the minimum level of log entries that are sent as alarms. Default is ERROR.

Valid levels are TRACE, DEBUG, INFO, WARN and ERROR. Use DEBUG or TRACE only for troubleshooting purposes.

![Tracing](/guides/images/devices/javatracing.png).

### Building the agent

Before building the agent ensure you have at least [JDK 1.7](http://www.oracle.com/technetwork/java/javase/downloads/index.html) and [Maven 3](http://maven.apache.org/download.cgi). You can check this by running:

	mvn -version
	javac -version

Your Maven "settings.xml" file needs to point to the Cumulocity repository as described in [https://github.com/SoftwareAG/cumulocity-clients-java](https://github.com/SoftwareAG/cumulocity-clients-java). Source code is available at [https://github.com/SoftwareAG/cumulocity-examples](https://github.com/SoftwareAG/cumulocity-examples) in the folder java-agent.

To build the agent simply run:

	mvn clean install

### Extending the agent

Extending the agent with drivers for new hardware and new devices requires the following steps:

* Create a Java class that implements the interface [Driver](https://github.com/SoftwareAG/cumulocity-examples/blob/develop/java-agent/jv-driver/src/main/java/c8y/lx/driver/Driver.java).
* Create a jar file with the class and an additional text file "META-INF/services/c8y.lx.driver.Driver". The text file needs to contain the fully-qualified class name of the Java class.
* Deploy the jar file either by copying it to the "lib" folder of the agent or by deploying it through Cumulocity's software management.

The [GitHub repository](https://github.com/SoftwareAG/cumulocity-examples) contains numerous examples of drivers. Usage of the Java client library is described in [Developing Java clients](/guides/device-sdk/java#developing-java-clients).


### Server-side agents

The [GitHub repository](https://github.com/SoftwareAG/cumulocity-examples) also contains a complete example of a server-side agent for closed devices in the folder "tracker-agent". This example works with tracking devices from Telic and Queclink, see Devices guide. It demonstrates solutions to various challenges when developing server-side functionality for mobile devices.
