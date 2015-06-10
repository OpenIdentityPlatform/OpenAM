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

package org.forgerock.openam.sts.config.user;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertEquals;

import org.forgerock.guava.common.collect.Lists;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithmType;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenIdConnectTokenConfigTest {
    private static final boolean WITH_ATTR_MAP = true;
    private static final boolean WITH_AZP = true;
    private static final long TOKEN_LIFETIME = 60 * 10;
    private static final String ISSUER = "da_issuer";
    private static final List<String> AUDIENCE = Lists.newArrayList("foo_audience");
    private static final byte[] CLIENT_SECRET = "super_bobo".getBytes();
    private static final String AUTHORIZED_PARTY = "foo_azp";
    private static final String SIGNATURE_KEY_ALIAS = "sig_key_alias";
    private static final byte[] SIGNATURE_KEY_PASSWORD = "super_secret".getBytes();
    private static final String KEYSTORE_LOCATION = "/user/home/dillrod/keystore";
    private static final byte[] KEYSTORE_PASSWORD = "super_secret".getBytes();
    private final Map<String, String> attributeMap;

    public OpenIdConnectTokenConfigTest() {
        attributeMap = new HashMap<String, String>();
        attributeMap.put("saml_attr", "ldap_attr");
        attributeMap.put("saml_attr1", "ldap_attr1");
    }

    @Test
    public void testEquals() {
        OpenIdConnectTokenConfig config1 = buildConfig(WITH_ATTR_MAP, WITH_AZP, JwsAlgorithm.RS256);
        OpenIdConnectTokenConfig config2 = buildConfig(WITH_ATTR_MAP, WITH_AZP, JwsAlgorithm.RS256);
        assertEquals(config1, config2);

        config1 = buildConfig(!WITH_ATTR_MAP, WITH_AZP, JwsAlgorithm.HS256);
        config2 = buildConfig(!WITH_ATTR_MAP, WITH_AZP, JwsAlgorithm.HS256);
        assertEquals(config1, config2);

        config1 = buildConfig(!WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.NONE);
        config2 = buildConfig(!WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.NONE);
        assertEquals(config1, config2);

        config1 = buildConfig(WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.HS256);
        config2 = buildConfig(WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.HS256);
        assertEquals(config1, config2);

        config1 = buildConfig(WITH_ATTR_MAP, WITH_AZP, JwsAlgorithm.HS256);
        config2 = buildConfig(!WITH_ATTR_MAP, WITH_AZP, JwsAlgorithm.HS256);
        assertNotEquals(config1, config2);

        config1 = buildConfig(WITH_ATTR_MAP, WITH_AZP, JwsAlgorithm.HS256);
        config2 = buildConfig(!WITH_ATTR_MAP, WITH_AZP, JwsAlgorithm.HS256);
        assertNotEquals(config1, config2);

        config1 = buildConfig(!WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.HS256);
        config2 = buildConfig(!WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.RS256);
        assertNotEquals(config1, config2);

        config1 = buildConfig(WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.NONE);
        config2 = buildConfig(WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.RS256);
        assertNotEquals(config1, config2);

        config1 = buildConfig(WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.HS256);
        config2 = buildConfig(WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.RS256);
        assertNotEquals(config1, config2);

        config1 = buildConfig(WITH_ATTR_MAP, WITH_AZP, JwsAlgorithm.RS256);
        config2 = buildConfig(WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.RS256);
        assertNotEquals(config1, config2);
    }

    @Test
    public void testJsonRoundTrip() {
        OpenIdConnectTokenConfig config = buildConfig(!WITH_ATTR_MAP, WITH_AZP, JwsAlgorithm.HS256);
        assertEquals(config, OpenIdConnectTokenConfig.fromJson(config.toJson()));

        config = buildConfig(!WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.RS256);
        assertEquals(config, OpenIdConnectTokenConfig.fromJson(config.toJson()));

        config = buildConfig(!WITH_ATTR_MAP, WITH_AZP, JwsAlgorithm.NONE);
        assertEquals(config, OpenIdConnectTokenConfig.fromJson(config.toJson()));

        config = buildConfig(WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.HS256);
        assertEquals(config, OpenIdConnectTokenConfig.fromJson(config.toJson()));

        config = buildConfig(WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.RS256);
        assertEquals(config, OpenIdConnectTokenConfig.fromJson(config.toJson()));

        config = buildConfig(WITH_ATTR_MAP, WITH_AZP, JwsAlgorithm.NONE);
        assertEquals(config, OpenIdConnectTokenConfig.fromJson(config.toJson()));
    }

    @Test
    public void testToString() {
        //build a bunch of instances and call toString to insure no NPE results
        buildConfig(!WITH_ATTR_MAP, WITH_AZP, JwsAlgorithm.HS256).toString();
        buildConfig(WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.RS256).toString();
        buildConfig(WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.NONE).toString();
    }

    @Test
    public void testFieldPersistenceAfterRoundTrip() {
        OpenIdConnectTokenConfig config = buildConfig(WITH_ATTR_MAP, WITH_AZP, JwsAlgorithm.RS256);
        OpenIdConnectTokenConfig reconsitutedConfig = OpenIdConnectTokenConfig.fromJson(config.toJson());
        assertEquals(ISSUER, reconsitutedConfig.getIssuer());
        assertEquals(JwsAlgorithm.RS256, reconsitutedConfig.getSignatureAlgorithm());
        assertEquals(TOKEN_LIFETIME, reconsitutedConfig.getTokenLifetimeInSeconds());
        assertEquals(attributeMap, reconsitutedConfig.getClaimMap());
        assertEquals(KEYSTORE_LOCATION, reconsitutedConfig.getKeystoreLocation());
        assertEquals(KEYSTORE_PASSWORD, reconsitutedConfig.getKeystorePassword());
        assertEquals(SIGNATURE_KEY_ALIAS, reconsitutedConfig.getSignatureKeyAlias());
        assertEquals(SIGNATURE_KEY_PASSWORD, reconsitutedConfig.getSignatureKeyPassword());
        assertEquals(AUDIENCE, reconsitutedConfig.getAudience());
        assertEquals(AUTHORIZED_PARTY, reconsitutedConfig.getAuthorizedParty());
    }

    @Test
    public void testMapMarshalRoundTrip() {
        OpenIdConnectTokenConfig oidcConfig = buildConfig(!WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.RS256);
        assertEquals(oidcConfig, OpenIdConnectTokenConfig.marshalFromAttributeMap(oidcConfig.marshalToAttributeMap()));

        oidcConfig = buildConfig(!WITH_ATTR_MAP, WITH_AZP, JwsAlgorithm.HS256);
        assertEquals(oidcConfig, OpenIdConnectTokenConfig.marshalFromAttributeMap(oidcConfig.marshalToAttributeMap()));

        oidcConfig = buildConfig(WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.NONE);
        assertEquals(oidcConfig, OpenIdConnectTokenConfig.marshalFromAttributeMap(oidcConfig.marshalToAttributeMap()));

        oidcConfig = buildConfig(!WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.RS256);
        assertEquals(oidcConfig, OpenIdConnectTokenConfig.marshalFromAttributeMap(oidcConfig.marshalToAttributeMap()));

        oidcConfig = buildConfig(WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.RS256);
        assertEquals(oidcConfig, OpenIdConnectTokenConfig.marshalFromAttributeMap(oidcConfig.marshalToAttributeMap()));

        oidcConfig = buildConfig(WITH_ATTR_MAP, WITH_AZP, JwsAlgorithm.RS256);
        assertEquals(oidcConfig, OpenIdConnectTokenConfig.marshalFromAttributeMap(oidcConfig.marshalToAttributeMap()));

        oidcConfig = buildConfig(WITH_ATTR_MAP, !WITH_AZP, JwsAlgorithm.HS256);
        assertEquals(oidcConfig, OpenIdConnectTokenConfig.marshalFromAttributeMap(oidcConfig.marshalToAttributeMap()));
    }

    private OpenIdConnectTokenConfig buildConfig(boolean withAttributeMap, boolean withAuthorizedParty, JwsAlgorithm signatureAlgorithm) {
        OpenIdConnectTokenConfig.OIDCIdTokenConfigBuilder builder = OpenIdConnectTokenConfig.builder()
                .tokenLifetimeInSeconds(TOKEN_LIFETIME)
                .signatureAlgorithm(signatureAlgorithm)
                .issuer(ISSUER)
                .setAudience(AUDIENCE);
        if (withAuthorizedParty) {
            builder.authorizedParty(AUTHORIZED_PARTY);
        }
        if (withAttributeMap) {
            builder.claimMap(attributeMap);
        }


        if (JwsAlgorithmType.RSA.equals(signatureAlgorithm.getAlgorithmType())) {
            builder.keystoreLocation(KEYSTORE_LOCATION);
            builder.keystorePassword(KEYSTORE_PASSWORD);
            builder.signatureKeyAlias(SIGNATURE_KEY_ALIAS);
            builder.signatureKeyPassword(SIGNATURE_KEY_PASSWORD);
        } else if (JwsAlgorithmType.HMAC.equals(signatureAlgorithm.getAlgorithmType())) {
            builder.clientSecret(CLIENT_SECRET);
        }
        return builder.build();
    }
}
