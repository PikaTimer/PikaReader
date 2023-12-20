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

import com.pikatimer.pikareader.conf.PikaConfig;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class DiscoveryListener {

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryListener.class);
    private static final PikaConfig pikaConfig = PikaConfig.getInstance();

    public static Thread startDiscoveryListener() {
        // Setup a network discovery listener so others can find us
        // Adapted from https://michieldemey.be/blog/network-discovery-using-udp-broadcast/
        Thread discoveryThread = new Thread(() -> {
            
                try {

                    //Open a socket to listen to all the UDP trafic that is destined for port 8080
                    DatagramSocket socket = new DatagramSocket(8080, InetAddress.getByName("0.0.0.0"));
                    socket.setBroadcast(true);
                    
                    // get the port that the web app is listening on
                    String webPort = pikaConfig.getKey("Web").optIntegerObject("Port", 8080).toString();

                    while (true) {
                        logger.info("Network Discovery Listener: Ready to receive broadcast packets");
                        //Receive a packet
                        byte[] recvBuf = new byte[15000];
                        DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                        socket.receive(packet);

                        //Packet received
                        String message = new String(packet.getData()).trim();
                        logger.debug("Discovery broadcast received from: {} with {}", packet.getAddress().getHostAddress(), message);

                        //See if the packet holds the right command (message)
                        if (message.equals("DISCOVER_PIKA_READER_REQUEST")) {
                            byte[] sendData = webPort.getBytes();
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                            socket.send(sendPacket);
                            logger.debug("Replied to: " + sendPacket.getAddress().getHostAddress());
                        }
                    }
                } catch (IOException ex) {
                    logger.debug("Exception in DiscoveryListener", ex);
                }
        });
        
        discoveryThread.setDaemon(true);
        discoveryThread.setName("DiscoveryListenerThread");
        discoveryThread.start();
        
        return discoveryThread;
    }
}
