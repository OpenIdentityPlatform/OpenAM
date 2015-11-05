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
import java.util.Set;

/**
 * Represents forgotten password console configuration.
 *
 * @since 13.0.0
 */
public final class ForgottenPasswordConsoleConfig extends CommonConsoleConfig {

    private final int minQuestionsToAnswer;

    private ForgottenPasswordConsoleConfig(ForgottenPasswordBuilder builder) {
        super(builder);
        minQuestionsToAnswer = builder.minQuestionsToAnswer;
    }

    /**
     * Get the minimum count of questions to answer.
     *
     * @return minimum count
     */
    public int getMinQuestionsToAnswer() {
        return minQuestionsToAnswer;
    }

    static final class ForgottenPasswordBuilder
            extends Builder<ForgottenPasswordConsoleConfig, ForgottenPasswordBuilder> {

        private int minQuestionsToAnswer;

        private ForgottenPasswordBuilder(Map<String, Set<String>> consoleAttributes) {
            super(consoleAttributes);
        }

        ForgottenPasswordBuilder setMinQuestionsToAnswer(int minQuestionsToAnswer) {
            this.minQuestionsToAnswer = minQuestionsToAnswer;
            return this;
        }

        @Override
        ForgottenPasswordBuilder getThis() {
            return this;
        }

        @Override
        ForgottenPasswordConsoleConfig internalBuild() {
            Reject.ifFalse(minQuestionsToAnswer > 0);
            return new ForgottenPasswordConsoleConfig(this);
        }
    }

    static ForgottenPasswordBuilder newBuilder(Map<String, Set<String>> consoleAttributes) {
        return new ForgottenPasswordBuilder(consoleAttributes);
    }

}
