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
package com.pikatimer.pikareader.readers.impinj;

import com.impinj.octane.AntennaConfigGroup;
import com.impinj.octane.ImpinjReader;
import com.impinj.octane.OctaneSdkException;
import com.impinj.octane.ReaderMode;
import com.impinj.octane.ReportConfig;
import com.impinj.octane.ReportMode;
import com.impinj.octane.RshellEngine;
import com.impinj.octane.RshellReply;
import com.impinj.octane.SearchMode;
import com.impinj.octane.Settings;
import com.pikatimer.pikareader.readers.RFIDReader;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class Impinj implements RFIDReader {

    private static final Logger logger = LoggerFactory.getLogger(Impinj.class);
    ImpinjReader reader;
    ImpinjPowerLevels powerLevel = ImpinjPowerLevels.HIGH;

    String readerIP = "192.168.1.131";
    Integer readerID;
    Boolean reading = false;

    public Impinj(Integer readerID, String readerIP) {
        this.readerID = readerID;
        this.readerIP = readerIP;
    }

    public Boolean isReading() {
        return reading;
    }

    @Override
    public void setPower(String powerLevel) {
        this.powerLevel = ImpinjPowerLevels.getLevel(powerLevel);
    }

    @Override
    public void setClock() {
        logger.trace("Entering Impinj::setClock()");
        try {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd'-'HH:mm:ss");

            RshellEngine rshell = new RshellEngine();

            /* login can take some time to give username and password */
            rshell.openSecureSession(readerIP, "root", "impinj", 10000);

            String cmd = "config network ntp disable";
            logger.debug("Sending command '" + cmd + "' to " + readerIP);

            String reply = rshell.send(cmd);

            logger.debug("Raw Reply");
            logger.debug(reply);

            // Issue the command as close to the turn of the second as possible 
            // We fudge the time by a 10th of a second to account for the latency of the set command. 
            int gap = 0;
            while (gap < Instant.now().plusNanos(100000000).getNano()) {
                gap = Instant.now().plusNanos(100000000).getNano();
                Thread.sleep(10);
            }

            logger.debug("setClock() Start Timestamp: " + Instant.now());

            cmd = "config system time " + OffsetDateTime.now(ZoneOffset.UTC).plusNanos(100000000).format(formatter);
            logger.debug("Sending command '" + cmd + "' to " + readerIP);
            
            reply = rshell.send(cmd);

            logger.trace("Raw Reply");
            logger.trace(reply);
            logger.debug("setClock() Timestamp: " + Instant.now());
            
            /* 
             * Sample from the code to send a command and 
             * parse the output
             * 

            cmd = "show system platform";

            reply = rshell.send(cmd);
            logger.trace("Raw Reply");
            logger.trace(reply);

            // parse the output. This works on most commands
            RshellReply r = new RshellReply(reply);

            String status = r.get("StatusString");

            if (status != null) {
                logger.debug("Command returned: " + status);

                if (status.equals("Success")) {
                    String uptime = r.get("UptimeSeconds");
                    if (uptime != null) {
                        logger.debug("Uptime for unit is: {} seconds", uptime);
                    }
                }
            } */

            rshell.close();

        } catch (OctaneSdkException ex) {
            logger.error("OctaneSdkExcepton", ex);
        } catch (Exception ex) {
            logger.error("Exception", ex);
        }

        logger.trace("Exiting Impinj::setClock()");

    }

    @Override
    public void startReading() {

        logger.trace("Entering Impinj::startReading()");
        
        if (reading) return;

        try {

            reader = new ImpinjReader();

            logger.debug("Connecting to {}", readerIP);
            reader.connect(readerIP);

            Settings settings = reader.queryDefaultSettings();

            ReportConfig report = settings.getReport();
            report.setIncludeAntennaPortNumber(true);
            report.setMode(ReportMode.Individual);
            report.setIncludePeakRssi(Boolean.TRUE);
            report.setIncludePhaseAngle(Boolean.TRUE);
            report.setIncludeFirstSeenTime(Boolean.TRUE);
            report.setIncludeLastSeenTime(Boolean.TRUE);

            // The reader can be set into various modes in which reader
            // dynamics are optimized for specific regions and environments.
            // The following mode, AutoSetDenseReader, monitors RF noise and interference and then automatically
            // and continuously optimizes the reader's configuration
            settings.setReaderMode(ReaderMode.AutoSetDenseReader);
            //
            //settings.setReaderMode(ReaderMode.MaxThroughput);

            settings.setSearchMode(SearchMode.DualTargetBtoASelect);
            //settings.setSession(1);
            settings.setTagPopulationEstimate(128);

            // TODO: Enable / Disable the antennas and set the power levels
            AntennaConfigGroup antennas = settings.getAntennas();
            antennas.disableAll();
            //antennas.enableById(new short[]{1});
            antennas.enableAll();
            antennas.setTxPowerinDbm(30.0); // Decent balance of dBm to power requried. 
            antennas.getAntenna((short) 1).setIsMaxRxSensitivity(true);
            //antennas.getAntenna((short) 1).setIsMaxTxPower(true);
            //antennas.getAntenna((short) 1).setTxPowerinDbm(32.0);
            //antennas.getAntenna((short) 1).setRxSensitivityinDbm(-70);

            reader.setTagReportListener(new ImpinjTagReportListener(readerID));
            reader.setAntennaChangeListener(new ImpinjAntennaChangeListener());

            reader.queryStatus().getAntennaStatusGroup().getAntennaList().forEach(a -> {
                logger.trace(" Antenna Status: Port: " + a.getPortNumber() + " Connected: " + a.isConnected());

            });

            logger.debug("Applying Settings");
            reader.applySettings(settings);

            logger.info("Starting reader {}", reader.getAddress());
            reader.start();
            reading = true;

        } catch (OctaneSdkException ex) {
            logger.error("OctaneSdkExcepton", ex);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            logger.error("OctaneSdkExcepton", ex);
        }

        logger.trace("Exiting Impinj::startReading()");

    }

    @Override
    public void stopReading() {

        if (reading) {
            try {
                reader.stop();
            } catch (OctaneSdkException ex) {
                logger.error("OctaneSdkExcepton", ex);
            }

            reader.disconnect();
            reading = false;
        }
    }

}
