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
 * Represents forgotten username console configuration.
 *
 * @supported.api
 * @since 13.0.0
 */
public final class ForgottenUsernameConsoleConfig extends CommonConsoleConfig {

    private final int minimumAnswersToVerify;
    private final boolean showUsernameEnabled;

    private ForgottenUsernameConsoleConfig(ForgottenUsernameBuilder builder) {
        super(builder);
        minimumAnswersToVerify = builder.minimumAnswersToVerify;
        showUsernameEnabled = builder.showUsernameEnabled;
    }

    /**
     * Get the minimum count of questions to verify.
     *
     * @return minimum count
     */
    public int getMinimumAnswersToVerify() {
        return minimumAnswersToVerify;
    }

    /**
     * Whether or the not the username should be displayed.
     *
     * @return whether username should be shown
     */
    public boolean isShowUsernameEnabled() {
        return showUsernameEnabled;
    }

    static final class ForgottenUsernameBuilder
            extends Builder<ForgottenUsernameConsoleConfig, ForgottenUsernameBuilder> {

        private int minimumAnswersToVerify;
        private boolean showUsernameEnabled;

        private ForgottenUsernameBuilder(Map<String, Set<String>> consoleAttributes) {
            super(consoleAttributes);
        }

        ForgottenUsernameBuilder setMinimumAnswersToVerify(int minimumAnswersToVerify) {
            this.minimumAnswersToVerify = minimumAnswersToVerify;
            return this;
        }

        ForgottenUsernameBuilder setShowUsernameEnabled(boolean showUsernameEnabled) {
            this.showUsernameEnabled = showUsernameEnabled;
            return this;
        }

        @Override
        ForgottenUsernameBuilder self() {
            return this;
        }

        @Override
        void verifyKbaConfig() {
            Reject.ifFalse(minimumAnswersToVerify > 0, "Minimum questions to be verified must be greater than 0");
        }

        @Override
        ForgottenUsernameConsoleConfig internalBuild() {
            return new ForgottenUsernameConsoleConfig(this);
        }

    }

    static ForgottenUsernameBuilder newBuilder(Map<String, Set<String>> consoleAttributes) {
        return new ForgottenUsernameBuilder(consoleAttributes);
    }

}
