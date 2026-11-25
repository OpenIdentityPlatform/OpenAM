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
package com.sun.identity.install.tools.configurator;

import static org.testng.Assert.assertEquals;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CreateProfileTaskTest {
    private static final String COOKIE_NAME = "iPlanetDirectoryPro";
    private static final String COOKIE_ONLY_BODY = "{\"successUrl\":\"/openam/console\",\"realm\":\"/\"}";
    private static final String BODY_WITH_TOKEN = "{\"tokenId\":\"body-token\",\"successUrl\":\"/openam/console\"}";

    @DataProvider
    public Object[][] authenticationResponses() {
        return new Object[][] {
            {"legacy body", 200, BODY_WITH_TOKEN, COOKIE_NAME, new String[0], "body-token"},
            {"HttpOnly cookie", 200, COOKIE_ONLY_BODY, COOKIE_NAME,
                new String[] {"iPlanetDirectoryPro=cookie-token; Path=/; HttpOnly"}, "cookie-token"},
            {"renamed cookie", 200, COOKIE_ONLY_BODY, "CustomSession",
                new String[] {"iPlanetDirectoryPro=wrong-token; Path=/; HttpOnly",
                    "CustomSession=custom-token; Path=/; HttpOnly"}, "custom-token"},
            {"body takes precedence", 200, BODY_WITH_TOKEN, COOKIE_NAME,
                new String[] {"iPlanetDirectoryPro=cookie-token; Path=/; HttpOnly"}, "body-token"},
            {"last non-empty cookie", 200, COOKIE_ONLY_BODY, COOKIE_NAME,
                new String[] {"iPlanetDirectoryPro=old-token; Path=/; HttpOnly",
                    "iPlanetDirectoryPro=new-token; Path=/; HttpOnly",
                    "iPlanetDirectoryPro=; Expires=Thu, 01-Jan-1970 00:00:10 GMT; Path=/",
                    "iPlanetDirectoryProExtra=wrong-token; Path=/", "amlbcookie=01; Path=/"}, "new-token"},
            {"URL-encoded cookie", 200, COOKIE_ONLY_BODY, COOKIE_NAME,
                new String[] {"iPlanetDirectoryPro=AQIC%2B%3D%40AAJTSQACMDE%23; Path=/; HttpOnly"},
                "AQIC+=@AAJTSQACMDE#"},
            {"unencoded cookie", 200, COOKIE_ONLY_BODY, COOKIE_NAME,
                new String[] {"iPlanetDirectoryPro=AQIC+=@AAJTSQACMDE#; Path=/; HttpOnly"},
                "AQIC+=@AAJTSQACMDE#"},
            {"empty body token", 200, "{\"tokenId\":\"\",\"successUrl\":\"/openam/console\"}", COOKIE_NAME,
                new String[] {"iPlanetDirectoryPro=cookie-token; Path=/; HttpOnly"}, "cookie-token"},
            {"no session", 200, COOKIE_ONLY_BODY, COOKIE_NAME,
                new String[] {"iPlanetDirectoryPro=; Path=/", "amlbcookie=01; Path=/"}, null},
            {"malformed cookie encoding", 200, COOKIE_ONLY_BODY, COOKIE_NAME,
                new String[] {"iPlanetDirectoryPro=invalid%zz; Path=/; HttpOnly"}, null},
            {"failed authentication", 401, "{\"code\":401,\"message\":\"Unauthorized\"}", COOKIE_NAME,
                new String[] {"iPlanetDirectoryPro=cookie-token; Path=/; HttpOnly"}, null},
        };
    }

    @Test(dataProvider = "authenticationResponses")
    public void createsProfileWithTheAuthenticatedSession(String scenario, int status, String authBody,
            String cookieName, String[] cookies, String expectedToken) throws Exception {
        AtomicInteger profileRequests = new AtomicInteger();
        AtomicReference<String> profileToken = new AtomicReference<>();
        AtomicReference<String> profileMethod = new AtomicReference<>();
        AtomicReference<String> profileBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        Path passwordFile = Files.createTempFile("agent-profile-test-", ".txt");
        try {
            Files.write(passwordFile, "test-password".getBytes(StandardCharsets.UTF_8));
            server.createContext("/openam/json/serverinfo/", exchange ->
                    respond(exchange, 200, "{\"cookieName\":\"" + cookieName + "\"}"));
            server.createContext("/openam/json/authenticate", exchange -> {
                for (String cookie : cookies) {
                    exchange.getResponseHeaders().add("Set-Cookie", cookie);
                }
                respond(exchange, status, authBody);
            });
            server.createContext("/openam/json/agents", exchange -> {
                profileRequests.incrementAndGet();
                profileToken.set(exchange.getRequestHeaders().getFirst(cookieName));
                profileMethod.set(exchange.getRequestMethod());
                profileBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                respond(exchange, 201, "{}");
            });
            server.start();

            IStateAccess state = new TransientStateAccess();
            state.put(InstallConstants.STR_CREATE_AGENT_PROFILE_NAME, "true");
            state.put(CreateProfileTask.STR_AGENT_PROFILE_NAME, "test-agent");
            state.put(CreateProfileTask.STR_AGENT_PASSWORD_FILE, passwordFile.toString());
            state.put(CreateProfileTask.STR_AGENT_ADMINISTRATOR_NAME, "agentadmin");
            state.put(CreateProfileTask.STR_AGENT_ADMINISTRATOR_PASSWORD_FILE, passwordFile.toString());
            state.put(CreateProfileTask.STR_AM_SERVER_URL, "http://127.0.0.1:" + server.getAddress().getPort() + "/openam");
            state.put(CreateProfileTask.STR_AGENT_URL, "http://agent.example.org");

            boolean result = new CreateProfileTask().execute("createAgent", state,
                    Collections.singletonMap(CreateProfileTask.STR_AGENT_PROFILE_TYPE, "J2EEAgent"));

            assertEquals(result, expectedToken != null, scenario);
            assertEquals(profileRequests.get(), expectedToken == null ? 0 : 1, scenario);
            assertEquals(profileToken.get(), expectedToken, scenario);
            if (expectedToken != null) {
                assertEquals(profileMethod.get(), "POST", scenario);
                assertEquals(new org.json.JSONObject(profileBody.get()).getString("username"), "test-agent", scenario);
            }
        } finally {
            server.stop(0);
            Files.deleteIfExists(passwordFile);
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        } finally {
            exchange.close();
        }
    }
}
