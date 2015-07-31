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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.soap.token.validator;

import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.ws.security.WSConstants;
import org.forgerock.openam.sts.TokenIdGenerationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSAccessTokenProvider;
import org.forgerock.openam.sts.token.CTSTokenIdGenerator;
import org.forgerock.openam.sts.token.provider.TokenServiceConsumer;
import org.w3c.dom.Element;

/**
 * TokenValidator implementation for SAML2 tokens issued by the sts. Simply consumes the TokenService to determine
 * whether a token with the specified id has been issued, and is not expired.
 */
public class SimpleSAML2TokenValidator extends SimpleTokenValidatorBase {
    private final XMLUtilities xmlUtilities;
    /*
    No @Inject as instances of this class are created by the TokenOperationFactoryImpl.
     */
    public SimpleSAML2TokenValidator(TokenServiceConsumer tokenServiceConsumer,
                                     SoapSTSAccessTokenProvider soapSTSAccessTokenProvider,
                                     CTSTokenIdGenerator ctsTokenIdGenerator,
                                     XMLUtilities xmlUtilities) {
        super(tokenServiceConsumer, soapSTSAccessTokenProvider, ctsTokenIdGenerator);
        this.xmlUtilities = xmlUtilities;
    }

    @Override
    public boolean canHandleToken(ReceivedToken validateTarget) {
        return canHandleToken(validateTarget, null);
    }

    @Override
    public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
        Object token = validateTarget.getToken();
        if (token instanceof Element) {
            Element tokenElement = (Element)token;
            return WSConstants.SAML2_NS.equals(tokenElement.getNamespaceURI());
        }
        return false;
    }

    @Override
    protected String generateIdFromValidateTarget(ReceivedToken validateTarget) throws TokenValidationException {
        //we know the token is an Element because of canHandleToken call above
        final Element samlTokenElement = (Element)validateTarget.getToken();
        try {
            return ctsTokenIdGenerator.generateTokenId(TokenType.SAML2, xmlUtilities.documentToStringConversion(samlTokenElement));
        } catch (TokenIdGenerationException e) {
            throw new TokenValidationException(e.getCode(), e.getMessage(), e);
        }
    }
}
