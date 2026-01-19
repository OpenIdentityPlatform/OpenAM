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
import org.openidentityplatform.openam.mcp.server.model.Realm;
import org.openidentityplatform.openam.mcp.server.model.RealmDTO;
import org.openidentityplatform.openam.mcp.server.model.SearchResponseDTO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RealmService extends OpenAMAbstractService {
    public RealmService(RestClient openAMRestClient, OpenAMConfig openAMConfig) {
        super(openAMRestClient, openAMConfig);
    }

    @Tool(name = "get_realms", description = "Returns OpenAM realm list")
    public List<Realm> getRealms() {
        String uri = "/json/global-config/realms?_queryFilter=true";
        String tokenId = getTokenId();
        SearchResponseDTO<RealmDTO> realmsResponse  = openAMRestClient.get().uri(uri)
                .header(openAMConfig.tokenHeader(), tokenId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return realmsResponse.result().stream().map(Realm::new).collect(Collectors.toList());
    }
}
