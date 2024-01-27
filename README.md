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
* Antenna Status Display via web UI

## Planned 
* Integration with PikaTimer application 
* Integration with PikaRelay for remote retrieval of data

* Raspberry PI integration 
    * GPIO Button for start/stop/trigger
    * I2C LCD Status Display
* Support for IP Based Readers:
    * Zebra / Mororola FX Series 
    * ThingMagic IZAR 
* Support for TSL and ThingMagic/Jadak UART based readers
* Support for generic LLRP Readers with reduced functionality
* Reader level tag filtering
* Web Based configuration tool


## Usage
Requires OpenJRE 21 or newer. 

Launch the jar file: java -jar PikaReader-0.6.jar 

Press the space bar to stop. 

The default web UI port is http on port 8080. 

REST api paths:
- / -- Basic System information and web UI for control of PikaReader 
- /start -- Start the reader
- /stop -- Stop the reader
- /rewind -- rewind all data from PikaReader
- /rewind/<from> -- Rewind all data after <from> date/time. Time in ISO format (YYYY-MM-DDTHH:MM:ss)
- /rewind/<from>/<to> -- Rewind data between from and to
- /debug -- Dump the debug log to the browser
- /trigger -- Record a "trigger" time (chip = 0, reader = 0, antenna = 0) to mark an event
- /antennaStatus - Show the current antenna port stats
- /status - dump the current reader status
- /events -- WebSocket (ws://) that will send a message for each read or status update

## Notice!
The first time the app is run, it will automatically create a basic config file. 
You will need to quit PikaReader and then edit the ~/.PikaReader.conf file to configure the app. 
Once this is done you can restart the app and being using itd. 



