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