package org.openidentityplatform.openam.mcp.server.service;

import org.openidentityplatform.openam.mcp.server.config.OpenAMConfig;
import org.openidentityplatform.openam.mcp.server.model.AuthChain;
import org.openidentityplatform.openam.mcp.server.model.AuthChainDTO;
import org.openidentityplatform.openam.mcp.server.model.CoreAuthModule;
import org.openidentityplatform.openam.mcp.server.model.CoreAuthModuleDTO;
import org.openidentityplatform.openam.mcp.server.model.ModuleDTO;
import org.openidentityplatform.openam.mcp.server.model.PropertySchemaDTO;
import org.openidentityplatform.openam.mcp.server.model.SchemaDTO;
import org.openidentityplatform.openam.mcp.server.model.SearchResponseDTO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthenticationConfigService extends OpenAMAbstractService {
    public AuthenticationConfigService(RestClient openAMRestClient, OpenAMConfig openAMConfig) {
        super(openAMRestClient, openAMConfig);
    }

    @Tool(name = "get_auth_modules", description = "Returns OpenAM authentication modules list")
    public void getAuthModules(@ToolParam(required = false, description = "If not set, uses root realm") String realm) {
        realm = getRealmOrDefault(realm);
        String tokenId = getTokenId();

        List<ModuleDTO> realmAuthModules = getRealmAuthModules(realm, tokenId);
        for(ModuleDTO moduleDTO : realmAuthModules) {
            Map<String, PropertySchemaDTO> schema = getModuleSchema(tokenId, realm, moduleDTO.type());
            Map<String, Object> settings = getModuleSettings(tokenId, realm, moduleDTO.id(), moduleDTO.type());
        }

    }

    @Tool(name = "get_auth_chains", description = "Returns OpenAM authentication chains with modules")
    public List<AuthChain> getOpenAMAuthChains(@ToolParam(required = false, description = "If not set, uses root realm") String realm) {
        realm = getRealmOrDefault(realm);
        String tokenId = getTokenId();

        final List<AuthChainDTO> chainsDTOList = getRealmAuthChains(realm, tokenId);
        final List<ModuleDTO> moduleDTOList = getRealmAuthModules(realm, tokenId);

        List<AuthChain> chains = chainsDTOList.stream().map(chainDTO -> {
            List<String> chainModules = chainDTO.modules().stream().map(AuthChainDTO.AuthChainModuleDTO::module).toList();
            List<String> moduleNames = chainModules.stream()
                    .map(cm -> moduleDTOList.stream()
                            .filter(rm -> cm.equals(rm.id())).findFirst().get().typeDescription()).toList();
            return new AuthChain(chainDTO.id(), moduleNames);
        }).collect(Collectors.toList());

        return chains;
    }

    @Tool(name = "get_available_module_list", description = "Returns all avialable authenticaion modules")
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

    List<ModuleDTO> getRealmAuthModules(String realm, String tokenId) {
        final String modulesUri = String.format("/json/realms/%s/realm-config/authentication/modules?_queryFilter=true", realm);
        SearchResponseDTO<ModuleDTO> modulesResponse = openAMRestClient.get().uri(modulesUri)
                .header(openAMConfig.tokenHeader(), tokenId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return modulesResponse.result();
    }

    Map<String, PropertySchemaDTO> getModuleSchema(String tokenId, String realm, String moduleType) {

        String schemaUrl = String.format( "/json/realms/%s/realm-config/authentication/modules/%s?_action=schema", realm, moduleType);
        SchemaDTO schemaDTO = openAMRestClient.post().uri(schemaUrl)
                .header(openAMConfig.tokenHeader(), tokenId)
                .retrieve()
                .body(SchemaDTO.class);

        return schemaDTO.properties();
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


