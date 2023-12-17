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
package com.pikatimer.pikareader.tags;

import com.pikatimer.pikareader.conf.PikaConfig;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Stream;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class TagDB {

    private static final BlockingQueue<Collection<TagRead>> tagQueue = new ArrayBlockingQueue(100);
    private static final Logger logger = LoggerFactory.getLogger(TagDB.class);
    private static final PikaConfig pikaConfig = PikaConfig.getInstance();
    private final JSONObject dbConfig;
    private static final List<TagRead> tagList = new ArrayList<>();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("/yyyy/MM/dd/HH/HH-mm-ss.SSS'.dat'");

    /**
     * SingletonHolder is loaded on the first execution of
     * Singleton.getInstance() or the first access to SingletonHolder.INSTANCE,
     * not before.
     */
    private static class SingletonHolder {

        private static final TagDB INSTANCE = new TagDB();

    }

    public static TagDB getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private TagDB() {

        // Get the config
        dbConfig = pikaConfig.getKey("DB");

        // if we have an empty config, build out the defaults
        if (dbConfig.isEmpty()) {
            dbConfig.put("Path", System.getProperty("DB", System.getProperty("user.home") + "/.PikaReader")); // Default Gating of 3 seconds
            dbConfig.put("Retention", 180); // 180 days

            pikaConfig.putObject("DB", dbConfig);
        }

        Path dbPath = Path.of(dbConfig.getString("Path"));

        try {
            Files.createDirectories(dbPath);
        } catch (IOException ex) {
            logger.error("Unable to create database directory {}", dbPath.toAbsolutePath());
        }

        Thread t = new Thread(() -> {

            // Setup the EclipseStore DB and populate the root object
//            logger.info("Starting EclipseStore with DB path of {}", dbPath.toAbsolutePath());
//
//            EmbeddedStorageManager storageManager = EmbeddedStorageConfigurationBuilder.New()
//                    .setBackupDirectory(dbBackupPath.toString())
//                    .setStorageDirectory(dbPath.toString())
//                    .createEmbeddedStorageFoundation()
//                    .createEmbeddedStorageManager();
//            storageManager.setRoot(tagDBRoot);
//            storageDB = storageManager.start();
            //storageDB = EmbeddedStorage.start(tagDBRoot, storageManager);
            Instant start = Instant.now();
            try (Stream<Path> walk = Files.walk(dbPath)) {
                walk.parallel().filter(Files::isRegularFile).forEach(f -> {
                    try {
                        Files.lines(f).forEach(s -> {
                            try {
                                tagList.add(new TagRead(new JSONObject(s)));
                            } catch (JSONException ex) {
                                logger.error("Error parsing {} in file at {}", s, f.toAbsolutePath().toString());
                            }
                        });
                    } catch (Exception ex) {
                        logger.error("Error reading file at {}", f.toAbsolutePath().toString(), ex);
                    }
                });
            } catch (IOException ex) {
                logger.error("Error reading DB at {}", dbPath.toAbsolutePath().toString(), ex);
            }
            Instant end = Instant.now();
            Duration elapsed = Duration.between(start, end);
            logger.info("Loaded TagDB in {}ms with {} existing reads", elapsed.toMillis(), tagList.size());

            // Setup loop to add new tag reads to a new file as they come in. 
            // To save on IO and more importantly, survive a sudden filesystem shutdown when the 
            // user pulls the power, each cycle will dump all of the tags into a single file. 
            // generally speaking, this will produce no more than 1 file per gating perod. 
            try {
                while (true) {
                    Collection<TagRead> tr = tagQueue.take();

                    // Add all of the tags to our list
                    tagList.addAll(tr);

                    StringBuilder data = new StringBuilder();
                    tr.forEach(read -> {
                        data.append(read.toJSON()).append(System.lineSeparator());
                    });

                    // target file of <dbbapth>/YYYY/MM/DD/HH/HH-MM-ss.SSS.dat 
                    File outputFile = new File(dbPath.toString() + LocalDateTime.now().format(formatter));

                    logger.debug("Writing to data to {}", outputFile.getAbsolutePath());

                    // Make the parent path
                    outputFile.getParentFile().mkdirs();

                    try (FileWriter output = new FileWriter(outputFile);) {
                        output.write(data.toString());
                    } catch (IOException ex) {
                        logger.error("Error writing to {}", outputFile.getAbsolutePath(), ex);
                    }
                }
            } catch (InterruptedException ex) {
            }

            logger.info("Exiting TagDBThread");
        });

        t.setDaemon(true);
        t.setName("TagDBThread");
        t.start();

        logger.info("TagDB Processing Thread started.");
    }

    public Collection<TagRead> getReads() {
        return new ArrayList<>(tagList);
    }

    public void addReads(Collection<TagRead> reads) {
        tagQueue.add(reads);

    }

}
