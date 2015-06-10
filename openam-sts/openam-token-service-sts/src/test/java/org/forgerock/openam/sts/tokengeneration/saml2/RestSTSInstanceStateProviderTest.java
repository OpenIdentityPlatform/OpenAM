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
import org.forgerock.openam.sts.rest.ServiceListenerRegistration;
import org.forgerock.openam.sts.rest.ServiceListenerRegistrationImpl;
import org.forgerock.openam.sts.config.user.DeploymentConfig;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.publish.rest.RestSTSInstanceConfigStore;
import org.forgerock.openam.sts.tokengeneration.config.TokenGenerationModule;
import org.forgerock.openam.sts.tokengeneration.oidc.crypto.OpenIdConnectTokenPKIProviderFactory;
import org.forgerock.openam.sts.tokengeneration.oidc.crypto.OpenIdConnectTokenPKIProviderFactoryImpl;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.SAML2CryptoProviderFactory;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.SAML2CryptoProviderFactoryImpl;
import org.forgerock.openam.sts.tokengeneration.state.RestSTSInstanceState;
import org.forgerock.openam.sts.tokengeneration.state.RestSTSInstanceStateFactoryImpl;
import org.forgerock.openam.sts.tokengeneration.state.RestSTSInstanceStateProvider;
import org.forgerock.openam.sts.tokengeneration.state.RestSTSInstanceStateServiceListener;
import org.forgerock.openam.sts.tokengeneration.state.STSInstanceStateFactory;
import org.forgerock.openam.sts.tokengeneration.state.STSInstanceStateProvider;
import org.slf4j.Logger;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;


public class RestSTSInstanceStateProviderTest {
    private static final String DEPLOYMENT_URL_ELEMENT = "bobo/inst1";
    private static final String REALM = "bobo";
    private RestSTSInstanceStateProvider provider;
    private RestSTSInstanceConfigStore mockConfigStore;

    class TestModule extends AbstractModule {
        @Override
        protected void configure() {
            mockConfigStore = mock(RestSTSInstanceConfigStore.class);
            bind(new TypeLiteral<STSInstanceConfigStore<RestSTSInstanceConfig>>(){}).toInstance(mockConfigStore);
            bind(Logger.class).toInstance(mock(Logger.class));
            bind(SAML2CryptoProviderFactory.class).to(SAML2CryptoProviderFactoryImpl.class);
            bind(OpenIdConnectTokenPKIProviderFactory.class).to(OpenIdConnectTokenPKIProviderFactoryImpl.class);
            bind(RestSTSInstanceStateProvider.class);
            bind(ServiceListenerRegistration.class).toInstance(mock(ServiceListenerRegistrationImpl.class));
            bind(ServiceListener.class).annotatedWith(Names.named(TokenGenerationModule.REST_STS_INSTANCE_STATE_LISTENER))
                    .to(RestSTSInstanceStateServiceListener.class);
            bind(new TypeLiteral<STSInstanceStateProvider<RestSTSInstanceState>>(){})
                    .to(RestSTSInstanceStateProvider.class).in(Scopes.SINGLETON);
            bind(new TypeLiteral<STSInstanceStateFactory<RestSTSInstanceState, RestSTSInstanceConfig>>(){}).to(RestSTSInstanceStateFactoryImpl.class);
        }
    }

    @BeforeTest
    public void setUpTest() {
        final Injector injector = Guice.createInjector(new TestModule());
        provider = injector.getInstance(RestSTSInstanceStateProvider.class);
    }

    @Test
    public void verifyLookup() throws TokenCreationException, STSPublishException {
        RestSTSInstanceConfig instanceConfig = createSAMLRestInstanceConfig();
        when(mockConfigStore.getSTSInstanceConfig(DEPLOYMENT_URL_ELEMENT, REALM)).thenReturn(instanceConfig);
        assertEquals(provider.getSTSInstanceState(DEPLOYMENT_URL_ELEMENT, REALM).getConfig(), instanceConfig);
    }

    @Test
    public void verifyCaching() throws TokenCreationException, STSPublishException {
        RestSTSInstanceConfig instanceConfig = createSAMLRestInstanceConfig();
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

    private RestSTSInstanceConfig createSAMLRestInstanceConfig() {
        Map<String, String> context = new HashMap<>();
        context.put(AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_AUTH_TARGET_HEADER_KEY, "oidc_id_token");
        AuthTargetMapping mapping = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "service", "ldapService")
                .addMapping(TokenType.OPENIDCONNECT, "module", "oidc", context)
                .build();
        DeploymentConfig deploymentConfig =
                DeploymentConfig.builder()
                        .uriElement(DEPLOYMENT_URL_ELEMENT)
                        .authTargetMapping(mapping)
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
