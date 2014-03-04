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

import org.apache.cxf.sts.request.ReceivedToken;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenMarshalException;

/**
 * The TokenValidatorParameters passed to the CXF-STS TokenValidator implementations represent the to-be-validated-token
 * as a ReceivedToken class. This class represents tokens as XML. The REST STS instances will take json token
 * representations. This interface defines functionality to marshal a json token into an XML element in a ReceivedToken
 * instance which can be used by the CXF-STS TokenValidators.
 */
public interface TokenRequestMarshaller {
    /**
     * Marshals a json token into an instance of the org.apache.cxf.sts.request.ReceivedToken class.
     * @param token the json representation of a token
     * @return a ReceivedToken instance which has an xml representation of the json token.
     * @throws org.forgerock.openam.sts.TokenMarshalException if the json string cannot be marshalled into a recognized token.
     */
    ReceivedToken marshallTokenRequest(JsonValue token) throws TokenMarshalException;

    /**
     * Returns the TokenType corresponding to this json token. The json token will specify its token type with
     * an AMSTSConstants.TOKEN_TYPE_KEY value.
     * @param token The json token representation
     * @return The TokenType represented by the json string.
     * @throws org.forgerock.openam.sts.TokenMarshalException if the TOKEN_TYPE_KEY is missing or unrecognized.
     */
    TokenType getTokenType(JsonValue token) throws TokenMarshalException;
}
