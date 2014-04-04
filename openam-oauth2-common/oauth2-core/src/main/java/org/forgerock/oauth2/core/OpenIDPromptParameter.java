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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Parses the OpenID Connect prompt parameter and interprets what to prompt for.
 *
 * @since 11.0.0
 */
public class OpenIDPromptParameter {

    /**
     * This is the delimeter that the prompt parameters are seperated by.
     */
    private static final String PROMPT_DELIMITER = " ";

    /**
     * This is the none prompt parameter.
     */
    private static final String PROMPT_NONE = "none";

    /**
     * This is the login prompt parameter.
     */
    private static final String PROMPT_LOGIN = "login";

    /**
     * This is the consent prompt parameter.
     */
    private static final String PROMPT_CONSENT = "consent";

    /**
     * This contains the set of the values given in the prompt parameter.
     */
    private final Set<String> prompts;

    /**
     * Constructor for OpenIDPromptParameter.
     *
     * @param promptString The prompt string sent in the http request.
     */
    public OpenIDPromptParameter(String promptString) {
        if (promptString == null || promptString.isEmpty()) {
            prompts = new HashSet<String>();
        } else {
            prompts = new HashSet<String>(Arrays.asList(promptString.toLowerCase().split(PROMPT_DELIMITER)));
        }
    }

    /**
     * Gets whether or not to show prompts to the end user.
     *
     * @return true if the prompts string is valid and contains the value none; false otherwise.
     */
    public boolean noPrompts() {
        if (isValid() && prompts.contains(PROMPT_NONE)) {
            return true;
        }
        return false;
    }

    /**
     * Gets whether or not to prompt the enduser to login.
     *
     * @return true if the prompt parameter contains login and is valid; false otherwise.
     */
    public boolean promptLogin() {
        if (isValid() && prompts.contains(PROMPT_LOGIN)) {
            return true;
        }
        return false;
    }

    /**
     * Gets whether or not to prompt the enduser to consent.
     *
     * @return true if the prompt parameter contains consent and is valid; false otherwise.
     */
    public boolean promptConsent() {
        if (isValid() && prompts.contains(PROMPT_CONSENT)) {
            return true;
        }
        return false;
    }

    /**
     * Gets whether or not the prompt parameter is valid.
     * @return true as long as the prompt parameter does not contain none and a valid value other than none; false
     * otherwise.
     */
    public boolean isValid() {
        if (prompts.contains(PROMPT_NONE) && (prompts.contains(PROMPT_CONSENT) || prompts.contains(PROMPT_LOGIN))) {
            return false;
        }
        return true;
    }
}
