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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.openam.sts.rest.operation;

import static org.forgerock.guava.common.collect.Iterables.filter;
import static org.forgerock.guava.common.collect.Iterables.toArray;
import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.shared.encode.Base64;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.ArrayUtils;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.ClientContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.config.user.CustomTokenOperation;
import org.forgerock.openam.sts.rest.operation.translate.CustomRestTokenProviderParametersImpl;
import org.forgerock.openam.sts.rest.operation.translate.OpenIdConnectRestTokenProviderParameters;
import org.forgerock.openam.sts.rest.operation.translate.Saml2RestTokenProviderParameters;
import org.forgerock.openam.sts.rest.token.canceller.RestIssuedTokenCancellerParameters;
import org.forgerock.openam.sts.rest.token.provider.RestTokenProviderParameters;
import org.forgerock.openam.sts.rest.token.provider.oidc.OpenIdConnectTokenCreationState;
import org.forgerock.openam.sts.rest.token.provider.saml.Saml2TokenCreationState;
import org.forgerock.openam.sts.rest.token.validator.RestIssuedTokenValidatorParameters;
import org.forgerock.openam.sts.rest.token.validator.RestTokenTransformValidatorParameters;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.token.model.OpenAMSessionToken;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;
import org.forgerock.openam.sts.token.model.RestUsernameToken;
import org.forgerock.openam.sts.user.invocation.ProofTokenState;
import org.forgerock.openam.sts.user.invocation.SAML2TokenCreationState;
import org.forgerock.openam.sts.user.invocation.SAML2TokenState;
import org.forgerock.openam.utils.ClientUtils;
import org.slf4j.Logger;

/**
 * @see TokenRequestMarshaller
 */
public class TokenRequestMarshallerImpl implements TokenRequestMarshaller {
    private static final String X509_CERTIFICATE_ATTRIBUTE = "javax.servlet.request.X509Certificate";
    private static final String ANY_HOST = "any";
    private final String offloadedTlsClientCertKey;
    /*
    A list containing the IP addresses of the hosts trusted to present client certificates in headers. Will correspond
    to the TLS-offload engines fronting an OpenAM deployment.
     */
    private final Set<String> tlsOffloadEngineHosts;
    private final Set<CustomTokenOperation> customTokenValidators;
    private final Set<CustomTokenOperation> customTokenProviders;
    private final Logger logger;

    @Inject
    TokenRequestMarshallerImpl(@Named(AMSTSConstants.OFFLOADED_TWO_WAY_TLS_HEADER_KEY) String  offloadedTlsClientCertKey,
                               @Named(AMSTSConstants.TLS_OFFLOAD_ENGINE_HOSTS) Set<String> tlsOffloadEngineHosts,
                               @Named(AMSTSConstants.REST_CUSTOM_TOKEN_VALIDATORS) Set<CustomTokenOperation> customTokenValidators,
                               @Named(AMSTSConstants.REST_CUSTOM_TOKEN_PROVIDERS) Set<CustomTokenOperation> customTokenProviders,
                               Logger logger) {
        this.offloadedTlsClientCertKey = offloadedTlsClientCertKey;
        this.tlsOffloadEngineHosts = tlsOffloadEngineHosts;
        this.customTokenValidators = customTokenValidators;
        this.customTokenProviders = customTokenProviders;
        this.logger = logger;
    }

    @Override
    public RestTokenTransformValidatorParameters<?> buildTokenTransformValidatorParameters(
            JsonValue receivedToken, Context context)
                                                        throws TokenMarshalException {
        if (!receivedToken.get(AMSTSConstants.TOKEN_TYPE_KEY).isString()) {
            String message = "The to-be-translated token does not contain a " + AMSTSConstants.TOKEN_TYPE_KEY +
                    " entry. The token: " + receivedToken;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        }
        String tokenType = receivedToken.get(AMSTSConstants.TOKEN_TYPE_KEY).asString();
        if (TokenType.USERNAME.name().equals(tokenType)) {
            return buildUsernameTokenTransformValidatorParameters(receivedToken);
        } else if (TokenType.OPENAM.name().equals(tokenType)) {
            return buildAMSessionTokenTransformValidatorParameters(receivedToken);
        } else if (TokenType.OPENIDCONNECT.name().equals(tokenType)) {
            return buildOpenIdConnectIdTokenTransformValidatorParameters(receivedToken);
        } else if (TokenType.X509.name().equals(tokenType)) {
            return buildX509CertTokenTransformValidatorParameters(context);
        } else {
            for (CustomTokenOperation customTokenOperation : customTokenValidators) {
                if (tokenType.equals(customTokenOperation.getCustomTokenName())) {
                    return buildCustomTokenTransformValidatorParameters(receivedToken);
                }
            }
        }
        throw new TokenMarshalException(ResourceException.BAD_REQUEST, "Unsupported input token type: " + tokenType);
    }

    @Override
    public RestIssuedTokenValidatorParameters<?> buildIssuedTokenValidatorParameters(JsonValue receivedToken) throws TokenMarshalException {
        if (!receivedToken.get(AMSTSConstants.TOKEN_TYPE_KEY).isString()) {
            String message = "The to-be-validated token does not contain a " + AMSTSConstants.TOKEN_TYPE_KEY +
                    " entry. The token: " + receivedToken;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        }
        String tokenType = receivedToken.get(AMSTSConstants.TOKEN_TYPE_KEY).asString();
        if (TokenType.OPENIDCONNECT.getId().equals(tokenType)) {
            return buildOpenIdConnectIssuedTokenValidatorParameters(receivedToken);
        } else if (TokenType.SAML2.getId().equals(tokenType)) {
            return buildSAML2IssuedTokenValidatorParameters(receivedToken);
        } else if (tokenType ==  null) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "Invalid invocation state: invocation must " +
                    "specify a validated_token_state key containing json which specifies a token_type of either " +
                    "OPENIDCONNECT or SAML2, and the corresponding token value. See RestSTSTokenValidationInvocationState " +
                    "for details.") ;
        } else {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "Unsupported to-be-validated token type: " + tokenType);
        }
    }

    @Override
    public RestIssuedTokenCancellerParameters<?> buildIssuedTokenCancellerParameters(JsonValue receivedToken) throws TokenMarshalException {
        if (!receivedToken.get(AMSTSConstants.TOKEN_TYPE_KEY).isString()) {
            String message = "The to-be-cancelled token does not contain a " + AMSTSConstants.TOKEN_TYPE_KEY +
                    " entry. The token: " + receivedToken;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        }
        String tokenType = receivedToken.get(AMSTSConstants.TOKEN_TYPE_KEY).asString();
        if (TokenType.OPENIDCONNECT.getId().equals(tokenType)) {
            return buildOpenIdConnectIssuedTokenCancellerParameters(receivedToken);
        } else if (TokenType.SAML2.getId().equals(tokenType)) {
            return buildSAML2IssuedTokenCancellerParameters(receivedToken);
        } else if (tokenType ==  null) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "Invalid invocation state: invocation must " +
                    "specify a cancelled_token_state key containing json which specifies a token_type of either " +
                    "OPENIDCONNECT or SAML2, and the corresponding token value. See RestSTSTokenCancellationInvocationState " +
                    "for details.") ;
        } else {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "Unsupported to-be-cancelled token type: " + tokenType);
        }
    }

    @Override
    public RestTokenProviderParameters<?> buildTokenProviderParameters(
                                                                        TokenTypeId inputTokenType,
                                                                        JsonValue inputToken,
                                                                        TokenTypeId desiredTokenType,
                                                                        JsonValue desiredTokenState) throws TokenMarshalException {
        if (TokenType.SAML2.getId().equals(desiredTokenType.getId())) {
            return createSAML2TokenProviderParameters(inputTokenType, inputToken, desiredTokenState);
        } else if (TokenType.OPENIDCONNECT.getId().equals(desiredTokenType.getId())) {
            return createOpenIdConnectTokenProviderParameters(inputTokenType, inputToken, desiredTokenState);
        } else {
            for (CustomTokenOperation customTokenOperation : customTokenProviders) {
                if (desiredTokenType.getId().equals(customTokenOperation.getCustomTokenName())) {
                    return buildCustomTokenProviderParameters(inputTokenType, inputToken, desiredTokenState);
                }
            }
        }
        throw new TokenMarshalException(ResourceException.BAD_REQUEST, "Unsupported output token type: " + desiredTokenType);
    }


    @Override
    public TokenTypeId getTokenType(JsonValue receivedToken) throws TokenMarshalException {
        JsonValue jsonTokenType = receivedToken.get(AMSTSConstants.TOKEN_TYPE_KEY);
        if (jsonTokenType.isNull() || !jsonTokenType.isString()) {
            String message = "REST STS invocation does not contain " + AMSTSConstants.TOKEN_TYPE_KEY +
                    " String entry. The json token: " + receivedToken;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        }
        final String tokenType = jsonTokenType.asString();
        return new TokenTypeId() {
            @Override
            public String getId() {
                return tokenType;
            }
        };
    }

    private SAML2SubjectConfirmation getSubjectConfirmation(JsonValue token) throws TokenMarshalException {
        try {
            SAML2TokenCreationState tokenState = SAML2TokenCreationState.fromJson(token);
            return tokenState.getSubjectConfirmation();
        } catch (TokenMarshalException e) {
            /*
            Try to get the value directly
             */
            String subjectConfirmationString = token.get(SAML2TokenCreationState.SUBJECT_CONFIRMATION).asString();
            try {
                return SAML2SubjectConfirmation.valueOf(subjectConfirmationString);
            } catch (IllegalArgumentException iae) {
                throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                        "Invalid subjectConfirmation specified in the JsonValue corresponding to SAML2TokenCreationState. " +
                                "The JsonValue: " + token.toString());
            } catch (NullPointerException npe) {
                throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                        "No subjectConfirmation specified in the JsonValue corresponding to SAML2TokenCreationState. " +
                                "The JsonValue: " + token.toString());
            }
        }
    }

    private ProofTokenState getProofTokenState(JsonValue token) throws TokenMarshalException {
        final SAML2TokenCreationState tokenState = SAML2TokenCreationState.fromJson(token);
        final ProofTokenState proofTokenState = tokenState.getProofTokenState();
        if (proofTokenState ==  null) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "No ProofTokenState specified in the" +
                    " SAML2TokenCreationState. The JsonValue: " + token);
        } else {
            return proofTokenState;
        }
    }

    private RestIssuedTokenValidatorParameters<OpenIdConnectIdToken> buildOpenIdConnectIssuedTokenValidatorParameters(JsonValue receivedToken) throws TokenMarshalException {
        if (!receivedToken.get(AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY).isString()) {
            String message = "Exception: json representation of a to-be-validated OIDC token does not contain a "
                    + AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY + " field containing the " +
                    "to-be-validated token. The representation: " + receivedToken;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        } else {
            final String tokenValue = receivedToken.get(AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY).asString();
            final OpenIdConnectIdToken openIdConnectIdToken = new OpenIdConnectIdToken(tokenValue);
            return new RestIssuedTokenValidatorParameters<OpenIdConnectIdToken>() {
                @Override
                public OpenIdConnectIdToken getInputToken() {
                    return openIdConnectIdToken;
                }
            };
        }
    }

    private RestIssuedTokenValidatorParameters<SAML2TokenState> buildSAML2IssuedTokenValidatorParameters(JsonValue receivedToken) throws TokenMarshalException {
        if (!receivedToken.get(AMSTSConstants.SAML2_TOKEN_KEY).isString()) {
            String message = "Exception: json representation of a to-be-validated SAML2 token does not contain a "
                    + AMSTSConstants.SAML2_TOKEN_KEY + " field containing the " +
                    "to-be-validated token. The representation: " + receivedToken;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        } else {
            final String tokenValue = receivedToken.get(AMSTSConstants.SAML2_TOKEN_KEY).asString();
            final SAML2TokenState saml2TokenState = SAML2TokenState.builder().tokenValue(tokenValue).build();
            return new RestIssuedTokenValidatorParameters<SAML2TokenState>() {
                @Override
                public SAML2TokenState getInputToken() {
                    return saml2TokenState;
                }
            };
        }
    }

    private RestIssuedTokenCancellerParameters<OpenIdConnectIdToken> buildOpenIdConnectIssuedTokenCancellerParameters(JsonValue receivedToken) throws TokenMarshalException {
        if (!receivedToken.get(AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY).isString()) {
            String message = "Exception: json representation of a to-be-cancelled OIDC token does not contain a "
                    + AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY + " field containing the " +
                    "to-be-cancelled token. The representation: " + receivedToken;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        } else {
            final String tokenValue = receivedToken.get(AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY).asString();
            final OpenIdConnectIdToken openIdConnectIdToken = new OpenIdConnectIdToken(tokenValue);
            return new RestIssuedTokenCancellerParameters<OpenIdConnectIdToken>() {
                @Override
                public OpenIdConnectIdToken getInputToken() {
                    return openIdConnectIdToken;
                }
            };
        }
    }

    private RestIssuedTokenCancellerParameters<SAML2TokenState> buildSAML2IssuedTokenCancellerParameters(JsonValue receivedToken) throws TokenMarshalException {
        if (!receivedToken.get(AMSTSConstants.SAML2_TOKEN_KEY).isString()) {
            String message = "Exception: json representation of a to-be-cancelled SAML2 token does not contain a "
                    + AMSTSConstants.SAML2_TOKEN_KEY + " field containing the " +
                    "to-be-cancelled token. The representation: " + receivedToken;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        } else {
            final String tokenValue = receivedToken.get(AMSTSConstants.SAML2_TOKEN_KEY).asString();
            final SAML2TokenState saml2TokenState = SAML2TokenState.builder().tokenValue(tokenValue).build();
            return new RestIssuedTokenCancellerParameters<SAML2TokenState>() {
                @Override
                public SAML2TokenState getInputToken() {
                    return saml2TokenState;
                }
            };
        }
    }

    private RestTokenTransformValidatorParameters<RestUsernameToken> buildUsernameTokenTransformValidatorParameters(JsonValue receivedToken) throws TokenMarshalException {
        if (!receivedToken.get(AMSTSConstants.USERNAME_TOKEN_USERNAME).isString()) {
            final String message = "Exception: json representation of UNT does not contain a username field. The representation: "
                    + receivedToken;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        }
        if (!receivedToken.get(AMSTSConstants.USERNAME_TOKEN_PASSWORD).isString()) {
            final String message = "Exception: json representation of UNT does not contain a password field. The representation: \n"
                    + receivedToken;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        }
        final String username = receivedToken.get(AMSTSConstants.USERNAME_TOKEN_USERNAME).asString();
        final String password = receivedToken.get(AMSTSConstants.USERNAME_TOKEN_PASSWORD).asString();

        try {
            final RestUsernameToken restUsernameToken =
                    new RestUsernameToken(username.getBytes(AMSTSConstants.UTF_8_CHARSET_ID), password.getBytes(AMSTSConstants.UTF_8_CHARSET_ID));
            return new RestTokenTransformValidatorParameters<RestUsernameToken>() {
                @Override
                public RestUsernameToken getInputToken() {
                    return restUsernameToken;
                }
            };
        } catch (UnsupportedEncodingException e) {
            throw new TokenMarshalException(ResourceException.INTERNAL_ERROR,
                    "Unable to marshal username token state to strings: " + e.getMessage(), e);
        }
    }

    private RestTokenTransformValidatorParameters<OpenAMSessionToken> buildAMSessionTokenTransformValidatorParameters(JsonValue receivedToken) throws TokenMarshalException {
        if (!receivedToken.get(AMSTSConstants.AM_SESSION_TOKEN_SESSION_ID).isString()) {
            String message = "Exception: json representation of AM Session Token does not contain a session_id field. " +
                    "The representation: " + receivedToken;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        } else {
            final String sessionId = receivedToken.get(AMSTSConstants.AM_SESSION_TOKEN_SESSION_ID).asString();
            final OpenAMSessionToken openAMSessionToken = new OpenAMSessionToken(sessionId);
            return new RestTokenTransformValidatorParameters<OpenAMSessionToken>() {
                @Override
                public OpenAMSessionToken getInputToken() {
                    return openAMSessionToken;
                }
            };
        }
    }

    private RestTokenTransformValidatorParameters<OpenIdConnectIdToken> buildOpenIdConnectIdTokenTransformValidatorParameters(JsonValue receivedToken)
            throws TokenMarshalException {
        if (!receivedToken.get(AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY).isString()) {
            String message = "Exception: json representation of Open ID Connect ID Token does not contain a "
                    + AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY + " field. The representation: " + receivedToken;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        } else {
            final String tokenValue = receivedToken.get(AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY).asString();
            final OpenIdConnectIdToken openIdConnectIdToken = new OpenIdConnectIdToken(tokenValue);
            return new RestTokenTransformValidatorParameters<OpenIdConnectIdToken>() {
                @Override
                public OpenIdConnectIdToken getInputToken() {
                    return openIdConnectIdToken;
                }
            };
        }
    }

    /**
     * For token transformations with x509 certificates as an input token type, a client's identity can only be asserted
     * via x509 certificates presented via two-way-tls. This certificate can be obtained via the attribute referenced by
     * the javax.servlet.request.X509Certificate key (if the container is deployed with two-way-tls), or from the header
     * referenced by offloadedTlsClientCertKey, in case OpenAM is deployed behind infrastructure which performs tls-offloading.
     * This method will consult header value if configured for this rest-sts instance, and if not configured, the
     * ClientInfoContxt will be consulted, which contains the state corresponding to the javax.servlet.request.X509Certificate attribute.
     * An exception will be thrown if the client cert cannot be obtained.
     * @param context The Context instance corresponding to this invocation
     * @throws org.forgerock.openam.sts.TokenMarshalException if the client's X509 token cannot be obtained from the
     * javax.servlet.request.X509Certificate attribute, or from the header referenced by the offloadedTlsClientCertKey value.
     * @return a RestTokenTransformValidatorParameters instance with a X509Certificate[] generic type.
     */
    private RestTokenTransformValidatorParameters<X509Certificate[]> buildX509CertTokenTransformValidatorParameters(
            Context context) throws TokenMarshalException {

        X509Certificate[] certificates;
        /*
        In non-offloaded tls deployments, the offloadedTlsClientCertKey won't be set in the RestSTSInstanceConfig. But
        because this value is injected, and the @Nullable attribute is not available(and thus null references cannot be
        injected), the @Provides method in the RestSTSInstanceModule will return "" if this value has not been set,
        so I will check to insure that this value has indeed been specified.
         */
        if (!"".equals(offloadedTlsClientCertKey)) {
            String clientIpAddress = ClientUtils.getClientIPAddress(context);
            if (!tlsOffloadEngineHosts.contains(clientIpAddress) && !tlsOffloadEngineHosts.contains(ANY_HOST)) {
                logger.error("A x509-based token transformation is being rejected because the client cert was to be referenced in " +
                        "the  " + offloadedTlsClientCertKey + " header, but the caller was not in the list of TLS offload engines." +
                        " The caller: " + clientIpAddress +
                        "; The list of TLS offload engine hosts: " + tlsOffloadEngineHosts);
                throw new TokenMarshalException(ResourceException.BAD_REQUEST, "In a x509 Certificate token transformation, " +
                        " the caller was not among the list of IP addresses corresponding to the TLS offload-engine hosts. " +
                        "Insure that your published rest-sts instance is configured with a complete list of TLS offload-engine hosts.");
            }
            certificates = pullClientCertFromHeader(context.asContext(HttpContext.class));
        } else {
            certificates = pullClientCertFromRequestAttribute(context.asContext(ClientContext.class));
        }

        if (!ArrayUtils.isEmpty(certificates)) {
            return marshalX509CertIntoTokenValidatorParameters(certificates);
        } else {
            if ("".equals(offloadedTlsClientCertKey)) {
                throw new TokenMarshalException(ResourceException.BAD_REQUEST, "A token transformation specifying an " +
                        "x509 token as input must be consumed via two-way-tls. No header was specified referencing the " +
                        "certificate, and the client's certificate was not found in the " +
                        "javax.servlet.request.X509Certificate attribute.");
            } else {
                throw new TokenMarshalException(ResourceException.BAD_REQUEST, "A token transformation specifying an " +
                        "x509 token as input must be consumed via two-way-tls. The " + offloadedTlsClientCertKey +
                        " header was specified in the rest-sts instance configuration as referencing the " +
                        "certificate, yet no certificate was found referenced by this header value.");
            }
        }
    }

    private X509Certificate[] pullClientCertFromRequestAttribute(ClientContext context) throws TokenMarshalException {
        // Filter the certs on the request to just the X509 ones and return as an array.
        return toArray(filter(context.getCertificates(), X509Certificate.class), X509Certificate.class);
    }

    private X509Certificate[] pullClientCertFromHeader(HttpContext httpContext) throws TokenMarshalException {
        List<String> clientCertHeader = httpContext.getHeader(offloadedTlsClientCertKey);
        if (clientCertHeader.isEmpty()) {
            return null;
        } else {
            int ndx = 0;
            X509Certificate[] certificates = new X509Certificate[clientCertHeader.size()];
            final CertificateFactory certificateFactory;
            try {
                certificateFactory = CertificateFactory.getInstance("X.509");
            } catch (CertificateException e) {
                throw new TokenMarshalException(ResourceException.INTERNAL_ERROR,
                        "Exception caught creating X.509 CertificateFactory: " + e, e);
            }
            for (String headerCertValue : clientCertHeader) {
                try {
                    certificates[ndx++] = (X509Certificate)certificateFactory.generateCertificate(
                            new ByteArrayInputStream(Base64.decode(headerCertValue.getBytes(AMSTSConstants.UTF_8_CHARSET_ID))));
                } catch (CertificateException | UnsupportedEncodingException e) {
                    throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                            "Exception caught marshalling X509 cert from value set in " + offloadedTlsClientCertKey + " header: " + e, e);
                }
            }
            return certificates;
        }
    }

    private RestTokenTransformValidatorParameters<X509Certificate[]> marshalX509CertIntoTokenValidatorParameters(
                                        final X509Certificate[] x509Certificates) throws TokenMarshalException {
        return new RestTokenTransformValidatorParameters<X509Certificate[]>() {
            @Override
            public X509Certificate[] getInputToken() {
                return x509Certificates;
            }
        };
    }

    private RestTokenTransformValidatorParameters<JsonValue> buildCustomTokenTransformValidatorParameters(final JsonValue inputToken) {
        return new RestTokenTransformValidatorParameters<JsonValue>() {
            @Override
            public JsonValue getInputToken() {
                return inputToken;
            }
        };
    }

    private RestTokenProviderParameters<Saml2TokenCreationState> createSAML2TokenProviderParameters(
                                                                                        final TokenTypeId inputTokenType,
                                                                                        final JsonValue inputToken,
                                                                                        final JsonValue desiredToken)
                                                                                        throws TokenMarshalException {
        final SAML2SubjectConfirmation subjectConfirmation = getSubjectConfirmation(desiredToken);
        if (SAML2SubjectConfirmation.HOLDER_OF_KEY.equals(subjectConfirmation)) {
            final ProofTokenState proofTokenState = getProofTokenState(desiredToken);
            final Saml2TokenCreationState saml2TokenCreationState = new Saml2TokenCreationState(subjectConfirmation, proofTokenState);
            return new Saml2RestTokenProviderParameters(saml2TokenCreationState, inputTokenType, inputToken);
        } else {
            final Saml2TokenCreationState saml2TokenCreationState = new Saml2TokenCreationState(subjectConfirmation);
            return new Saml2RestTokenProviderParameters(saml2TokenCreationState, inputTokenType, inputToken);
        }
    }

    private RestTokenProviderParameters<OpenIdConnectTokenCreationState> createOpenIdConnectTokenProviderParameters(
                                                                        final TokenTypeId inputTokenType,
                                                                        final JsonValue inputToken,
                                                                        final JsonValue desiredToken)
                                                                        throws TokenMarshalException {
        org.forgerock.openam.sts.user.invocation.OpenIdConnectTokenCreationState userSpecifiedTokenCreationState =
                org.forgerock.openam.sts.user.invocation.OpenIdConnectTokenCreationState.fromJson(desiredToken);
        if (!userSpecifiedTokenCreationState.getAllowAccess()) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "The OpenIdConnectTokenCreation state must " +
                    "indicate access to the caller's identity with a field of allow_access:true.");
        }
        final OpenIdConnectTokenCreationState openIdConnectTokenCreationState =
                new OpenIdConnectTokenCreationState(userSpecifiedTokenCreationState.getNonce(), currentTimeMillis() / 1000);
        return new OpenIdConnectRestTokenProviderParameters(openIdConnectTokenCreationState, inputTokenType, inputToken);
    }

    private RestTokenProviderParameters<JsonValue> buildCustomTokenProviderParameters(final TokenTypeId inputTokenType,
                                                                                      final JsonValue inputToken,
                                                                                      final JsonValue tokenCreationState) {
        return new CustomRestTokenProviderParametersImpl(tokenCreationState, inputTokenType, inputToken);
    }
}
