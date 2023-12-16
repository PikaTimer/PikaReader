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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public enum ReaderGatingStyle {

    BOX("Box"),
    ANTENNA("Antenna"),
    READER("Reader");

    private final String style;

    private static final Map<String, ReaderGatingStyle> styleMap = createMap();

    ReaderGatingStyle(String s) {
        style = s;
    }

    private static Map<String, ReaderGatingStyle> createMap() {
        Map<String, ReaderGatingStyle> map = new HashMap<>();

        for (ReaderGatingStyle l : ReaderGatingStyle.values()) {
            map.put(l.getStyle(),l);
        }

        return Collections.unmodifiableMap(map);
    }
    
    public static ReaderGatingStyle getStyle(String s){
        return styleMap.getOrDefault(s, READER);
    }

    public String getStyle() {
        return style;
    }

 

    @Override
    public String toString() {
        return this.style;
    }
}
