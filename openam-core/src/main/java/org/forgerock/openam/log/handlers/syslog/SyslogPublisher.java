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
 * Portions copyright 2014-2016 ForgeRock AS.
 */
package org.forgerock.openam.log.handlers.syslog;

import com.sun.identity.log.spi.Debug;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

/**
 * This class manages the real socket that is connected to syslog daemon. The socket could be either TCP or UDP socket.
 * If TCP socket is closed, it is (re)opened before sending a message.
 */
public abstract class SyslogPublisher {

    private static final int MAX_ATTEMPT_LIMIT = 3;
    protected final SocketAddress socketAddress;

    /**
     * Creates a new SyslogPublisher instance.
     *
     * @param socketAddress The socket address to be used for sending the syslog messages.
     */
    public SyslogPublisher(final InetSocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }

    /**
     * Flush underlying connection. Sends buffered bytes as UDP datagram or as the next message on TCP stream. The real
     * socket is (re)opened before, if necessary.
     *
     * @param logRecords The logRecords that needs to be published to Syslog.
     */
    public void publishLogRecords(final List<String> logRecords) {
        int failedAttempts = 0;
        Iterator<String> iterator = logRecords.iterator();
        while (iterator.hasNext()) {
            String logRecord = iterator.next();
            byte[] bytes = logRecord.getBytes(Charset.forName("UTF-8"));

            try {
                reconnect();
                sendLogRecord(bytes);
            } catch (IOException ioe) {
                Debug.error("Unable to publish syslog records due to " + ioe.getMessage()
                        + "\nAudit record was: " + logRecord);
                try {
                    closeConnection();
                } catch (IOException ioe2) {
                }
                if (++failedAttempts == MAX_ATTEMPT_LIMIT) {
                    while (iterator.hasNext()) {
                        // Just give up and log the rest of the audit entries without trying
                        Debug.error("Unable to publish syslog record due to non-transient network errors"
                                + "\nAudit record was: " + iterator.next());
                    }
                }
            }
        }
    }

    /**
     * Checks if the currently opened connection is still valid, and connects/reconnects to the Syslog server if
     * necessary.
     *
     * @throws IOException If there was an IO error while trying to connect to the Syslog server.
     */
    protected abstract void reconnect() throws IOException;

    /**
     * Sends the log record to the syslog server by the means of the chosen protocol (TCP or UDP).
     *
     * @param bytes The logRecord's byte[] representation.
     * @throws IOException If there was an IO error while sending the audit entry to Syslog.
     */
    protected abstract void sendLogRecord(byte[] bytes) throws IOException;

    /**
     * Closes the underlying connection.
     *
     * @throws IOException If there was an IO error while closing the connection.
     */
    protected abstract void closeConnection() throws IOException;
}
