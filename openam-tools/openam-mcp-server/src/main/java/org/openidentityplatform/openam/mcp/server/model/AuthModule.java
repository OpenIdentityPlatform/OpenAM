package org.openidentityplatform.openam.mcp.server.model;

import java.util.Map;

public record AuthModule(String name, Map<String, Object> settings){}
