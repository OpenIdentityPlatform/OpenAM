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
import org.forgerock.oauth2.core.ResourceOwnerConsentVerifier;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerConsentRequiredException;

import javax.inject.Singleton;

/**
 * Verifier for determining whether a resource owner has saved its consent for the authorization grant, taking into
 * account and OpenID Connect prompt parameter.
 *
 * @since 12.0.0
 */
@Singleton
public class OpenIdResourceOwnerConsentVerifier implements ResourceOwnerConsentVerifier {

    /**
     * {@inheritDoc}
     */
    public boolean verify(boolean consentSaved, OAuth2Request request) throws ResourceOwnerConsentRequiredException {
        final OpenIdPrompt prompt = new OpenIdPrompt(request);

        if (prompt.containsNone() && !consentSaved) {
            throw new ResourceOwnerConsentRequiredException();
        }

        return consentSaved && !prompt.containsConsent();
    }
}
