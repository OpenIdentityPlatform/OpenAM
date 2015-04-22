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

import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;
import org.forgerock.openam.sts.token.validator.AuthenticationHandler;

import java.security.cert.X509Certificate;

/**
 * This class is a RestTokenValidator responsible for validating X509 Certificates. It will only pull certificates
 * presented to the rest-sts via two-way tls. These certificates will be obtained from either the
 * javax.servlet.request.X509Certificate in the HttpServletRequest (OpenAM container is supporting two-way tls directly),
 * or from a header configured in the RestDeploymentConfig (to support deployments in which OpenAM is deployed behind
 * a tls-offloader). The {@code AuthenticationHandler<X509Certificate>} will ultimately consume the Certificate authN module
 * via 'portal' mode, which is where the Certificate module expects to find the certificate in a header. Thus the
 * AuthTargetMapping for X509 token-transformations must be configured with the name of this header (similar to
 * OIDC token transformations). Note that this is not the same header value configured in the RestDeploymentConfig
 * for rest-sts instances, which specifies the header key where the rest-sts expects to find the client certificate. (The
 * header for the AuthTargetMapping could be re-used for this purpose, but rest-sts instances should be able to unequivocally
 * determine where the user intends the certificate to be found(in a header, or in the javax.servlet.request.X509Certificate
 * attribute. Because the AuthTargetMapping has to be defined for all X509 token transformations, the presence/absence
 * of this state cannot be used to determine where the rest-sts should find the client's certificate (and simply looking
 * in both places is sloppy/imprecise)).
 */
public class RestCertificateTokenValidator implements RestTokenValidator<X509Certificate[]> {
    private final AuthenticationHandler<X509Certificate[]> authenticationHandler;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final PrincipalFromSession principalFromSession;
    private final ValidationInvocationContext validationInvocationContext;
    private final boolean invalidateAMSession;


    /*
    ctor not injected as it is constructed by the TokenTransformFactoryImpl
     */
    public RestCertificateTokenValidator(AuthenticationHandler<X509Certificate[]> authenticationHandler,
                                         ThreadLocalAMTokenCache threadLocalAMTokenCache,
                                         PrincipalFromSession principalFromSession,
                                         ValidationInvocationContext validationInvocationContext,
                                         boolean invalidateAMSession) {
        this.authenticationHandler = authenticationHandler;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.principalFromSession = principalFromSession;
        this.validationInvocationContext = validationInvocationContext;
        this.invalidateAMSession = invalidateAMSession;
    }

    @Override
    public RestTokenValidatorResult validateToken(RestTokenValidatorParameters<X509Certificate[]> tokenParameters) throws TokenValidationException {
        final String sessionId = authenticationHandler.authenticate(tokenParameters.getInputToken(), TokenType.X509);
        threadLocalAMTokenCache.cacheSessionIdForContext(validationInvocationContext, sessionId, invalidateAMSession);
        return new RestTokenValidatorResult(principalFromSession.getPrincipalFromSession(sessionId), sessionId);
    }
}

