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

package org.forgerock.openam.sts.rest.marshal;

import com.sun.identity.shared.encode.Base64;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.rest.service.RestSTSServiceHttpServletContext;
import org.forgerock.openam.sts.rest.token.provider.RestTokenProviderParameters;
import org.forgerock.openam.sts.rest.token.provider.Saml2TokenCreationState;
import org.forgerock.openam.sts.rest.token.validator.RestTokenValidatorParameters;
import org.forgerock.openam.sts.service.invocation.ProofTokenState;
import org.forgerock.openam.sts.service.invocation.SAML2TokenState;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.token.model.OpenAMSessionToken;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;
import org.forgerock.openam.sts.token.model.RestUsernameToken;
import org.forgerock.openam.utils.ClientUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

/**
 * @see org.forgerock.openam.sts.rest.marshal.TokenRequestMarshaller
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
    private final Logger logger;

    @Inject
    TokenRequestMarshallerImpl(@Named(AMSTSConstants.OFFLOADED_TWO_WAY_TLS_HEADER_KEY) String  offloadedTlsClientCertKey,
                               @Named(AMSTSConstants.TLS_OFFLOAD_ENGINE_HOSTS) Set<String> tlsOffloadEngineHosts,
                               Logger logger) {
        this.offloadedTlsClientCertKey = offloadedTlsClientCertKey;
        this.tlsOffloadEngineHosts = tlsOffloadEngineHosts;
        this.logger = logger;
    }

    @Override
    public RestTokenValidatorParameters<?> buildTokenValidatorParameters(
                                                        JsonValue receivedToken, HttpContext httpContext,
                                                        RestSTSServiceHttpServletContext restSTSServiceHttpServletContext)
                                                        throws TokenMarshalException {
        if (!receivedToken.get(AMSTSConstants.TOKEN_TYPE_KEY).isString()) {
            String message = "The to-be-translated token does not contain a " + AMSTSConstants.TOKEN_TYPE_KEY +
                    " entry. The token: " + receivedToken;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        }
        String tokenType = receivedToken.get(AMSTSConstants.TOKEN_TYPE_KEY).asString();
        if (TokenType.USERNAME.name().equals(tokenType)) {
            return buildUsernameTokenValidatorParameters(receivedToken);
        } else if (TokenType.OPENAM.name().equals(tokenType)) {
            return buildAMSessionTokenValidatorParameters(receivedToken);
        } else if (TokenType.OPENIDCONNECT.name().equals(tokenType)) {
            return buildOpenIdConnectIdTokenValidatorParameters(receivedToken);
        } else if (TokenType.X509.name().equals(tokenType)) {
            return buildX509CertTokenValidatorParameters(httpContext, restSTSServiceHttpServletContext);
        }

        throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                "Unsupported token translation operation for token: " + receivedToken);

    }

    @Override
    public RestTokenProviderParameters<? extends TokenTypeId> buildTokenProviderParameters(
                                                                        TokenTypeId inputTokenType,
                                                                        JsonValue inputToken,
                                                                        TokenTypeId desiredTokenType,
                                                                        JsonValue desiredTokenState) throws TokenMarshalException {
        if (TokenType.SAML2.equals(desiredTokenType)) {
            return createSAML2TokenProviderParameters(inputTokenType, inputToken, desiredTokenState);
        } else {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "Unsupported output token type: " + desiredTokenType);
        }
    }


    @Override
    public TokenTypeId getTokenType(JsonValue receivedToken) throws TokenMarshalException {
        JsonValue jsonTokenType = receivedToken.get(AMSTSConstants.TOKEN_TYPE_KEY);
        if (jsonTokenType.isNull() || !jsonTokenType.isString()) {
            String message = "REST STS invocation does not contain " + AMSTSConstants.TOKEN_TYPE_KEY +
                    " String entry. The json token: " + receivedToken;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        }
        try {
            return TokenType.valueOf(jsonTokenType.asString());
        } catch (IllegalArgumentException e) {
            //TODO: If we are dealing with a custom token type (AME-6554), we would enter this branch
            //part of implementing a custom token type would involve some sort of function that takes a string, and returns a TokenTypeId impl-
            //or perhaps there is simply a CustomTokenType class that takes the string as a ctor parameter??
            String message = "Error marshalling from " + AMSTSConstants.TOKEN_TYPE_KEY +
                    " value to token-type enum. The json token: " + receivedToken + " The exception: " + e;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        } catch (NullPointerException e) {
            String message = "Error marshalling from " + AMSTSConstants.TOKEN_TYPE_KEY +
                    " value to token-type enum. The json token: " + receivedToken + " The exception: " + e;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        }
    }

    private SAML2SubjectConfirmation getSubjectConfirmation(JsonValue token) throws TokenMarshalException {
        try {
            SAML2TokenState tokenState = SAML2TokenState.fromJson(token);
            return tokenState.getSubjectConfirmation();
        } catch (TokenMarshalException e) {
            /*
            Try to get the value directly
             */
            String subjectConfirmationString = token.get(SAML2TokenState.SUBJECT_CONFIRMATION).asString();
            try {
                return SAML2SubjectConfirmation.valueOf(subjectConfirmationString);
            } catch (IllegalArgumentException iae) {
                throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                        "Invalid subjectConfirmation specified in the JsonValue corresponding to SAML2TokenState. " +
                                "The JsonValue: " + token.toString());
            } catch (NullPointerException npe) {
                throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                        "No subjectConfirmation specified in the JsonValue corresponding to SAML2TokenState. " +
                                "The JsonValue: " + token.toString());
            }
        }
    }

    private ProofTokenState getProofTokenState(JsonValue token) throws TokenMarshalException {
        final SAML2TokenState tokenState = SAML2TokenState.fromJson(token);
        final ProofTokenState proofTokenState = tokenState.getProofTokenState();
        if (proofTokenState ==  null) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "No ProofTokenState specified in the" +
                    " SAML2TokenState. The JsonValue: " + token);
        } else {
            return proofTokenState;
        }
    }

    private RestTokenValidatorParameters<RestUsernameToken> buildUsernameTokenValidatorParameters(JsonValue receivedToken) throws TokenMarshalException {
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
            return new RestTokenValidatorParameters<RestUsernameToken>() {
                @Override
                public String getId() {
                    return TokenType.USERNAME.getId();
                }

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

    private RestTokenValidatorParameters<OpenAMSessionToken> buildAMSessionTokenValidatorParameters(JsonValue receivedToken) throws TokenMarshalException {
        if (!receivedToken.get(AMSTSConstants.AM_SESSION_TOKEN_SESSION_ID).isString()) {
            String message = "Exception: json representation of AM Session Token does not contain a session_id field. " +
                    "The representation: " + receivedToken;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        } else {
            final String sessionId = receivedToken.get(AMSTSConstants.AM_SESSION_TOKEN_SESSION_ID).asString();
            final OpenAMSessionToken openAMSessionToken = new OpenAMSessionToken(sessionId);
            return new RestTokenValidatorParameters<OpenAMSessionToken>() {
                @Override
                public OpenAMSessionToken getInputToken() {
                    return openAMSessionToken;
                }

                @Override
                public String getId() {
                    return TokenType.OPENAM.getId();
                }
            };
        }
    }

    private RestTokenValidatorParameters<OpenIdConnectIdToken> buildOpenIdConnectIdTokenValidatorParameters(JsonValue receivedToken)
            throws TokenMarshalException {
        if (!receivedToken.get(AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY).isString()) {
            String message = "Exception: json representation of Open ID Connect ID Token does not contain a "
                    + AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY + " field. The representation: " + receivedToken;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        } else {
            final String tokenValue = receivedToken.get(AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY).asString();
            final OpenIdConnectIdToken openIdConnectIdToken = new OpenIdConnectIdToken(tokenValue);
            return new RestTokenValidatorParameters<OpenIdConnectIdToken>() {
                @Override
                public OpenIdConnectIdToken getInputToken() {
                    return openIdConnectIdToken;
                }

                @Override
                public String getId() {
                    return TokenType.OPENIDCONNECT.getId();
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
     * javax.servlet.request.X509Certificate attribute will be consulted.
     * An exception will be thrown if the client cert cannot be obtained.
     * @param httpContext The HttpContext instance corresponding to this invocation
     * @param restSTSServiceHttpServletContext The AbstractContext instance which provides access to the HttpServletRequest,
     *                                         and with it, access to the client cert presented via two-way-tls
     * @throws org.forgerock.openam.sts.TokenMarshalException if the client's X509 token cannot be obtained from the
     * javax.servlet.request.X509Certificate attribute, or from the header referenced by the offloadedTlsClientCertKey value.
     * @return a RestTokenValidatorParameters instance with a X509Certificate[] generic type.
     */
    private RestTokenValidatorParameters<X509Certificate[]> buildX509CertTokenValidatorParameters(HttpContext httpContext, RestSTSServiceHttpServletContext
            restSTSServiceHttpServletContext) throws TokenMarshalException {

        X509Certificate[] certificates;
        /*
        In non-offloaded tls deployments, the offloadedTlsClientCertKey won't be set in the RestSTSInstanceConfig. But
        because this value is injected, and the @Nullable attribute is not available(and thus null references cannot be
        injected), the @Provides method in the RestSTSInstanceModule will return "" if this value has not been set,
        so I will check to insure that this value has indeed been specified.
         */
        if (!"".equals(offloadedTlsClientCertKey)) {
            String clientIpAddress = ClientUtils.getClientIPAddress(restSTSServiceHttpServletContext.getHttpServletRequest());
            if (!tlsOffloadEngineHosts.contains(clientIpAddress) && !tlsOffloadEngineHosts.contains(ANY_HOST)) {
                logger.error("A x509-based token transformation is being rejected because the client cert was to be referenced in " +
                        "the  " + offloadedTlsClientCertKey + " header, but the caller was not in the list of TLS offload engines." +
                        " The caller: " + clientIpAddress +
                        "; The list of TLS offload engine hosts: " + tlsOffloadEngineHosts);
                throw new TokenMarshalException(ResourceException.BAD_REQUEST, "In a x509 Certificate token transformation, " +
                        " the caller was not among the list of IP addresses corresponding to the TLS offload-engine hosts. " +
                        "Insure that your published rest-sts instance is configured with a complete list of TLS offload-engine hosts.");
            }
            certificates = pullClientCertFromHeader(httpContext);
        } else {
            certificates = pullClientCertFromRequestAttribute(restSTSServiceHttpServletContext);
        }

        if (certificates != null) {
            return marshalX509CertIntoTokenValidatorParameters(certificates);
        } else {
            if (!"".equals(offloadedTlsClientCertKey)) {
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

    private X509Certificate[] pullClientCertFromRequestAttribute(RestSTSServiceHttpServletContext restSTSServiceHttpServletContext) throws TokenMarshalException {
        return (X509Certificate[])restSTSServiceHttpServletContext.getHttpServletRequest().getAttribute(X509_CERTIFICATE_ATTRIBUTE);
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
                } catch (CertificateException e) {
                    throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                            "Exception caught marshalling X509 cert from value set in " + offloadedTlsClientCertKey + " header: " + e, e);
                } catch (UnsupportedEncodingException e) {
                    throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                            "Exception caught marshalling X509 cert from value set in " + offloadedTlsClientCertKey + " header: " + e, e);
                }
            }
            return certificates;
        }
    }

    private RestTokenValidatorParameters<X509Certificate[]> marshalX509CertIntoTokenValidatorParameters(
                                        final X509Certificate[] x509Certificates) throws TokenMarshalException {
        return new RestTokenValidatorParameters<X509Certificate[]>() {
            @Override
            public X509Certificate[] getInputToken() {
                return x509Certificates;
            }

            @Override
            public String getId() {
                return TokenType.X509.getId();
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
            return new RestTokenProviderParameters<Saml2TokenCreationState>() {
                @Override
                public Saml2TokenCreationState getTokenCreationState() {
                    return saml2TokenCreationState;
                }

                @Override
                public TokenTypeId getInputTokenType() {
                    return inputTokenType;
                }

                @Override
                public JsonValue getInputToken() {
                    return inputToken;
                }
            };
        } else {
            final Saml2TokenCreationState saml2TokenCreationState = new Saml2TokenCreationState(subjectConfirmation);
            return new RestTokenProviderParameters<Saml2TokenCreationState>() {
                @Override
                public Saml2TokenCreationState getTokenCreationState() {
                    return saml2TokenCreationState;
                }

                @Override
                public TokenTypeId getInputTokenType() {
                    return inputTokenType;
                }

                @Override
                public JsonValue getInputToken() {
                    return inputToken;
                }
            };
        }
    }
}
