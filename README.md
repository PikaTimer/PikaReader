# PikaReader: OpenSource RFID Reader

***

PikaReader is intended to sit on a small single board computer (Raspberry Pi, etc) and be directly connected to RFID Readers such as an Impinj R420. 

## Current Features
* Support for Impinj Speedway Readers (Octane SDK)
* Ability to store tag reads for later retrieval by the timing application
* Live streaming of tag reads via a websocket
* Unlimited number of connected RFID Readers
* User Selectable gating to reduce the number of tags transmitted to the timing application
* Debug log available via built in http server

## Planned 
* Integration with PikaTimer application 
* Integration with PikaRelay for remote retrieval of data
* Antenna Status Display
* Raspberry PI integration 
    * GPIO Button for start/stop/trigger
    * I2C Status Display
* Support for Zebra / Mororola FX Series Readers
* Support for generic LLRP Readers
* Tag filtering
* Web Based configuration tool


## Usage
Requires OpenJRE 21 or newer. 

Launch the jar file: java -jar PikaReader-0.5.jar 
Press the space bar to stop. 

The first time the app is run, it will automatically create a config file. You will need to edit the ~/.PikaReader.conf file to configure the app. 



