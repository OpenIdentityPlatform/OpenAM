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

package org.forgerock.openam.sts.tokengeneration.saml2;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
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
import org.forgerock.openam.sts.rest.config.user.RestDeploymentConfig;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.rest.publish.RestSTSInstanceConfigStore;
import org.forgerock.openam.sts.tokengeneration.config.TokenGenerationModule;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.STSKeyProviderFactory;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.STSKeyProviderFactoryImpl;
import org.slf4j.Logger;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class RestSTSInstanceStateProviderTest {
    private static final String DEPLOYMENT_URL_ELEMENT = "bobo/inst1";
    private static final String REALM = "/";
    private RestSTSInstanceStateProvider provider;
    private RestSTSInstanceConfigStore mockConfigStore;
    private RestSTSInstanceStateFactory mockRestSTSInstanceStateFactory;
    private RestSTSInstanceState mockRestSTSInstanceState;

    class TestModule extends AbstractModule {
        @Override
        protected void configure() {
            mockRestSTSInstanceState = mock(RestSTSInstanceState.class);
            mockRestSTSInstanceStateFactory = mock(RestSTSInstanceStateFactory.class);
            mockConfigStore = mock(RestSTSInstanceConfigStore.class);
            bind(new TypeLiteral<STSInstanceConfigStore<RestSTSInstanceConfig>>(){}).toInstance(mockConfigStore);
            bind(Logger.class).toInstance(mock(Logger.class));
            bind(RestSTSInstanceStateFactory.class).toInstance(mockRestSTSInstanceStateFactory);
            bind(STSKeyProviderFactory.class).to(STSKeyProviderFactoryImpl.class);
            bind(RestSTSInstanceStateProvider.class);
            bind(ServiceListenerRegistration.class).toInstance(mock(ServiceListenerRegistrationImpl.class));
            bind(ServiceListener.class).annotatedWith(Names.named(TokenGenerationModule.REST_STS_INSTANCE_STATE_LISTENER))
                    .to(RestSTSInstanceStateServiceListener.class);
            bind(new TypeLiteral<STSInstanceStateProvider<RestSTSInstanceState>>(){})
                    .to(RestSTSInstanceStateProvider.class).in(Scopes.SINGLETON);
        }
    }

    @BeforeTest
    public void setUpTest() {
        provider = Guice.createInjector(new TestModule()).getInstance(RestSTSInstanceStateProvider.class);
    }

    @Test
    public void verifyCaching() throws TokenCreationException, STSPublishException {
        RestSTSInstanceConfig instanceConfig = createSAMLRestInstanceConfig(DEPLOYMENT_URL_ELEMENT);
        when(mockConfigStore.getSTSInstanceConfig(DEPLOYMENT_URL_ELEMENT, REALM)).thenReturn(instanceConfig);
        when(mockRestSTSInstanceStateFactory.createRestSTSInstanceState(any(RestSTSInstanceConfig.class))).thenReturn(mockRestSTSInstanceState);
        provider.getSTSInstanceState(DEPLOYMENT_URL_ELEMENT, REALM);
        provider.getSTSInstanceState(DEPLOYMENT_URL_ELEMENT, REALM);
        verify(mockRestSTSInstanceStateFactory, times(1)).createRestSTSInstanceState(instanceConfig);
    }

    private RestSTSInstanceConfig createSAMLRestInstanceConfig(String urlElement) {
        Map<String, String> context = new HashMap<String, String>();
        context.put(AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_AUTH_TARGET_HEADER_KEY, "oidc_id_token");
        AuthTargetMapping mapping = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "service", "ldapService")
                .addMapping(TokenType.OPENIDCONNECT, "module", "oidc", context)
                .build();
        RestDeploymentConfig deploymentConfig =
                RestDeploymentConfig.builder()
                        .uriElement(urlElement)
                        .authTargetMapping(mapping)
                        .build();

        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("email", "mail");
        SAML2Config saml2Config =
                SAML2Config.builder()
                        .attributeMap(attributes)
                        .nameIdFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent")
                        .spEntityId("http://host.com/sp/entity/id")
                        .build();

        return RestSTSInstanceConfig.builder()
                .deploymentConfig(deploymentConfig)
                .saml2Config(saml2Config)
                .issuerName("http://macbook.dirk.internal.forgerock.com:8080/openam")
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
