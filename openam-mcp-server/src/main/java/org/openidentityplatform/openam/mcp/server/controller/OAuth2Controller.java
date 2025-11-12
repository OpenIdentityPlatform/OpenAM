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
 * Copyright 2025 3A Systems LLC.
 */

package org.openidentityplatform.openam.mcp.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Set;

@RestController
public class OAuth2Controller {
    private final RestClient openAMRestClient;

    public OAuth2Controller(RestClient openAMRestClient) {
        this.openAMRestClient = openAMRestClient;
    }

    private static final Set<String> IGNORE_HEADERS = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailers", "transfer-encoding", "upgrade"
    );

    @GetMapping("/.well-known/**")
    public ResponseEntity<String> openAMWellKnown(HttpServletRequest request) {
        RestClient.RequestBodySpec requestSpec = openAMRestClient
                .method(HttpMethod.valueOf(request.getMethod()))
                .uri("/oauth2".concat(request.getRequestURI()))
                .headers(headers -> request.getHeaderNames().asIterator().forEachRemaining(name -> {
                    if (IGNORE_HEADERS.contains(name.toLowerCase())) {
                        return;
                    }
                    String value = request.getHeader(name);
                    headers.add(name, value);
                }));
        try {
            return requestSpec.retrieve()
                    .toEntity(String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAs(String.class));
        }

    }

}
