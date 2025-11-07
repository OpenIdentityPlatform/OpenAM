package org.openidentityplatform.openam.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AuthChainDTO(@JsonProperty("_id") String id,
                           @JsonProperty("authChainConfiguration") List<AuthChainModuleDTO> modules) {

    public record AuthChainModuleDTO(String module) {}
}
