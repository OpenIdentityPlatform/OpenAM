package org.openidentityplatform.openam.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ModuleDTO(@JsonProperty("_id") String id, String typeDescription, String type) {}
