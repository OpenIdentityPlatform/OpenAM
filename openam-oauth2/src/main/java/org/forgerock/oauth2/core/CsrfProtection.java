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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import javax.inject.Inject;
import java.nio.charset.Charset;
import java.security.MessageDigest;

import com.iplanet.sso.SSOToken;

/**
 * This class provides methods for checking if a request is a part of a cross-site request forgery attack (CSRF).
 */
public class CsrfProtection {

    private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");
    private final ResourceOwnerSessionValidator resourceOwnerSessionValidator;

    @Inject
    public CsrfProtection(ResourceOwnerSessionValidator resourceOwnerSessionValidator) {
        this.resourceOwnerSessionValidator = resourceOwnerSessionValidator;
    }

    /**
     * Checks if the request contains the required "csrf" parameter and check it equals the users session id.
     *
     * @param request The request.
     * @return {@code true} if the request is a CSRF attack, {@code false} if not.
     */
    public boolean isCsrfAttack(OAuth2Request request) {
        SSOToken ssoToken = resourceOwnerSessionValidator.getResourceOwnerSession(request);
        String ssoTokenId = ssoToken.getTokenID().toString();
        String csrfValue = request.getParameter("csrf");
        return csrfValue == null
                || !MessageDigest.isEqual(ssoTokenId.getBytes(UTF_8_CHARSET), csrfValue.getBytes(UTF_8_CHARSET));
    }
}
