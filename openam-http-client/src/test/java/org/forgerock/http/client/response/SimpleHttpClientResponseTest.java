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
package org.forgerock.http.client.response;

import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class SimpleHttpClientResponseTest {

    @Test
    public void shouldConstructSimpleRequestWithCorrectMethod() {
        Integer statusCode = new Integer(200);
        String reasonPhrase = "OK";
        String messageBody = "Example message body";
        Map<String,String> headers = new HashMap<String, String>();
        headers.put("key", "value");
        Map<String,String> cookies = new HashMap<String, String>();
        cookies.put("key", "value");

        HttpClientResponse response = new SimpleHttpClientResponse(statusCode, reasonPhrase, headers, messageBody,
                cookies);

        assertSame(response.getStatusCode(), statusCode, "Response status code should match set status code");
        assertSame(response.getReasonPhrase(), reasonPhrase, "Response reason phrase should match set reason phrase");
        assertSame(response.getEntity(), messageBody, "Response entity should match set entity");
        assertSame(response.getHeaders(), headers, "Response headers should match set headers");
        assertSame(response.getCookies(), cookies, "Response cookies should match set cookies");
        assertTrue(response.hasHeaders(), "Response should have headers");
        assertTrue(response.hasCookies(), "Response should have cookies");
    }

}
