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

package org.forgerock.openam.sts.rest.marshal;

import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenType;

/**
 * The TokenProviderResponse instance returned by the CXF-STS TokenProvider instances represents the token as a
 * org.w3c.dom.Element. It is likely that the REST-STS will only return json, and thus the XML elements corresponding
 * to an e.g. SAML token will likely be returned in a json value. This interface defines the functionality necessary
 * to create this json value. Other token types may not have a native xml format, yet will represent their generated tokens
 * as XML, due to the contract demanded by the TokenProviderResponse. This interface implementation will also marshal
 * these token types, which do not mandate a native xml format, from xml to json.
 *
 * It may be decided that either xml or json can be returned from the REST-STS - if so, the interface will be enhanced with
 * the specification of the returned type.
 *
 */
public interface TokenResponseMarshaller {
    /**
     *
     * @param desiredTokenType The token type returned by the transformation. Allows the implementation to know the nature
     *                         of the xml in the TokenProviderResponse
     * @param response The response from the TokenProvider
     * @return A JsonValue with the json representation of the returned token.
     * @throws TokenMarshalException if the marshalling fails
     */
    JsonValue marshalTokenResponse(TokenType desiredTokenType, TokenProviderResponse response) throws TokenMarshalException;
}
