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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.sun.identity.shared.encode.Base64;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.XMLUtilitiesImpl;
import org.forgerock.openam.sts.XmlMarshaller;
import org.forgerock.openam.sts.rest.service.RestSTSServiceHttpServletContext;
import org.forgerock.openam.sts.service.invocation.ProofTokenState;
import org.forgerock.openam.sts.service.invocation.SAML2TokenState;
import org.forgerock.openam.sts.service.invocation.X509TokenState;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.token.model.OpenAMSessionToken;
import org.forgerock.openam.sts.token.model.OpenAMSessionTokenMarshaller;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdTokenMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Set;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.mockito.Mockito.*;

public class TokenRequestMarshallerImplTest {
    private TokenRequestMarshaller tokenMarshaller;
    static class MyModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(new TypeLiteral<XmlMarshaller<OpenAMSessionToken>>(){}).to(OpenAMSessionTokenMarshaller.class);
            bind(new TypeLiteral<XmlMarshaller<OpenIdConnectIdToken>>(){}).to(OpenIdConnectIdTokenMarshaller.class);
            bind(TokenRequestMarshaller.class).to(TokenRequestMarshallerImpl.class);
            bind(XMLUtilities.class).to(XMLUtilitiesImpl.class);
        }

        @Provides
        Logger getSlf4jLogger() {
            return LoggerFactory.getLogger(AMSTSConstants.REST_STS_DEBUG_ID);
        }

        //Must be empty for the testX509CertificateTokenMarshalling() to reference cert from ServletRequest attribute
        @Provides
        @Named(AMSTSConstants.OFFLOADED_TWO_WAY_TLS_HEADER_KEY)
        String getOffloadedTwoWayTLSHeaderKey() {
            return "";
        }

        @Provides
        @Named(AMSTSConstants.TLS_OFFLOAD_ENGINE_HOSTS)
        Set<String> getTlsOffloadEngineHosts() {
            return Collections.EMPTY_SET;
        }

        @Provides
        @Named(AMSTSConstants.CREST_VERSION_AUTHN_SERVICE)
        String getCrestVersionAuthNService() {
            return "protocol=1.0, resource=1.0";
        }

        @Provides
        @Named(AMSTSConstants.CREST_VERSION_SESSION_SERVICE)
        String getCrestVersionSessionService() {
            return "protocol=1.0, resource=1.0";
        }

        @Provides
        @Named(AMSTSConstants.CREST_VERSION_USERS_SERVICE)
        String getCrestVersionUsersService() {
            return "protocol=1.0, resource=1.0";
        }

        @Provides
        @Named(AMSTSConstants.CREST_VERSION_TOKEN_GEN_SERVICE)
        String getCrestVersionTokenGenService() {
            return "protocol=1.0, resource=1.0";
        }
    }

    @BeforeTest
    public void initialize() {
        tokenMarshaller = Guice.createInjector(new MyModule()).getInstance(TokenRequestMarshaller.class);
    }

    @Test
    public void marshallUsernameToken() throws TokenMarshalException {
        JsonValue jsonUnt = json(object(field("token_type", "USERNAME"),
                field("username", "bobo"), field("password", "cornholio")));
        ReceivedToken token = tokenMarshaller.marshallInputToken(jsonUnt, null, null);
        assertTrue(token.isUsernameToken());
        assertFalse(token.isBinarySecurityToken());
        assertFalse(token.isDOMElement());
        assertTrue("bobo".equals(token.getPrincipal().getName()));
    }

    @Test
    public void marshallOpenAMToken() throws TokenMarshalException {
        JsonValue jsonOpenAM = json(object(field("token_type", "OPENAM"),
                field("session_id", "super_random")));
        ReceivedToken token = tokenMarshaller.marshallInputToken(jsonOpenAM, null, null);
        assertFalse(token.isUsernameToken());
        assertFalse(token.isBinarySecurityToken());
        assertTrue(token.isDOMElement());
        assertNull(token.getPrincipal());
    }

    @Test
    public void testMarshalSubjectConfirmation() throws TokenMarshalException {
        SAML2TokenState tokenState = SAML2TokenState.builder()
                .saml2SubjectConfirmation(SAML2SubjectConfirmation.BEARER)
                .build();
        assertEquals(SAML2SubjectConfirmation.BEARER, tokenMarshaller.getSubjectConfirmation(tokenState.toJson()));
    }

    @Test
    public void testMarshalDegradedSubjectConfirmation() throws TokenMarshalException {
        JsonValue json = json(object(field(SAML2TokenState.SUBJECT_CONFIRMATION, SAML2SubjectConfirmation.BEARER.name())));
        assertEquals(SAML2SubjectConfirmation.BEARER, tokenMarshaller.getSubjectConfirmation(json));
    }

    @Test
    public void testMarshalProofTokenState() throws Exception {
        ProofTokenState proofTokenState = ProofTokenState.builder().x509Certificate(getCertificate()).build();
        SAML2TokenState tokenState = SAML2TokenState.builder()
                .saml2SubjectConfirmation(SAML2SubjectConfirmation.HOLDER_OF_KEY)
                .proofTokenState(proofTokenState)
                .build();
        assertEquals(proofTokenState, tokenMarshaller.getProofTokenState(tokenState.toJson()));
    }

    @Test
    public void testX509CertificateTokenMarshalling() throws Exception {
        HttpServletRequest mockServletRequest = mock(HttpServletRequest.class);
        RestSTSServiceHttpServletContext mockServletContext = mock(RestSTSServiceHttpServletContext.class);
        when(mockServletContext.getHttpServletRequest()).thenReturn(mockServletRequest);
        X509Certificate certificate = getCertificate();
        X509Certificate[] certificates = new X509Certificate[] {certificate};
        when(mockServletRequest.getAttribute("javax.servlet.request.X509Certificate")).thenReturn(certificates);
        ReceivedToken receivedToken = tokenMarshaller.marshallInputToken(new X509TokenState().toJson(), null, mockServletContext);
        X509Certificate roundTripCert = marshalBinarySecurityTokenToCertArray((BinarySecurityTokenType)receivedToken.getToken());
        assertEquals(certificate.getEncoded(), roundTripCert.getEncoded());
    }

    private X509Certificate marshalBinarySecurityTokenToCertArray(BinarySecurityTokenType binarySecurityType)
            throws CertificateException, UnsupportedEncodingException {
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(
                    new ByteArrayInputStream(Base64.decode(binarySecurityType.getValue().getBytes(AMSTSConstants.UTF_8_CHARSET_ID))));
    }

    private String getEncodedCertificate() throws IOException, CertificateException {
        return Base64.encode(getCertificate().getEncoded());
    }
    private X509Certificate getCertificate() throws IOException, CertificateException {
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(getClass().getResourceAsStream("/cert.jks"));
    }
}
