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

import org.forgerock.oauth2.core.AuthorizeRequestValidator;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.util.Reject;

/**
 * Implementation of the AuthorizeRequestValidator for OAuth2 request validation.
 *
 * @since 12.0.0
 */
public class OpenIdConnectAuthorizeRequestValidator implements AuthorizeRequestValidator {

    /**
     * {@inheritDoc}
     */
    public void validateRequest(OAuth2Request request) throws BadRequestException {
        try {
            Reject.ifFalse(new OpenIdPrompt(request.<String>getParameter("prompt")).isValid(),
                    "Prompt parameter is invalid");
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
