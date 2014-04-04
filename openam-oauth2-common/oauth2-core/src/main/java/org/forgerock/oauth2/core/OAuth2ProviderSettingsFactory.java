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

package org.forgerock.oauth2.core;

import java.util.Map;

/**
 * Allows for a layer of abstraction between the OAuth2 process and the OAuth2 Provider implementation.
 * <br/>
 * This allows for multiple OAuth2 Provider Settings to be configured for a single OAuth2 Provider server.
 * For example, OpenAM allows a OAuth2 Provider per realm.
 * <br/>
 * The OAuth2 Provider will implement this interface and use the context map to return/create the appropriate
 * OAuth2 Provider Settings instance.
 *
 * @since 12.0.0
 */
public interface OAuth2ProviderSettingsFactory {

    /**
     * Gets the OAuth2 Provider Settings instance based on the content of the specified context map.
     *
     * @param context A {@code Map<String, Object>} containing OAuth2 Provider implementation specific context
     *                information.
     * @return The OAuth2 Provider Settings instance.
     */
    OAuth2ProviderSettings getProviderSettings(final Map<String, Object> context);
}
