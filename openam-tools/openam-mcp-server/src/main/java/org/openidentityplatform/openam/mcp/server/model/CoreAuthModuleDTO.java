package org.openidentityplatform.openam.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CoreAuthModuleDTO(@JsonProperty("_id") String id, String name) {}
