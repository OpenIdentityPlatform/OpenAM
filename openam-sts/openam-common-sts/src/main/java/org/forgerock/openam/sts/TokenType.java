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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts;

import org.apache.ws.security.WSConstants;
import org.forgerock.json.resource.ResourceException;

/**
 * This enum represents the types of transformed tokens.
 */
public enum TokenType {
    SAML2, USERNAME, OPENAM, OPENIDCONNECT;

    /**
     * Used to marshal the TokenType to a String recognized by TokenProvider implementations, in particular those
     * provided in the CXF-STS. Not including USERNAME or OPEN_ID_CONNECT, as neither is not a token type which token transformation
     * will return.
     */
    public static String getProviderParametersTokenType(TokenType tokenType) throws TokenCreationException {
        if (OPENAM.equals(tokenType)) {
            return OPENAM.name();
        } else if (SAML2.equals(tokenType)) {
            return WSConstants.WSS_SAML2_TOKEN_TYPE;
        } else {
            throw new TokenCreationException(ResourceException.BAD_REQUEST,
                    "The specified tokenType, " + tokenType.name() + ", is unknown.");
        }
    }
}
