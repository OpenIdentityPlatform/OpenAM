package org.openidentityplatform.openam.mcp.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openidentityplatform.openam.mcp.server.model.Realm;
import org.openidentityplatform.openam.mcp.server.model.RealmDTO;
import org.openidentityplatform.openam.mcp.server.model.SearchResponseDTO;
import org.springframework.core.ParameterizedTypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class RealmServiceTest extends OpenAMServiceTest {

    RealmService realmService = null;

    @Override
    @BeforeEach
    public void setupMocks() {
        super.setupMocks();
        realmService = new RealmService(restClient, openAMConfig);
    }

    @Test
    void getRealms() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("realms/realms-response.json");
        SearchResponseDTO<RealmDTO> realmsResponse = objectMapper.readValue(is, new TypeReference<>() {});

        when(responseSpec.body(eq(new ParameterizedTypeReference<SearchResponseDTO<RealmDTO>>() {}))).thenReturn(realmsResponse);

        List<Realm> realmList = realmService.getRealms();
        assertEquals(realmList.size(), 2);
    }
}