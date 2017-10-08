/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file at legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package com.forgerock.openam.functionaltest.sts.frmwk.soap;

import com.forgerock.openam.functionaltest.sts.frmwk.common.CommonConstants;
import com.forgerock.openam.functionaltest.sts.frmwk.common.STSPublishContext;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.config.user.OpenIdConnectTokenConfig;
import org.forgerock.openam.sts.config.user.OpenIdConnectTokenPublicKeyReferenceType;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.soap.EndpointSpecification;
import org.forgerock.openam.sts.soap.config.user.SoapDelegationConfig;
import org.forgerock.openam.sts.soap.config.user.SoapDeploymentConfig;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;
import org.forgerock.openam.sts.soap.config.user.SoapSTSKeystoreConfig;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.forgerock.openam.functionaltest.sts.frmwk.common.STSPublishContext.publishOIDC;
import static com.forgerock.openam.functionaltest.sts.frmwk.common.STSPublishContext.publishSAML2;

/**
 * This class allows for the generation of SoapSTSInstanceConfig state used to publish soap-sts instances. It is intended
 * to allow for the convenient generation of SoapSTSInstanceConfig instances used to publish soap-sts instances. It is
 * guided by STSPublishContext state, but hard-codes many of the options for caller convenience. If any of the
 * currently-hard-coded options need to be configurable, they can be added to the STSPublishContext class, and referenced
 * from there. The point is to be able to generate a reasonably small set of SoapSTSInstanceConfig instances to be used
 * in the functional tests, while encapsulating all of the myriad configuration options for caller convenience.
 *
 */
public class SoapSTSInstanceConfigFactory {

    public static final String AM_BARE_WSDL = "sts_am_bare.wsdl";
    public static final String AM_TRANSPORT_WSDL = "sts_am_transport.wsdl";
    public static final String UT_ASYM_WSDL = "sts_ut_asymmetric.wsdl";
    public static final String UT_SYM_WSDL = "sts_ut_symmetric.wsdl";
    public static final String UT_TRANSPORT_WSDL = "sts_ut_transport.wsdl";
    public static final String X509_SYM_WSDL = "sts_x509_symmetric.wsdl";
    public static final String X509_ASYM_WSDL = "sts_x509_asymmetric.wsdl";

    public SoapSTSInstanceConfig createSoapSTSInstanceConfig(String urlElement, String realm,
                                                              EndpointSpecification endpointSpecification,
                                                              STSPublishContext publishContext,
                                                              String wsdlFile, String amDeploymentUrl,
                                                             SoapSTSServerCryptoState soapSTSServerCryptoState) throws IOException {
        /*
        if you want to target a specific module or service for a particular token type, add it via the
        addMapping call below. See org.forgerock.openam.forgerockrest.authn.core.AuthIndexType for the
        list of valid authIndexType values (the second parameter in addMapping). The third parameter is
        simply the name of the specified module, service, etc. If you want to target the default service,
        don't add a mapping, or add a mapping corresponding to the default service, as below.
        */

        AuthTargetMapping.AuthTargetMappingBuilder mappingBuilder = AuthTargetMapping.builder();
        mappingBuilder.addMapping(TokenType.USERNAME, "service", "ldapService");
        if (x509MappingNecessary(wsdlFile)) {
            /*
             Build the context necessary for Cert validation. The value in the map must correspond to the
             header which the Cert module will consult to obtain the client's Certificate.
             */
            Map<String, String> certContext = new HashMap<>();
            certContext.put(AMSTSConstants.X509_TOKEN_AUTH_TARGET_HEADER_KEY, CommonConstants.DEFAULT_CERT_MODULE_TOKEN_HEADER_NAME);
            mappingBuilder.addMapping(TokenType.X509, "module", CommonConstants.DEFAULT_CERT_MODULE_NAME, certContext);
        }

        SoapDeploymentConfig.SoapDeploymentConfigBuilder deploymentConfigBuilder =
                SoapDeploymentConfig.builder()
                        .uriElement(urlElement)
                        .authTargetMapping(mappingBuilder.build())
                        .realm(realm)
                        .amDeploymentUrl(amDeploymentUrl)
                        .portQName(endpointSpecification.getPortQName())
                        .serviceQName(endpointSpecification.getServiceQName())
                        .wsdlLocation(wsdlFile);
        /*
        If the clientCertHeader field is specified, this implies that the client cert for x509 transformations should
        be specified in a header field. If this is the case, then the set of IP addrs corresponding to the
        tls offload engines must also be specified. I will simply specify the ip addr of this client, as it will be
        the one invoking token transformation functionality. If all hosts should be trusted, add 'any' to the list.
         */
        if (publishContext.getClientCertHeaderName() != null) {
            Set<String> offloadHostsSet = new HashSet<>();
            offloadHostsSet.add(InetAddress.getLocalHost().getHostAddress());
            offloadHostsSet.add("127.0.0.1");
            deploymentConfigBuilder
                    .offloadedTwoWayTLSHeaderKey(publishContext.getClientCertHeaderName())
                    .tlsOffloadEngineHostIpAddrs(offloadHostsSet);
        }
        SoapDeploymentConfig deploymentConfig = deploymentConfigBuilder.build();

        SoapDelegationConfig soapDelegationConfig = SoapDelegationConfig.builder()
                .addValidatedDelegationTokenType(TokenType.USERNAME, true)
                .addValidatedDelegationTokenType(TokenType.OPENAM, false)
                .build();

        Map<String, String> attributeMapping = new HashMap<>();
        attributeMapping.put("email", "mail");
        //put in a faux mapping to see if it appears
        attributeMapping.put("faux_claim", "faux_attribute");

        OpenIdConnectTokenConfig oidcIdTokenConfig = null;
        if (publishOIDC(publishContext)) {
            if (STSPublishContext.OIDCSigningAlgorithmType.RSA.equals(publishContext.getOidcSigningAlgorithmType())) {
                oidcIdTokenConfig = buildRSATokenConfig(publishContext, attributeMapping);
            } else {
                oidcIdTokenConfig = buildHMACTokenConfig(publishContext, attributeMapping);
            }
        }
        /*
        Note that the SAML2Config keystore state must reference a keystore available to the home OpenAM .war file,
        as this keystore state will be referenced by the TokenGenerationService in order to generate a SAML2 assertion
        on the OpenAM home server. To repeat: the soap-sts consumes the TokenGenerationService, hosted on OpenAM, in
        order to obtain a SAML2 assertion. Thus the SAML2Config state must reference a keystore file available to the
        consumed OpenAM deployment.
         */
        SAML2Config saml2Config = null;
        if (publishSAML2(publishContext)) {
            saml2Config = buildSAML2Config(publishContext, attributeMapping);
        }

        /*
        Note that the SoapSTSKeystoreConfig will reference state deployed in the soap-sts .war file, remote
        from the OpenAM home deployment. As such, it must reference a keystore available (via the classpath or
        the filesystem) in the remote soap-sts deployment. Also note the mismatch between decryption and encryption.
        The bottom line is that the CXF CallbackHandler registered with the CXF STSClient, or registered with a published
        soap-sts instance, will identify a callback with a DECRYPT constant on both the STSClient and sts instance, sides.
        This is because, in an asymmetric binding, messages between client and server are encrypted with the peer's public
        key, and thus the recipient's private key must be used to decrypt this message. Thus the encryption fields in
        SoapSTSKeystoreConfig should really be called decryption.
         */
        SoapSTSKeystoreConfig soapSTSKeystoreConfig = SoapSTSKeystoreConfig.builder()
                .keystoreFileName(soapSTSServerCryptoState.getKeystoreLocation())
                .keystorePassword(soapSTSServerCryptoState.getKeystorePassword().getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .encryptionKeyAlias(soapSTSServerCryptoState.getDecryptionKeyAlias())
                .encryptionKeyPassword(soapSTSServerCryptoState.getDecryptionKeyPassword().getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .signatureKeyAlias(soapSTSServerCryptoState.getSignatureKeyAlias())
                .signatureKeyPassword(soapSTSServerCryptoState.getSignatureKeyPassword().getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .build();

        TokenType securityPolicyTokenValidationTokenType = getSecurityPolicyTokenValidationConfigurationFromWsdlFile(wsdlFile);
        SoapSTSInstanceConfig.SoapSTSInstanceConfigBuilder builder = SoapSTSInstanceConfig.builder()
                .deploymentConfig(deploymentConfig)
                .delegationRelationshipsSupported(true)
                .soapDelegationConfig(soapDelegationConfig)
                .soapSTSKeystoreConfig(soapSTSKeystoreConfig)
                .saml2Config(saml2Config)
                .persistIssuedTokensInCTS(publishContext.persistIssuedTokensInCTS())
                .addSecurityPolicyTokenValidationConfiguration(securityPolicyTokenValidationTokenType, true);
        if (publishSAML2(publishContext)) {
            builder
                    .addIssueTokenType(TokenType.SAML2)
                    .saml2Config(saml2Config);
        }
        if (publishOIDC(publishContext)) {
            builder
                    .addIssueTokenType(TokenType.OPENIDCONNECT)
                    .oidcIdTokenConfig(oidcIdTokenConfig);
        }
        return builder.build();
    }

    private TokenType getSecurityPolicyTokenValidationConfigurationFromWsdlFile(String wsdlFile) {
        if (X509_ASYM_WSDL.equals(wsdlFile) || X509_SYM_WSDL.equals(wsdlFile)) {
            return TokenType.X509;
        } else if (UT_ASYM_WSDL.equals(wsdlFile) || UT_SYM_WSDL.equals(wsdlFile) || UT_TRANSPORT_WSDL.equals(wsdlFile)) {
            return TokenType.USERNAME;
        } else if (AM_BARE_WSDL.equals(wsdlFile) || AM_TRANSPORT_WSDL.equals(wsdlFile)) {
            return TokenType.OPENAM;
        }
        throw new IllegalArgumentException("Unexpected wsdl file specifcation: " + wsdlFile);
    }

    private boolean x509MappingNecessary(String wsdlFile) {
        return X509_ASYM_WSDL.equals(wsdlFile) || X509_SYM_WSDL.equals(wsdlFile);
    }

    private OpenIdConnectTokenConfig buildHMACTokenConfig(STSPublishContext publishContext, Map<String, String> claimMapping) {
        return OpenIdConnectTokenConfig.builder()
                .clientSecret(publishContext.getOidcClientSecret().getBytes())
                .signatureAlgorithm("HS256")
                .setAudience(publishContext.getOidcAudiences())
                .issuer(publishContext.getOidcIssuer())
                .claimMap(claimMapping)
                .build();

    }

    private OpenIdConnectTokenConfig buildRSATokenConfig(STSPublishContext publishContext, Map<String, String> claimMapping) throws UnsupportedEncodingException {
        return OpenIdConnectTokenConfig.builder()
                .keystoreLocation(publishContext.getAmKeystorePath())
                .keystorePassword("changeit".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .signatureKeyAlias("test")
                .signatureKeyPassword("changeit".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .signatureAlgorithm("RS256")
                .setAudience(publishContext.getOidcAudiences())
                .issuer(publishContext.getOidcIssuer())
                        //due to CREST-273, the JwtReconstruction class, used in functional test verification, will throw an exception when
                        //reconstituting an RSA-signed OIDC token which encapsulates a JWK reference to the key which can be used to verify the signature -
                        //so publish with a reference of NONE
                .publicKeyReferenceType(OpenIdConnectTokenPublicKeyReferenceType.NONE)
                .claimMap(claimMapping)
                .build();
    }

    private SAML2Config buildSAML2Config(STSPublishContext publishContext, Map<String, String> attributeMapping) throws IOException {
        try {
            return SAML2Config.builder()
                    .idpId(publishContext.getIdpEntityId())
                    .keystoreFile(publishContext.getAmKeystorePath())
                    .keystorePassword("changeit".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                    .encryptionKeyAlias("test")
                    .signatureKeyAlias("test")
                    .signatureKeyPassword("changeit".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                    .signAssertion(true)
                    .encryptAssertion(false)
                    .encryptAttributes(false)
                    .encryptNameID(false)
                    .encryptionAlgorithm("http://www.w3.org/2001/04/xmlenc#aes128-cbc")
                    .encryptionAlgorithmStrength(128)
                    .attributeMap(attributeMapping)
                    .nameIdFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent")
                    .spEntityId(publishContext.getSpEntityId())
                    .spAcsUrl(publishContext.getSpAcsUrl())
                            //custom statement providers could also be specified.
                    .build();
        } catch (UnsupportedEncodingException e) {
            throw new IOException(e);
        }
    }
}