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
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenIdGenerationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSAccessTokenProvider;
import org.forgerock.openam.sts.token.CTSTokenIdGenerator;
import org.forgerock.openam.sts.token.provider.TokenServiceConsumer;

/**
 * TokenValidator implementation for OpenIdConnect tokens issued by the sts. Simply consumes the TokenService to determine
 * whether a token with the specified id has been issued, and is not expired.
 */
public class SimpleOpenIdConnectTokenValidator extends SimpleTokenValidatorBase {
    /*
    No @Inject as instances of this class are created by the TokenOperationFactoryImpl.
     */
    public SimpleOpenIdConnectTokenValidator(TokenServiceConsumer tokenServiceConsumer,
                                     SoapSTSAccessTokenProvider soapSTSAccessTokenProvider, CTSTokenIdGenerator ctsTokenIdGenerator) {
        super(tokenServiceConsumer, soapSTSAccessTokenProvider, ctsTokenIdGenerator);
    }

    @Override
    public boolean canHandleToken(ReceivedToken validateTarget) {
        return canHandleToken(validateTarget, null);
    }

    @Override
    public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
        if (validateTarget.isBinarySecurityToken()) {
            final BinarySecurityTokenType binarySecurityTokenType = (BinarySecurityTokenType)validateTarget.getToken();
            return AMSTSConstants.AM_OPEN_ID_CONNECT_TOKEN_ASSERTION_TYPE.equals(binarySecurityTokenType.getValueType());
        }
        return false;
    }

    @Override
    protected String generateIdFromValidateTarget(ReceivedToken validateTarget) throws TokenValidationException {
        //we know we are dealing with a BinarySecurityTokenType because of the canHandleToken invocation above
        String bstContent = ((BinarySecurityTokenType)validateTarget.getToken()).getValue();
        try {
            return ctsTokenIdGenerator.generateTokenId(TokenType.OPENIDCONNECT, bstContent);
        } catch (TokenIdGenerationException e) {
            throw new TokenValidationException(e.getCode(), e.getMessage(), e);
        }
    }
}
