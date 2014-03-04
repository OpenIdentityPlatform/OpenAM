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
import org.forgerock.openam.sts.token.provider.OpenAMSessionIdElementBuilder;

import javax.inject.Inject;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * {@link org.forgerock.openam.sts.rest.marshal.TokenResponseMarshaller}
 */
public class TokenResponseMarshallerImpl implements TokenResponseMarshaller {
    private static final String ISSUED_TOKEN = "issued_token";

    private final OpenAMSessionIdElementBuilder elementBuilder;

    @Inject
    TokenResponseMarshallerImpl(OpenAMSessionIdElementBuilder elementBuilder) {
        this.elementBuilder = elementBuilder;
    }

    @Override
    public JsonValue marshalTokenResponse(TokenType desiredTokenType, TokenProviderResponse tokenProviderResponse) throws TokenMarshalException {
        if (TokenType.SAML2.equals(desiredTokenType)) {
            Transformer transformer = null;
            try {
                transformer = TransformerFactory.newInstance().newTransformer();
                StreamResult res =  new StreamResult(new ByteArrayOutputStream());
                transformer.transform(new DOMSource(tokenProviderResponse.getToken()), res);
                String token = new String(((ByteArrayOutputStream)res.getOutputStream()).toByteArray());
                return json(object(field(ISSUED_TOKEN, token)));
            } catch (TransformerConfigurationException e) {
                throw new TokenMarshalException("Could not marshall saml2 token to string: " + e.getMessage(), e);
            } catch (TransformerException e) {
                throw new TokenMarshalException("Could not marshall saml2 token to string: " + e.getMessage(), e);
            }
        } else if (TokenType.OPENAM.equals(desiredTokenType)) {
            return json(object(field(ISSUED_TOKEN, elementBuilder.extractOpenAMSessionId(tokenProviderResponse.getToken()))));
        } else {
            throw new TokenMarshalException("Unexpected token type,  " + desiredTokenType + " passed to marshallTokenResponse");
        }
    }
}
