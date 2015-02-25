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
package org.forgerock.http.client.request;

import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class SimpleHttpClientRequestTest {

    @Test
    public void shouldConstructSimpleRequestWithCorrectMethod() {
        String method = "GET";

        HttpClientRequest request = new SimpleHttpClientRequest();
        request.setMethod(method);

        assertSame(request.getMethod(), method, "Request method should match set method");
    }

    @Test
    public void shouldConstructSimpleRequestWithCorrectUri() {
        String uri = "http://www.example.com";

        HttpClientRequest request = new SimpleHttpClientRequest();
        request.setUri(uri);

        assertSame(request.getUri(), uri, "Request URI should match set URI");
    }

    @Test
    public void shouldConstructSimpleRequestWithCorrectMessageBody() {
        String entity = "Example message body";

        HttpClientRequest request = new SimpleHttpClientRequest();
        request.setEntity(entity);

        assertSame(request.getEntity(), entity, "Request entity should match set entity");
    }

    @Test
    public void shouldConstructSimpleRequestWithCorrectHeaders() {
        String field = "Content-Type";
        String value = "text/html";

        HttpClientRequest request = new SimpleHttpClientRequest();
        request.addHeader(field, value);

        assertTrue(request.getHeaders().size() == 1, "Request should have a header");
        assertSame(request.getHeaders().get(field), value, "Request header should match set header");
    }

    @Test
    public void shouldConstructSimpleRequestWithCorrectQueryParameters() {
        String field = "field";
        String value = "value";

        HttpClientRequest request = new SimpleHttpClientRequest();
        request.addQueryParameter(field, value);

        assertTrue(request.getQueryParameters().size() == 1, "Request should have a query parameter");
        assertSame(request.getQueryParameters().get(field), value, "Request query parameter should match set query parameter");
    }

    @Test
    public void shouldConstructSimpleRequestWithCorrectCookies() {
        Map<String,String> cookies = new HashMap<String, String>();
        String domain = "domain";
        String field = "field";
        String value = "value";

        HttpClientRequest request = new SimpleHttpClientRequest();
        request.addCookie(domain, field, value);

        assertTrue(request.getCookies().size() == 1, "Request should have a cookie");
        Iterator<HttpClientRequestCookie> iterator = request.getCookies().iterator();
        HttpClientRequestCookie cookie = iterator.next();
        assertSame(cookie.getDomain(), domain, "Request cookie domain should match set cookie domain");
        assertSame(cookie.getField(), field, "Request cookie field should match set cookie field");
        assertSame(cookie.getValue(), value, "Request cookie value should match set cookie value");
    }

}
