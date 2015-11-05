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
public final class UserRegistrationConsoleConfig extends CommonConsoleConfig {

    private final int minAnswersToProvide;

    private UserRegistrationConsoleConfig(UserRegistrationBuilder builder) {
        super(builder);
        minAnswersToProvide = builder.minAnswersToProvide;
    }

    /**
     * Get the minimum count of answers to provide.
     *
     * @return minimum count
     */
    public int getMinAnswersToProvide() {
        return minAnswersToProvide;
    }

    static final class UserRegistrationBuilder
            extends Builder<UserRegistrationConsoleConfig, UserRegistrationBuilder> {

        private int minAnswersToProvide;

        private UserRegistrationBuilder(Map<String, Set<String>> consoleAttributes) {
            super(consoleAttributes);
        }

        UserRegistrationBuilder setMinAnswersToProvide(int minAnswersToProvide) {
            this.minAnswersToProvide = minAnswersToProvide;
            return this;
        }

        @Override
        UserRegistrationBuilder getThis() {
            return this;
        }

        @Override
        UserRegistrationConsoleConfig internalBuild() {
            Reject.ifFalse(minAnswersToProvide > 0);
            return new UserRegistrationConsoleConfig(this);
        }
    }

    static UserRegistrationBuilder newBuilder(Map<String, Set<String>> consoleAttributes) {
        return new UserRegistrationBuilder(consoleAttributes);
    }

}
