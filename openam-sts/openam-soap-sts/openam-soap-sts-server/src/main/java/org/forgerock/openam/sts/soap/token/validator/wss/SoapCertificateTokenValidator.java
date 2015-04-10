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

import com.google.inject.Inject;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.SignatureTrustValidator;

import java.security.cert.X509Certificate;

import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;
import org.forgerock.openam.sts.token.validator.wss.AuthenticationHandler;
import org.slf4j.Logger;

/**
 * This class extends the Apache WSS4j TokenValidator deployed by Apache CXF to validate x509 tokens presented as part
 * of consuming an STS instance protected by SecurityPolicy bindings specifying an x509 SupportingToken. This class will
 * integrate this validation with OpenAM by consuming the OpenAM REST authN functionality.
 *
 * This validation functionality will only augment the existing CXF validation functionality to determine whether the
 * included certs are present in the OpenAM LDAP certificate store.
 *
 * In the soap-sts, the validity of the caller asserting its identity via presenting a x509 cert
 * (SecurityPolicyExamples 2.2.1 and 2.2.2) will be insured because messages sent to the caller will be encrypted using the
 * caller's public key. In this case, only trust needs to be established - something which this class, by virtue of
 * extending the SignatureTrustValidator, provides. It may be a configuration option whether this class should
 * consume the OpenAM Certificate module, so that the additional invocation against the OpenAM Certificate module, performed
 * by the AuthenticationHandler<X509Certificate>, should be performed. The bottom line is that this class will be
 * plugged-in as the SecurityConstants.SIGNATURE_TRUST_VALIDATOR only when X509 tokens are specified in the TokenValidationConfig
 * of the SoapSTSInstanceConfig of the published soap-sts instance. Otherwise, the SignatureTrustValidator will be
 * plugged-in as a cxf default. So this class will only be plugged-in when Certificate validation should be explicitly
 * dispatched to OpenAM and the Cert module. An open question is whether the validate functionality of the SignatureTrustValidator
 * needs to be invoked in order to enforce the symmetric and asymmetric bindings.
 */
public class SoapCertificateTokenValidator extends SignatureTrustValidator {
    private final AuthenticationHandler<X509Certificate[]> authenticationHandler;
    private final ValidationInvocationContext validationInvocationContext;
    private final boolean invalidateAMSession;
    private final Logger logger;

    /*
    No @Inject - instances created by the WSSValidatorFactoryImpl.
     */
    public SoapCertificateTokenValidator(AuthenticationHandler<X509Certificate[]> authenticationHandler,
                                         ValidationInvocationContext validationInvocationContext, boolean invalidateAMSession, Logger logger) {
        this.authenticationHandler = authenticationHandler;
        this.validationInvocationContext = validationInvocationContext;
        this.invalidateAMSession = invalidateAMSession;
        this.logger = logger;
    }

    @Override
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        /*
        TODO: this method will only be entered when X509 tokens are specified in the TokenValidationConfig of the published
        soap-sts instance. An open question is whether the super.validate method must be invoked in order to enforce the
        symmetric and asymmetric bindings. If so, the ctor for this class would have to take state to determine whether
        the soap-sts was deployed with a symmetric or asymmetric binding, so that super.validate could be called.
         */
        Credential localCredential = super.validate(credential, data);
        try {
            authenticationHandler.authenticate(data, credential.getCertificates(), validationInvocationContext, invalidateAMSession);
            return credential;
        } catch (Exception e) {
            logger.error("Exception caught authenticating X509Certificate with OpenAM: " + e, e);
            throw new WSSecurityException(WSSecurityException.FAILED_AUTHENTICATION, e.getMessage());
        }
    }
}
