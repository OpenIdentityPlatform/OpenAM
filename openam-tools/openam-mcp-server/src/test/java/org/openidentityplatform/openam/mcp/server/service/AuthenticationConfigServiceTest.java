package org.openidentityplatform.openam.mcp.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openidentityplatform.openam.mcp.server.model.AuthChainDTO;
import org.openidentityplatform.openam.mcp.server.model.ModuleDTO;
import org.openidentityplatform.openam.mcp.server.model.SearchResponseDTO;
import org.springframework.core.ParameterizedTypeReference;

import java.io.IOException;
import java.io.InputStream;

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
        SearchResponseDTO<ModuleDTO> modulesResponse = objectMapper.readValue(modulesResponseStream, new TypeReference<>() {});
        doReturn(modulesResponse.result()).when(authenticationConfigService).getRealmAuthModules(anyString(), anyString());
        when(responseSpec.body(eq(new ParameterizedTypeReference<SearchResponseDTO<AuthChainDTO>>() {}))).thenReturn(chainsResponse);
        authenticationConfigService.getOpenAMAuthChains(null);
        //assertEquals(realmList.size(), 2);
    }
}