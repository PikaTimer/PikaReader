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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImpinjTagReportListener implements TagReportListener {

    //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final Logger logger = LoggerFactory.getLogger(ImpinjTagReportListener.class);

    private final TagReadProcessor tagReadProcessor = TagReadProcessor.getInstance();
    private Integer readerID = -1;
    ZoneId zoneId = ZoneId.systemDefault();

    public ImpinjTagReportListener(Integer readerID) {
        this.readerID = readerID;

        try {
            Pattern pattern = Pattern.compile("[+-]\\d+");
            zoneId = switch (PikaConfig.getInstance().getStringValue("Timezone").toUpperCase()) {
                case "", "AUTO" ->
                    ZoneId.systemDefault();
                case String s when pattern.matcher(s).matches() ->
                    ZoneId.ofOffset("UTC", ZoneOffset.of(PikaConfig.getInstance().getStringValue("Timezone")));
                default ->
                    ZoneId.of(PikaConfig.getInstance().getStringValue("Timezone"));
            };
        } catch (Exception e) {
            logger.error("Unable to parse timezone {}, falling back to {}", PikaConfig.getInstance().getStringValue("Timezone"), zoneId.toString());
        }

        logger.info("ImpinjTagReporteListener using zoneID " + zoneId.toString() + " Offset: " + ZonedDateTime.now(zoneId).getOffset());

    }

    @Override
    public void onTagReported(ImpinjReader reader, TagReport report) {
        List<Tag> tags = report.getTags();

        for (Tag t : tags) {

            TagRead tr = new TagRead();

            tr.setReaderID(readerID);

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

            // short can't directly convert to an Integer. Thank kids. 
            tr.setReaderAntenna(Integer.valueOf(t.getAntennaPortNumber()));

            // Impinj returns a Date object, even if it is really a LocalDateTime under the covers. So we get to convert it. 
            tr.setTimestamp(LocalDateTime.ofInstant(t.getLastSeenTime().getLocalDateTime().toInstant(), zoneId));
            tr.setPeakRSSI(t.getPeakRssiInDbm());

            tagReadProcessor.processTagRead(tr);

        }
    }
}
