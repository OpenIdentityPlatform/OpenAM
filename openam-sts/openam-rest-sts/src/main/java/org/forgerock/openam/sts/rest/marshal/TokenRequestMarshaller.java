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
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.rest.service.RestSTSServiceHttpServletContext;
import org.forgerock.openam.sts.service.invocation.ProofTokenState;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;

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
     * @param httpContext The HttpContext, which is necessary to obtain the client's x509 cert
     *                    presented via two-way-tls for token transformations with x509 certs as input token types.
     * @param restSTSServiceHttpServletContext Provides direct access to the HttpServletRequest so that
     *                                                            client certificate state, presented via two-way-tls, can
     *                                                            be obtained
     * @return a ReceivedToken instance which has an xml representation of the json token.
     * @throws org.forgerock.openam.sts.TokenMarshalException if the json string cannot be marshalled into a recognized token.
     */
    ReceivedToken marshallInputToken(JsonValue token, HttpContext httpContext, RestSTSServiceHttpServletContext
            restSTSServiceHttpServletContext) throws TokenMarshalException;

    /**
     * Returns the TokenType corresponding to the JsonValue. The JsonValue will be pulled from the RestSTSServiceInvocationState.
     * @param token The token definition
     * @return The TokenType represented by the json string.
     * @throws org.forgerock.openam.sts.TokenMarshalException if the TOKEN_TYPE_KEY is missing or unrecognized.
     */
    TokenType getTokenType(JsonValue token) throws TokenMarshalException;

    /**
     * Returns the SAML2SubjectConfirmation specified in the JsonValue
     * @param token The JsonValue encoding the state defined in the SAML2TokenState class.
     * @return The SAML2SubjectConfirmation defined in the json representation of the SAML2TokenState
     * @throws TokenMarshalException if the SubjectConfirmation cannot be determined - i.e. if the JsonValue does not
     * correlate to SAML2TokenState.
     */
    SAML2SubjectConfirmation getSubjectConfirmation(JsonValue token) throws TokenMarshalException;

    /**
     * Returns the X509Certificated proof token for HolderOfKey assertions specified in the SAML2TokenState. Note that
     * this method should only be called if a HolderOfKey assertion is specified by the SAML2TokenState.
     * @param token The JsonValue corresponding to the SAML2TokenState
     * @return The SP ACS URL
     * @throws TokenMarshalException if the value could not be obtained
     */
    ProofTokenState getProofTokenState(JsonValue token) throws TokenMarshalException;
}
