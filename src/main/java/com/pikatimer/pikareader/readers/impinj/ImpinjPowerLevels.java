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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public enum ImpinjPowerLevels {

    LOW("Low", 23.0),
    MEDIUM("Medium", 27.0),
    HIGH("High", 30.0),
    MAX("Max", 32.5);

    private final Double dBm;
    private final String level;

    private static final Map<String, ImpinjPowerLevels> levelMap = createMap();

    ImpinjPowerLevels(String s, Double l) {
        level = s;
        dBm = l;
    }

    private static Map<String, ImpinjPowerLevels> createMap() {
        Map<String, ImpinjPowerLevels> map = new HashMap<>();

        for (ImpinjPowerLevels l : ImpinjPowerLevels.values()) {
            map.put(l.getLevel(),l);
        }

        return Collections.unmodifiableMap(map);
    }
    
    public static ImpinjPowerLevels getLevel(String s){
        return levelMap.containsKey(s)?levelMap.get(s):HIGH;
    }

    public String getLevel() {
        return level;
    }

    public Double getDBm() {
        return dBm;
    }

    @Override
    public String toString() {
        return this.level;
    }
}
