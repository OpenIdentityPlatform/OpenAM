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
 * Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.openam.sts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.sun.net.httpserver.HttpServer;

import org.testng.annotations.Test;

public class HttpURLConnectionWrapperFactoryTest {

    @Test
    public void keepsSetCookieHeadersInWireOrder() throws Exception {
        String[][] headers = {
                {null, "HTTP/1.1 200 OK"},
                {"Set-Cookie", "iPlanetDirectoryPro=AQIC5wM2LY4Sfczn-old-token; Path=/"},
                {"Content-Type", "application/json"},
                {"set-cookie", "iPlanetDirectoryPro=AQIC5wM2LY4Sfczn-new-token; Path=/; HttpOnly"},
                {"Set-Cookie", "amlbcookie=01; Path=/"},
        };
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(connection.getInputStream()).thenReturn(
                new ByteArrayInputStream("{\"successUrl\":\"/openam/console\"}".getBytes(StandardCharsets.UTF_8)));
        for (int i = 0; i < headers.length; i++) {
            when(connection.getHeaderFieldKey(i)).thenReturn(headers[i][0]);
            when(connection.getHeaderField(i)).thenReturn(headers[i][1]);
        }
        HttpURLConnectionFactory connectionFactory = mock(HttpURLConnectionFactory.class);
        when(connectionFactory.getHttpURLConnection(any(URL.class))).thenReturn(connection);

        HttpURLConnectionWrapper.ConnectionResult result = new HttpURLConnectionWrapperFactory(connectionFactory)
                .httpURLConnectionWrapper(new URL("http://openam.example.org/openam/json/authenticate"))
                .withoutAuditTransactionIdHeader()
                .makeInvocation();

        assertEquals(result.getResult().trim(), "{\"successUrl\":\"/openam/console\"}");
        assertEquals(result.getSetCookieHeaders(), Arrays.asList(
                "iPlanetDirectoryPro=AQIC5wM2LY4Sfczn-old-token; Path=/",
                "iPlanetDirectoryPro=AQIC5wM2LY4Sfczn-new-token; Path=/; HttpOnly",
                "amlbcookie=01; Path=/"));
    }

    /*
    Pins the two HttpURLConnection facts the index-based reader relies on against a real connection: repeated
    Set-Cookie headers come back in wire order when read by index (getHeaderFields() reverses them on JDK 11 and
    17, and keys them by exact wire spelling), and an HttpOnly cookie's value comes back empty once a JVM-wide
    CookieHandler is installed, so this test also fails if a fixture ever installs one.
     */
    @Test
    public void seesHttpOnlySetCookieOnARealConnectionInWireOrder() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/openam/json/authenticate", exchange -> {
            exchange.getResponseHeaders().add("Set-Cookie", "iPlanetDirectoryPro=AQIC5wM2LY4Sfczn-old-token; Path=/");
            exchange.getResponseHeaders().add("Set-Cookie",
                    "iPlanetDirectoryPro=AQIC5wM2LY4Sfczn-new-token; Path=/; HttpOnly");
            byte[] body = "{\"successUrl\":\"/openam/console\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            HttpURLConnectionWrapper.ConnectionResult result =
                    new HttpURLConnectionWrapperFactory(new DefaultHttpURLConnectionFactory())
                            .httpURLConnectionWrapper(new URL("http://127.0.0.1:" + server.getAddress().getPort()
                                    + "/openam/json/authenticate"))
                            .withoutAuditTransactionIdHeader()
                            .makeInvocation();

            assertEquals(result.getResult().trim(), "{\"successUrl\":\"/openam/console\"}");
            assertEquals(result.getSetCookieHeaders(), Arrays.asList(
                    "iPlanetDirectoryPro=AQIC5wM2LY4Sfczn-old-token; Path=/",
                    "iPlanetDirectoryPro=AQIC5wM2LY4Sfczn-new-token; Path=/; HttpOnly"));
        } finally {
            server.stop(0);
        }
    }
}
