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

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
import com.pikatimer.pikareader.tags.TagRead;
import com.impinj.octane.ImpinjReader;
import com.impinj.octane.Tag;
import com.impinj.octane.TagReport;
import com.impinj.octane.TagReportListener;
import com.pikatimer.pikareader.conf.PikaConfig;
import com.pikatimer.pikareader.tags.TagReadProcessor;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImpinjTagReportListener implements TagReportListener {

    //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final Logger logger = LoggerFactory.getLogger(ImpinjTagReportListener.class);

    private final TagReadProcessor tagReadProcessor = TagReadProcessor.getInstance();
    private Integer readerID = -1;

    public ImpinjTagReportListener(Integer readerID) {
        this.readerID = readerID;

    }

    @Override
    public void onTagReported(ImpinjReader reader, TagReport report) {
        List<Tag> tags = report.getTags();

        ZoneId zoneId = PikaConfig.getInstance().getTimezoneId();

        for (Tag t : tags) {

            TagRead tr = new TagRead();

            tr.setReaderID(readerID);
            
            logger.trace("Tag Read: {} at {}",  t.getEpc().toHexString(), t.getLastSeenTime().getLocalDateTime().toInstant().toString());

            /*
            System.out.print(" EPC: " + t.getEpc().toHexString());
            //Long epc = Long.decode("0x" + t.getEpc().toHexString());
            BigInteger epc = new BigInteger(t.getEpc().toHexString(),16);
            
            //TagData td = t.getEpc();
            System.out.print(" Tag: " + td.toWordList());
            System.out.print(" Chip #: " + epc.toString());
             */
            tr.setEPC(t.getEpc().toHexString());
//            tr.setReaderIP(reader.getAddress());

            // Save the antenna port number
            // short can't directly convert to an Integer. Thank kids. 
            tr.setReaderAntenna(Integer.valueOf(t.getAntennaPortNumber()));

            // Stash the RSSI as it helps with the antenna stats 
            tr.setPeakRSSI(t.getPeakRssiInDbm());

            // Impinj returns a Date object in UTC, So we get to convert it. 
            Instant timestamp = t.getLastSeenTime().getLocalDateTime().toInstant();

            tr.setEpochMilli(timestamp.toEpochMilli());
            tr.setTZOffset(ZonedDateTime.ofInstant(timestamp, zoneId).getOffset().toString());
            tr.setTimestamp(LocalDateTime.ofInstant(timestamp, zoneId));

            tagReadProcessor.processTagRead(tr);

        }
    }
}
