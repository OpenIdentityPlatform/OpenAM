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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.token.validator;

import com.sun.identity.shared.encode.Base64;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.handler.RequestData;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.AMSTSRuntimeException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.forgerock.openam.sts.token.validator.wss.AuthenticationHandler;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * This class is a CXF-STS TokenValidator responsible for validating X509 Certificates. It will only pull certificates
 * presented to the rest-sts via two-way tls. These certificates will be obtained from either the
 * javax.servlet.request.X509Certificate in the HttpServletRequest (OpenAM container is supporting two-way tls directly),
 * or from a header configured in the RestDeploymentConfig (to support deployments in which OpenAM is deployed behind
 * a tls-offloader). The AuthenticationHandler<X509Certificate[]> will ultimately consume the Certificate authN module
 * via 'portal' mode, which is where the Certificate module expects to find the certificate in a header. Thus the
 * AuthTargetMapping for X509 token-transformations must be configured with the name of this header (similar to
 * OIDC token transformations). Note that this is not the same header value configured in the RestDeploymentConfig
 * for rest-sts instances, which specifies the header key where the rest-sts expects to find the client certificate. (The
 * header for the AuthTargetMapping could be re-used for this purpose, but rest-sts instances should be able to uniquivocally
 * determine where the user intends the certificate to be found(in a header, or in the javax.servlet.request.X509Certificate
 * attribute. Because the AuthTargetMapping has to be defined for all X509 token transformations, the presence/absence
 * of this state cannot be used to determine where the rest-sts should find the client's certificate (and simply looking
 * in both places is sloppy/imprecise)).
 */
public class RestCertificateTokenValidator implements TokenValidator {
    public static final String X509_V3_TYPE = WSConstants.X509TOKEN_NS + "#X509v3";
    public static final String BASE64_ENCODING_TYPE = WSConstants.SOAPMESSAGE_NS + "#Base64Binary";
    private final AuthenticationHandler<X509Certificate[]> authenticationHandler;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final PrincipalFromSession principalFromSession;


    /*
    ctor not injected as it is constructed by the TokenTransformFactoryImpl
     */
    public RestCertificateTokenValidator(AuthenticationHandler<X509Certificate[]> authenticationHandler,
                                         ThreadLocalAMTokenCache threadLocalAMTokenCache,
                                         PrincipalFromSession principalFromSession) {
        this.authenticationHandler = authenticationHandler;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.principalFromSession = principalFromSession;

    }
    @Override
    public boolean canHandleToken(ReceivedToken validateTarget) {
        return canHandleToken(validateTarget, null);
    }

    @Override
    public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
        Object token = validateTarget.getToken();
        return (token instanceof BinarySecurityTokenType)
                && X509_V3_TYPE.equals(((BinarySecurityTokenType)token).getValueType())
                && BASE64_ENCODING_TYPE.equals(((BinarySecurityTokenType)token).getEncodingType());
    }

    @Override
    public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
        TokenValidatorResponse response = new TokenValidatorResponse();
        ReceivedToken validateTarget = tokenParameters.getToken();
        validateTarget.setState(ReceivedToken.STATE.INVALID);
        response.setToken(validateTarget);

        //no concern about the blind cast, as the validateToken will not be called unless canHandleToken returned true, and
        //it returned the type check.
        X509Certificate[] x509Certificates =
                marshalBinarySecurityTokenToCertArray((BinarySecurityTokenType)validateTarget.getToken());

        try {
            authenticationHandler.authenticate(makeRequestData(tokenParameters), x509Certificates);
            /*
            a successful call to the authenticationHandler will put the sessionId in the tokenCache. Pull it
            out and use it to obtain the principal corresponding to the Session.
             */
            Principal principal = principalFromSession.getPrincipalFromSession(threadLocalAMTokenCache.getAMToken());
            response.setPrincipal(principal);
            validateTarget.setState(ReceivedToken.STATE.VALID);
        } catch (TokenValidationException e) {
            throw new AMSTSRuntimeException(e.getCode(),
                    "Exception caught validating X509 token with authentication handler: " + e, e);
        } catch (TokenCreationException e) {
            throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR,
                    "No OpenAM Session token cached: " + e, e);
        }
        return response;
    }

    /*
     * Creates the RequestData object in a manner similar to the org.apache.cxf.sts.token.validator.UsernameTokenValidator.
     * As the name implies, the RequestData provides request context. For the AuthenticationHandler<OpenIDConnectToken>
     * encapsulated in this class, the entity which will dispatch the request to the OpenAM Rest authN context, this
     * data is not used, though it is provided simply to fulfill the contract.
     */
    private RequestData makeRequestData(TokenValidatorParameters parameters) {
        STSPropertiesMBean stsProperties = parameters.getStsProperties();
        RequestData requestData = new RequestData();
        requestData.setSigCrypto(stsProperties.getSignatureCrypto());
        requestData.setCallbackHandler(stsProperties.getCallbackHandler());
        return requestData;
    }

    /*
    Part of paying the 'tax' of running the rest-sts on the cxf-sts engine. The TokenRequestMarshallerImpl has marshaled the
    X509Cert set by the container or tls-offloader into a ReceivedToken, which requires a BinarySecurityTokenType, and the
    validator will  marshal this representation back to the X509Certificate required by the AuthenticationHandler. This is
    all because the cxf-sts wants its tokens represented in xml format.
     */
    private X509Certificate[] marshalBinarySecurityTokenToCertArray(BinarySecurityTokenType binarySecurityType) {
        try {
            final X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(
                    new ByteArrayInputStream(Base64.decode(binarySecurityType.getValue().getBytes(AMSTSConstants.UTF_8_CHARSET_ID))));
            return new X509Certificate[]{certificate};
        } catch (CertificateException e) {
            throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR,
                    "Could not marshal BinarySecurityTokenType back to X509Certificate: " + e, e);
        } catch (UnsupportedEncodingException e) {
            throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR,
                    "Could not marshal BinarySecurityTokenType back to X509Certificate: " + e, e);
        }
    }
}
