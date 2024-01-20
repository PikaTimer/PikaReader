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

import com.impinj.octane.AntennaChangeListener;
import com.impinj.octane.AntennaEvent;
import com.impinj.octane.ImpinjReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class ImpinjAntennaChangeListener implements AntennaChangeListener {
    
        private static final Logger logger = LoggerFactory.getLogger(AntennaChangeListener.class);

    
    Impinj reader = null;
    @Override
    public void onAntennaChanged(ImpinjReader r, AntennaEvent e) {
        logger.info("Antenna Status Change: Reader: {} Port: {} State: {}",reader.getID(), e.getPortNumber(), e.getState().toString());
        reader.getAntennaStatus().put((int) e.getPortNumber(), e.getState().toString().replace("Antenna", ""));
    }
    
    ImpinjAntennaChangeListener(Impinj reader){
        this.reader = reader;
    }
}