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
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * A HTTP Rest client for JavaScript auth module
 */
public class JavaScriptHttpClient extends RestletHttpClient {

    /**
     * @param uri URI of resource to be accessed
     * @param requestData Data to be sent during the request
     * @return The response from the REST call
     * @throws UnsupportedEncodingException
     */
    public HttpClientResponse get(String uri, NativeObject requestData) throws UnsupportedEncodingException {
        return getHttpClientResponse(uri, null, convertRequestData(requestData), "GET");
    }

    /**
     * @param uri URI of resource to be accessed
     * @param body The body of the http request
     * @param requestData Data to be sent during the request
     * @return The response from the REST call
     * @throws UnsupportedEncodingException
     */
    public HttpClientResponse post(String uri, String body, NativeObject requestData) throws UnsupportedEncodingException {
        return getHttpClientResponse(uri, body, convertRequestData(requestData), "POST");
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
