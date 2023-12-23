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

import com.impinj.octane.ConnectionLostListener;
import com.impinj.octane.ImpinjReader;
import com.impinj.octane.OctaneSdkException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class ImpinjConnectionLostListener implements ConnectionLostListener {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ImpinjConnectionLostListener.class);

    public ImpinjConnectionLostListener() {
    }

    @Override
    public void onConnectionLost(ImpinjReader reader) {
        logger.error("Connection to reader lost! {}", reader.getAddress());
        reader.disconnect();
        try {
            logger.info("Attempting to reconnect...");

            reader.connect();
        } catch (OctaneSdkException ex) {
            logger.info("Reconnect timeout. Sleeping 15 seconds and will Try again...");
            try {
                Thread.sleep(5000);

                reader.connect();
            } catch (OctaneSdkException | InterruptedException ex1) {
                logger.error("2nd Try Failure!", ex1);
            }
        }
    }

}
