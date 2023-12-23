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

import com.impinj.octane.ImpinjReader;
import com.impinj.octane.ReaderStartEvent;
import com.impinj.octane.ReaderStartListener;
import com.impinj.octane.ReaderStopEvent;
import com.impinj.octane.ReaderStopListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
class ImpinjReaderStartStopListener implements ReaderStartListener,ReaderStopListener {
    private static final Logger logger = LoggerFactory.getLogger(ImpinjReaderStartStopListener.class);
    
    Impinj impinjReader;

    ImpinjReaderStartStopListener(Impinj reader) {
        impinjReader = reader;
    }

    @Override
    public void onReaderStart(ImpinjReader reader, ReaderStartEvent rse) {
        logger.info("ReaderID {} started: {}",impinjReader.getID());
        impinjReader.setReading(true);
    }

    @Override
    public void onReaderStop(ImpinjReader reader, ReaderStopEvent rse) {
        logger.info("ReaderID {} stopped: {}",impinjReader.getID());
        impinjReader.setReading(false);
    }
    
}
