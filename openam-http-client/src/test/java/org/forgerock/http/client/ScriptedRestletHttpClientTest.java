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
import org.forgerock.http.client.request.HttpClientRequestFactory;
import org.forgerock.http.client.response.HttpClientResponse;
import org.testng.annotations.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.net.HttpURLConnection;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * This additional test is provided for two main reasons:
 *
 * 1) To ensure that the basic operation of the HTTP client when embedded in a script, works.
 * 2) To document by example the expected general case of use of the HTTP client.
 */
public class ScriptedRestletHttpClientTest {

    // Guice would not normally be used in a unit test, however this is to show the intended operation of the
    // library with scripting, and therefore I've kept it as close to the intended use as possible.
    final HttpClientRequestFactory httpClientRequestFactory = InjectorHolder.getInstance(HttpClientRequestFactory.class);
    final HttpClientFactory httpClientFactory = InjectorHolder.getInstance(HttpClientFactory.class);

    @Test
    public void shouldSendRequestAndReceiveResponse() {

        ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");

        HttpClientRequest request = httpClientRequestFactory.createRequest();
        // Set parameter in Java example:
        request.setMethod("GET");
        engine.put("request", request);

        HttpClient client = httpClientFactory.createHttpClient();
        engine.put("client", client);

        try {
            // Set parameter in Javascript example:
            engine.eval("request.setUri('http://www.example.com')");
            engine.eval("response = client.perform(request)");
            // Get parameter in Javascript examples:
            engine.eval("println(response.getStatusCode())");
            engine.eval("println(response.getReasonPhrase())");
            engine.eval("println(response.getEntity())");
        } catch (ScriptException e) {
            e.printStackTrace();
        }

        // Get parameter in Java examples:
        HttpClientResponse response = (HttpClientResponse)engine.get("response");
        assertEquals(response.getStatusCode(), (Integer) HttpURLConnection.HTTP_OK, "Response should have 200 status code");
        assertEquals(response.getReasonPhrase(), "OK", "Response should have 'OK' reason phrase");
        assertTrue(!(response.getEntity().isEmpty()), "Response should have an entity");

    }

}
