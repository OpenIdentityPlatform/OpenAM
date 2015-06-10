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
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.sun.identity.sm.ServiceListener;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.publish.STSInstanceConfigStore;
import org.forgerock.openam.sts.publish.soap.SoapSTSInstanceConfigStore;
import org.forgerock.openam.sts.rest.ServiceListenerRegistration;
import org.forgerock.openam.sts.rest.ServiceListenerRegistrationImpl;
import org.forgerock.openam.sts.soap.config.user.SoapDeploymentConfig;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;
import org.forgerock.openam.sts.soap.config.user.SoapSTSKeystoreConfig;
import org.forgerock.openam.sts.tokengeneration.config.TokenGenerationModule;
import org.forgerock.openam.sts.tokengeneration.oidc.crypto.OpenIdConnectTokenPKIProviderFactory;
import org.forgerock.openam.sts.tokengeneration.oidc.crypto.OpenIdConnectTokenPKIProviderFactoryImpl;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.SAML2CryptoProviderFactory;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.SAML2CryptoProviderFactoryImpl;
import org.forgerock.openam.sts.tokengeneration.state.STSInstanceStateFactory;
import org.forgerock.openam.sts.tokengeneration.state.STSInstanceStateProvider;
import org.forgerock.openam.sts.tokengeneration.state.SoapSTSInstanceState;
import org.forgerock.openam.sts.tokengeneration.state.SoapSTSInstanceStateFactoryImpl;
import org.forgerock.openam.sts.tokengeneration.state.SoapSTSInstanceStateProvider;
import org.forgerock.openam.sts.tokengeneration.state.SoapSTSInstanceStateServiceListener;
import org.slf4j.Logger;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;


public class SoapSTSInstanceStateProviderTest {
    private static final String DEPLOYMENT_URL_ELEMENT = "bobo/inst1";
    private static final String REALM = "bobo";
    private static final String AM_DEPLOYMENT_URL = "http://host.com:8080/am";
    private SoapSTSInstanceStateProvider provider;
    private SoapSTSInstanceConfigStore mockConfigStore;

    class TestModule extends AbstractModule {
        @Override
        protected void configure() {
            mockConfigStore = mock(SoapSTSInstanceConfigStore.class);
            bind(new TypeLiteral<STSInstanceConfigStore<SoapSTSInstanceConfig>>(){}).toInstance(mockConfigStore);
            bind(Logger.class).toInstance(mock(Logger.class));
            bind(SAML2CryptoProviderFactory.class).to(SAML2CryptoProviderFactoryImpl.class);
            bind(OpenIdConnectTokenPKIProviderFactory.class).to(OpenIdConnectTokenPKIProviderFactoryImpl.class);
            bind(SoapSTSInstanceStateProvider.class);
            bind(ServiceListenerRegistration.class).toInstance(mock(ServiceListenerRegistrationImpl.class));
            bind(ServiceListener.class).annotatedWith(Names.named(TokenGenerationModule.SOAP_STS_INSTANCE_STATE_LISTENER))
                    .to(SoapSTSInstanceStateServiceListener.class);
            bind(new TypeLiteral<STSInstanceStateProvider<SoapSTSInstanceState>>(){})
                    .to(SoapSTSInstanceStateProvider.class).in(Scopes.SINGLETON);
            bind(new TypeLiteral<STSInstanceStateFactory<SoapSTSInstanceState, SoapSTSInstanceConfig>>(){}).to(SoapSTSInstanceStateFactoryImpl.class);
        }
    }

    @BeforeTest
    public void setUpTest() {
        final Injector injector = Guice.createInjector(new TestModule());
        provider = injector.getInstance(SoapSTSInstanceStateProvider.class);
    }

    @Test
    public void verifyLookup() throws TokenCreationException, STSPublishException, UnsupportedEncodingException {
        SoapSTSInstanceConfig instanceConfig = createInstanceConfig();
        when(mockConfigStore.getSTSInstanceConfig(DEPLOYMENT_URL_ELEMENT, REALM)).thenReturn(instanceConfig);
        assertEquals(provider.getSTSInstanceState(DEPLOYMENT_URL_ELEMENT, REALM).getConfig(), instanceConfig);
    }

    @Test
    public void verifyCaching() throws TokenCreationException, STSPublishException, UnsupportedEncodingException {
        SoapSTSInstanceConfig instanceConfig = createInstanceConfig();
        when(mockConfigStore.getSTSInstanceConfig(DEPLOYMENT_URL_ELEMENT, REALM)).thenReturn(instanceConfig);
        provider.getSTSInstanceState(DEPLOYMENT_URL_ELEMENT, REALM); //initializes the cache with the mocked config
        verify(mockConfigStore, times(1)).getSTSInstanceConfig(DEPLOYMENT_URL_ELEMENT, REALM);
        //now insure that the config store will return null, to insure that only the cache can return a valid result
        when(mockConfigStore.getSTSInstanceConfig(DEPLOYMENT_URL_ELEMENT, REALM)).thenReturn(null);
        assertEquals(provider.getSTSInstanceState(DEPLOYMENT_URL_ELEMENT, REALM).getConfig(), instanceConfig);
        //the mockConfigStore should only have been called once, with the first invocation of provider.getSTSInstanceState -
        //the second call should be resolved in the caching layer.
        verify(mockConfigStore, times(1)).getSTSInstanceConfig(DEPLOYMENT_URL_ELEMENT, REALM);
    }

    private SoapSTSInstanceConfig createInstanceConfig() throws UnsupportedEncodingException {
        AuthTargetMapping mapping = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "service", "ldap")
                .build();

        SoapDeploymentConfig deploymentConfig =
                SoapDeploymentConfig.builder()
                        .portQName(AMSTSConstants.AM_TRANSPORT_STS_SERVICE_PORT)
                        .serviceQName(AMSTSConstants.AM_TRANSPORT_STS_SERVICE)
                        .wsdlLocation("wsdl_loc")
                        .realm("realm")
                        .amDeploymentUrl(AM_DEPLOYMENT_URL)
                        .uriElement(DEPLOYMENT_URL_ELEMENT)
                        .authTargetMapping(mapping)
                        .build();

        SoapSTSKeystoreConfig keystoreConfig =
                    SoapSTSKeystoreConfig.builder()
                            .keystoreFileName("stsstore.jks")
                            .keystorePassword("stsspass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                            .encryptionKeyAlias("mystskey")
                            .signatureKeyAlias("mystskey")
                            .encryptionKeyPassword("stskpass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                            .signatureKeyPassword("stskpass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                            .build();
        Map<String, String> attributes = new HashMap<>();
        attributes.put("email", "mail");
        SAML2Config saml2Config =
                SAML2Config.builder()
                        .attributeMap(attributes)
                        .nameIdFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent")
                        .spEntityId("http://host.com/sp/entity/id")
                        .idpId("da_idp")
                        .build();

        return  SoapSTSInstanceConfig.builder()
                .addIssueTokenType(TokenType.SAML2)
                .addSecurityPolicyTokenValidationConfiguration(TokenType.OPENAM, false)
                .deploymentConfig(deploymentConfig)
                .soapSTSKeystoreConfig(keystoreConfig)
                .saml2Config(saml2Config)
                .build();
    }
}
