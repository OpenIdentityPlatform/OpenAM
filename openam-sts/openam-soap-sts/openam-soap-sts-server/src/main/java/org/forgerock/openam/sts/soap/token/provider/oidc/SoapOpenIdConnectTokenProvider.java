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
* Copyright 2015-2016 ForgeRock AS.
*/

package org.forgerock.openam.sts.soap.token.provider.oidc;

import static org.forgerock.openam.utils.Time.*;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.message.token.BinarySecurity;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.JwtReconstructionException;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.AMSTSRuntimeException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSAccessTokenProvider;
import org.forgerock.openam.sts.soap.token.provider.SoapTokenProviderBase;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.provider.AMSessionInvalidator;
import org.forgerock.openam.sts.token.provider.TokenServiceConsumer;
import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Set;

/**
 * Responsible for issuing OpenIdConnect tokens in the soap-sts context.
 */
public class SoapOpenIdConnectTokenProvider extends SoapTokenProviderBase {
    /*
    OpenIdConnect defines a nonce claim, which must be sent by the client in the Authentication Request, and which will
    be included in the issued OIDC token. See http://openid.net/specs/openid-connect-core-1_0.html#Claims for details.
    The question is how to include this state in the WS-Trust defined RequestSecurityToken request in an interoperable fashion.
    Certainly WS-Trust defines numerous xs:any extension points for the RST, but actually supporting this in a manner
    consumable across soap-toolkits may be problematic. For the moment, the nonce will simply be set to null, as it is
    an optional claim.
     */
    private static final String NULL_NONCE = null;
    public static class SoapOpenIdConnectTokenProviderBuilder {
        private TokenServiceConsumer tokenServiceConsumer;
        private AMSessionInvalidator amSessionInvalidator;
        private ThreadLocalAMTokenCache threadLocalAMTokenCache;
        private String stsInstanceId;
        private String realm;
        private XMLUtilities xmlUtilities;
        private SoapSTSAccessTokenProvider soapSTSAccessTokenProvider;
        private SoapOpenIdConnectTokenAuthnContextMapper authnContextMapper;
        private SoapOpenIdConnectTokenAuthnMethodsReferencesMapper methodsReferencesMapper;
        private Logger logger;

        private SoapOpenIdConnectTokenProviderBuilder() {}

        public SoapOpenIdConnectTokenProviderBuilder tokenGenerationServiceConsumer(TokenServiceConsumer tokenServiceConsumer) {
            this.tokenServiceConsumer = tokenServiceConsumer;
            return this;
        }

        public SoapOpenIdConnectTokenProviderBuilder amSessionInvalidator(AMSessionInvalidator amSessionInvalidator) {
            this.amSessionInvalidator = amSessionInvalidator;
            return this;
        }

        public SoapOpenIdConnectTokenProviderBuilder threadLocalAMTokenCache(ThreadLocalAMTokenCache threadLocalAMTokenCache) {
            this.threadLocalAMTokenCache = threadLocalAMTokenCache;
            return this;
        }

        public SoapOpenIdConnectTokenProviderBuilder stsInstanceId(String stsInstanceId) {
            this.stsInstanceId = stsInstanceId;
            return this;
        }

        public SoapOpenIdConnectTokenProviderBuilder realm(String realm) {
            this.realm = realm;
            return this;
        }

        public SoapOpenIdConnectTokenProviderBuilder xmlUtilities(XMLUtilities xmlUtilities) {
            this.xmlUtilities = xmlUtilities;
            return this;
        }

        public SoapOpenIdConnectTokenProviderBuilder soapSTSAccessTokenProvider(SoapSTSAccessTokenProvider soapSTSAccessTokenProvider) {
            this.soapSTSAccessTokenProvider = soapSTSAccessTokenProvider;
            return this;
        }

        public SoapOpenIdConnectTokenProviderBuilder authenticationContextReferenceMapper(SoapOpenIdConnectTokenAuthnContextMapper authnContextMapper) {
            this.authnContextMapper = authnContextMapper;
            return this;
        }

        public SoapOpenIdConnectTokenProviderBuilder authenticationMethodsReferencesMapper(SoapOpenIdConnectTokenAuthnMethodsReferencesMapper methodsReferencesMapper) {
            this.methodsReferencesMapper = methodsReferencesMapper;
            return this;
        }

        public SoapOpenIdConnectTokenProviderBuilder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public SoapOpenIdConnectTokenProvider build() {
            return new SoapOpenIdConnectTokenProvider(this);
        }
    }

    /*
    A BinarySecurity subclass, which will facilitate the encapsulation of an issued OpenIdConnectToken in a
    BinarySecurityToken.
     */
    private static class OpenIdConnectTokenBST extends BinarySecurity {
        OpenIdConnectTokenBST(Document doc) {
            super(doc);
            setValueType(AMSTSConstants.AM_OPEN_ID_CONNECT_TOKEN_ASSERTION_TYPE);
        }

        /**
         * The BinarySecurity#setData method is used to set the binary data, but it base64-encodes by default. Adding a method
         * to set the token data, without any encoding.
         * @param oidcToken the OpenIdConnect token to-be-included in the BST.
         */
        void setToken(String oidcToken) {
            getFirstNode().setData(oidcToken);
        }
    }

    private final TokenServiceConsumer tokenServiceConsumer;
    private final AMSessionInvalidator amSessionInvalidator;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final String stsInstanceId;
    private final String realm;
    private final SoapOpenIdConnectTokenAuthnContextMapper authnContextMapper;
    private final SoapOpenIdConnectTokenAuthnMethodsReferencesMapper methodsReferencesMapper;


    /*
    ctor not injected as this class created by TokenOperationFactoryImpl
     */
    private SoapOpenIdConnectTokenProvider(SoapOpenIdConnectTokenProviderBuilder builder) {
        super(builder.soapSTSAccessTokenProvider, builder.xmlUtilities, builder.logger);
        this.tokenServiceConsumer = builder.tokenServiceConsumer;
        this.amSessionInvalidator = builder.amSessionInvalidator;
        this.threadLocalAMTokenCache = builder.threadLocalAMTokenCache;
        this.stsInstanceId = builder.stsInstanceId;
        this.realm = builder.realm;
        this.authnContextMapper = builder.authnContextMapper;
        this.methodsReferencesMapper = builder.methodsReferencesMapper;
    }
    @Override
    public boolean canHandleToken(String tokenType) {
        return canHandleToken(tokenType, null);
    }

    @Override
    public boolean canHandleToken(String tokenType, String realm) {
        return AMSTSConstants.AM_OPEN_ID_CONNECT_TOKEN_ASSERTION_TYPE.equals(tokenType);
    }

    @Override
    public TokenProviderResponse createToken(TokenProviderParameters tokenProviderParameters) {
        try {
            final TokenProviderResponse tokenProviderResponse = new TokenProviderResponse();
            final SoapTokenProviderBase.AuthenticationContextMapperState mapperState =
                    getAuthenticationContextMapperState(tokenProviderParameters);
            String authNContextClassRef;
            Set<String> authNMethodsReferences;
            final List<WSHandlerResult> securityPolicyBindingTraversalYield = mapperState.getSecurityPolicyBindingTraversalYield();
            if (mapperState.isDelegatedContext()) {
                final ReceivedToken delegatedToken = mapperState.getDelegatedToken();
                authNContextClassRef = authnContextMapper.getAuthnContextForDelegatedToken(
                        securityPolicyBindingTraversalYield, delegatedToken);
                authNMethodsReferences = methodsReferencesMapper.getAuthnMethodsReferencesForDelegatedToken(
                        securityPolicyBindingTraversalYield, delegatedToken);
            } else {
                authNContextClassRef = authnContextMapper.getAuthnContext(securityPolicyBindingTraversalYield);
                authNMethodsReferences = methodsReferencesMapper.getAuthnMethodsReferences(securityPolicyBindingTraversalYield);
            }

            String token;
            try {
                token = getAssertion(getValidationInvocationContext(tokenProviderParameters), authNContextClassRef,
                        authNMethodsReferences, currentTimeMillis() / 1000, NULL_NONCE);
                Element tokenElement = buildTokenElement(token);
                tokenProviderResponse.setToken(tokenElement);
                tokenProviderResponse.setTokenId(getTokenId(token));
                return tokenProviderResponse;
            } catch (TokenCreationException e) {
                throw new AMSTSRuntimeException(e.getCode(), e.getMessage(), e);
            }
        } finally {
            try {
                amSessionInvalidator.invalidateAMSessions(threadLocalAMTokenCache.getToBeInvalidatedAMSessionIds());
            } catch (Exception e) {
                String message = "Exception caught invalidating interim AMSession in SoapOpenIdConnectTokenProvider: " + e;
                logger.warn(message, e);
                /*
                The fact that the interim OpenAM session was not invalidated should not prevent a token from being issued, so
                I will not throw a AMSTSRuntimeException
                */
            }
        }
    }

    private String getAssertion(ValidationInvocationContext validationInvocationContext, String authNContextClassRef,
                                Set<String> authenticationMethodReferences, long authTimeInSeconds,
                                String nonce) throws TokenCreationException {
        return tokenServiceConsumer.getOpenIdConnectToken(
                threadLocalAMTokenCache.getSessionIdForContext(validationInvocationContext),
                stsInstanceId, realm, authNContextClassRef, authenticationMethodReferences,
                authTimeInSeconds, nonce, getTokenGenerationServiceConsumptionToken());
    }

    /*
    Calls a superclass method to determine this is an issue operation with ActAs/OnBehalfOf elements, which will determine
    which OpenAM session, cached in the ThreadLocalAMTokenCache, will be used to assert the identity in the issued OIDC
    token.
     */
    private ValidationInvocationContext getValidationInvocationContext(TokenProviderParameters tokenProviderParameters) {
        if (isDelegatedIssueOperation(tokenProviderParameters)) {
            return ValidationInvocationContext.SOAP_TOKEN_DELEGATION;
        }
        return ValidationInvocationContext.SOAP_SECURITY_POLICY;
    }

    /*
    The TokenProviderResponse returned by the createToken method represents the created token as an Element. This method
    will take the OIDC token, and turn it into a BinarySecurityToken element encapsulating the OIDC token, with the appropriate
    BinarySecurityToken ValueType.
     */
    private Element buildTokenElement(String oidcToken) {
        OpenIdConnectTokenBST bst = new OpenIdConnectTokenBST(DOMUtils.createDocument());
        bst.setToken(oidcToken);
        return bst.getElement();
    }

    private String getTokenId(String oidcToken) throws TokenCreationException {
        try {
            SignedJwt jwt = new JwtReconstruction().reconstructJwt(oidcToken, SignedJwt.class);
            return jwt.getClaimsSet().getJwtId();
        } catch ( JwtReconstructionException | JsonValueException e) {
            String message = "SoapOpenIdConnectTokenProvider: unable to reconstruct newly-issued oidc token: " + e.getMessage();
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR, message, e);
        }
    }

    public static SoapOpenIdConnectTokenProviderBuilder builder() {
        return new SoapOpenIdConnectTokenProviderBuilder();
    }
}
