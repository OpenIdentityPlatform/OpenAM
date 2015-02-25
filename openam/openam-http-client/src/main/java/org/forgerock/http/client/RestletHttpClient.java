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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.http.client;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.client.request.HttpClientRequest;
import org.forgerock.http.client.request.HttpClientRequestCookie;
import org.forgerock.http.client.request.HttpClientRequestFactory;
import org.forgerock.http.client.response.HttpClientResponse;
import org.forgerock.http.client.response.SimpleHttpClientResponse;
import org.restlet.Client;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.*;
import org.restlet.engine.util.CookieSettingSeries;
import org.restlet.util.Series;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * A basic http client that can be used to send
 * {@link org.forgerock.http.client.request.HttpClientRequest} objects and receive {@link org.forgerock.http.client.response.HttpClientResponse} objects.
 *
 * @since 12.0.0
 */
public class RestletHttpClient {
    final HttpClientRequestFactory httpClientRequestFactory = InjectorHolder.getInstance(HttpClientRequestFactory.class);

    protected HttpClientResponse getHttpClientResponse(String uri, String body, Map<String, List<Map<String,String>>> requestData, String method) throws UnsupportedEncodingException {
        HttpClientRequest httpClientRequest = httpClientRequestFactory.createRequest();

        httpClientRequest.setMethod(method);
        httpClientRequest.setUri(uri);
        httpClientRequest.setEntity(body);

        if (requestData != null) {
            List<Map<String,String>> cookies = requestData.get("cookies");
            if (cookies != null) {
                for (Map cookie : cookies) {
                    httpClientRequest.addCookie((String) cookie.get("domain"),
                            (String) cookie.get("field"), (String) cookie.get("value"));
                }
            }

            List<Map<String,String>> headers = requestData.get("headers");
            if (headers != null) {
                for (Map header : headers) {
                    httpClientRequest.addQueryParameter((String) header.get("field"),
                            (String) header.get("value"));
                }
            }
        }

        return perform(httpClientRequest);
    }

    private HttpClientResponse perform(HttpClientRequest httpClientRequest) throws UnsupportedEncodingException {
        Request request = createRequest(httpClientRequest);

        Client client = new Client(Protocol.HTTP);
        Response response = new Response(request);
        client.handle(request, response);

        return createHttpClientResponse(response);
    }

    private HttpClientResponse createHttpClientResponse(Response response) {
        Integer statusCode = null;
        String reasonPhrase = null;
        Map<String, String> headersMap = null;
        Map<String, String> cookiesMap = null;

        if (response.getStatus() != null) {
            statusCode = response.getStatus().getCode();
            reasonPhrase = response.getStatus().getDescription();
        }
        String messageBody = response.getEntityAsText();
        Series headersSeries = (Series) response.getAttributes().get("org.restlet.http.headers");
        if (headersSeries != null) {
            headersMap = headersSeries.getValuesMap();
        }
        Series<CookieSetting> cookieSettings = response.getCookieSettings();
        if (cookieSettings != null) {
            cookiesMap = response.getCookieSettings().getValuesMap();
        }

        return new SimpleHttpClientResponse(statusCode, reasonPhrase, headersMap, messageBody, cookiesMap);
    }

    private Request createRequest(HttpClientRequest httpClientRequest) throws UnsupportedEncodingException {
        Request request = new Request();
        request.setMethod(Method.valueOf(httpClientRequest.getMethod()));
        request.setResourceRef(httpClientRequest.getUri());
        if (hasEntity(httpClientRequest)) {
            request.setEntity(httpClientRequest.getEntity(), MediaType.ALL);
        }
        if (hasHeaders(httpClientRequest)) {
            addHeadersToRequest(httpClientRequest, request);
        }
        if (hasQueryParameters(httpClientRequest)) {
            addQueryParametersToRequest(httpClientRequest, request);
        }
        if (hasCookies(httpClientRequest)) {
            addCookiesToRequest(httpClientRequest, request);
        }

        return request;
    }

    private void addHeadersToRequest(HttpClientRequest httpClientRequest, Request request) {
        Map<String, Object> headersMap = new HashMap<String, Object>();
        Form headersForm = new Form();
        for (String key : httpClientRequest.getHeaders().keySet()) {
            headersForm.set(key, httpClientRequest.getHeaders().get(key));
        }
        headersMap.put("org.restlet.http.headers", headersForm);
        request.setAttributes(headersMap);
    }

    private void addQueryParametersToRequest(HttpClientRequest httpClientRequest, Request request) throws UnsupportedEncodingException {
        String queryParameterString = "?";
        for (String field : httpClientRequest.getQueryParameters().keySet()) {
            String encodedField = "";
            String encodedValue = "";
            encodedField = URLEncoder.encode(field, "UTF-8");
            encodedValue = URLEncoder.encode(httpClientRequest.getQueryParameters().get(field), "UTF-8");
            queryParameterString += encodedField + "=" + encodedValue + "&";
        }
        queryParameterString = queryParameterString.substring(0, queryParameterString.length() - 1);
        String uri = httpClientRequest.getUri();
        String uriWithQueryParameters = uri + queryParameterString;

        request.setResourceRef(uriWithQueryParameters);
    }

    private void addCookiesToRequest(HttpClientRequest httpClientRequest, Request request) {
        Series cookieSettingSeries = new CookieSettingSeries();
        for (HttpClientRequestCookie cookie : httpClientRequest.getCookies()) {
            CookieSetting cookieSetting = new CookieSetting();
            cookieSetting.setDomain(cookie.getDomain());
            cookieSetting.setName(cookie.getField());
            cookieSetting.setValue(cookie.getValue());
            cookieSettingSeries.add(cookieSetting);
        }
        request.setCookies(cookieSettingSeries);
    }

    private boolean hasEntity(HttpClientRequest httpClientRequest) {
        return (httpClientRequest.getEntity() != null && !(httpClientRequest.getEntity().isEmpty()));
    }

    private boolean hasHeaders(HttpClientRequest httpClientRequest) {
        return (httpClientRequest.getHeaders() != null && !(httpClientRequest.getHeaders().isEmpty()));
    }

    private boolean hasQueryParameters(HttpClientRequest httpClientRequest) {
        return (httpClientRequest.getQueryParameters() != null &&
                !(httpClientRequest.getQueryParameters().isEmpty()));
    }

    private boolean hasCookies(HttpClientRequest httpClientRequest) {
        return (httpClientRequest.getCookies() != null && !(httpClientRequest.getCookies().isEmpty()));
    }

}