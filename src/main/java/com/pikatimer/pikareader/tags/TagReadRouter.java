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

import com.pikatimer.pikareader.conf.PikaConfig;
import com.pikatimer.pikareader.http.HTTPHandler;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class TagReadRouter implements Runnable {

    private static final BlockingQueue<Collection<TagRead>> tagQueue = new ArrayBlockingQueue(100);
    private static final Logger logger = LoggerFactory.getLogger(TagReadRouter.class);
    private static final PikaConfig pikaConfig = PikaConfig.getInstance();
    private static final HTTPHandler httpHandler = HTTPHandler.getInstance();
    private static final TagDB tagDB = TagDB.getInstance();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd' 'HH:mm:ss.SSS");

    private Thread tagRoutingThread;

    /**
     * SingletonHolder is loaded on the first execution of
     * Singleton.getInstance() or the first access to SingletonHolder.INSTANCE,
     * not before.
     */
    private static class SingletonHolder {

        private static final TagReadRouter INSTANCE = new TagReadRouter();

    }

    public static TagReadRouter getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public void run() {

        try {
            while (true) {
                Collection<TagRead> tr = tagQueue.take();
                             
                // send a copy to the DB
                tagDB.addReads(tr);
                
                // send a copy to the websocket handler
                httpHandler.sendTags(tr);
                
                // if we are uploading to PikaTagRelay, send a copy there too
                
            }
        } catch (InterruptedException ex) {
            logger.trace("Exiting " + tagRoutingThread.getName());
        }
    }

    public void processTagReads(Collection<TagRead> tr) {
        logger.trace("Entering TagReadRouter::processTagReads");

        // Start the tag processing thread
        if (tagRoutingThread == null) {

            //tagProcessingThread = Thread.ofVirtual().name("TagReadProcessingThread").start(this);
            tagRoutingThread = new Thread(TagReadRouter.getInstance());
            tagRoutingThread.setName("TagRouterThread");
            tagRoutingThread.setDaemon(true);
            tagRoutingThread.setPriority(1);

            tagRoutingThread.start();

        }

        logger.info("TagRouter::processTagReads: Recieved {} new tags to process.", tr.size());
        tagQueue.add(tr);

        logger.trace("Exiting TagReadRouter::processTagReads");

    }

}
