/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013 Cybernetica AS
 * Portions copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.log.handlers.syslog;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

/**
 * A {@link SyslogPublisher} implementation that publishes the audit log records via UDP packets.
 */
public class SyslogUdpPublisher extends SyslogPublisher {

    private DatagramSocket datagramSocket;

    public SyslogUdpPublisher(InetSocketAddress socketAddress) {
        super(socketAddress);
    }

    @Override
    protected void reconnect() throws IOException {
        if (datagramSocket == null) {
            datagramSocket = new DatagramSocket();
        }
    }

    @Override
    protected void sendLogRecord(byte[] bytes) throws IOException {
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, socketAddress);
        datagramSocket.send(packet);
    }

    @Override
    protected void closeConnection() throws IOException {
        if (datagramSocket != null) {
            datagramSocket.close();
        }
        datagramSocket = null;
    }
}
