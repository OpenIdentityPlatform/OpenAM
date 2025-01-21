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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.oauth2.core;

import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

/**
 * Implementation of the request validator for the OAuth2 client credentials grant.
 *
 * @since 12.0.0
 */
public class ClientCredentialsRequestValidatorImpl implements ClientCredentialsRequestValidator {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Factory");

    /**
     * {@inheritDoc}
     */
    public void validateRequest(OAuth2Request request, ClientRegistration clientRegistration)
            throws UnauthorizedClientException {

        if (!clientRegistration.isConfidential()) {
            logger.error("Client is not confidential. Public clients cannot use the client credentials grant.");
            throw new UnauthorizedClientException("Public clients can't use client credentials grant.");
        }
    }
}
