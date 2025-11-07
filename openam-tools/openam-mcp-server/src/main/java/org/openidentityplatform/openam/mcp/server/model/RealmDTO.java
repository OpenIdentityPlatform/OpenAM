package org.openidentityplatform.openam.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RealmDTO(@JsonProperty("_id") String id,
                       @JsonProperty("parentPath") String parentPath,
                       @JsonProperty("active") boolean active,
                       @JsonProperty("name") String name,
                       @JsonProperty("aliases") List<String> aliases) {

}