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

import com.google.auto.service.AutoService;
import com.impinj.octane.AntennaConfigGroup;
import com.impinj.octane.ImpinjReader;
import com.impinj.octane.OctaneSdkException;
import com.impinj.octane.ReaderMode;
import com.impinj.octane.ReportConfig;
import com.impinj.octane.ReportMode;
import com.impinj.octane.RshellEngine;
import com.impinj.octane.SearchMode;
import com.impinj.octane.Settings;
import com.pikatimer.pikareader.readers.RFIDReader;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
@AutoService(RFIDReader.class)
public class Impinj implements RFIDReader {

    private static final Logger logger = LoggerFactory.getLogger(Impinj.class);
    private ImpinjReader reader = new ImpinjReader();
    private ImpinjPowerLevels powerLevel = ImpinjPowerLevels.HIGH; // 30.0dBm / 1.0W. A go-to value for most stuff. 

    JSONObject readerConfig = new JSONObject();

    private String readerIP;
    private Integer readerID;
    private Boolean reading = false;

    public Impinj() {
        // make the service discovery happy
        readerID = -1;
        readerIP = "";
    }

    @Override
    public Boolean isReading() {
        return reading;
    }

    @Override
    public String getType() {
        return "IMPINJ";
    }

    @Override
    public void setClock() {
        logger.trace("Entering Impinj::setClock()");
        try {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd'-'HH:mm:ss");

            RshellEngine rshell = new RshellEngine();

            /* login using ssh*/
            rshell.openSecureSession(readerIP, "root", "impinj", 10000);
            //rshell.open(readerIP, "root", "impinj", 10000, RShellConnectionType.Telnet);

            // make sure NTP is disabled
            String cmd = "config network ntp disable";
            logger.debug("Sending command '" + cmd + "' to " + readerIP);

            String reply = rshell.send(cmd);

            logger.trace("Raw Reply");
            logger.trace(reply);

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
            logger.debug("setClock() Finish Timestamp: " + Instant.now());

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

        if (reading) {
            return;
        }

        try {

            //reader = new ImpinjReader();
            if (!reader.isConnected()) {
                logger.debug("Connecting to {}", readerIP);
                reader.connect(readerIP);

            }

            // Make sure the reader's time is correct
            logger.info("Setting the clock...");
            setClock();
            logger.info("Clock set");

            reader.setReaderStartListener(new ImpinjReaderStartStopListener(this));
            reader.setReaderStopListener(new ImpinjReaderStartStopListener(this));

            Settings settings = reader.queryDefaultSettings();

            ReportConfig report = settings.getReport();
            report.setIncludeAntennaPortNumber(true);
            report.setMode(ReportMode.Individual);
            report.setIncludePeakRssi(Boolean.TRUE);
            //report.setIncludePhaseAngle(Boolean.TRUE);
            report.setIncludeFirstSeenTime(Boolean.TRUE);
            //report.setIncludeLastSeenTime(Boolean.TRUE);
            //settings.setReport(report);

            // Let the reader monitor the environment and adapt as needed
            settings.setReaderMode(ReaderMode.AutoSetDenseReader);
            //
            //settings.setReaderMode(ReaderMode.MaxThroughput);

            settings.setSearchMode(SearchMode.DualTargetBtoASelect);
            //settings.setSession(1);
            settings.setTagPopulationEstimate(256);

            // Enable keepalivbes
            settings.getKeepalives().setEnabled(true);
            settings.getKeepalives().setPeriodInMs(3000);
            reader.setKeepaliveListener(new ImpinjKeepaliveTimeoutListener());
            
            //TODO:  More testing and incorporating the exampels
            // show in the SDK samples DisconnectedOperation.java file
            // Enable Connection Monitoring
            settings.getKeepalives().setEnableLinkMonitorMode(true);
            settings.getKeepalives().setLinkDownThreshold(5);
            // set up a listener for connection Lost
            reader.setConnectionLostListener(new ImpinjConnectionLostListener());


            // Antenna Settings
            // Antennas are numbered from 1 -> 4
            AntennaConfigGroup antennas = settings.getAntennas();
            //antennas.enableById(new short[]{1});
            //antennas.enablePolarizedAntennas();

            // TODO: Enable / Disable the antennas in the config
            antennas.enableAll();

            // Set the power levels and sensitivity
            powerLevel = ImpinjPowerLevels.getLevel(readerConfig.optString("Power Level", "HIGH"));
            antennas.setIsMaxRxSensitivity(true);
            antennas.setTxPowerinDbm(powerLevel.getDBm());

            // TODO: Per-Antenna power levels
            //antennas.getAntenna((short) 1).setIsMaxRxSensitivity(true);
            //antennas.getAntenna((short) 1).setIsMaxTxPower(true);
            //antennas.getAntenna((short) 1).setTxPowerinDbm(32.0);
            //antennas.getAntenna((short) 1).setRxSensitivityinDbm(-70);
            reader.setTagReportListener(new ImpinjTagReportListener(readerID));
            reader.setAntennaChangeListener(new ImpinjAntennaChangeListener());

            reader.queryStatus().getAntennaStatusGroup().getAntennaList().forEach(a -> {
                logger.info(" Antenna Status: Reader: {} Port: {} Connected: {} ", readerID, a.getPortNumber(),  a.isConnected());
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

    @Override
    public Integer getID() {
        return readerID;
    }

    @Override
    public String getIP() {
        return readerIP;
    }

    @Override
    public RFIDReader getInstance(JSONObject config) {
        Impinj newReader = new Impinj();
        newReader.readerID = config.optInt("Index", 0);
        newReader.readerIP = config.optString("IP", "127.0.0.1");

        readerConfig = config;

        return newReader;
    }

    void setReading(boolean b) {
        reading = b;

        //TODO: Trigger a status change event
    }

}
