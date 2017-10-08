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

package com.forgerock.openam.functionaltest.sts.frmwk.rest;

import com.forgerock.openam.functionaltest.sts.frmwk.common.CommonConstants;
import com.forgerock.openam.functionaltest.sts.frmwk.common.STSPublishContext;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.config.user.DeploymentConfig;
import org.forgerock.openam.sts.config.user.OpenIdConnectTokenConfig;
import org.forgerock.openam.sts.config.user.OpenIdConnectTokenPublicKeyReferenceType;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;

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
 * This class allows for the generation of RestSTSInstanceConfig state used to publish rest-sts instances. It is intended
 * to allow for the convenient generation of RestSTSInstanceConfig instances used to publish rest-sts instances. It is
 * guided by STSPublishContext state, but hard-codes many of the options for caller convenience. If any of the
 * currently-hard-coded options need to be configurable, they can be added to the STSPublishContext class, and referenced
 * from there. The point is to be able to generate a reasonably small set of RestSTSInstanceConfig instances to be used
 * in the functional tests, while encapsulating all of the myriad configuration options for caller convenience.
 *
 *
 */
public class RestSTSInstanceConfigFactory {
    public RestSTSInstanceConfig createRestSTSInstanceConfig(String urlElement, String realm,
                                                             STSPublishContext stsPublishContext) throws IOException {
        return createRestSTSInstanceConfig(urlElement, realm, stsPublishContext, CustomTokenOperationContext.builder().build());
    }

    /**
     * This method creates the RestSTSInstanceConfig instance which determines the nature of the published rest sts
     * instance. Note that this method does not take parameters corresponding to all options. It is there only to demonstrate
     * some of the options which could be set.
     * @param urlElement The deployment url of the rest-sts instance.
     * @param realm The realm in which the rests-sts instance will be deployed. Note that the url of the published rest-sts
     *              instance will be composed of the OpenAM deploymentUrl + realm + /urlElement.
     * @param stsPublishContext Determines what sort of tokens the published sts instance will produce
     * @param customTokenOperationContext encapsulates custom token operation state - usually the custom operation definitions will simply be
     *                                    empty lists.
     * @return A RestSTSInstanceConfig configuring the published rest-sts instance
     * @throws Exception If something goes wrong
     */
    public RestSTSInstanceConfig createRestSTSInstanceConfig(String urlElement, String realm,
                                                             STSPublishContext stsPublishContext,
                                                             CustomTokenOperationContext customTokenOperationContext) throws IOException {
        /*
        if you want to target a specific module or service for a particular token type, add it via the
        addMapping call below. See org.forgerock.openam.forgerockrest.authn.core.AuthIndexType for the
        list of valid authIndexType values (the second parameter in addMapping). The third parameter is
        simply the name of the specified module, service, etc. If you want to target the default service,
        don't add a mapping, or add a mapping corresponding to the default service, as below.
        */

        /*
        Build the context necessary for the OIDC token validation. The value in the map must correspond to the
        header which the OIDC module will consult to obtain the oidc id token.
         */
        Map<String, String> oidcContext = new HashMap<>();
        oidcContext.put(AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_AUTH_TARGET_HEADER_KEY, CommonConstants.DEFAULT_OIDC_TOKEN_HEADER_NAME);

        /*
         Build the context necessary for Cert validation. The value in the map must correspond to the
         header which the Cert module will consult to obtain the client's Certificate.
         */
        Map<String, String> certContext = new HashMap<>();
        certContext.put(AMSTSConstants.X509_TOKEN_AUTH_TARGET_HEADER_KEY, CommonConstants.DEFAULT_CERT_MODULE_TOKEN_HEADER_NAME);
        AuthTargetMapping mapping = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "service", "ldapService")
                .addMapping(TokenType.OPENIDCONNECT, "module", CommonConstants.DEFAULT_OIDC_BEARER_TOKEN_MODULE_NAME, oidcContext)
                .addMapping(TokenType.X509, "module", CommonConstants.DEFAULT_CERT_MODULE_NAME, certContext)
                .build();

        DeploymentConfig.DeploymentConfigBuilderBase<?> deploymentConfigBuilder =
                DeploymentConfig.builder()
                        .uriElement(urlElement)
                        .authTargetMapping(mapping)
                        .realm(realm);
        /*
        If the clientCertHeader field is specified, this implies that the client cert for x509 transformations should
        be specified in a header field. If this is the case, then the set of IP addrs corresponding to the
        tls offload engines must also be specified. I will simply specify the ip addr of this client, as it will be
        the one invoking token transformation functionality. If all hosts should be trusted, add 'any' to the list.
         */
        if (stsPublishContext.getClientCertHeaderName() != null) {
            Set<String> offloadHostsSet = new HashSet<>();
            offloadHostsSet.add(InetAddress.getLocalHost().getHostAddress());
            offloadHostsSet.add("127.0.0.1");
            deploymentConfigBuilder
                    .offloadedTwoWayTLSHeaderKey(stsPublishContext.getClientCertHeaderName())
                    .tlsOffloadEngineHostIpAddrs(offloadHostsSet);
        }
        DeploymentConfig deploymentConfig = deploymentConfigBuilder.build();

        Map<String, String> attributeMapping = new HashMap<>();
        attributeMapping.put("email", "mail");
        SAML2Config saml2Config = null;
        if (publishSAML2(stsPublishContext)) {
            saml2Config = buildSAML2Config(stsPublishContext, attributeMapping);
        }

        OpenIdConnectTokenConfig oidcIdTokenConfig = null;
        if (publishOIDC(stsPublishContext)) {
            if (STSPublishContext.OIDCSigningAlgorithmType.RSA.equals(stsPublishContext.getOidcSigningAlgorithmType())) {
                oidcIdTokenConfig = buildRSATokenConfig(stsPublishContext, attributeMapping);
            } else {
                oidcIdTokenConfig = buildHMACTokenConfig(stsPublishContext, attributeMapping);
            }
        }
        RestSTSInstanceConfig.RestSTSInstanceConfigBuilder builder =
                RestSTSInstanceConfig.builder()
                        .deploymentConfig(deploymentConfig)
                        .saml2Config(saml2Config)
                        .oidcIdTokenConfig(oidcIdTokenConfig)
                        .persistIssuedTokensInCTS(stsPublishContext.persistIssuedTokensInCTS())
                        .setCustomProviders(customTokenOperationContext.getCustomProviders())
                        .setCustomValidators(customTokenOperationContext.getCustomValidators())
                        .setCustomTokenTransforms(customTokenOperationContext.getCustomTransforms());

        if (publishSAML2(stsPublishContext)) {
            builder
                    .addSupportedTokenTransform(
                            TokenType.USERNAME,
                            TokenType.SAML2,
                            AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                    .addSupportedTokenTransform(
                            TokenType.OPENAM,
                            TokenType.SAML2,
                            !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                    .addSupportedTokenTransform(
                            TokenType.OPENIDCONNECT,
                            TokenType.SAML2,
                            AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                    .addSupportedTokenTransform(
                            TokenType.X509,
                            TokenType.SAML2,
                            AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        }
        if (publishOIDC(stsPublishContext)) {
            builder
                    .addSupportedTokenTransform(
                            TokenType.USERNAME,
                            TokenType.OPENIDCONNECT,
                            AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                    .addSupportedTokenTransform(
                            TokenType.OPENAM,
                            TokenType.OPENIDCONNECT,
                            !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                    .addSupportedTokenTransform(
                            TokenType.OPENIDCONNECT,
                            TokenType.OPENIDCONNECT,
                            AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                    .addSupportedTokenTransform(
                            TokenType.X509,
                            TokenType.OPENIDCONNECT,
                            AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);

        }
        return builder.build();
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