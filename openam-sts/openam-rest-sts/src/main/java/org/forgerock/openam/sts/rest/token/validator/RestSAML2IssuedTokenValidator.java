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

package org.forgerock.openam.sts.rest.token.validator;

import com.sun.identity.security.AdminTokenAction;
import org.forgerock.openam.sts.TokenIdGenerationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.token.CTSTokenIdGenerator;
import org.forgerock.openam.sts.token.provider.TokenServiceConsumer;
import org.forgerock.openam.sts.user.invocation.SAML2TokenState;

import java.security.AccessController;

/**
 * RestIssuedTokenValidator implementation responsible for validating SAML2 assertions issued by the rest-sts.
 */
public class RestSAML2IssuedTokenValidator implements RestIssuedTokenValidator<SAML2TokenState> {
    protected final CTSTokenIdGenerator ctsTokenIdGenerator;
    protected final TokenServiceConsumer tokenServiceConsumer;

    /**
     * No @Inject as the ctor is called by the IssuedtokenValidatorFactoryImpl
     * @param ctsTokenIdGenerator encapsulation of logic to generate a CTS token id, given a token of a specific type
     * @param tokenServiceConsumer encapsulation of consumption of the TokenService, which ultimately performs token validation
     */
    public RestSAML2IssuedTokenValidator(CTSTokenIdGenerator ctsTokenIdGenerator, TokenServiceConsumer tokenServiceConsumer) {
        this.ctsTokenIdGenerator = ctsTokenIdGenerator;
        this.tokenServiceConsumer = tokenServiceConsumer;
    }

    @Override
    public boolean canValidateToken(TokenTypeId tokenType) {
        return TokenType.SAML2.getId().equals(tokenType.getId());
    }

    @Override
    public boolean validateToken(RestIssuedTokenValidatorParameters<SAML2TokenState> validatorParameters) throws TokenValidationException {
        return tokenServiceConsumer.validateToken(generateIdFromValidateTarget(validatorParameters.getInputToken()), getAdminToken());
    }

    private String generateIdFromValidateTarget(SAML2TokenState validateTarget) throws TokenValidationException {
        try {
            return ctsTokenIdGenerator.generateTokenId(TokenType.SAML2, validateTarget.getSAML2TokenValue());
        } catch (TokenIdGenerationException e) {
            throw new TokenValidationException(e.getCode(), e.getMessage(), e);
        }
    }

    protected String getAdminToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance()).getTokenID().toString();
    }
}
