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

    private final int minQuestionsToAnswer;
    private final boolean showUsernameEnabled;

    private ForgottenUsernameConsoleConfig(ForgottenUsernameBuilder builder) {
        super(builder);
        minQuestionsToAnswer = builder.minQuestionsToAnswer;
        showUsernameEnabled = builder.showUsernameEnabled;
    }

    /**
     * Get the minimum count of questions to answer.
     *
     * @return minimum count
     */
    public int getMinQuestionsToAnswer() {
        return minQuestionsToAnswer;
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

        private int minQuestionsToAnswer;
        private boolean showUsernameEnabled;

        private ForgottenUsernameBuilder(Map<String, Set<String>> consoleAttributes) {
            super(consoleAttributes);
        }

        ForgottenUsernameBuilder setMinQuestionsToAnswer(int minQuestionsToAnswer) {
            this.minQuestionsToAnswer = minQuestionsToAnswer;
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
            Reject.ifFalse(minQuestionsToAnswer > 0, "Minimum questions to be answered must be greater than 0");
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
