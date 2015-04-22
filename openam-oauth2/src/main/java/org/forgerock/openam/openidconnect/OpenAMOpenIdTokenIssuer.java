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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.openidconnect;

import javax.inject.Inject;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.ResourceOwnerSessionValidator;
import org.forgerock.openam.utils.OpenAMSettings;
import org.forgerock.openidconnect.OpenIDTokenIssuer;
import org.forgerock.openidconnect.OpenIdConnectTokenStore;

/**
 * Issues OpenId Connect tokens and stores them in the OpenID Connect Token Store, when an access token is required
 * and the OAuth2 request scope contains 'openid'.
 *
 * @since 12.0.0
 */
public class OpenAMOpenIdTokenIssuer extends OpenIDTokenIssuer {

    private final OpenAMSettings openAMSettings;

    /**
     * Constructs a new OpenAMOpenIdTokenIssuer.
     *
     * @param tokenStore An instance of the OpenIdConnectTokenStore.
     * @param openAMSettings An instance of the OpenAMSettings.
     */
    @Inject
    public OpenAMOpenIdTokenIssuer(OpenIdConnectTokenStore tokenStore, OpenAMSettings openAMSettings,
                                   ResourceOwnerSessionValidator resourceOwnerSessionValidator) {
        super(tokenStore, resourceOwnerSessionValidator);
        this.openAMSettings = openAMSettings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getOps(AccessToken accessToken, OAuth2Request request) {
        return accessToken.getSessionId();
    }

}
