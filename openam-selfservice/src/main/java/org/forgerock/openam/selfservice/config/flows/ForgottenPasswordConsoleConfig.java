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

package org.forgerock.openam.selfservice.config.flows;

import org.forgerock.util.Reject;

import java.util.Map;
import java.util.Set;

/**
 * Represents forgotten password console configuration.
 *
 * @supported.api
 * @since 13.0.0
 */
public final class ForgottenPasswordConsoleConfig extends CommonConsoleConfig {

    private final String emailVerificationUrl;
    private final int minimumAnswersToVerify;

    private ForgottenPasswordConsoleConfig(ForgottenPasswordBuilder builder) {
        super(builder);
        emailVerificationUrl = builder.emailVerificationUrl;
        minimumAnswersToVerify = builder.minimumAnswersToVerify;
    }

    /**
     * Gets the verification Url to be sent with the email body.
     *
     * @return email verification Url
     */
    public String getEmailVerificationUrl() {
        return emailVerificationUrl;
    }

    /**
     * Get the minimum count of questions to verify.
     *
     * @return minimum count
     */
    public int getMinimumAnswersToVerify() {
        return minimumAnswersToVerify;
    }

    static final class ForgottenPasswordBuilder
            extends Builder<ForgottenPasswordConsoleConfig, ForgottenPasswordBuilder> {

        private String emailVerificationUrl;
        private int minimumAnswersToVerify;

        private ForgottenPasswordBuilder(Map<String, Set<String>> consoleAttributes) {
            super(consoleAttributes);
        }

        ForgottenPasswordBuilder setEmailVerificationUrl(String emailVerificationUrl) {
            this.emailVerificationUrl = emailVerificationUrl;
            return this;
        }

        ForgottenPasswordBuilder setMinimumAnswersToVerify(int minimumAnswersToVerify) {
            this.minimumAnswersToVerify = minimumAnswersToVerify;
            return this;
        }

        @Override
        ForgottenPasswordBuilder self() {
            return this;
        }

        @Override
        void verifyEmailConfig() {
            Reject.ifNull(emailVerificationUrl, "Email verification Url is required");
        }

        @Override
        void verifyKbaConfig() {
            Reject.ifFalse(minimumAnswersToVerify > 0, "Minimum questions to be verified must be greater than 0");
        }

        @Override
        ForgottenPasswordConsoleConfig internalBuild() {
            return new ForgottenPasswordConsoleConfig(this);
        }

    }

    static ForgottenPasswordBuilder newBuilder(Map<String, Set<String>> consoleAttributes) {
        return new ForgottenPasswordBuilder(consoleAttributes);
    }

}
