package org.openidentityplatform.openam.mcp.server.model;

import java.util.Map;

public record AuthModuleSchemaDTO(String type, Map<String, PropertySchemaDTO> properties) {
}
