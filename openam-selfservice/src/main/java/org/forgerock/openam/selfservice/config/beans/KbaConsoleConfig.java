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

package org.forgerock.openam.selfservice.config.beans;

import org.forgerock.openam.sm.config.ConfigAttribute;
import org.forgerock.openam.sm.config.ConfigSource;
import org.forgerock.openam.sm.config.ConsoleConfigBuilder;
import org.forgerock.util.Reject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Console configuration for knowledge based answers (KBA).
 *
 * @since 13.0.0
 */
public final class KbaConsoleConfig {

    private final Map<String, Map<String, String>> securityQuestions;
    private final int minimumAnswersToDefine;
    private final int minimumAnswersToVerify;

    private KbaConsoleConfig(KbaBuilder builder) {
        securityQuestions = builder.securityQuestions;
        minimumAnswersToDefine = builder.minimumAnswersToDefine;
        minimumAnswersToVerify = builder.minimumAnswersToVerify;
    }

    /**
     * Gets the security questions.
     *
     * @return the security questions
     */
    public Map<String, Map<String, String>> getSecurityQuestions() {
        return securityQuestions;
    }

    /**
     * Gets the minimum number of answers to be defined.
     *
     * @return minimum answers to be defined
     */
    public int getMinimumAnswersToDefine() {
        return minimumAnswersToDefine;
    }

    /**
     * Gets the minimum number of answers to be verified.
     *
     * @return minimum answers to be verified
     */
    public int getMinimumAnswersToVerify() {
        return minimumAnswersToVerify;
    }

    @ConfigSource("RestSecurity")
    public static final class KbaBuilder implements ConsoleConfigBuilder<KbaConsoleConfig> {

        private final Map<String, Map<String, String>> securityQuestions;
        private int minimumAnswersToDefine;
        private int minimumAnswersToVerify;

        public KbaBuilder() {
            securityQuestions = new HashMap<>();
        }

        @ConfigAttribute(value = "forgerockRESTSecurityKBAQuestions", transformer = SecurityQuestionTransformer.class)
        public void setSecurityQuestions(Map<String, Map<String, String>> securityQuestions) {
            this.securityQuestions.putAll(securityQuestions);
        }

        @ConfigAttribute("forgerockRESTSecurityAnswersUserMustProvide")
        public void setMinimumAnswersToDefine(int minimumAnswersToDefine) {
            this.minimumAnswersToDefine = minimumAnswersToDefine;
        }

        @ConfigAttribute("forgerockRESTSecurityQuestionsUserMustAnswer")
        public void setMinimumAnswersToVerify(int minimumAnswersToVerify) {
            this.minimumAnswersToVerify = minimumAnswersToVerify;
        }

        @Override
        public KbaConsoleConfig build(Map<String, Set<String>> attributes) {
            Reject.ifTrue(minimumAnswersToVerify > minimumAnswersToDefine,
                    "Number of answers to verify must be equal or less that those defined");
            return new KbaConsoleConfig(this);
        }

    }

}
