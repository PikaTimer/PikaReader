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

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.json.JSONObject;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class TagRead implements Comparable<TagRead>{

    protected String hexEPC;
    //protected String readerIP;
    protected LocalDateTime timestamp;
    protected Double rssi;
    protected Integer antennaPortNumber;
    protected Integer readerID;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd' 'HH:mm:ss.SSS");

    public void setEPC(String epc) {
        hexEPC = epc;
    }

    public String getEPC() {
        return hexEPC;
    }

    public String getEPCDecimal() {
        return new BigInteger(hexEPC, 16).toString();
    }

//    public void setReaderIP(String address) {
//        readerIP = address;
//    }
//    public String getReaderIP(){
//        return readerIP;
//    }
    public void setTimestamp(LocalDateTime tagTimestamp) {
        timestamp = tagTimestamp;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setPeakRSSI(Double peakRssiInDbm) {
        rssi = peakRssiInDbm;
    }

    public Double getPeakRSSI() {
        return rssi;
    }

    public void setReaderAntenna(Integer antennaPortNumber) {
        this.antennaPortNumber = antennaPortNumber;
    }

    public Integer getReaderAntenna() {
        return antennaPortNumber;
    }

    public Integer getReaderID() {
        return readerID;
    }

    public void setReaderID(Integer readerID) {
        this.readerID = readerID;
    }

    public String toJSON() {
        return toJSONObject().toString();
    }
    
    public JSONObject toJSONObject(){
                JSONObject msg = new JSONObject();
        msg.put("chip", getEPCDecimal());
        msg.put("timestamp", getTimestamp().format(formatter));
        msg.put("reader", getReaderID());
        msg.put("antenna", getReaderAntenna());
        msg.put("rssi", getPeakRSSI());
        return msg;
    }
    
    public TagRead(){
        // do nothing here
    }
    
    public TagRead(JSONObject o){
        readerID = o.optInt("reader");
        antennaPortNumber = o.getInt("antenna");
        rssi = o.getDouble("rssi");
        timestamp = LocalDateTime.parse(o.getString("timestamp"), formatter);
        hexEPC = new BigInteger(o.getString("chip"),10).toString(16);
    }
    
    @Override
    public int compareTo(TagRead other){
        return this.timestamp.compareTo(other.timestamp);
    }
}
