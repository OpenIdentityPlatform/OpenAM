package org.openidentityplatform.openam.mcp.server.model;

import java.util.List;

public record AuthChain(String id, List<String> modules) {
}
