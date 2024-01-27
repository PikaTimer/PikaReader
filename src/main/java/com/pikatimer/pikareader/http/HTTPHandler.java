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
package com.pikatimer.pikareader.http;

import com.pikatimer.pikareader.conf.PikaConfig;
import com.pikatimer.pikareader.readers.ReaderHandler;
import com.pikatimer.pikareader.status.Status;
import com.pikatimer.pikareader.status.StatusHandler;
import com.pikatimer.pikareader.tags.TagDB;
import com.pikatimer.pikareader.tags.TagRead;
import com.pikatimer.pikareader.tags.TagReadRouter;
import com.pikatimer.pikareader.util.DebugLogHolder;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.websocket.WsContext;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class HTTPHandler {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd' 'HH:mm:ss.SSS");

    private static final Logger logger = LoggerFactory.getLogger(HTTPHandler.class);

    private static final PikaConfig pikaConfig = PikaConfig.getInstance();
    private final JSONObject webConfig;

    private Thread javalinThread;
    private Javalin javalinApp;
    private static final List<WsContext> webSocketConnections = new ArrayList<>();

//    ReaderHandler readerHandler = ReaderHandler.getInstance();
    private static class SingletonHolder {

        private static final HTTPHandler INSTANCE = new HTTPHandler();

    }

    public static HTTPHandler getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private HTTPHandler() {
        logger.trace("Starting HTTPHandler constructor");
        // get the config and build the hierarchy
        webConfig = pikaConfig.getKey("Web");

        // if we have an empty config, build out the defaults
        if (webConfig.isEmpty()) {
            webConfig.put("Port", 8080); // Default Gating of 3 seconds
            pikaConfig.putObject("Web", webConfig);
        }

        logger.info("Starting Javalin on port {}", webConfig.optIntegerObject("Port", 8080));

        javalinThread = new Thread(() -> {
            javalinApp = Javalin.create(config -> {
                config.staticFiles.add("/public", Location.CLASSPATH);
            });

            // WebSocket to send all events along
            javalinApp.ws("/events", ws -> {
                ws.onConnect(ctx -> {
                    ctx.enableAutomaticPings();
                    webSocketConnections.add(ctx);
                });
                ws.onClose(ctx -> {
                    webSocketConnections.remove(ctx);
                });
                ws.onMessage(ctx -> {

                });
            });

            // Stop Readers
            javalinApp.get("/start", ctx -> {
                logger.info("HTTPD Request: /start -> Starting Readers...");
                ReaderHandler.getInstance().startReading();
                JSONObject response = new JSONObject();
                response.put("readers", "started");
                response.put("timestamp", LocalDateTime.now(PikaConfig.getInstance().getTimezoneId()).format(formatter));
                ctx.json(response.toString());
            });

            // Start Readers
            javalinApp.get("/stop", ctx -> {
                logger.info("HTTPD Request: /stop -> Stopping Readers...");
                ReaderHandler.getInstance().stopReading();
                JSONObject response = new JSONObject();
                response.put("readers", "stopped");
                response.put("timestamp", LocalDateTime.now(PikaConfig.getInstance().getTimezoneId()).format(formatter));
                ctx.json(response.toString());
            });

            // debug info
            javalinApp.get("/debug", ctx -> {
                StringBuilder response = new StringBuilder();
                response.append("<html><head><Title>PikaReader Debug</title></head><body>\n");
                DebugLogHolder.getEventList().forEach(event -> {
                    //ILoggingEvent event = DebugLogHolder.eventMap.get(e);
                    response.append(event.getInstant()).append(": ");
                    response.append(event.getLevel().toString()).append(": ");
                    response.append(event.getLoggerName()).append(": ");
                    response.append(event.getFormattedMessage());
                    //if (event.hasCallerData()) response.append(event.getCallerData());
                    response.append("<BR>\n");
                });
                response.append("<body><html>\n");
                ctx.html(response.toString());
            });

            // Trigger
            javalinApp.get("/trigger", ctx -> {
                Instant now = Instant.now();
                TagRead tr = new TagRead();
                tr.setEPC("0");
                tr.setEpochMilli(now.getEpochSecond());
                tr.setTimestamp(LocalDateTime.now(PikaConfig.getInstance().getTimezoneId()));
                tr.setTZOffset(ZonedDateTime.ofInstant(now, PikaConfig.getInstance().getTimezoneId()).getOffset().toString());
                tr.setReaderID(0);
                tr.setReaderAntenna(0);
                tr.setPeakRSSI(0.0);
                List<TagRead> trigger = new ArrayList<>();
                trigger.add(tr);
                TagReadRouter.getInstance().processTagReads(trigger);
                ctx.json(tr.toJSON());
            });

            // Rewinds the data. from and to are in ISO_LOCAL_DATE_TIME 2011-12-03T10:15:30
            javalinApp.get("/rewind/{from}/{to}", ctx -> {

                LocalDateTime fromTime = LocalDateTime.parse(ctx.pathParam("from"));
                LocalDateTime toTime = LocalDateTime.parse(ctx.pathParam("to"));

                JSONArray data = new JSONArray();
                TagDB.getInstance().getReads().stream().sorted().forEach(read -> {
                    if (fromTime.isBefore(read.getTimestamp()) && toTime.isAfter(read.getTimestamp())) {
                        data.put(read.toJSONObject());
                    }
                });
                ctx.json(data.toString());
            });

            // Rewinds the data. from is in ISO_LOCAL_DATE_TIME: 2011-12-03T10:15:30
            javalinApp.get("/rewind/{from}", ctx -> {

                LocalDateTime fromTime = LocalDateTime.parse(ctx.pathParam("from"));
                JSONArray data = new JSONArray();
                TagDB.getInstance().getReads().stream().sorted().forEach(read -> {

                    if (fromTime.isBefore(read.getTimestamp())) {
                        data.put(read.toJSONObject());
                    }
                });
                ctx.json(data.toString());
            });

            javalinApp.get("/rewind", ctx -> {
                JSONArray data = new JSONArray();
                TagDB.getInstance().getReads().stream().filter(Objects::nonNull).sorted().forEach(read -> {
                    data.put(read.toJSONObject());
                });
                ctx.json(data.toString());
            });

            // TODO: Status Page
            javalinApp.get("/status", ctx -> {
                ctx.json(StatusHandler.getInstance().getStatus().toString(4));
            });
            // TODO: Live Antenna Monitor page
            // TODO: Reader config page
            // TODO: Uploader config page
            // Start the javalin server
            javalinApp.start(webConfig.optIntegerObject("Port", 8080));

        });

        javalinThread.setDaemon(true);
        javalinThread.setName("JavalinThread");
        javalinThread.start();

        logger.trace("Exiting HTTPHandler constructor");

    }

    public void stopHTTPD() {
        javalinApp.stop();
    }

    public void sendTag(TagRead tr) {
        webSocketConnections.stream().filter(ctx -> ctx.session.isOpen()).forEach(ws -> {
            JSONObject read = tr.toJSONObject();
            read.put("type", "READ");
            ws.send(read.toString());
        });
    }

    public void sendTags(Collection<TagRead> reads) {
        reads.forEach(tr -> {
            sendTag(tr);
        });
    }

    public void sendTag(Status s) {

    }

    public void postStatus(JSONObject statusReport) {
        statusReport.put("type", "STATUS");
        webSocketConnections.stream().filter(ctx -> ctx.session.isOpen()).forEach(ws -> {
            ws.send(statusReport.toString());
        });
    }

}
