/*
 * Copyright (C) 2023 John Garner <segfaultcoredump@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either VERSION 3 of the License, or
 * (at your option) any later VERSION.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.pikatimer.pikareader.conf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class PikaConfig {

    private static final String VERSION = "0.5";

    private static final Logger logger = LoggerFactory.getLogger(PikaConfig.class);

    private JSONObject configRoot;
    private final File configFile = new File(System.getProperty("CONFIG", System.getProperty("user.home") + "/.PikaReader.cfg"));

    /**
     * SingletonHolder is loaded on the first execution of
     * Singleton.getInstance() or the first access to SingletonHolder.INSTANCE,
     * not before.
     */
    private static class SingletonHolder {

        private static final PikaConfig INSTANCE = new PikaConfig();

    }

    public static PikaConfig getInstance() {

        return SingletonHolder.INSTANCE;
    }

    private PikaConfig() {
        loadConfig();
    }

    private void loadConfig() {
        logger.trace("Starting PikaConfig::loadConfig()");
        JSONObject root= new JSONObject();
        try {
            // does the config file exist?
            if (configFile.createNewFile()) {
                logger.info("Created new config file at {}", configFile.getAbsolutePath());
                root.put("Version", VERSION);
                configRoot = root;
                saveConfig();
            } else {
                logger.info("Reading existing config file at {}", configFile.getAbsolutePath());
                try (FileReader file = new FileReader(configFile, StandardCharsets.UTF_8)) {
                    BufferedReader r = new BufferedReader(file);
                    root = new JSONObject(r.lines().collect(Collectors.joining()));
                    r.close();
                } catch (Exception ex) {
                    logger.error("Error in config file at {}", configFile.getAbsolutePath(), ex);
                    System.exit(0);
                }
                logger.debug("Read existing config file with version {}", root.optString("Version"));
                configRoot = root;
                if (!root.optString("Version").equals(VERSION)) {
                    logger.info("Version mismatch, updating to {}", VERSION);
                    putValue("Version", VERSION);
                }
                
            }
        } catch (IOException ex) {
            logger.error("Unable to save config file at {}", configFile.getAbsolutePath(), ex);
            System.exit(0);
        }
        
        // look for some defaults:
        
        // UnitID and IP
        // the IP can change so we always update it. The MAC is used as a unit ID and should not change
        
        String MAC = "UNKNOWN";
        String IP = "UNKNOWN";
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback() && ni.getHardwareAddress() != null) {
                    //System.out.print(HexFormat.ofDelimiter(":").formatHex(ni.getHardwareAddress()) + "-> ");
                    for (InetAddress a : ni.inetAddresses().toList()) {
                        if (a.getAddress().length == 4) {
                            IP = a.getHostAddress();
                            MAC = HexFormat.ofDelimiter(":").formatHex(ni.getHardwareAddress()).toUpperCase().substring(9);
                        }
                    }
                }
            }
        } catch (SocketException ex) {

        }
        logger.info("PikaConfig: Using the following UnitID and IP: " + MAC + "  -> " + IP);
        
        if (configRoot.optString("UnitID").isBlank()){
            putValue("UnitID", MAC);
        }
        
        putValue("UnitIP", IP);
        
        // Timezone
        if (configRoot.optString("Timezone").isBlank()){
            putValue("Timezone", "AUTO");
        }

        logger.trace("Exiting PikaConfig::loadConfig()");
    }

    private void saveConfig() {
        // write the config file out to disk
        logger.info("Saving config");
        try (FileWriter file = new FileWriter(configFile, StandardCharsets.UTF_8)) {
            //file.write(configRoot.toString(4));
            configRoot.write(file, 4, 0);
            logger.info("Saved PikaReader config file to {}", configFile.getAbsolutePath());
        } catch (IOException ex) {
            // TODO Auto-generated catch block
            logger.error("Unable to save config file at {}", configFile.getAbsolutePath(), ex);
        }
    }

    public JSONObject getKey(String key) {
        return configRoot.optJSONObject(key, new JSONObject());
    }

    public JSONArray getArray(String key) {
        return configRoot.optJSONArray(key);
    }

    public void putArray(String key, JSONArray array) {
        configRoot.put(key, array);
        saveConfig();
    }

    public void putObject(String key, JSONObject object) {
        configRoot.put(key, object);
        saveConfig();
    }

    public void putValue(String key, Object value) {
        configRoot.put(key, value);
        saveConfig();
    }

    public String getStringValue(String key) {
        return configRoot.optString(key);
    }

    public Integer getIntegerValue(String key) {
        return configRoot.optIntegerObject(key);
    }
}
