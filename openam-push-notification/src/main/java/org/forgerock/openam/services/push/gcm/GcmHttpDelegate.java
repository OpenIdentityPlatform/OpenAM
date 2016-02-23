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
package org.forgerock.openam.services.push.gcm;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.services.push.gcm.GcmConstants.*;

import com.sun.identity.shared.debug.Debug;
import java.io.IOException;
import java.net.URISyntaxException;
import org.forgerock.http.Client;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.services.push.PushMessage;
import org.forgerock.openam.services.push.PushNotificationDelegate;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationServiceConfig;

/**
 * Delegate for communicating with GCM over HTTP.
 */
public class GcmHttpDelegate implements PushNotificationDelegate {

    private final Debug debug;

    private final Client client;
    private final HttpClientHandler handler;

    private PushNotificationServiceConfig config;

    /**
     * Generates a new GCM HTTP Delegate, used to communicate over the internet with
     * the GCM cloud service.
     *
     * @param handler configures the HTTP client used to connect to the remote service.
     * @param config Necessary to configure this delegate.
     * @param debug to write out debug messages.
     */
    public GcmHttpDelegate(HttpClientHandler handler, PushNotificationServiceConfig config, Debug debug) {
        this.handler = handler;
        this.client = new Client(handler);
        this.config = config;
        this.debug = debug;
    }

    @Override
    public void send(PushMessage message) {
        Request httpRequest = new Request();
        httpRequest.getHeaders().put(HTTP_AUTH_HEADER, HTTP_AUTH_KEY + config.getApiKey());
        httpRequest.getHeaders().put(HTTP_CONTENT_TYPE_HEADER, HTTP_CONTENT_TYPE);
        httpRequest.setMethod(HTTP_METHOD);

        try {
            httpRequest.setUri(config.getEndpoint());
        } catch (URISyntaxException e) {
            debug.error("Unable to realize requested URL {}", config.getEndpoint(), e);
        }

        httpRequest.getEntity().setJson(convertToGcm(message).getObject());
        send(httpRequest); //do nothing with the response in VS1
    }

    @Override
    public boolean isRequireNewDelegate(PushNotificationServiceConfig newConfig) {
        return !config.equals(newConfig);
    }

    @Override
    public void updateDelegate(PushNotificationServiceConfig newConfig) {
        //This section intentionally left blank.
    }

    @Override
    public void startServices() throws PushNotificationException {
        //This section intentionally left blank.
    }

    @Override
    public void close() throws IOException {
        handler.close();
    }

    private JsonValue convertToGcm(PushMessage message) {
        JsonValue toSend = json(object(field(TO, message.getRecipient())));

        if (message.getData().size() > 0) {
            toSend.put(DATA, message.getData());
        }

        return toSend;
    }

    private Response send(Request request) {
        return client.send(request).getOrThrowUninterruptibly();
    }

}