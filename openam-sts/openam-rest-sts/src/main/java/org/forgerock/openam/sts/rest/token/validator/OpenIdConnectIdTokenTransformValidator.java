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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.rest.token.validator;

import com.iplanet.services.util.Crypt;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.rest.operation.validate.IssuedTokenValidatorFactory;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;
import org.forgerock.openam.sts.token.validator.AuthenticationHandler;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;

/**
 * The RestTokenTransformValidator implementation responsible for dispatching OpenID Connect ID Tokens to the OpenAM Rest authN
 * context.
 */
public class OpenIdConnectIdTokenTransformValidator implements RestTokenTransformValidator<OpenIdConnectIdToken> {
    private final AuthenticationHandler<OpenIdConnectIdToken> authenticationHandler;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final PrincipalFromSession principalFromSession;
    private final ValidationInvocationContext validationInvocationContext;
    private final IssuedTokenValidatorFactory  issuedTokenValidatorFactory;
    private final boolean invalidateAMSession;
    public OpenIdConnectIdTokenTransformValidator(AuthenticationHandler<OpenIdConnectIdToken> authenticationHandler,
                                                  ThreadLocalAMTokenCache threadLocalAMTokenCache,
                                                  PrincipalFromSession principalFromSession,
                                                  ValidationInvocationContext validationInvocationContext,
                                                  IssuedTokenValidatorFactory issuedTokenValidatorFactory,
                                                  boolean invalidateAMSession) {
        this.authenticationHandler = authenticationHandler;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.principalFromSession = principalFromSession;
        this.validationInvocationContext = validationInvocationContext;
        this.issuedTokenValidatorFactory = issuedTokenValidatorFactory;
        this.invalidateAMSession = invalidateAMSession;
    }

    @Override
    public RestTokenTransformValidatorResult validateToken(RestTokenTransformValidatorParameters<OpenIdConnectIdToken> tokenParameters) throws TokenValidationException {
        RestTokenTransformValidatorResult result = validateInternal(tokenParameters);
        if(result != null) {
            return result;
        }
        final String sessionId = authenticationHandler.authenticate(tokenParameters.getInputToken(), TokenType.OPENIDCONNECT);
        threadLocalAMTokenCache.cacheSessionIdForContext(validationInvocationContext, sessionId, invalidateAMSession);
        return new RestTokenTransformValidatorResult(principalFromSession.getPrincipalFromSession(sessionId), sessionId);
    }

    private RestTokenTransformValidatorResult validateInternal(RestTokenTransformValidatorParameters<OpenIdConnectIdToken> tokenParameters) {
          try {
            RestIssuedTokenValidatorParameters<OpenIdConnectIdToken> issuedTokenParams = () -> tokenParameters.getInputToken();
            RestIssuedTokenValidator issuedTokenValidator = issuedTokenValidatorFactory.getTokenValidator(TokenType.OPENIDCONNECT);
            boolean openamTokenValid = issuedTokenValidator.validateToken(issuedTokenParams);
            if(openamTokenValid) {
                JwtReconstruction jwtReconstruction = new JwtReconstruction();
                SignedJwt signedJwt = jwtReconstruction.reconstructJwt(tokenParameters.getInputToken().getTokenValue(), SignedJwt.class);
                JwtClaimsSet claims = signedJwt.getClaimsSet();
                if (claims.isDefined("auth:token:encrypt")) {
                    String encryptedToken = claims.getClaim("auth:token:encrypt", String.class);
                    String sessionId = Crypt.decryptLocal(encryptedToken);
                    threadLocalAMTokenCache.cacheSessionIdForContext(validationInvocationContext, sessionId, invalidateAMSession);
                    return new RestTokenTransformValidatorResult(principalFromSession.getPrincipalFromSession(sessionId), sessionId);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
