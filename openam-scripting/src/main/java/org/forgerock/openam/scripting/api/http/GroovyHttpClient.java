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
 * Copyright 2010-2015 ForgeRock AS.
 */

package org.forgerock.openam.scripting.api.http;

import javax.inject.Inject;
import javax.inject.Named;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.http.Client;
import org.forgerock.http.client.RestletHttpClient;
import org.forgerock.http.client.response.HttpClientResponse;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

/**
 * A HTTP Rest client for Groovy auth module.
 *
 * @deprecated Will be replaced in a later release by {@link Client}.
 */
@Deprecated
public class GroovyHttpClient extends RestletHttpClient {

    private static final Debug DEBUG = Debug.getInstance("amScript");

    private final Client client;

    @Inject
    public GroovyHttpClient(@Named("ScriptingHttpClient") Client client) {
        this.client = client;
    }

    /**
     * @param uri URI of resource to be accessed
     * @param requestData Data to be sent during the request
     * @return The response from the REST call
     * @throws UnsupportedEncodingException
     */
    public HttpClientResponse get(String uri, Map<String, List<Map<String,String>>> requestData)
            throws UnsupportedEncodingException {
        DEBUG.warning("'get' has been deprecated. Use 'send' instead");
        return getHttpClientResponse(uri, null, requestData, "GET");
    }

    /**
     * @param uri URI of resource to be accessed
     * @param body The body of the http request
     * @param requestData Data to be sent during the request
     * @return The response from the REST call
     * @throws UnsupportedEncodingException
     */
    public HttpClientResponse post(String uri, String body, Map<String, List<Map<String,String>>> requestData)
            throws UnsupportedEncodingException {
        DEBUG.warning("'post' has been deprecated. Use 'send' instead");
        return getHttpClientResponse(uri, body, requestData, "POST");
    }

    /**
     * Sends an HTTP request and returns a {@code Promise} representing the
     * pending HTTP response.
     *
     * @param request
     *            The HTTP request to send.
     * @return A promise representing the pending HTTP response.
     */
    public Promise<Response, NeverThrowsException> send(final Request request) {
        return client.send(request);
    }
}
