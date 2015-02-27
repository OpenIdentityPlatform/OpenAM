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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.soap.publish;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ws.security.sts.provider.SecurityTokenServiceProvider;
import org.forgerock.guava.common.collect.Sets;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.soap.config.user.SoapDeploymentConfig;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;
import org.forgerock.openam.sts.soap.config.user.SoapSTSKeystoreConfig;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SoapSTSInstancePublisherImplTest {
    private static final boolean WITH_KEYSTORE_CONFIG = true;
    private static final boolean WITH_VALIDATE_TRANSFORM = true;
    private static final boolean WITH_ISSUE_OPERATION = true;

    private SoapSTSInstanceLifecycleManager mockLifecycleManager;
    private PublishServiceConsumer mockPublishServiceConsumer;
    private SoapSTSInstancePublisher instancePublisher;
    private Server mockServer;

    class MyModule extends AbstractModule {
        @Override
        protected void configure() {
            mockLifecycleManager = mock(SoapSTSInstanceLifecycleManager.class);
            mockPublishServiceConsumer = mock(PublishServiceConsumer.class);
            mockServer = mock(Server.class);
            Logger mockLogger = mock(Logger.class);
            bind(SoapSTSInstanceLifecycleManager.class).toInstance(mockLifecycleManager);
            bind(PublishServiceConsumer.class).toInstance(mockPublishServiceConsumer);
            bind(Logger.class).toInstance(mockLogger);
            bind(SoapSTSInstancePublisher.class).to(SoapSTSInstancePublisherImpl.class);
        }
    }

    //need to re-create the bindings before each test method because invocations against a mock being calculated.
    @BeforeMethod
    public void setUp() {
        instancePublisher = Guice.createInjector(new MyModule()).getInstance(SoapSTSInstancePublisher.class);
    }

    @Test
    public void testPublishAndRemove() throws ResourceException, UnsupportedEncodingException {
        Set<SoapSTSInstanceConfig> initialSet = Sets.newHashSet(createInstanceConfig("instanceOne",
                "http://host.com:8080/am", WITH_KEYSTORE_CONFIG, WITH_VALIDATE_TRANSFORM, WITH_ISSUE_OPERATION));
        when(mockPublishServiceConsumer.getPublishedInstances()).thenReturn(initialSet);
        when(mockLifecycleManager.exposeSTSInstanceAsWebService(
                any(Map.class), any(SecurityTokenServiceProvider.class), any(SoapSTSInstanceConfig.class))).thenReturn(mockServer);
        instancePublisher.run();
        verify(mockPublishServiceConsumer, times(1)).getPublishedInstances();
        verify(mockLifecycleManager, times(1)).exposeSTSInstanceAsWebService(any(Map.class),
                any(SecurityTokenServiceProvider.class), any(SoapSTSInstanceConfig.class));

        when(mockPublishServiceConsumer.getPublishedInstances()).thenReturn(Sets.<SoapSTSInstanceConfig>newHashSet());
        instancePublisher.run();
        verify(mockPublishServiceConsumer, times(2)).getPublishedInstances();
        verify(mockLifecycleManager, times(1)).exposeSTSInstanceAsWebService(any(Map.class),
                any(SecurityTokenServiceProvider.class), any(SoapSTSInstanceConfig.class));
        verify(mockLifecycleManager, times(1)).destroySTSInstance(any(Server.class));
    }

    @Test
    public void testNoUpdate() throws ResourceException, UnsupportedEncodingException {
        SoapSTSInstanceConfig instanceConfig = createInstanceConfig("instanceOne",
                "http://host.com:8080/am", WITH_KEYSTORE_CONFIG, WITH_VALIDATE_TRANSFORM, WITH_ISSUE_OPERATION);
        Set<SoapSTSInstanceConfig> initialSet = Sets.newHashSet(instanceConfig);
        when(mockPublishServiceConsumer.getPublishedInstances()).thenReturn(initialSet);
        when(mockLifecycleManager.exposeSTSInstanceAsWebService(
                any(Map.class), any(SecurityTokenServiceProvider.class), any(SoapSTSInstanceConfig.class))).thenReturn(mockServer);
        instancePublisher.run();
        verify(mockLifecycleManager, times(1)).exposeSTSInstanceAsWebService(any(Map.class),
                any(SecurityTokenServiceProvider.class), any(SoapSTSInstanceConfig.class));

        when(mockPublishServiceConsumer.getPublishedInstances()).thenReturn(initialSet);
        instancePublisher.run();
        verify(mockLifecycleManager, times(1)).exposeSTSInstanceAsWebService(any(Map.class),
                any(SecurityTokenServiceProvider.class), any(SoapSTSInstanceConfig.class));
        verify(mockLifecycleManager, times(0)).destroySTSInstance(any(Server.class));
    }

    @Test
    public void testUpdate() throws ResourceException, UnsupportedEncodingException {
        SoapSTSInstanceConfig instanceConfig = createInstanceConfig("instanceOne",
                "http://host.com:8080/am", WITH_KEYSTORE_CONFIG, WITH_VALIDATE_TRANSFORM, WITH_ISSUE_OPERATION);
        Set<SoapSTSInstanceConfig> initialSet = Sets.newHashSet(instanceConfig);
        when(mockPublishServiceConsumer.getPublishedInstances()).thenReturn(initialSet);
        when(mockLifecycleManager.exposeSTSInstanceAsWebService(
                any(Map.class), any(SecurityTokenServiceProvider.class), any(SoapSTSInstanceConfig.class))).thenReturn(mockServer);
        instancePublisher.run();
        verify(mockLifecycleManager, times(1)).exposeSTSInstanceAsWebService(any(Map.class),
                any(SecurityTokenServiceProvider.class), any(SoapSTSInstanceConfig.class));

        SoapSTSInstanceConfig updatedConfig = createInstanceConfig("instanceOne",
                "http://host.com:8080/am2", WITH_KEYSTORE_CONFIG, WITH_VALIDATE_TRANSFORM, WITH_ISSUE_OPERATION);
        when(mockPublishServiceConsumer.getPublishedInstances()).thenReturn(Sets.newHashSet(updatedConfig));
        instancePublisher.run();
        verify(mockLifecycleManager, times(2)).exposeSTSInstanceAsWebService(any(Map.class),
                any(SecurityTokenServiceProvider.class), any(SoapSTSInstanceConfig.class));
        verify(mockLifecycleManager, times(1)).destroySTSInstance(any(Server.class));
    }

    private SoapSTSInstanceConfig createInstanceConfig(String uriElement, String amDeploymentUrl,
                                                       boolean withKeystoreConfig, boolean withValidateTransform,
                                                       boolean withIssueOperation) throws UnsupportedEncodingException {
        AuthTargetMapping mapping = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "service", "ldap")
                .build();

        SoapDeploymentConfig deploymentConfig =
                SoapDeploymentConfig.builder()
                        .portQName(AMSTSConstants.UNPROTECTED_STS_SERVICE_PORT)
                        .serviceQName(AMSTSConstants.UNPROTECTED_STS_SERVICE)
                        .wsdlLocation("wsdl_loc")
                        .realm("realm")
                        .amDeploymentUrl(amDeploymentUrl)
                        .uriElement(uriElement)
                        .authTargetMapping(mapping)
                        .build();

        SoapSTSKeystoreConfig keystoreConfig = null;
        if (withKeystoreConfig) {
            keystoreConfig =
                    SoapSTSKeystoreConfig.builder()
                            .keystoreFileName("stsstore.jks")
                            .keystorePassword("frstssrvkspw".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                            .encryptionKeyAlias("frstssrval")
                            .encryptionKeyPassword("frstssrvpw".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                            .signatureKeyAlias("frstssrval")
                            .signatureKeyPassword("frstssrvpw".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                            .build();
        }

        SoapSTSInstanceConfig.SoapSTSInstanceConfigBuilderBase<?> builder = SoapSTSInstanceConfig.builder();
        if (withValidateTransform) {
            builder.addValidateTokenTranslation(TokenType.OPENAM, TokenType.SAML2, false);
        }
        if (withIssueOperation) {
            builder.addIssueTokenType(TokenType.SAML2);
        }
        Map<String,String> attributeMap = new HashMap<String, String>();
        attributeMap.put("mail", "email");
        attributeMap.put("uid", "id");
        SAML2Config saml2Config =
                SAML2Config.builder()
                        .nameIdFormat("transient")
                        .tokenLifetimeInSeconds(500000)
                        .spEntityId("http://host.com/saml2/sp/entity/id")
                        .encryptAssertion(true)
                        .signAssertion(true)
                        .encryptionAlgorithm("http://www.w3.org/2001/04/xmlenc#aes128-cbc")
                        .encryptionKeyAlias("test")
                        .signatureKeyAlias("test")
                        .signatureKeyPassword("super.secret".getBytes())
                        .encryptionAlgorithmStrength(128)
                        .keystoreFile("da/directory/file")
                        .keystorePassword("super.secret".getBytes())
                        .attributeMap(attributeMap)
                        .build();

        return  builder
                .deploymentConfig(deploymentConfig)
                .soapSTSKeystoreConfig(keystoreConfig)
                .issuerName("Cornholio")
                .saml2Config(saml2Config)
                .build();
    }

}
