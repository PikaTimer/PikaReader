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

import java.util.Map;
import org.json.JSONObject;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */

public interface RFIDReader {
    
    // Reader Status getStatus()
    public String getType();
    public Boolean isReading();
    public Boolean isConnected();
    public Integer getID();
    public String getIP();
    public String getStatus();
    public void setClock();
    public void startReading();
    public void stopReading();
    //public void setPower(String powerLevel);
    

    public RFIDReader create(JSONObject rc);
    
    public Map<Integer,String> getAntennaStatus();
}
