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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.marshal;

import com.sun.identity.shared.encode.Base64;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.ws.security.sts.provider.model.secext.AttributedString;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.secext.PasswordString;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.ws.security.WSConstants;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSPrincipal;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.XmlMarshaller;
import org.forgerock.openam.sts.rest.service.RestSTSServiceHttpServletContext;
import org.forgerock.openam.sts.rest.token.validator.RestCertificateTokenValidator;
import org.forgerock.openam.sts.service.invocation.ProofTokenState;
import org.forgerock.openam.sts.service.invocation.SAML2TokenState;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.token.model.OpenAMSessionToken;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;
import org.forgerock.openam.utils.ClientUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.JAXBElement;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @see org.forgerock.openam.sts.rest.marshal.TokenRequestMarshaller
 */
public class TokenRequestMarshallerImpl implements TokenRequestMarshaller {
    private static final String X509_CERTIFICATE_ATTRIBUTE = "javax.servlet.request.X509Certificate";
    private static final String ANY_HOST = "any";
    private final XmlMarshaller<OpenAMSessionToken> amSessionTokenXmlMarshaller;
    private final XmlMarshaller<OpenIdConnectIdToken> openIdConnectXmlMarshaller;
    private final String offloadedTlsClientCertKey;
    /*
    A list containing the IP addresses of the hosts trusted to present client certificates in headers. Will correspond
    to the TLS-offload engines fronting an OpenAM deployment.
     */
    private final Set<String> tlsOffloadEngineHosts;
    private final Logger logger;

    @Inject
    TokenRequestMarshallerImpl(XmlMarshaller<OpenAMSessionToken> amSessionTokenXmlMarshaller,
                               XmlMarshaller<OpenIdConnectIdToken> openIdConnectXmlMarshaller,
                               @Named(AMSTSConstants.OFFLOADED_TWO_WAY_TLS_HEADER_KEY) String  offloadedTlsClientCertKey,
                               @Named(AMSTSConstants.TLS_OFFLOAD_ENGINE_HOSTS) Set<String> tlsOffloadEngineHosts,
                               Logger logger) {
        this.amSessionTokenXmlMarshaller = amSessionTokenXmlMarshaller;
        this.openIdConnectXmlMarshaller = openIdConnectXmlMarshaller;
        this.offloadedTlsClientCertKey = offloadedTlsClientCertKey;
        this.tlsOffloadEngineHosts = tlsOffloadEngineHosts;
        this.logger = logger;
    }

    public ReceivedToken marshallInputToken(JsonValue receivedToken, HttpContext httpContext,
                                            RestSTSServiceHttpServletContext restSTSServiceHttpServletContext) throws TokenMarshalException {
        Map<String,Object> tokenAsMap = receivedToken.asMap();
        String tokenType = (String)tokenAsMap.get(AMSTSConstants.TOKEN_TYPE_KEY);
        if (tokenType == null) {
            String message = "The to-be-translated token does not contain a " + AMSTSConstants.TOKEN_TYPE_KEY +
                    " entry. The token: " + receivedToken;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        }
        if (TokenType.USERNAME.name().equals(tokenType)) {
            return marshallUsernameToken(tokenAsMap);
        } else if (TokenType.OPENAM.name().equals(tokenType)) {
            return marshallAMSessionToken(tokenAsMap);
        } else if (TokenType.OPENIDCONNECT.name().equals(tokenType)) {
            return marshallOpenIdConnectIdToken(tokenAsMap);
        } else if (TokenType.X509.name().equals(tokenType)) {
            return marshalX509CertToken(httpContext, restSTSServiceHttpServletContext);
        }

        throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                "Unsupported token translation operation for token: " + receivedToken);

    }

    public TokenType getTokenType(JsonValue receivedToken) throws TokenMarshalException {
        JsonValue jsonTokenType = receivedToken.get(AMSTSConstants.TOKEN_TYPE_KEY);
        if (jsonTokenType.isNull() || !jsonTokenType.isString()) {
            String message = "REST STS invocation does not contain " + AMSTSConstants.TOKEN_TYPE_KEY +
                    " String entry. The json token: " + receivedToken;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        }
        try {
            return TokenType.valueOf(jsonTokenType.asString());
        } catch (IllegalArgumentException e) {
            String message = "Error marshalling from " + AMSTSConstants.TOKEN_TYPE_KEY +
                    " value to token-type enum. The json token: " + receivedToken + " The exception: " + e;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        } catch (NullPointerException e) {
            String message = "Error marshalling from " + AMSTSConstants.TOKEN_TYPE_KEY +
                    " value to token-type enum. The json token: " + receivedToken + " The exception: " + e;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        }
    }

    public SAML2SubjectConfirmation getSubjectConfirmation(JsonValue token) throws TokenMarshalException {
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

    public ProofTokenState getProofTokenState(JsonValue token) throws TokenMarshalException {
        final SAML2TokenState tokenState = SAML2TokenState.fromJson(token);
        final ProofTokenState proofTokenState = tokenState.getProofTokenState();
        if (proofTokenState ==  null) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "No ProofTokenState specified in the" +
                    " SAML2TokenState. The JsonValue: " + token);
        } else {
            return proofTokenState;
        }
    }

    private ReceivedToken marshallUsernameToken(Map<String, Object> tokenAsMap) throws TokenMarshalException {
        String tokenUserName = (String)tokenAsMap.get(AMSTSConstants.USERNAME_TOKEN_USERNAME);
        if (tokenUserName == null) {
            String message = "Exception: json representation of UNT does not contain a username field. The representation: " + tokenAsMap;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        }
        String password = (String)tokenAsMap.get(AMSTSConstants.USERNAME_TOKEN_PASSWORD);
        if (password == null) {
            String message = "Exception: json representation of UNT does not contain a password field. The representation: " + tokenAsMap;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        }

        UsernameTokenType usernameTokenType = new UsernameTokenType();
        AttributedString usernameAttributedString = new AttributedString();
        usernameAttributedString.setValue(tokenUserName);
        usernameTokenType.setUsername(usernameAttributedString);
        PasswordString passwordString = new PasswordString();
        passwordString.setValue(password);
        passwordString.setType(WSConstants.PASSWORD_TEXT);
        JAXBElement <PasswordString> passwordType =
                new JAXBElement<PasswordString>(QNameConstants.PASSWORD, PasswordString.class, passwordString);
        usernameTokenType.getAny().add(passwordType);
        JAXBElement<UsernameTokenType> jaxbUsernameTokenType =
                new JAXBElement<UsernameTokenType>(QNameConstants.USERNAME_TOKEN, UsernameTokenType.class, usernameTokenType);
        /*
        TODO - setting the nonce? If I don't set it, subsequent requests will fail because my validator will not be called
        (as the token is found in the cache), and thus the interim OpenAM token is not stored to be used to define the
        principal in the issued token. At the moment, I am not setting the TokenStore in the TokenValidatorParameters, so
        this solves the problem. But I have to answer larger questions like:
        1. are there some tokens I don't want to store, so that their validation can be resolved at a caching layer? Caching tokens
        is full of headaches like making sure that the token lifetime in the STS TokenStore matches that of any token/session state
        generated by the ultimate authority for this token. The bottom line is that caching tokens in the STS TokenStore should be
        the exception rather than the rule - i.e. it makes sense for SCT instances, as otherwise, there is no way to validate them,
        but less so for UNTs.
        2. Should the REST-STS accept UNTs without a nonce or origination instant? Without a unique bit of entropy, UNT instances
        with the same <username,password> value will hash to the same entry in the STS TokenStore. But if UNTs are not cached, it
        might not matter. The bottom line is whether the REST-STS should prevent the replay of token transformations - so perhaps each
        input token type in supported transformations should be associated with a token lifetime, but if we are taking UNTs in the clear,
        what's the point (i.e. the timestamp could just be updated). I imagine that we will ultimately just take a UNT in a JWT enveloped
        in JWS/JWE.
         */
        ReceivedToken token = new ReceivedToken(jaxbUsernameTokenType);
        token.setState(ReceivedToken.STATE.NONE);
        token.setPrincipal(new STSPrincipal(tokenUserName));
        token.setUsernameToken(true);
        return token;
    }

    private ReceivedToken marshallAMSessionToken(Map<String, Object> tokenAsMap) throws TokenMarshalException {
        String sessionId = (String)tokenAsMap.get(AMSTSConstants.AM_SESSION_TOKEN_SESSION_ID);
        if (sessionId == null) {
            String message = "Exception: json representation of AM Session Token does not contain a session_id field. The representation: " + tokenAsMap;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        } else {
            ReceivedToken token = new ReceivedToken(amSessionTokenXmlMarshaller.toXml(new OpenAMSessionToken(sessionId)));
            token.setState(ReceivedToken.STATE.NONE);
            token.setUsernameToken(false);
            return token;
        }
    }

    private ReceivedToken marshallOpenIdConnectIdToken(Map<String, Object> tokenAsMap) throws TokenMarshalException {
        String tokenValue = (String)tokenAsMap.get(AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY);
        if (tokenValue == null) {
            String message = "Exception: json representation of Open ID Connect ID Token does not contain a "
                    + AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY + " field. The representation: " + tokenAsMap;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        } else {
            ReceivedToken token = new ReceivedToken(openIdConnectXmlMarshaller.toXml(new OpenIdConnectIdToken(tokenValue)));
            token.setState(ReceivedToken.STATE.NONE);
            token.setUsernameToken(false);
            return token;
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
     * @return a ReceivedToken instance encapsulating the X509Certificate.
     */
    private ReceivedToken marshalX509CertToken(HttpContext httpContext, RestSTSServiceHttpServletContext
            restSTSServiceHttpServletContext) throws TokenMarshalException {

        X509Certificate certificate = null;
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
            certificate = pullClientCertFromHeader(httpContext);
        } else {
            certificate = pullClientCertFromRequestAttribute(restSTSServiceHttpServletContext);
        }

        if (certificate != null) {
            return marshalX509CertIntoReceivedToken(certificate);
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

    private X509Certificate pullClientCertFromRequestAttribute(RestSTSServiceHttpServletContext restSTSServiceHttpServletContext) throws TokenMarshalException {
        X509Certificate[] certificates =
                (X509Certificate[])restSTSServiceHttpServletContext.getHttpServletRequest().getAttribute(X509_CERTIFICATE_ATTRIBUTE);
        if (certificates != null) {
            /*
            The cxf-sts and wss4j convention is to pull the first cert from the array, as it is the leaf of any chain, and
            all non-leaf certificates should be in the trust store of the ultimate recipient.
             */
            return certificates[0];
        } else {
            return null;
        }
    }

    private X509Certificate pullClientCertFromHeader(HttpContext httpContext) throws TokenMarshalException {
        List<String> clientCertHeader = httpContext.getHeader(offloadedTlsClientCertKey);
        if (clientCertHeader.isEmpty()) {
            return null;
        } else {
            if (clientCertHeader.size() > 1) {
                logger.warn("In TokenRequestMarshallerImpl#marshalX509CertToken, more than a single header value " +
                        "corresponding to the header " + offloadedTlsClientCertKey + ". The headers: "
                        + clientCertHeader + ". Using the first element in the list.");
            }
            final String certString = clientCertHeader.get(0);
            try {
                /*
                Note that in the ServletRequest attribute, a X509Certificate[] is returned, but when the value is
                set in the header, I expect to marshal this state into a single certificate. Here I am following the
                lead of the com.sun.identity.authentication.modules.cert.Cert class, which also expects to find a
                single Cert in the header.
                 */
                return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(
                        new ByteArrayInputStream(Base64.decode(certString.getBytes(AMSTSConstants.UTF_8_CHARSET_ID))));
            } catch (CertificateException e) {
                throw new TokenMarshalException(ResourceException.INTERNAL_ERROR,
                        "Exception caught marshalling X509 cert from value set in " + offloadedTlsClientCertKey + " header: " + e, e);
            } catch (UnsupportedEncodingException e) {
                throw new TokenMarshalException(ResourceException.INTERNAL_ERROR,
                        "Exception caught marshalling X509 cert from value set in " + offloadedTlsClientCertKey + " header: " + e, e);
            }
        }
    }

    private ReceivedToken marshalX509CertIntoReceivedToken(X509Certificate x509Certificate) throws TokenMarshalException {
        BinarySecurityTokenType binarySecurityToken = new BinarySecurityTokenType();
        JAXBElement<BinarySecurityTokenType> tokenType = new JAXBElement<BinarySecurityTokenType>(
                        QNameConstants.BINARY_SECURITY_TOKEN, BinarySecurityTokenType.class, binarySecurityToken);
        try {
            binarySecurityToken.setValue(Base64.encode(x509Certificate.getEncoded()));
            binarySecurityToken.setValueType(RestCertificateTokenValidator.X509_V3_TYPE);
            binarySecurityToken.setEncodingType(RestCertificateTokenValidator.BASE64_ENCODING_TYPE);
        } catch (CertificateEncodingException e) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "Could not obtain encoded representation of " +
                    "client cert presented via two-way-tls: " + e, e);
        }
        return new ReceivedToken(tokenType);
    }
}
