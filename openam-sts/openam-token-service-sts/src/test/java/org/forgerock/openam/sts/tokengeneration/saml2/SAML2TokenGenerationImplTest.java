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

package org.forgerock.openam.sts.tokengeneration.saml2;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.iplanet.sso.SSOToken;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.XMLUtilitiesImpl;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.tokengeneration.oidc.crypto.OpenIdConnectTokenPKIProviderFactory;
import org.forgerock.openam.sts.tokengeneration.oidc.crypto.OpenIdConnectTokenPKIProviderFactoryImpl;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.SAML2CryptoProviderFactoryImpl;
import org.forgerock.openam.sts.user.invocation.ProofTokenState;
import org.forgerock.openam.sts.config.user.DeploymentConfig;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.service.invocation.SAML2TokenGenerationState;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.tokengeneration.SSOTokenIdentity;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.AttributeMapper;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.DefaultAttributeStatementsProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.DefaultAuthenticationStatementsProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.DefaultAuthzDecisionStatementsProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.DefaultConditionsProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.DefaultSubjectProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.KeyInfoFactory;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.KeyInfoFactoryImpl;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.SAML2CryptoProviderFactory;
import org.forgerock.openam.sts.service.invocation.TokenGenerationServiceInvocationState;
import org.forgerock.openam.sts.tokengeneration.state.RestSTSInstanceStateFactoryImpl;
import org.forgerock.openam.sts.tokengeneration.state.STSInstanceState;
import org.forgerock.openam.sts.tokengeneration.state.STSInstanceStateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;

public class SAML2TokenGenerationImplTest {
    private SAML2TokenGeneration saml2TokenGeneration;
    private STSInstanceStateFactory restSTSInstanceStateFactory;
    private Injector injector;
    private static final String AUTHN_CONTEXT = "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport";
    private static final String SSO_TOKEN_STRING = "irrelevant";
    private static final String STS_INSTANCE_ID = "also_irrelevant";
    private static final boolean SIGN_ASSERTION = true;
    private static final String HOLDER_OF_KEY = "urn:oasis:names:tc:SAML:2.0:cm:holder-of-key";
    private static final String BEARER = "urn:oasis:names:tc:SAML:2.0:cm:bearer";
    private static final String SENDER_VOUCHES = "urn:oasis:names:tc:SAML:2.0:cm:sender-vouches";
    private static final String SIGNATURE = "Signature";

    public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
            SSOTokenIdentity mockTokenIdentity = mock(SSOTokenIdentity.class);
            try {
                when(mockTokenIdentity.validateAndGetTokenPrincipal(any(SSOToken.class))).thenReturn("bobo");
            } catch (TokenCreationException e) {
                throw new IllegalStateException("Could not mock SSOTokenIdentity: " + e, e);
            }
            bind(SAML2TokenGeneration.class).to(SAML2TokenGenerationImpl.class);
            bind(STSInstanceStateFactory.class).to(RestSTSInstanceStateFactoryImpl.class);
            bind(SAML2CryptoProviderFactory.class).to(SAML2CryptoProviderFactoryImpl.class);
            bind(OpenIdConnectTokenPKIProviderFactory.class).to(OpenIdConnectTokenPKIProviderFactoryImpl.class);
            bind(SSOTokenIdentity.class).toInstance(mockTokenIdentity);
        }

        @Provides
        XMLUtilities getXMLUtilities() {
            return new XMLUtilitiesImpl();
        }

        @Provides
        @Inject
        KeyInfoFactory getKeyInfoFactory(XMLUtilities xmlUtilities) {
            return new KeyInfoFactoryImpl(xmlUtilities);
        }

        @Provides
        @Inject
        StatementProvider getStatementProvider(KeyInfoFactory keyInfoFactory) {
            AttributeMapper mockAttributeMapper = mock(AttributeMapper.class);
            StatementProvider mockStatementProvider;
            try {
                when(mockAttributeMapper.getAttributes(any(SSOToken.class), any(Map.class))).thenReturn(Collections.<com.sun.identity.saml2.assertion.Attribute>emptyList());
                mockStatementProvider = mock(StatementProvider.class);
                when(mockStatementProvider.getAttributeMapper(any(SAML2Config.class))).thenReturn(mockAttributeMapper);
                when(mockStatementProvider.getAttributeStatementsProvider(any(SAML2Config.class))).thenReturn(new DefaultAttributeStatementsProvider());
                when(mockStatementProvider.getAuthenticationStatementsProvider(any(SAML2Config.class))).thenReturn(new DefaultAuthenticationStatementsProvider());
                when(mockStatementProvider.getAuthzDecisionStatementsProvider(any(SAML2Config.class))).thenReturn(new DefaultAuthzDecisionStatementsProvider());
                when(mockStatementProvider.getConditionsProvider(any(SAML2Config.class))).thenReturn(new DefaultConditionsProvider());
                when(mockStatementProvider.getSubjectProvider(any(SAML2Config.class))).thenReturn(new DefaultSubjectProvider(keyInfoFactory));
            } catch (TokenCreationException e) {
                throw new IllegalArgumentException("Exception caught creating StatementProvider");
            }
            return mockStatementProvider;
        }

        @Provides
        Logger getSlf4jLogger() {
            return LoggerFactory.getLogger(AMSTSConstants.REST_STS_DEBUG_ID);
        }

    }

    @BeforeTest
    public void setup() {
        injector = Guice.createInjector(new MyModule());
        saml2TokenGeneration = injector.getInstance(SAML2TokenGeneration.class);
        restSTSInstanceStateFactory = injector.getInstance(STSInstanceStateFactory.class);
    }

    @Test
    public void testBearerIssue() throws Exception {
        String assertion = saml2TokenGeneration.generate(mock(SSOToken.class), getSTSInstanceState(SIGN_ASSERTION),
                getTokenGenerationInvocationState(SAML2SubjectConfirmation.BEARER));
        assertTrue(assertion.contains(BEARER));
        assertTrue(assertion.contains(SIGNATURE));
    }

    @Test
    public void testSenderVouchesIssue() throws Exception {
        String assertion = saml2TokenGeneration.generate(mock(SSOToken.class), getSTSInstanceState(SIGN_ASSERTION),
                getTokenGenerationInvocationState(SAML2SubjectConfirmation.SENDER_VOUCHES));
        assertTrue(assertion.contains(SENDER_VOUCHES));
        assertTrue(assertion.contains(SIGNATURE));
    }

    @Test
    public void testHolderOfKeyIssue() throws Exception {
        String assertion = saml2TokenGeneration.generate(mock(SSOToken.class), getSTSInstanceState(SIGN_ASSERTION),
                getTokenGenerationInvocationState(SAML2SubjectConfirmation.HOLDER_OF_KEY));
        assertTrue(assertion.contains(HOLDER_OF_KEY));
        assertTrue(assertion.contains(SIGNATURE));
    }

    @Test
    public void testSignatureOmission() throws Exception {
        String assertion = saml2TokenGeneration.generate(mock(SSOToken.class), getSTSInstanceState(!SIGN_ASSERTION),
                getTokenGenerationInvocationState(SAML2SubjectConfirmation.BEARER));
        assertTrue(assertion.contains(BEARER));
        assertTrue(!assertion.contains(SIGNATURE));
    }

    private STSInstanceState getSTSInstanceState(boolean signAssertion) throws Exception {
        return restSTSInstanceStateFactory.createSTSInstanceState(getRestSTSInstanceConfig(signAssertion));
    }

    private TokenGenerationServiceInvocationState getTokenGenerationInvocationState(
            SAML2SubjectConfirmation subjectConfirmation) throws Exception {
        return TokenGenerationServiceInvocationState.builder()
                .stsType(AMSTSConstants.STSType.REST)
                .ssoTokenString(SSO_TOKEN_STRING)
                .saml2GenerationState(buildSAML2TokenGenerationState(subjectConfirmation))
                .tokenType(TokenType.SAML2)
                .stsInstanceId(STS_INSTANCE_ID)
                .build();

    }
    SAML2TokenGenerationState buildSAML2TokenGenerationState(SAML2SubjectConfirmation subjectConfirmation)
            throws Exception {
        SAML2TokenGenerationState.SAML2TokenGenerationStateBuilder builder = SAML2TokenGenerationState.builder();
        builder
                .authenticationContextClassReference(AUTHN_CONTEXT)
                .subjectConfirmation(subjectConfirmation);
        if (SAML2SubjectConfirmation.HOLDER_OF_KEY.equals(subjectConfirmation)) {
            builder.proofTokenState(ProofTokenState.builder().x509Certificate(getCertificate()).build());
        }
        return builder.build();
    }

    private X509Certificate getCertificate() throws Exception {
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(getClass().getResourceAsStream("/cert.jks"));
    }

    private RestSTSInstanceConfig getRestSTSInstanceConfig(boolean signAssertion) throws UnsupportedEncodingException {
        Map<String, String> context = new HashMap<>();
        context.put(AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_AUTH_TARGET_HEADER_KEY, "oidc_id_token");
        AuthTargetMapping mapping = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "service", "ldapService")
                .addMapping(TokenType.OPENIDCONNECT, "module", "oidc", context)
                .build();
        DeploymentConfig deploymentConfig =
                DeploymentConfig.builder()
                        .uriElement("boborealm/inst1")
                        .authTargetMapping(mapping)
                        .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put("email", "mail");
        SAML2Config saml2Config =
                SAML2Config.builder()
                        .attributeMap(attributes)
                        .nameIdFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent")
                        .spEntityId("http://host.com/sp/entity/id")
                        .signAssertion(signAssertion)
                        .keystoreFile("/keystore.jks")
                        .keystorePassword("changeit".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .encryptionKeyAlias("test")
                        .signatureKeyAlias("test")
                        .signatureKeyPassword("changeit".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .idpId("da_idp")
                        .build();

        return RestSTSInstanceConfig.builder()
                .deploymentConfig(deploymentConfig)
                .saml2Config(saml2Config)
                .addSupportedTokenTranslation(
                        TokenType.X509,
                        TokenType.SAML2,
                        !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                .addSupportedTokenTranslation(
                        TokenType.USERNAME,
                        TokenType.SAML2,
                        AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                .addSupportedTokenTranslation(
                        TokenType.OPENAM,
                        TokenType.SAML2,
                        !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                .addSupportedTokenTranslation(
                        TokenType.OPENIDCONNECT,
                        TokenType.SAML2,
                        AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                .build();
    }
}
