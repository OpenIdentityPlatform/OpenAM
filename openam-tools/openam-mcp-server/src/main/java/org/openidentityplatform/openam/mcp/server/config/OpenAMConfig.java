package org.openidentityplatform.openam.mcp.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("openam")
public record OpenAMConfig(
        String url,

        boolean useOAuthForAuthentication,

        String oidcAuthChain,
        String oidcAuthHeader,

        String tokenHeader,
        String username,
        String password
) {}
