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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidconnect;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.forgerock.oauth2.core.Utils.isEmpty;

/**
 * Parses and validates the OpenId Connect prompt parameter.s
 *
 * @since 12.0.0
 */
public class OpenIdPrompt {

    public static final String PROMPT_DELIMITER = " ";
    public static final String PROMPT_NONE = "none";
    public static final String PROMPT_LOGIN = "login";
    public static final String PROMPT_CONSENT = "consent";

    private final Set<String> prompts;

    /**
     * Constructs a new OpenIdPrompt instance from the given prompt String.
     * <br/>
     * Parses the prompt String by splitting on the ' ' character.
     *
     * @param prompt The prompt.
     */
    public OpenIdPrompt(String prompt) {
        if (isEmpty(prompt)) {
            prompts = Collections.emptySet();
            return;
        }

        prompts = new HashSet<String>(Arrays.asList(prompt.toLowerCase().split(PROMPT_DELIMITER)));
    }

    /**
     * Determines if the prompt contains 'none'.
     *
     * @return {@code true} if the prompt includes 'none'.
     */
    public boolean isNoPrompt() {
        return prompts.contains(PROMPT_NONE);
    }

    /**
     * Determines if the prompt contains 'login'.
     *
     * @return {@code true} if the prompt includes 'login'.
     */
    public boolean isPromptLogin() {
        return prompts.contains(PROMPT_LOGIN);
    }

    /**
     * Determines if the prompt contains 'consent'.
     *
     * @return {@code true} if the prompt includes 'consent'.
     */
    public boolean isPromptConsent() {
        return prompts.contains(PROMPT_CONSENT);
    }

    /**
     * Determines whether the prompt parameter is valid.
     *
     * @return {@code false} if the prompt includes 'none' combined with either 'consent' or 'login'.
     */
    public boolean isValid() {
        return !(prompts.contains(PROMPT_NONE) && (prompts.contains(PROMPT_CONSENT) || prompts.contains(PROMPT_LOGIN)));
    }
}
