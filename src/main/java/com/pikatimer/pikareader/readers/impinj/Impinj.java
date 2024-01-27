/*
 * Copyright (C) 2024 John Garner <segfaultcoredump@gmail.com>
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
import com.impinj.octane.FeatureSet;
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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
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
    private final ImpinjReader reader;
    private ImpinjPowerLevels powerLevel = ImpinjPowerLevels.HIGH; // 30.0dBm / 1.0W. A go-to value for most stuff. 

    private Map<Integer, String> antennaStatus = new HashMap<>();

    private JSONObject readerConfig = new JSONObject();

    private String readerIP;
    private Integer readerID;
    private Boolean reading = false;

    private Integer antennaCount = 0;

    public Impinj() {
        readerID = -1;
        readerIP = "";

        try {
            Class.forName("com.impinj.octane.ImpinjReader");
        } catch (ClassNotFoundException ex) {
            logger.error("Impinj Octane Libraries not found. Please make sure that the octane jar is in the classpath");
            System.exit(1);
        }
        reader = new ImpinjReader();
    }

    @Override
    public Boolean isReading() {
        return reading;
    }
    
    @Override
    public String getStatus(){
        return reading?"Reading":reader.isConnected()?"Connected":"Disconnected";
    }

    @Override
    public String getType() {
        return "IMPINJ";
    }

    @Override
    public void setClock() {
        logger.trace("Entering Impinj::setClock() for {}", readerIP);
        try {

            logger.info("Setting the clock for {}...", readerIP);

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

            logger.debug("{} setClock() Start Timestamp: {}", readerIP, Instant.now());

            cmd = "config system time " + OffsetDateTime.now(ZoneOffset.UTC).plusNanos(100000000).format(formatter);
            logger.debug("Sending command '" + cmd + "' to " + readerIP);

            reply = rshell.send(cmd);

            logger.trace("Raw Reply");
            logger.trace(reply);
            logger.debug("{} setClock() Finish Timestamp: {}", readerIP, Instant.now());

            rshell.close();
            logger.info("Clock set for {}", readerIP);

        } catch (OctaneSdkException ex) {
            logger.error("OctaneSdkExcepton", ex);
        } catch (Exception ex) {
            logger.error("Exception", ex);
        }

        logger.trace("Exiting Impinj::setClock()");

    }

    @Override
    public Boolean isConnected() {
        return reader.isConnected();
    }

    protected Boolean connect() {
        try {

            //reader = new ImpinjReader();
            if (!reader.isConnected()) {
                logger.info("Connecting to {}", readerIP);
                reader.connect(readerIP);

                FeatureSet features = reader.queryFeatureSet();
                antennaCount = Long.valueOf(features.getAntennaCount()).intValue();

                // Make sure the reader's time is correct
                setClock();

                reader.setReaderStartListener(new ImpinjReaderStartStopListener(this));
                reader.setReaderStopListener(new ImpinjReaderStartStopListener(this));

                Settings settings = reader.queryDefaultSettings();
                //logger.info("Current Reader Mode: {}", settings.getReaderMode().toString());
                //logger.info("Current Reader Search Mode: {}", settings.getSearchMode().toString());

                ReportConfig report = settings.getReport();
                report.setIncludeAntennaPortNumber(true);
                report.setMode(ReportMode.Individual);
                report.setIncludePeakRssi(Boolean.TRUE);
                //report.setIncludePhaseAngle(Boolean.TRUE);
                //report.setIncludeFirstSeenTime(Boolean.TRUE);
                report.setIncludeLastSeenTime(Boolean.TRUE);
                //settings.setReport(report);

                // We will default to the static fast mode 
                // as we have a dynamic tag environment and want a faster
                // read rate at the cost of a slight loss of range
                // See https://support.impinj.com/hc/en-us/articles/360000046899
                // AutoSetStaticFast is not available on 1 and 2 port readers
                // so fall back to AutoSetDenseReader
                if (antennaCount > 2) {
                    settings.setReaderMode(ReaderMode.AutoSetStaticFast);
                } else {
                    settings.setReaderMode(ReaderMode.AutoSetDenseReader);
                }

                // We don't want to miss new tags in the 'A' state entering
                // the field when we are inventorying the 'B' tags
                // So we will go with the Dual Target B -> A Select mode
                // See https://support.impinj.com/hc/en-us/articles/202756158
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

                // Antenna Settings
                // Antennas are numbered from 1 -> 4
                AntennaConfigGroup antennas = settings.getAntennas();

                antennas.enableAll();

                // Set the power levels and sensitivity
                powerLevel = ImpinjPowerLevels.getLevel(readerConfig.optString("Power Level"));
                logger.info("Setting power level for reader {} to {} ({})", readerID, powerLevel.getLevel(), powerLevel.getDBm());
                antennas.setIsMaxRxSensitivity(true);
                
                // Set the setIsMaxTxPower to false otherwise 
                // the system ignores the setTxPowerinDbm call. 
                if(ImpinjPowerLevels.MAX.equals(powerLevel)) {
                    antennas.setIsMaxTxPower(true);
                } else {
                    antennas.setIsMaxTxPower(false);
                    antennas.setTxPowerinDbm(powerLevel.getDBm());
                }

                // Enable / Disable antennas based on the reader config
                antennas.enableAll();
                JSONObject antennaConfig = readerConfig.optJSONObject("Antenna Config");
                if (antennaConfig != null) {
                    antennaConfig.keySet().forEach(k -> {
                        if (antennaConfig.optString(k).toLowerCase().startsWith("disable")) try {
                            antennas.getAntenna(Integer.valueOf(k)).setEnabled(false);
                        } catch (Exception ex) {
                            logger.warn("Exception disabling antenna port {} for reader {}", k, readerID);
                        }
                    });
                }
                
               
                reader.setTagReportListener(new ImpinjTagReportListener(readerID));
                reader.setAntennaChangeListener(new ImpinjAntennaChangeListener(this));

                reader.queryStatus().getAntennaStatusGroup().getAntennaList().forEach(a -> {
                    try {
                        Boolean enabled = antennas.getAntenna(a.getPortNumber()).isEnabled();
                        logger.info(" Antenna Status: Reader: {} Port: {} Enabled: {} Connected: {} ", readerID, a.getPortNumber(), enabled, a.isConnected());
                        String s = enabled ? a.isConnected() ? "Connected" : "Disconnected" : "Disabled";
                        Integer port = (int) a.getPortNumber();
                        antennaStatus.put(port, s);
                    } catch (Exception e) {
                        logger.warn("Er, it did not like port {}", a.getPortNumber());
                    }
                });

                logger.debug("Applying Settings for readerID {}", readerID);
                reader.applySettings(settings);

                // set up a listener for connection Lost
                reader.setConnectionLostListener(new ImpinjConnectionLostListener(this));

            }

        } catch (OctaneSdkException ex) {
            logger.error("OctaneSdkExcepton connecting to {} with stack trace {}", readerIP, ex.getMessage());
            reader.disconnect();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            logger.error("Impinj Connect Exception", ex);
        }

        return (reader != null && reader.isConnected());

    }

    @Override
    public void startReading() {

        logger.trace("Entering Impinj::startReading()");

        logger.info("Starting the reader with the following config: " + readerConfig.toString(4));

        if (reading) {
            return;
        }

        try {

            if (connect()) {

                // Make sure the reader's time is correct
                setClock();

                logger.info("Starting reader {}", reader.getAddress());
                reader.start();
                reading = true;

            }

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
    public RFIDReader create(JSONObject config) {
        Impinj newReader = new Impinj();
        newReader.readerID = config.optInt("Index", 0);
        newReader.readerIP = config.optString("IP", "127.0.0.1");
        newReader.readerConfig = config;

        logger.info("Created new IMPINJ Reader with the following config: {}", config.toString(4));

        Thread.startVirtualThread(() -> newReader.connect());
        return newReader;
    }

    void setReading(boolean b) {
        reading = b;
    }

    @Override
    public Map<Integer, String> getAntennaStatus() {
        return antennaStatus;

    }

}
