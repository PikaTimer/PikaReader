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
package com.pikatimer.pikareader.status;

import com.pikatimer.pikareader.conf.PikaConfig;
import com.pikatimer.pikareader.http.HTTPHandler;
import com.pikatimer.pikareader.readers.RFIDReader;
import com.pikatimer.pikareader.readers.ReaderHandler;
import com.pikatimer.pikareader.tags.TagRead;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class StatusHandler {

    private static final Logger logger = LoggerFactory.getLogger(StatusHandler.class);
    private static final BlockingQueue<TagRead> tagQueue = new ArrayBlockingQueue<>(100000);

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private static final HTTPHandler httpHandler = HTTPHandler.getInstance();
    private static final ReaderHandler readerHandler = ReaderHandler.getInstance();
    private static final PikaConfig pikaConfig = PikaConfig.getInstance();

    private JSONObject lastStatus = new JSONObject();

    public JSONObject getStatus() {
        return lastStatus;
    }

    private static class SingletonHolder {

        private static final StatusHandler INSTANCE = new StatusHandler();

    }

    public static StatusHandler getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private StatusHandler() {

        logger.debug("StatusHandler Init starting");

        Runnable statusUpdate = () -> {
            logger.trace("statusUpdate fired");
            JSONObject statusReport = new JSONObject();

            statusReport.put("reading", readerHandler.isReading());

            Collection<RFIDReader> readers = readerHandler.getReaders();

            
            // Track the strongest read on a given antenna
            Map<String, Map<Integer, Double>> antennaReadStrengthMap = new HashMap<>(32);
            
            JSONArray readerStatus = new JSONArray();
            readers.forEach(r -> {
                JSONObject reader = new JSONObject();
                reader.put("id", r.getID());
                reader.put("reading", r.isReading());
                reader.put("antennas", r.getAntennaStatus());
                reader.put("type", r.getType());
                readerStatus.put(reader);
                
                Map<Integer, Double> s = new HashMap<>(4);
                r.getAntennaStatus().keySet().forEach(v -> s.put(v,0.0));
                antennaReadStrengthMap.put(r.getID().toString(), s);
                
            });
            statusReport.put("readers", readerStatus);

            
            List<TagRead> tags = new ArrayList<>();
            tagQueue.drainTo(tags);
            
            statusReport.put("Raw Tag Reads/s", tags.size());
            
            tags.forEach(t -> {
                String readerID = t.getReaderID().toString();
                Integer antennaID = t.getReaderAntenna();
                Double rssi = t.getPeakRSSI() + 130.0;

                if (antennaReadStrengthMap.containsKey(readerID)) {
                    Map<Integer, Double> stat = antennaReadStrengthMap.get(readerID);
                    if (stat.containsKey(antennaID)) {
                        if (rssi.compareTo(stat.get(antennaID)) > 0) {
                            stat.put(antennaID, rssi);
                        }
                    } else {
                        stat.put(antennaID, rssi);
                    }
                } else {
                    Map<Integer, Double> s = new HashMap<>(4);
                    s.put(antennaID, rssi);
                    antennaReadStrengthMap.put(readerID, s);
                }
            });

            JSONObject antennaStats = new JSONObject();
            antennaReadStrengthMap.keySet().forEach(a -> {
                antennaStats.put(a, antennaReadStrengthMap.get(a));
            });
            statusReport.put("antenna read strength", antennaReadStrengthMap);

            // TODO: Raspberry Pi Stats: 
            // GPS Stats
            // NTP Stats
            // Battery Level / Voltage
            
            Instant now = Instant.now();
            
            statusReport.put("timestamp", now.toString());
            statusReport.put("local Time", LocalDateTime.ofInstant(now, pikaConfig.getTimezoneId()).toString());
            statusReport.put("timezone", pikaConfig.getTimezoneId().toString());
            lastStatus = statusReport;
            httpHandler.postStatus(statusReport);
            logger.debug("Status Report: " + statusReport.toString(4));
            logger.trace("statusUpdate complete");
        };

        // Fire off once a second as close to the second as possible
        Integer delay = (1000000000 - Instant.now().getNano()) / 1000000;
        executor.scheduleAtFixedRate(statusUpdate, delay, 1000, TimeUnit.MILLISECONDS);

        logger.debug("StatusHandler Init Done");

    }

    public void postRead(TagRead read) {
        tagQueue.add(read);
    }

    public void postReads(Collection<TagRead> reads) {
        reads.forEach(r -> {
            tagQueue.add(r);
        });

    }

}
