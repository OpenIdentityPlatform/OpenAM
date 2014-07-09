/**
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
 * Copyright 2014 ForgeRock AS.
 */

/*
 * Portions Copyrighted 2010-2014 ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.scripted.http;

import org.forgerock.http.client.RestletHttpClient;
import org.forgerock.http.client.response.HttpClientResponse;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A HTTP Rest client for Groovy auth module
 */
public class GroovyHttpClient extends RestletHttpClient {

    /**
     * @param uri URI of resource to be accessed
     * @param requestData Data to be sent during the request
     * @return The response from the REST call
     * @throws UnsupportedEncodingException
     */
    public HttpClientResponse get(String uri, Map<String, List<Map<String,String>>> requestData) throws UnsupportedEncodingException {
        return getHttpClientResponse(uri, null, requestData, "GET");
    }

    /**
     * @param uri URI of resource to be accessed
     * @param body The body of the http request
     * @param requestData Data to be sent during the request
     * @return The response from the REST call
     * @throws UnsupportedEncodingException
     */
    public HttpClientResponse post(String uri, String body, Map<String, List<Map<String,String>>> requestData) throws UnsupportedEncodingException {
        return getHttpClientResponse(uri, body, requestData, "POST");
    }

}
