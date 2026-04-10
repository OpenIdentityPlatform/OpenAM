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

package org.openidentityplatform.openam.mcp.server;

import org.openidentityplatform.openam.mcp.server.config.OpenAMConfig;
import org.openidentityplatform.openam.mcp.server.service.AuthenticationConfigService;
import org.openidentityplatform.openam.mcp.server.service.RealmService;
import org.openidentityplatform.openam.mcp.server.service.UserService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OpenAmMcpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpenAmMcpServerApplication.class, args);
    }

    @Bean
    public RestClient getOpenAMRestClient(OpenAMConfig openAMConfig) {
        return RestClient.builder().baseUrl(openAMConfig.url())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json").build();
    }

    @Bean
    public ToolCallbackProvider getTools(UserService userService,
                                         RealmService realmService,
                                         AuthenticationConfigService configService) {
        return MethodToolCallbackProvider.builder().toolObjects(userService, realmService, configService).build();
    }
}


