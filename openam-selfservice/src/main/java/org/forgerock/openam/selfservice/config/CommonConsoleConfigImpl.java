/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.selfservice.config;

import org.forgerock.util.Reject;

/**
 * Represents common console configuration used by all self services.
 *
 * @since 13.0.0
 */
final class CommonConsoleConfigImpl implements CommonConsoleConfig {

    private final String configProviderClass;
    private final boolean enabled;
    private final String emailUrl;
    private final long tokenExpiry;

    private CommonConsoleConfigImpl(Builder builder) {
        configProviderClass = builder.configProviderClass;
        enabled = builder.enabled;
        emailUrl = builder.emailUrl;
        tokenExpiry = builder.tokenExpiry;
    }

    @Override
    public String getConfigProviderClass() {
        return configProviderClass;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getEmailUrl() {
        return emailUrl;
    }

    @Override
    public long getTokenExpiry() {
        return tokenExpiry;
    }

    final static class Builder {

        private String configProviderClass;
        private boolean enabled;
        private String emailUrl;
        private long tokenExpiry;

        Builder setConfigProviderClass(String configProviderClass) {
            this.configProviderClass = configProviderClass;
            return this;
        }

        Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        Builder setEmailUrl(String emailUrl) {
            this.emailUrl = emailUrl;
            return this;
        }

        Builder setTokenExpiry(long tokenExpiry) {
            this.tokenExpiry = tokenExpiry;
            return this;
        }

        CommonConsoleConfig build() {
            Reject.ifNull(configProviderClass);
            Reject.ifNull(emailUrl);
            Reject.ifFalse(tokenExpiry > 0);
            return new CommonConsoleConfigImpl(this);
        }

    }

    static Builder newBuilder() {
        return new Builder();
    }

}
