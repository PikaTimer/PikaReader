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
import com.pikatimer.pikareader.readers.impinj.Impinj;
import java.util.ArrayList;
import java.util.List;
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
    private static final List<RFIDReader> readers = new ArrayList();
    
    private final JSONObject readerConfig; 

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
        
        // if we have an empty config, build out the defaults
        if (readerConfig.isEmpty()) {
            readerConfig.put("Gating", 3); // Default Gating of 3 seconds
            readerConfig.put("Gating Style", ReaderGatingStyle.READER);
            JSONArray defaultReaders = new JSONArray();
            JSONObject defaultReader = new JSONObject();
            defaultReader.put("Index", 0);
            defaultReader.put("Type", "IMPINJ");
            defaultReader.put("IP", "127.0.0.1");
            defaultReader.put("Power Level" , "HIGH");
            defaultReaders.put(defaultReader);
            
            readerConfig.put("Readers", defaultReaders);
            
            pikaConfig.putObject("Reader", readerConfig);
        }
        
        // itterate through the readers and set them up
        readerConfig.getJSONArray("Readers").forEach(r -> {
           JSONObject rc = (JSONObject) r;
           Integer index = rc.optInt("Index", 0);
           String ip = rc.optString("IP","127.0.0.1");
           String powerLevel = rc.optString("Power Level", "HIGH");
           String type = rc.optString("Type", "IMPINJ");
           
           RFIDReader reader = switch (type) {
                        case "IMPINJ" -> new Impinj(index, ip);
                        default -> new Impinj(index, ip);
                    };
           
           reader.setPower(powerLevel);
           
           readers.add(reader);
        
        });
        
        
    }
    
    public void setClocks() {
        readers.stream().parallel().forEach(r -> r.setClock());
    }
    
    public void startReading() {
        readers.stream().parallel().forEach(r -> r.startReading());
    }
    
    public void stopReading() {
        readers.stream().parallel().forEach(r -> r.stopReading());
    }

}
