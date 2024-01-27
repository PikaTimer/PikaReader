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
package com.pikatimer.pikareader.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.pikatimer.pikareader.tags.TagRead;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class DebugLogHolder extends AppenderBase<ILoggingEvent> {

    //public static final ConcurrentMap<Integer, ILoggingEvent> eventMap = new ConcurrentHashMap<>();
    private static final List<ILoggingEvent> eventList = new ArrayList<>();
    //Integer eventCounter = 1;

    @Override
    protected void append(ILoggingEvent event) {
        synchronized (this) {
            eventList.add(event);
            //eventMap.put(eventCounter++, event);
        }
    }

    public static List<ILoggingEvent> getEventList() {
        return new ArrayList<ILoggingEvent>(eventList);
    }
}
