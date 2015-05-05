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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.soap.token.validator.wss;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.validate.Credential;

import java.security.cert.X509Certificate;

import org.apache.ws.security.validate.Validator;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;
import org.forgerock.openam.sts.token.validator.AuthenticationHandler;
import org.slf4j.Logger;

/**
 * This class extends the Apache WSS4j TokenValidator deployed by Apache CXF to validate x509 tokens presented as part
 * of consuming an STS instance protected by SecurityPolicy bindings specifying an x509 Certificate which serves to
 * identify the client (examples  2.2.2, and 2.2.4 of http://docs.oasis-open.org/ws-sx/security-policy/examples/ws-sp-usecases-examples.html)
 * In these cases, the presented x509Certificate[] must be validated with OpenAM so that an OpenAM session token can
 * be generated as the basis of the identity asserted in a generated token. In other words, if the SecurityPolicy binding
 * is the UNT over the asymmetric binding (sts_ut_asymmetric.wsdl), then the SignatureTrustValidator will be invoked
 * to insure that the client-presented certificate is valid, not revoked (optionally), and trusted. I don't want to hook
 * in the OpenAM Cert module in this case, as I don't need an OpenAM session id, and the KeyStore state deployed with the
 * STS instance is sufficient for the default signature trust validation.
 * This class will be
 * plugged-in as the SecurityConstants.SIGNATURE_TRUST_VALIDATOR only when X509 tokens are specified in the TokenValidationConfig
 * of the SoapSTSInstanceConfig of the published soap-sts instance. Otherwise, the SignatureTrustValidator will be
 * plugged-in as a cxf default. So this class will only be plugged-in when Certificate validation should be explicitly
 * dispatched to OpenAM and the Cert module.
 */
public class SoapCertificateTokenValidator implements Validator {
    private final AuthenticationHandler<X509Certificate[]> authenticationHandler;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final ValidationInvocationContext validationInvocationContext;
    private final boolean invalidateAMSession;
    private final Logger logger;

    /*
    No @Inject - instances created by the WSSValidatorFactoryImpl.
     */
    public SoapCertificateTokenValidator(AuthenticationHandler<X509Certificate[]> authenticationHandler,
                                         ThreadLocalAMTokenCache threadLocalAMTokenCache,
                                         ValidationInvocationContext validationInvocationContext, boolean invalidateAMSession, Logger logger) {
        this.authenticationHandler = authenticationHandler;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.validationInvocationContext = validationInvocationContext;
        this.invalidateAMSession = invalidateAMSession;
        this.logger = logger;
    }

    @Override
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        try {
            final String sessionId = authenticationHandler.authenticate(credential.getCertificates(), TokenType.X509);
            threadLocalAMTokenCache.cacheSessionIdForContext(validationInvocationContext, sessionId, invalidateAMSession);
            return credential;
        } catch (TokenValidationException e) {
            logger.error("Exception caught authenticating X509Certificate with OpenAM: " + e, e);
            throw new WSSecurityException(WSSecurityException.FAILED_AUTHENTICATION, e.getMessage());
        }
    }
}
