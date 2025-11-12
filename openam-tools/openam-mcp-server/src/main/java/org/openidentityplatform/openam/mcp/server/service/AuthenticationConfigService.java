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

import org.openidentityplatform.openam.mcp.server.config.OpenAMConfig;
import org.openidentityplatform.openam.mcp.server.model.AuthChain;
import org.openidentityplatform.openam.mcp.server.model.AuthChainDTO;
import org.openidentityplatform.openam.mcp.server.model.AuthModule;
import org.openidentityplatform.openam.mcp.server.model.CoreAuthModule;
import org.openidentityplatform.openam.mcp.server.model.CoreAuthModuleDTO;
import org.openidentityplatform.openam.mcp.server.model.AuthModuleDTO;
import org.openidentityplatform.openam.mcp.server.model.PropertySchemaDTO;
import org.openidentityplatform.openam.mcp.server.model.AuthModuleSchemaDTO;
import org.openidentityplatform.openam.mcp.server.model.SearchResponseDTO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthenticationConfigService extends OpenAMAbstractService {
    public AuthenticationConfigService(RestClient openAMRestClient, OpenAMConfig openAMConfig) {
        super(openAMRestClient, openAMConfig);
    }

    @Tool(name = "get_auth_modules", description = "Returns OpenAM authentication modules list")
    public List<AuthModule> getAuthModules(@ToolParam(required = false, description = "If not set, uses root realm") String realm) {
        realm = getRealmOrDefault(realm);
        String tokenId = getTokenId();

        List<AuthModuleDTO> realmAuthModules = getRealmAuthModules(realm, tokenId);
        List<AuthModule> authModules = new ArrayList<>();
        for(AuthModuleDTO authModuleDTO : realmAuthModules) {
            Map<String, PropertySchemaDTO> schema = getModuleSchema(tokenId, realm, authModuleDTO.type());
            Map<String, Object> settings = getModuleSettings(tokenId, realm, authModuleDTO.id(), authModuleDTO.type());

            var moduleSettings = new HashMap<String, Object>();
            for (var prop : schema.entrySet()) {
                var setting = settings.get(prop.getKey());
                var settingName = prop.getValue().title();
                moduleSettings.put(settingName, setting);
            }
            var authModuleId = authModuleDTO.typeDescription();
            authModules.add(new AuthModule(authModuleId, moduleSettings));
        }
        return authModules;
    }

    @Tool(name = "get_auth_chains", description = "Returns OpenAM authentication chains with modules")
    public List<AuthChain> getOpenAMAuthChains(@ToolParam(required = false, description = "If not set, uses root realm") String realm) {
        realm = getRealmOrDefault(realm);
        String tokenId = getTokenId();

        final List<AuthChainDTO> chainsDTOList = getRealmAuthChains(realm, tokenId);
        final List<AuthModuleDTO> authModuleDTOList = getRealmAuthModules(realm, tokenId);

        return chainsDTOList.stream().map(chainDTO -> {
            List<String> chainModules = chainDTO.modules().stream().map(AuthChainDTO.AuthChainModuleDTO::module).toList();
            List<String> moduleNames = chainModules.stream()
                    .map(cm -> authModuleDTOList.stream()
                            .filter(rm -> cm.equals(rm.id())).findFirst().get().typeDescription()).toList();
            return new AuthChain(chainDTO.id(), moduleNames);
        }).collect(Collectors.toList());
    }

    @Tool(name = "get_available_modules", description = "Returns all available authentication modules")
    public List<CoreAuthModule> getAvailableModuleList() {
        String tokenId = getTokenId();
        String coreModulesUri = "/json/global-config/authentication/modules?_action=getAllTypes";
        SearchResponseDTO<CoreAuthModuleDTO> coreAuthModulesResponse = openAMRestClient.post().uri(coreModulesUri)
                .header(openAMConfig.tokenHeader(), tokenId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return coreAuthModulesResponse.result().stream().map(CoreAuthModule::new).collect(Collectors.toList());
    }

    List<AuthChainDTO> getRealmAuthChains(String realm, String tokenId) {
        final String chainsUri = String.format("/json/realms/%s/realm-config/authentication/chains?_queryFilter=true", realm);
        SearchResponseDTO<AuthChainDTO> authChainsResponse = openAMRestClient.get().uri(chainsUri)
                .header(openAMConfig.tokenHeader(), tokenId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return authChainsResponse.result();
    }

    List<AuthModuleDTO> getRealmAuthModules(String realm, String tokenId) {
        final String modulesUri = String.format("/json/realms/%s/realm-config/authentication/modules?_queryFilter=true", realm);
        SearchResponseDTO<AuthModuleDTO> modulesResponse = openAMRestClient.get().uri(modulesUri)
                .header(openAMConfig.tokenHeader(), tokenId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return modulesResponse.result();
    }

    Map<String, PropertySchemaDTO> getModuleSchema(String tokenId, String realm, String moduleType) {

        String schemaUrl = String.format( "/json/realms/%s/realm-config/authentication/modules/%s?_action=schema", realm, moduleType);
        AuthModuleSchemaDTO authModuleSchemaDTO = openAMRestClient.post().uri(schemaUrl)
                .header(openAMConfig.tokenHeader(), tokenId)
                .retrieve()
                .body(AuthModuleSchemaDTO.class);

        return authModuleSchemaDTO.properties();
    }

    Map<String, Object> getModuleSettings(String tokenId, String realm, String moduleId, String moduleType) {
        String settingsUrl = String.format("/json/realms/%s/realm-config/authentication/modules/%s/%s", realm, moduleType, moduleId);

        return openAMRestClient.get().uri(settingsUrl)
                .header(openAMConfig.tokenHeader(), tokenId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}


