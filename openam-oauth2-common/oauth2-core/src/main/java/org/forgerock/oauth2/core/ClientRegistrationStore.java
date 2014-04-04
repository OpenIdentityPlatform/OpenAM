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
 * Represents the store in which all clients registrations are kept on the OAuth2 Provider.
 *
 * @since 12.0.0
 */
public interface ClientRegistrationStore {

    /**
     * Gets the Client Registration from the store.
     *
     * @param clientId The identifier of the client.
     * @param context A {@code Map<String, Object>} containing OAuth2 Provider implementation specific context
     *                information.
     * @return The Client Registration.
     */
    ClientRegistration get(final String clientId, final Map<String, Object> context);
}
