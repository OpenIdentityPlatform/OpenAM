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

package org.forgerock.openam.sts.soap.token.delegation;

import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.soap.config.user.SoapDelegationConfig;
import org.forgerock.openam.sts.soap.config.user.SoapDeploymentConfig;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;
import org.forgerock.openam.sts.soap.config.user.SoapSTSKeystoreConfig;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.slf4j.Logger;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertTrue;


public class TokenDelegationHandlersProviderTest {
    private static final boolean DELEGATION_VALIDATORS_SPECIFIED = true;
    private static final boolean CUSTOM_DELEGATION_HANDLER = true;

    @Test
    public void testNoDelegationSupported() throws UnsupportedEncodingException {
        Logger mockLogger = mock(Logger.class);
        ThreadLocalAMTokenCache mockTokenCache = mock(ThreadLocalAMTokenCache.class);
        SoapSTSInstanceConfig instanceConfig = createInstanceConfig(!DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER);
        assertTrue(new TokenDelegationHandlersProvider(instanceConfig, mockTokenCache, mockLogger).get().isEmpty());
    }

    @Test
    public void testDefaultDelegationHandler() throws UnsupportedEncodingException {
        Logger mockLogger = mock(Logger.class);
        ThreadLocalAMTokenCache mockTokenCache = mock(ThreadLocalAMTokenCache.class);
        SoapSTSInstanceConfig instanceConfig = createInstanceConfig(DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER);
        assertTrue(new TokenDelegationHandlersProvider(instanceConfig, mockTokenCache, mockLogger).get().get(0) instanceof DefaultTokenDelegationHandler);
    }

    @Test
    public void testWrappedCustomDelegationHandler() throws UnsupportedEncodingException {
        Logger mockLogger = mock(Logger.class);
        ThreadLocalAMTokenCache mockTokenCache = mock(ThreadLocalAMTokenCache.class);
        SoapSTSInstanceConfig instanceConfig = createInstanceConfig(!DELEGATION_VALIDATORS_SPECIFIED, CUSTOM_DELEGATION_HANDLER);
        assertTrue(new TokenDelegationHandlersProvider(instanceConfig, mockTokenCache, mockLogger).get().get(0) instanceof CustomDelegationHandlerWrapper);
    }

    private SoapSTSInstanceConfig createInstanceConfig(boolean delegationValidatorsSpecified,
                                                       boolean customDelegationHandler) throws UnsupportedEncodingException {
        AuthTargetMapping mapping = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "service", "ldap")
                .build();

        SoapDeploymentConfig deploymentConfig =
                SoapDeploymentConfig.builder()
                        .portQName(AMSTSConstants.UNPROTECTED_STS_SERVICE_PORT)
                        .serviceQName(AMSTSConstants.UNPROTECTED_STS_SERVICE)
                        .wsdlLocation("wsdl_loc")
                        .realm("realm")
                        .amDeploymentUrl("http://host.com/am:443")
                        .uriElement("inst1222")
                        .authTargetMapping(mapping)
                        .build();

        SoapSTSKeystoreConfig keystoreConfig = null;
        keystoreConfig =
                SoapSTSKeystoreConfig.builder()
                        .keystoreFileName("stsstore.jks")
                        .keystorePassword("stsspass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .encryptionKeyAlias("mystskey")
                        .signatureKeyAlias("mystskey")
                        .encryptionKeyPassword("stskpass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .signatureKeyPassword("stskpass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .build();

        SoapSTSInstanceConfig.SoapSTSInstanceConfigBuilderBase<?> builder = SoapSTSInstanceConfig.builder();
        builder.addValidateTokenTranslation(TokenType.OPENAM, TokenType.SAML2, false);
        builder.addIssueTokenType(TokenType.SAML2);
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
        boolean delegationRelationshipsSupported = customDelegationHandler || delegationValidatorsSpecified;
        if (delegationRelationshipsSupported) {
            SoapDelegationConfig.SoapDelegationConfigBuilder delegationConfigBuilder = SoapDelegationConfig.builder();
            if (delegationValidatorsSpecified) {
                delegationConfigBuilder
                        .addValidatedDelegationTokenType(TokenType.USERNAME)
                        .addValidatedDelegationTokenType(TokenType.OPENAM);
            }
            if (customDelegationHandler) {
                delegationConfigBuilder.addCustomDelegationTokenHandler("org.forgerock.openam.sts.soap.token.delegation.DefaultTokenDelegationHandler");
            }
            builder.soapDelegationConfig(delegationConfigBuilder.build());
        }
        return  builder
                .deploymentConfig(deploymentConfig)
                .soapSTSKeystoreConfig(keystoreConfig)
                .issuerName("Cornholio")
                .saml2Config(saml2Config)
                .delegationRelationshipsSupported(delegationRelationshipsSupported)
                .build();
    }
}
