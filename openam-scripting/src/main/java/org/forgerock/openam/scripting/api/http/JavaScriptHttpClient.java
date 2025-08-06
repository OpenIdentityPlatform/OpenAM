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
 * Copyright 2010-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.scripting.api.http;

import static org.forgerock.openam.scripting.ScriptConstants.SCRIPTING_HTTP_CLIENT_NAME;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.http.Client;
import org.forgerock.http.client.RestletHttpClient;
import org.forgerock.http.client.response.HttpClientResponse;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * A HTTP Rest client for JavaScript auth module.
 *
 * @deprecated Will be replaced in a later release by {@link Client}.
 */
@Deprecated
public class JavaScriptHttpClient extends RestletHttpClient {

    private static final Debug DEBUG = Debug.getInstance("amScript");

    private final Client client;

    @Inject
    public JavaScriptHttpClient(@Named(SCRIPTING_HTTP_CLIENT_NAME) Client client) {
        this.client = client;
    }

    /**
     * @param uri URI of resource to be accessed
     * @param requestData Data to be sent during the request
     * @return The response from the REST call
     * @throws UnsupportedEncodingException
     */
    public HttpClientResponse get(String uri, NativeObject requestData) throws UnsupportedEncodingException {
        DEBUG.warning("'get' has been deprecated. Use 'send' instead");
        return getHttpClientResponse(uri, null, convertRequestData(requestData), "GET");
    }

    /**
     * @param uri URI of resource to be accessed
     * @param body The body of the http request
     * @param requestData Data to be sent during the request
     * @return The response from the REST call
     * @throws UnsupportedEncodingException
     */
    public HttpClientResponse post(String uri, String body, NativeObject requestData)
            throws UnsupportedEncodingException {
        DEBUG.warning("'post' has been deprecated. Use 'send' instead");
        return getHttpClientResponse(uri, body, convertRequestData(requestData), "POST");
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

    private Map convertRequestData(NativeObject requestData) {
        HashMap<String, ArrayList<HashMap>> convertedRequestData = new HashMap<String, ArrayList<HashMap>>();

        if (requestData != null) {
            NativeArray cookies = (NativeArray) NativeObject.getProperty(requestData, "cookies");
            ArrayList<HashMap> convertedCookies = new ArrayList<HashMap>();

            if (cookies != null) {
                Object[] cookieIds = cookies.getIds();

                for (Object id : cookieIds) {
                    NativeObject cookie = (NativeObject) cookies.get((Integer) id, null);
                    String domain = (String) cookie.get("domain", null);
                    String field = (String) cookie.get("field", null);
                    String value = (String) cookie.get("value", null);

                    convertedCookies.add(convertCookie(domain, field, value));
                }
            }

            convertedRequestData.put("cookies", convertedCookies);

            NativeArray headers = (NativeArray) NativeObject.getProperty(requestData, "headers");
            ArrayList<HashMap> convertedHeaders = new ArrayList<HashMap>();

            if (headers != null) {
                Object[] headerIds = headers.getIds();

                for (Object id : headerIds) {
                    NativeObject header = (NativeObject) headers.get((Integer) id, null);
                    String field = (String) header.get("field", null);
                    String value = (String) header.get("value", null);
                    convertedHeaders.add(convertHeader(field, value));
                }
            }

            convertedRequestData.put("headers", convertedHeaders);
        }
        return convertedRequestData;
    }

    private HashMap convertHeader(String field, String value) {
        HashMap<String, String> convertedHeader = new HashMap<String,String>();
        convertedHeader.put("field", field);
        convertedHeader.put("value", value);
        return convertedHeader;
    }

    private HashMap<String,String> convertCookie(String domain, String field, String value) {
        HashMap<String,String> convertedCookie = new HashMap<String,String>();
        convertedCookie.put("domain", domain);
        convertedCookie.put("field", field);
        convertedCookie.put("value", value);
        return convertedCookie;
    }

}
