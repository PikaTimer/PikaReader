# PikaReader: OpenSource RFID Reader

***

PikaReader is intended to sit on a small single board computer (Raspberry Pi, etc) and be directly connected to RFID Readers such as an Impinj R420. 
The timing laptop can then connect to PikaReader to retrieve the tags. 
This setup significantly reduces the risk of data loss compared to a setup where the timing laptop connects to all readers.
It also permits the setup and usage of a remote or offline reader for on-course splits. d

## Current Features
* Support for Impinj Speedway Readers (via Octane SDK)
* Ability to store tag reads for later retrieval by the timing application
* Live streaming of tag reads via a websocket
* Ability to "rewind" and retrieve previously read tags
* Unlimited number of connected RFID Readers
* Unlimited number of connected clients
* Antenna read stats via API
* User Selectable gating to reduce the number of tags transmitted to the timing application
* Debug log available via built in http server

## Planned 
* Integration with PikaTimer application 
* Integration with PikaRelay for remote retrieval of data
* Antenna Status Display
* Raspberry PI integration 
    * GPIO Button for start/stop/trigger
    * I2C Status Display
* Support for IP Based Readers:
    * Zebra / Mororola FX Series 
    * ThingMagic IZAR 
* Support for TSL and ThingMagic / Jadak UART based readers
* Support for generic LLRP Readers with reduced functionality
* Reader level tag filtering
* Web Based configuration tool


## Usage
Requires OpenJRE 21 or newer. 

Launch the jar file: java -jar PikaReader-0.5.jar 

Press the space bar to stop. 

The first time the app is run, it will automatically create a config file. You will need to edit the ~/.PikaReader.conf file to configure the app. 



