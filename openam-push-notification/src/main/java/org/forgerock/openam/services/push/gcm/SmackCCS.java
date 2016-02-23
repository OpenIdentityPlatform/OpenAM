/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Portions Copyright 2016 ForgeRock AS.
 *  - Based on SmackCCS from Google reference impl.
 */

package org.forgerock.openam.services.push.gcm;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLSocketFactory;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

/**
 * SmackCCS provides communication with GCM Cloud Connection Server (XMPP Server).
 * This sample uses Smack version 4.1.0.
 */
public class SmackCCS {

    private static final Logger logger = Logger.getLogger("SmackCssClient");

    private AbstractXMPPConnection connection;

    /**
     * Generate a new SmackCCS communicator.
     *
     * @param apiKey Used to authenticate the senderId.
     * @param senderId Used to identify the sender.
     * @param serviceName The name of this service for logging.
     * @param host Location of remote service.
     * @param port Port of remote service.
     */
    public SmackCCS(String apiKey, String senderId, String serviceName, String host, int port)
            throws IOException, XMPPException, SmackException {
        XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                .setServiceName(serviceName)
                .setHost(host)
                .setSocketFactory(SSLSocketFactory.getDefault())
                .setSendPresence(false)
                .setPort(port)
                .build();

        connection = new XMPPTCPConnection(config);
        Roster.getInstanceFor(connection).setRosterLoadedAtLogin(false);

        connection.addConnectionListener(new ConnectionListener() {
            @Override
            public void connected(XMPPConnection connection) {
                logger.info("Connected to CCS");
            }

            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                logger.info("Authenticated with CCS");
            }

            @Override
            public void connectionClosed() {
                logger.info("Connection to CCS closed");
            }

            @Override
            public void connectionClosedOnError(Exception e) {
                logger.log(Level.WARNING, "Connection closed because of an error.", e);
            }

            @Override
            public void reconnectionSuccessful() {
                logger.info("Reconnected to CCS");
            }

            @Override
            public void reconnectingIn(int seconds) {
                logger.info("Reconnecting to CCS in " + seconds);
            }

            @Override
            public void reconnectionFailed(Exception e) {
                logger.log(Level.WARNING, "Reconnection to CCS failed", e);
            }
        });

        // Connect and authenticate with to XMPP server.
        connection.connect();
        connection.login(senderId, apiKey);
    }

    /**
     * Begin listening for incoming messages.
     *
     * @param stanzaListener Listener that handles accepted messages. This is defined in
     *                       FriendlyPingServer.
     * @param stanzaFilter Filter that determines what messages are handled by the listener.
     */
    public void listen(StanzaListener stanzaListener, StanzaFilter stanzaFilter) {
        connection.addAsyncStanzaListener(stanzaListener, stanzaFilter);
        logger.info("Listening for incoming XMPP Stanzas...");
    }

    /**
     * Sends data out across the wire.
     *
     * @param stanza XMPP stanza to send.
     */
    public void sendStanza(Stanza stanza) {
        try {
            connection.sendStanza(stanza);
        } catch (SmackException.NotConnectedException e) {
            logger.log(Level.SEVERE, "Error occurred while sending stanza.", e);
        }
    }
}
