/*
 * Copyright (C) 2023 John Garner <segfaultcoredump@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.pikatimer.pikareader;

import com.pikatimer.pikareader.conf.PikaConfig;
import com.pikatimer.pikareader.http.HTTPHandler;
import com.pikatimer.pikareader.readers.ReaderHandler;
import com.pikatimer.pikareader.status.StatusHandler;
import com.pikatimer.pikareader.tags.TagDB;
import com.pikatimer.pikareader.util.DiscoveryListener;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class PikaReader {

    private static final Logger logger = LoggerFactory.getLogger(PikaReader.class);

    public static void main(String[] args) {

        PikaReader pr = new PikaReader();
        pr.start();

    }

    public void start() {

        System.out.println("""

            8888888b.  d8b 888               8888888b.                         888                  
            888   Y88b Y8P 888               888   Y88b                        888                  
            888    888     888               888    888                        888                  
            888   d88P 888 888  888  8888b.  888   d88P  .d88b.   8888b.   .d88888  .d88b.  888d888 
            8888888P"  888 888 .88P     "88b 8888888P"  d8P  Y8b     "88b d88" 888 d8P  Y8b 888P"   
            888        888 888888K  .d888888 888 T88b   88888888 .d888888 888  888 88888888 888     
            888        888 888 "88b 888  888 888  T88b  Y8b.     888  888 Y88b 888 Y8b.     888     
            888        888 888  888 "Y888888 888   T88b  "Y8888  "Y888888  "Y88888  "Y8888  888     

                                            ©2023 by John Garner
                                           https://PikaTimer.com/
                                      Released under the GPL-3.0 license.
                                                                                       
                                                                                                                   """);
        // Read Config
        PikaConfig pikaConfig = PikaConfig.getInstance();

        // Startup DB;
        TagDB tagDB = TagDB.getInstance();

        // Setup Readers
        ReaderHandler readerHandler = ReaderHandler.getInstance();

        // Hard code this until the Web UI and buttons are setup
        //readerHandler.setClocks();
        //readerHandler.startReading();

        // Start http listener
        HTTPHandler httpHandler = HTTPHandler.getInstance();

        // TODO: Setup Raspberry PI interfaces
        // Start broadcast listener so others can find us
        DiscoveryListener.startDiscoveryListener();
        
        // start the status handler
        StatusHandler statusHandler = StatusHandler.getInstance();

        // This is good for debugging
        System.out.println("Press Enter to exit.");
        Scanner s = new Scanner(System.in);
        s.nextLine();

        logger.info("Stopping Readers...");
        readerHandler.stopReading();

        logger.info("Stopping Jetty");
        httpHandler.stopHTTPD();

        //logger.info("Stopping TagDB");
        logger.info("PikaReader Stopped...");
        
        // Kills any background web sessions that are still hanging out there
        System.exit(0);
        
    }
}
