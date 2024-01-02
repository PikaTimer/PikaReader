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
package com.pikatimer.pikareader.readers;

import com.pikatimer.pikareader.conf.PikaConfig;
import com.pikatimer.pikareader.status.StatusHandler;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class ReaderHandler {

    private static final Logger logger = LoggerFactory.getLogger(ReaderHandler.class);

    private static final PikaConfig pikaConfig = PikaConfig.getInstance();
    private static final Map<Integer, RFIDReader> readers = new HashMap<>();
    private static final Map<String, RFIDReader> rfidReaderFactory = new HashMap<>();

    private final JSONObject readerConfig;

    private Boolean isReading = false;

    /**
     * SingletonHolder is loaded on the first execution of
     * Singleton.getInstance() or the first access to SingletonHolder.INSTANCE,
     * not before.
     */
    private static class SingletonHolder {

        private static final ReaderHandler INSTANCE = new ReaderHandler();

    }

    public static ReaderHandler getInstance() {

        return SingletonHolder.INSTANCE;
    }

    private ReaderHandler() {
        // get the config and build the hierarchy
        readerConfig = pikaConfig.getKey("Reader");

        // We will use a service loader to make it easeir for somebody to add
        // a new reader type by just wiring up their own RFIDReader implementation
        // and attaching a jar file. 
        ServiceLoader<RFIDReader> serviceLoader = ServiceLoader.load(RFIDReader.class);

        for (RFIDReader service : serviceLoader) {
            logger.info("Loaded {} RFIDReader handler.", service.getType());
            rfidReaderFactory.put(service.getType(), service);
        }

        logger.info("Found " + rfidReaderFactory.size() + " services!");

        // if we have an empty config, build out the defaults
        if (readerConfig.isEmpty()) {
            readerConfig.put("Gating", 3); // Default Gating of 3 seconds
            readerConfig.put("Gating Style", ReaderGatingStyle.READER);
            JSONArray defaultReaders = new JSONArray();
            JSONObject defaultReader = new JSONObject();
            defaultReader.put("Index", 0);
            defaultReader.put("Type", "IMPINJ");
            defaultReader.put("IP", "127.0.0.1");
            defaultReader.put("Power Level", "HIGH");
            defaultReaders.put(defaultReader);

            readerConfig.put("Readers", defaultReaders);

            pikaConfig.putObject("Reader", readerConfig);
        }

        // itterate through the readers and set them up
        readerConfig.getJSONArray("Readers").forEach(r -> {
            JSONObject rc = (JSONObject) r; // FFS
            Integer index = rc.optInt("Index", 0);
            String type = rc.optString("Type", "NOT SET");

            if (rfidReaderFactory.containsKey(type)) {
                RFIDReader reader = rfidReaderFactory.get(type).create(rc);
                readers.put(index, reader);
            } else {
                logger.error("RFID Reader Config Error! No handler found for RFID Reader type {}" , type);
            }
        });

    }

    public void setClocks() {
        readers.values().stream().parallel().forEach(r -> r.setClock());
    }

    public Boolean isReading() {
        return isReading;
    }

    public void startReading() {
        CountDownLatch latch = new CountDownLatch(readers.size());

        // Start all readers in parallel. 
        // A parallelStream() is a suggestion but this guarantees it
        logger.info("Starting Readers");
        readers.values().stream().forEach(r -> {
            Thread.startVirtualThread(() -> {
                r.startReading();
                latch.countDown();
            });
        });
        try {
            latch.await(30, TimeUnit.SECONDS);

        } catch (InterruptedException ex) {
        }
        isReading = true;
        StatusHandler.getInstance().clearReadCount();

        logger.info("Readers Started");

    }

    public void stopReading() {
        logger.info("Stopping Readers");
        CountDownLatch latch = new CountDownLatch(readers.size());
        readers.values().parallelStream().forEach(r -> {
            r.stopReading();
            latch.countDown();
        });
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
        }
        isReading = false;
        logger.info("Readers Stopped");
    }

    public Collection<RFIDReader> getReaders() {
        return readers.values();
    }

}
