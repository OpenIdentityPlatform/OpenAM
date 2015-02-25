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

import org.forgerock.oauth2.core.exceptions.InvalidClientException;

/**
 * The OAuth2 providers store for all client registrations.
 *
 * @since 12.0.0
 */
public interface ClientRegistrationStore {

    /**
     * Gets the client registration for the given client id.
     *
     * @param clientId The client id
     * @param request The OAuth2 request.
     * @return The ClientRegistration.
     * @throws InvalidClientException If client cannot be retrieved from the store.
     */
    ClientRegistration get(String clientId, OAuth2Request request) throws InvalidClientException;
}
