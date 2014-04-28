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

import org.forgerock.oauth2.core.exceptions.ResourceOwnerConsentRequiredException;

/**
 * Verifier for determining whether a resource owner has saved its consent for the authorization grant, taking into
 * account and OpenID Connect prompt parameter.
 *
 * @since 12.0.0
 */
public interface ResourceOwnerConsentVerifier {

    /**
     * Determines whether if the resource owner has previously saved consent and whether it should be used.
     * <br/>
     * OpenID Connect prompt parameter can mandate that the resource owner is forced to give consent.
     *
     * @param consentSaved {@code true} if the resource owner has previously saved consent.
     * @param request The OAuth2 request.
     * @return {@code true} if the resource owner has saved consent and it can be used.
     * @throws ResourceOwnerConsentRequiredException If the OpenID Connect prompt parameter enforces that the resource
     *          owner is not asked for consent, but the resource owners consent has not been previously stored.
     */
    boolean verify(boolean consentSaved, OAuth2Request request) throws ResourceOwnerConsentRequiredException;
}
