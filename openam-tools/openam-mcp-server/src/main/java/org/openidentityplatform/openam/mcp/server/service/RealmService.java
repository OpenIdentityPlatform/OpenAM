package org.openidentityplatform.openam.mcp.server.service;

import org.openidentityplatform.openam.mcp.server.config.OpenAMConfig;
import org.openidentityplatform.openam.mcp.server.model.Realm;
import org.openidentityplatform.openam.mcp.server.model.RealmDTO;
import org.openidentityplatform.openam.mcp.server.model.SearchResponseDTO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Collectors;

public class RealmService extends OpenAMAbstractService {
    public RealmService(RestClient openAMRestClient, OpenAMConfig openAMConfig) {
        super(openAMRestClient, openAMConfig);
    }

    @Tool(name = "get_realm", description = "Returns OpenAM realm list")
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
