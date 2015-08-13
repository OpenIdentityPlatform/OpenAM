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

package org.forgerock.openam.uma;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2Request;

/**
 * An interface for gathering claims from an authorization request containing
 * claim tokens.
 *
 * @since 13.0.0
 */
public interface ClaimGatherer {

    /**
     * Attempts to get the requesting party id from the claim token.
     *
     * @param oAuth2Request The OAuth2 request.
     * @param authorizationApiToken The AAT.
     * @param claimToken The claim token.
     * @return The requesting party id or {@code null} if invalid token.
     */
    String getRequestingPartyId(OAuth2Request oAuth2Request, AccessToken authorizationApiToken,
            JsonValue claimToken);

    /**
     * Gets the details of the required claims.
     *
     * @param issuer The issuer.
     * @return A {@code JsonValue} containing the details of the required claims.
     */
    JsonValue getRequiredClaimsDetails(String issuer);
}
