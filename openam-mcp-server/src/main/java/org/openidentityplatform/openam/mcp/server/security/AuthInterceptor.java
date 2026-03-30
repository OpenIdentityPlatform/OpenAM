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
 * Copyright 2025-2026 3A Systems LLC.
 */

package org.openidentityplatform.openam.mcp.server.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.openidentityplatform.openam.mcp.server.config.OpenAMConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String LOGIN_PASSWORD_TOKEN_KEY = "login-password-token";

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    private final RestClient openAMRestClient;

    private final OpenAMConfig openAMConfig;


    private final Cache<String, String> tokenCache;

    @Autowired
    public AuthInterceptor(RestClient openAMRestClient, OpenAMConfig openAMConfig) {
        this(openAMRestClient, openAMConfig, Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES).build());
    }

    AuthInterceptor(RestClient openAMRestClient, OpenAMConfig openAMConfig, Cache<String, String> tokenCache) {
        this.openAMRestClient = openAMRestClient;
        this.openAMConfig = openAMConfig;
        this.tokenCache = tokenCache;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(openAMConfig.useOAuthForAuthentication()) {
            return preHandleOAuth(request, response);
        } else {
            return preHandleUsernamePassword(request);
        }
    }

    long tokenValidSeconds(String tokenId) {
        String sessionUri = "/json/sessions/?_action=getSessionInfo";
        try {
            Map<String, String> tokenProps = openAMRestClient.post()
                    .uri(sessionUri)
                    .header(openAMConfig.tokenHeader(), tokenId)
                    .retrieve().body(new ParameterizedTypeReference<>() {
                    });
            String expTime = tokenProps.get("maxIdleExpirationTime");
            ZonedDateTime dateTime = ZonedDateTime.parse(expTime);
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            Duration duration = Duration.between(now, dateTime);
            return duration.getSeconds();
        } catch (Exception e) {
            log.warn("error getting token properties: ", e);
            return -1;
        }
    }

    String getUserNamePasswordToken() {
        Map<String, String> tokenResponse = openAMRestClient.post().uri("/json/authenticate")
                .header("X-OpenAM-Username", openAMConfig.username())
                .header("X-OpenAM-Password", openAMConfig.password())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return tokenResponse.get("tokenId");
    }

    private String getTokenIdFromAccessToken(String accessToken) {
        Map<String, String> tokenResponse = openAMRestClient.post()
                .uri("/json/authenticate?authIndexType=service&authIndexValue=".concat(openAMConfig.oidcAuthChain()))
                .header(openAMConfig.oidcAuthHeader(), accessToken)
                .body("{}")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return tokenResponse.get("tokenId");
    }

    boolean preHandleUsernamePassword(HttpServletRequest request) {
        final int maxAttempts = 2;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String token = tokenCache.get(LOGIN_PASSWORD_TOKEN_KEY, (k) -> getUserNamePasswordToken());

            long seconds = tokenValidSeconds(token);
            if (seconds > 1) {
                request.setAttribute("tokenId", token);
                return true;
            }
            log.info("preHandleUsernamePassword: token {} is about to expire in {} s (attempt {}/{})",
                    token, seconds, attempt + 1, maxAttempts);
            tokenCache.invalidate(LOGIN_PASSWORD_TOKEN_KEY);
            token = getUserNamePasswordToken();
            tokenCache.put(LOGIN_PASSWORD_TOKEN_KEY, token);

        }
        log.error("preHandleUsernamePassword: unable to obtain a valid token after {} attempts; " +
                "the session endpoint may be unavailable or the token TTL cannot be determined.", maxAttempts);
        throw new IllegalStateException(
                "Failed to obtain a valid OpenAM session token after " + maxAttempts + " attempts. " +
                        "Check connectivity to the OpenAM session endpoint.");
    }

    boolean preHandleOAuth(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setHeader("WWW-Authenticate", "Bearer realm=\"OpenAM\"");
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return false;
        }
        String accessToken = authHeader.substring(7);

        if (!accessTokenValid(accessToken)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setHeader("WWW-Authenticate", "Bearer realm=\"OpenAM\"");
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return false;
        }
        final String token;
        try {
            token = tokenCache.get(accessToken, this::getTokenIdFromAccessToken);
        } catch (Exception e) {
            log.warn("error getting token:", e);
            throw e;
        }
        long seconds = tokenValidSeconds(token);
        if(seconds > 1) {
            request.setAttribute("tokenId", token);
            return true;
        }
        log.info("preHandleOAuth: token {} is about to expire in {} s", token, seconds);
        tokenCache.invalidate(accessToken);
        return preHandleOAuth(request, response);
    }

    boolean accessTokenValid(String accessToken) {

        //validate access token
        try {
            Map<String, String> response = openAMRestClient.get()
                    .uri("/oauth2/userinfo")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if(response.containsKey("name")) {
                return true;
            } else {
                log.warn("got invalid response: {} for access token: {}", response, accessToken);
                return false;
            }
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}
