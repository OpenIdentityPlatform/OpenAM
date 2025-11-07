package org.openidentityplatform.openam.mcp.server.model;

import java.util.Map;

public record SchemaDTO(String type, Map<String, PropertySchemaDTO> properties) {
}
