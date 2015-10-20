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

import java.util.Map;

/**
 * Represents forgotten password console configuration.
 *
 * @since 13.0.0
 */
public final class UserRegistrationConsoleConfig implements CommonConsoleConfig {

    private final CommonConsoleConfig commonConfig;
    private final int minAnswersToProvide;

    UserRegistrationConsoleConfig(Builder builder) {
        commonConfig = builder.commonConfig;
        minAnswersToProvide = builder.minAnswersToProvide;
    }

    @Override
    public String getConfigProviderClass() {
        return commonConfig.getConfigProviderClass();
    }

    @Override
    public boolean isEnabled() {
        return commonConfig.isEnabled();
    }

    @Override
    public String getEmailUrl() {
        return commonConfig.getEmailUrl();
    }

    @Override
    public long getTokenExpiry() {
        return commonConfig.getTokenExpiry();
    }

    @Override
    public boolean isKbaEnabled() {
        return commonConfig.isKbaEnabled();
    }

    @Override
    public Map<String, Map<String, String>> getSecurityQuestions() {
        return commonConfig.getSecurityQuestions();
    }

    /**
     * Get the minimum count of answers to provide.
     *
     * @return minimum count
     */
    public int getMinAnswersToProvide() {
        return minAnswersToProvide;
    }

    static final class Builder {

        private final CommonConsoleConfig commonConfig;
        private int minAnswersToProvide;

        Builder(CommonConsoleConfig commonConfig) {
            this.commonConfig = commonConfig;
        }

        Builder setMinAnswersToProvide(int minAnswersToProvide) {
            this.minAnswersToProvide = minAnswersToProvide;
            return this;
        }

        UserRegistrationConsoleConfig build() {
            Reject.ifFalse(minAnswersToProvide > 0);
            return new UserRegistrationConsoleConfig(this);
        }

    }

    static Builder newBuilder(CommonConsoleConfig commonConfig) {
        return new Builder(commonConfig);
    }

}
