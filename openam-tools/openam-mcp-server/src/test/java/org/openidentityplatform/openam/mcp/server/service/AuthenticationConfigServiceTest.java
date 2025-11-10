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

package org.openidentityplatform.openam.mcp.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openidentityplatform.openam.mcp.server.model.AuthChain;
import org.openidentityplatform.openam.mcp.server.model.AuthChainDTO;
import org.openidentityplatform.openam.mcp.server.model.AuthModule;
import org.openidentityplatform.openam.mcp.server.model.AuthModuleDTO;
import org.openidentityplatform.openam.mcp.server.model.AuthModuleSchemaDTO;
import org.openidentityplatform.openam.mcp.server.model.SearchResponseDTO;
import org.springframework.core.ParameterizedTypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class AuthenticationConfigServiceTest extends OpenAMServiceTest {

    AuthenticationConfigService authenticationConfigService = null;

    @Override
    @BeforeEach
    public void setupMocks() {
        super.setupMocks();
        authenticationConfigService = spy(new AuthenticationConfigService(restClient, openAMConfig));
    }

    @Test
    void getOpenAMAuthChains() throws IOException {
        InputStream chainsResponseStream = getClass().getClassLoader().getResourceAsStream("auth/chains-response.json");
        SearchResponseDTO<AuthChainDTO> chainsResponse = objectMapper.readValue(chainsResponseStream, new TypeReference<>() {});
        doReturn(chainsResponse.result()).when(authenticationConfigService).getRealmAuthChains(anyString(), anyString());

        InputStream modulesResponseStream = getClass().getClassLoader().getResourceAsStream("auth/modules-response.json");
        SearchResponseDTO<AuthModuleDTO> modulesResponse = objectMapper.readValue(modulesResponseStream, new TypeReference<>() {});
        doReturn(modulesResponse.result()).when(authenticationConfigService).getRealmAuthModules(anyString(), anyString());
        when(responseSpec.body(eq(new ParameterizedTypeReference<SearchResponseDTO<AuthChainDTO>>() {}))).thenReturn(chainsResponse);
        List<AuthChain> authChains = authenticationConfigService.getOpenAMAuthChains(null);

        assertTrue(authChains.size() > 0);

    }

    @Test
    void getAuthModules() throws IOException {
        InputStream modulesResponseStream = getClass().getClassLoader().getResourceAsStream("auth/modules-response.json");
        SearchResponseDTO<AuthModuleDTO> modulesResponse = objectMapper.readValue(modulesResponseStream, new TypeReference<>() {});
        doReturn(modulesResponse.result()).when(authenticationConfigService).getRealmAuthModules(anyString(), anyString());

        InputStream moduleSchemaStream = getClass().getClassLoader().getResourceAsStream("auth/module-schema-response.json");
        AuthModuleSchemaDTO authModuleSchemaDto = objectMapper.readValue(moduleSchemaStream, new TypeReference<>() {});

        doReturn(authModuleSchemaDto.properties()).when(authenticationConfigService).getModuleSchema(anyString(), anyString(), anyString());

        InputStream moduleSettingsStream = getClass().getClassLoader().getResourceAsStream("auth/module-settings-response.json");
        Map<String, Object> moduleSettings = objectMapper.readValue(moduleSettingsStream, new TypeReference<>() {});

        doReturn(moduleSettings).when(authenticationConfigService).getModuleSettings(anyString(), anyString(), anyString(), anyString());

        List<AuthModule> authModules = authenticationConfigService.getAuthModules(null);
        assertTrue(authModules.size() > 0);

    }
}