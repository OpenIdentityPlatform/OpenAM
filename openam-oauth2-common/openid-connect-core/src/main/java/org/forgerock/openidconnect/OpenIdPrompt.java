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

import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.forgerock.oauth2.core.Utils.isEmpty;
import static org.forgerock.oauth2.core.Utils.stringToSet;

/**
 * Parses and validates the OpenId Connect prompt parameters.
 *
 * @since 12.0.0
 */
public class OpenIdPrompt {

    public static final String PROMPT_NONE = "none";
    public static final String PROMPT_LOGIN = "login";
    public static final String PROMPT_CONSENT = "consent";

    private String originalValue;
    private final Set<String> prompts;

    /**
     * Constructs a new OpenIdPrompt instance from the given prompt String.
     * <br/>
     * Parses the prompt string (converted to lowercase) by splitting on the ' ' character.
     *
     * @param prompt The prompt.
     */
    public OpenIdPrompt(String prompt) {
        originalValue = prompt;
        if (isEmpty(prompt)) {
            prompts = Collections.emptySet();
        } else {
            prompts = stringToSet(prompt.toLowerCase());
        }
    }

    /**
     * Constructs a new OpenIdPrompt instance directly from the request object
     * by using the constant defined in OAuth2Constants and calling the
     * existing constructor with the string obtained.
     *
     * @param request The request object
     */
    public OpenIdPrompt(OAuth2Request request) {
        this((String) request.getParameter(OAuth2Constants.Custom.PROMPT));
    }

    /**
     * Determines if the prompt contains 'none'.
     *
     * @return {@code true} if the prompt includes 'none'.
     */
    public boolean containsNone() {
        return prompts.contains(PROMPT_NONE);
    }

    /**
     * Determines if the prompt contains 'login'.
     *
     * @return {@code true} if the prompt includes 'login'.
     */
    public boolean containsLogin() {
        return prompts.contains(PROMPT_LOGIN);
    }

    /**
     * Determines if the prompt contains 'consent'.
     *
     * @return {@code true} if the prompt includes 'consent'.
     */
    public boolean containsConsent() {
        return prompts.contains(PROMPT_CONSENT);
    }

    /**
     * Determines whether the prompt parameter is valid.
     *
     * @return {@code false} if the prompt includes 'none' combined with either 'consent' or 'login'.
     */
    public boolean isValid() {
        return !(containsNone() && (containsConsent() || containsLogin()));
    }

    /**
     * Get to the original value passed in.  This is in case we took the "invoke the constructor with the
     * http request" route and may not have access to the value used.
     *
     * @return the original "prompt" value used to initialise this object
     */
    public String getOriginalValue() {
        return originalValue;
    }
}
