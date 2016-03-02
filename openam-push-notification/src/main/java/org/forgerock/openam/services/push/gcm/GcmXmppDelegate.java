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
* Copyright 2016 ForgeRock AS.
*/

/**
 * Portions copyright 2015 Google Inc.
 */
package org.forgerock.openam.services.push.gcm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.identity.shared.debug.Debug;
import java.io.IOException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.services.push.PushMessage;
import org.forgerock.openam.services.push.PushNotificationDelegate;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationServiceConfig;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.joda.time.DateTimeUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Delegate for communicating with GCM over XMPP.
 */
public class GcmXmppDelegate implements PushNotificationDelegate {

    private static final String GCM_NAMESPACE = "google:mobile:data";
    private static final String GCM_ELEMENT_NAME = "gcm";

    private final JsonParser jsonParser;
    private final Debug debug;

    private PushNotificationServiceConfig config;
    private SmackCCS smackCcsClient;

    // Filter to determine what messages get handled here, passed to external handler or ignored.
    private StanzaFilter stanzaFilter;

    // Handle normal, ack, nack and control type, incoming GCM messages. For normal messages,
    // call onMessage to be handled externally. For other message types log their receipt but
    // more involved handling could be done.
    private StanzaListener stanzaListener;

    /**
     * Generate a new GcmXMPPDelegate ready for its services to be started.
     *
     * @param config Used to configure this delegate instance.
     * @param debug Used to report errors.
     */
    public GcmXmppDelegate(PushNotificationServiceConfig config, final Debug debug) {
        this.debug = debug;
        this.config = config;

        jsonParser = new JsonParser();
    }

    /**
     * Begins the inbound listeners and outbound communications lines to GCM.
     *
     * @throws PushNotificationException If anything goes wrong.
     */
    public void startServices() throws PushNotificationException {
        try {
            smackCcsClient = new SmackCCS(config.getApiKey(), config.getSenderId(), "XMPP Service",
                    config.getEndpoint(), config.getPort());
        } catch (XMPPException | SmackException | IOException e) {
            throw new PushNotificationException("Unable to instantiate and configure a connection to GCM XMPP.", e);
        }

        // Add the GcmPacketExtension as an extension provider.
        ProviderManager.addExtensionProvider(GCM_ELEMENT_NAME, GCM_NAMESPACE,
                new ExtensionElementProvider<GcmPacketExtension>() {
                    @Override
                    public GcmPacketExtension parse(XmlPullParser parser, int initialDepth)
                            throws XmlPullParserException, IOException, SmackException {
                        String json = parser.nextText();
                        return new GcmPacketExtension(json);
                    }
                });

        stanzaFilter = new StanzaFilter() {
            @Override
            public boolean accept(Stanza stanza) {
                // Accept messages from GCM CCS.
                if (stanza.hasExtension(GCM_ELEMENT_NAME, GCM_NAMESPACE)) {
                    return true;
                }
                // Reject messages that are not from GCM CCS.
                return false;
            }
        };

        stanzaListener = new StanzaListener() {
            @Override
            public void processPacket(Stanza packet) throws SmackException.NotConnectedException {
                // Extract the GCM message from the packet.
                GcmPacketExtension packetExtension =
                        (GcmPacketExtension) packet.getExtension(GCM_NAMESPACE);

                JsonObject jGcmMessage = jsonParser.parse(packetExtension.getJson()).getAsJsonObject();
                String from = jGcmMessage.get("from").getAsString();

                // If there is no message_type normal GCM message is assumed.
                if (!jGcmMessage.has("message_type")) {
                    if (StringUtils.isNotEmpty(from)) {
                        JsonObject jData = jGcmMessage.get("data").getAsJsonObject();
                        onMessage(from, jData);

                        // Send Ack to CCS to confirm receipt of upstream message.
                        String messageId = jGcmMessage.get("message_id").getAsString();
                        if (StringUtils.isNotEmpty(messageId)) {
                            sendAck(from, messageId);
                        } else {
                            debug.error("Message ID is null or empty.");
                        }
                    } else {
                        debug.error("From is null or empty.");
                    }
                } else {
                    // Handle message_type here.
                    String messageType = jGcmMessage.get("message_type").getAsString();
                    if (messageType.equals("ack")) {
                        // Handle ACK. Here the ack is logged, you may want to further process the ACK at this
                        // point.
                        String messageId = jGcmMessage.get("message_id").getAsString();
                        debug.message("ACK received for message " + messageId + " from " + from);
                    } else if (messageType.equals("nack")) {
                        // Handle NACK. Here the nack is logged, you may want to further process the NACK at
                        // this point.
                        String messageId = jGcmMessage.get("message_id").getAsString();
                        debug.message("NACK received for message " + messageId + " from " + from);
                    } else if (messageType.equals("control")) {
                        debug.message("Control message received.");
                        String controlType = jGcmMessage.get("control_type").getAsString();
                        if (controlType.equals("CONNECTION_DRAINING")) {
                            // Handle connection draining
                            // SmackCcsClient only maintains one connection the CCS to reduce complexity. A real
                            // world application should be capable of maintaining multiple connections to GCM,
                            // allowing the application to continue to onMessage for incoming messages on the
                            // draining connection and sending all new out going messages on a newly created
                            // connection.
                            debug.message("Current connection will be closed soon.");
                        } else {
                            // Currently the only control_type is CONNECTION_DRAINING, if new control messages
                            // are added they should be handled here.
                            debug.message("New control message has been received.");
                        }
                    }
                }
            }
        };

        smackCcsClient.listen(stanzaListener, stanzaFilter);
    }

    /**
     * Define the handling of received upstream GCM message data.
     *
     * @param from Sender of the upstream message.
     * @param jData JSON data representing the payload of the GCM message.
     */
    public void onMessage(String from, JsonObject jData) {
        //No action currently. Convert from GCMXMPP to general PushMessage, and handle later.
    }

    /**
     * Send messages to recipient via GCM.
     *
     * @param pushMessage message to send.
     */
    public void send(PushMessage pushMessage) {
        String to = pushMessage.getRecipient();
        JsonValue message = pushMessage.getData();
        message.add("to", to);
        message.add("message_id", DateTimeUtils.currentTimeMillis() + "");

        final String payload = message.toString();
        Stanza stanza = new Stanza() {
            @Override
            public CharSequence toXML() {
                return wrapWithXML(payload);
            }
        };

        debug.message("sending msg: " + stanza);
        smackCcsClient.sendStanza(stanza);
    }

    @Override
    public boolean isRequireNewDelegate(PushNotificationServiceConfig newConfig) {
        return !config.equals(newConfig);
    }

    @Override
    public void updateDelegate(PushNotificationServiceConfig newConfig) {
        config = newConfig;
    }

    @Override
    public void close() throws IOException {
        //close down the xmpp listener
    }

    /**
     * Send Ack message back to CCS to acknowledged the receipt of the message with ID msg_id.
     *
     * @param to Registration token of the sender of the message being acknowledged.
     * @param msgId ID of message being acknowledged.
     */
    private void sendAck(String to, String msgId) {
        JsonObject jPayload = new JsonObject();
        jPayload.addProperty("to", to);
        jPayload.addProperty("message_id", msgId);
        jPayload.addProperty("message_type", "ack");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final String payload = gson.toJson(jPayload);
        Stanza stanza = new Stanza() {
            @Override
            public CharSequence toXML() {
                return wrapWithXML(payload);
            }
        };

        debug.message("sending ack: " + stanza);
        smackCcsClient.sendStanza(stanza);
    }

    /**
     * Wrap payload with appropriate xml for XMPP transport.
     * @param payload String to be wrapped.
     */
    private String wrapWithXML(String payload) {

        return String.format("<message><%s xmlns=\"%s\">%s</%s></message>",
                GCM_ELEMENT_NAME, GCM_NAMESPACE, payload, GCM_ELEMENT_NAME);
    }

    /**
     * Extension of Packet to allow production and consumption of packets, to and from GCM.
     */
    private class GcmPacketExtension implements ExtensionElement {

        private String json;

        public GcmPacketExtension(String json) {
            this.json = json;
        }

        public String getJson() {
            return json;
        }

        @Override
        public String getNamespace() {
            return GCM_NAMESPACE;
        }

        @Override
        public String getElementName() {
            return GCM_ELEMENT_NAME;
        }

        @Override
        public CharSequence toXML() {
            return String.format("<%s xmlns=\"%s\">%s</%s>", getElementName(), getNamespace(), json,
                    getElementName());
        }
    }

}
