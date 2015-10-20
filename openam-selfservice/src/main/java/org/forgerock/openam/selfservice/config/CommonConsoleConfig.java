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

import java.util.Map;

/**
 * Represents common console configuration used by all self services.
 *
 * @since 13.0.0
 */
public interface CommonConsoleConfig extends ConsoleConfig {

    /**
     * Whether the service is enabled.
     *
     * @return whether the service is enabled
     */
    boolean isEnabled();

    /**
     * Gets the url to be used within the email.
     *
     * @return the email url
     */
    String getEmailUrl();

    /**
     * Gets the token expiry time in seconds.
     *
     * @return the token expiry time
     */
    long getTokenExpiry();

    /**
     * Whether the KBA stage is enabled.
     *
     * @return whether the KBA stage is enabled
     */
    boolean isKbaEnabled();

    /**
     * Gets the security questions in the expected format:
     * <pre>Map&lt;id,Map&lt;locale,question&gt;&gt;</pre>
     *
     * @return security questions
     */
    Map<String, Map<String, String>> getSecurityQuestions();

}
