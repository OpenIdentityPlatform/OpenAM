package org.openidentityplatform.openam.mcp.server.model;

public record CoreAuthModule(String id, String name) {
    public CoreAuthModule(CoreAuthModuleDTO coreAuthModuleDTO) {
        this(coreAuthModuleDTO.id(), coreAuthModuleDTO.name());
    }
}
