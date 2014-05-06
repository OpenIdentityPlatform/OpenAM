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

package org.forgerock.openam.sts.config.user;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SAML2ConfigTest {
    private static final String AUTH_CONTEXT = "authContext";
    private static final String NAME_ID_FORMAT = "nameidformat";
    private static final long TOKEN_LIFETIME = 60 * 10;
    private static final String CUSTOM_SUBJECT_PROVIDER = "org.foo.MyCustomSubjectProvider";
    private static final String CUSTOM_CONDITIONS_PROVIDER = "org.foo.MyCustomConditionsProvider";
    private static final String CUSTOM_ATTRIBUTE_STATEMENTS_PROVIDER = "org.foo.MyCustomAttributeStatementsProvider";
    private static final String CUSTOM_ATTRIBUTE_MAPPER = "org.foo.MyCustomAttributeMapper";
    private static final String CUSTOM_AUTHENTICATION_STATEMENTS_PROVIDER = "org.foo.MyCustomAuthenticationStatementsProvider";
    private static final boolean WITH_ATTR_MAP = true;
    private static final boolean WITH_AUDIENCES = true;
    private static final boolean WITH_CUSTOM_PROVIDERS = true;

    @Test
    public void testEquals() {
        SAML2Config config1 = buildConfig(WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS);
        SAML2Config config2 = buildConfig(WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS);
        assertTrue(config1.equals(config2));

        config1 = buildConfig(!WITH_ATTR_MAP, WITH_AUDIENCES, !WITH_CUSTOM_PROVIDERS);
        config2 = buildConfig(!WITH_ATTR_MAP, WITH_AUDIENCES, !WITH_CUSTOM_PROVIDERS);
        assertTrue(config1.equals(config2));

        config1 = buildConfig(WITH_ATTR_MAP, !WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS);
        config2 = buildConfig(WITH_ATTR_MAP, !WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS);
        assertTrue(config1.equals(config2));

        config1 = buildConfig(WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS);
        config2 = buildConfig(!WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS);
        assertFalse(config1.equals(config2));

        config1 = buildConfig(!WITH_ATTR_MAP, !WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS);
        config2 = buildConfig(!WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS);
        assertFalse(config1.equals(config2));

        config1 = buildConfig(WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS);
        config2 = buildConfig(WITH_ATTR_MAP, WITH_AUDIENCES, !WITH_CUSTOM_PROVIDERS);
        assertFalse(config1.equals(config2));
    }

    @Test
    public void testJsonRoundTrip1() {
        SAML2Config config = buildConfig(WITH_ATTR_MAP, WITH_AUDIENCES, !WITH_CUSTOM_PROVIDERS);
        assertTrue(config.equals(SAML2Config.fromJson(config.toJson())));
    }

    @Test
    public void testJsonRoundTrip2() {
        SAML2Config config = buildConfig(!WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS);
        assertTrue(config.equals(SAML2Config.fromJson(config.toJson())));
    }

    @Test
    public void testJsonRoundTrip3() {
        SAML2Config config = buildConfig(WITH_ATTR_MAP, !WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS);
        assertTrue(config.equals(SAML2Config.fromJson(config.toJson())));
    }

    @Test
    public void testJsonRoundTrip4() {
        SAML2Config config = buildConfig(!WITH_ATTR_MAP, !WITH_AUDIENCES, !WITH_CUSTOM_PROVIDERS);
        assertTrue(config.equals(SAML2Config.fromJson(config.toJson())));
    }

    @Test
    public void testFieldPersistenceAfterRoundTrip() {
        SAML2Config config = buildConfig(!WITH_ATTR_MAP, !WITH_AUDIENCES, !WITH_CUSTOM_PROVIDERS);
        SAML2Config reconsitutedConfig = SAML2Config.fromJson(config.toJson());
        assertTrue(AUTH_CONTEXT.equals(reconsitutedConfig.getAuthenticationContext()));
        assertTrue(NAME_ID_FORMAT.equals(reconsitutedConfig.getNameIdFormat()));
        assertTrue(TOKEN_LIFETIME == reconsitutedConfig.getTokenLifetimeInSeconds());

        config = buildConfig(WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS);
        reconsitutedConfig = SAML2Config.fromJson(config.toJson());
        assertTrue(AUTH_CONTEXT.equals(reconsitutedConfig.getAuthenticationContext()));
        assertTrue(NAME_ID_FORMAT.equals(reconsitutedConfig.getNameIdFormat()));
        assertTrue(TOKEN_LIFETIME == reconsitutedConfig.getTokenLifetimeInSeconds());
        assertTrue(CUSTOM_CONDITIONS_PROVIDER.equals(reconsitutedConfig.getCustomConditionsProviderClassName()));
        assertTrue(CUSTOM_SUBJECT_PROVIDER.equals(reconsitutedConfig.getCustomSubjectProviderClassName()));
        assertTrue(CUSTOM_AUTHENTICATION_STATEMENTS_PROVIDER.equals(reconsitutedConfig.getCustomAuthenticationStatementsProviderClassName()));
        assertTrue(CUSTOM_ATTRIBUTE_STATEMENTS_PROVIDER.equals(reconsitutedConfig.getCustomAttributeStatementsProviderClassName()));
        assertTrue(CUSTOM_ATTRIBUTE_MAPPER.equals(reconsitutedConfig.getCustomAttributeMapperClassName()));
    }

    private SAML2Config buildConfig(boolean withAttributeMap, boolean withAudiences, boolean withCustomProviders) {
        SAML2Config.SAML2ConfigBuilder builder = SAML2Config.builder()
                .authenticationContext(AUTH_CONTEXT)
                .tokenLifetimeInSeconds(TOKEN_LIFETIME)
                .nameIdFormat(NAME_ID_FORMAT);
        if (withAttributeMap) {
            Map<String, String> attrMap = new HashMap<String, String>();
            attrMap.put("saml_attr", "ldap_attr");
            attrMap.put("saml_attr1", "ldap_attr1");
            builder.attributeMap(attrMap);
        }
        if (withAudiences) {
            List<String> audiences = new ArrayList<String>();
            audiences.add("sp_acs1");
            audiences.add("sp_acs2");
            builder.audiences(audiences);
        }

        if (withCustomProviders) {
            builder.customConditionsProviderClassName(CUSTOM_CONDITIONS_PROVIDER);
            builder.customSubjectProviderClassName(CUSTOM_SUBJECT_PROVIDER);
            builder.customAuthenticationStatementsProviderClassName(CUSTOM_AUTHENTICATION_STATEMENTS_PROVIDER);
            builder.customAttributeStatementsProviderClassName(CUSTOM_ATTRIBUTE_STATEMENTS_PROVIDER);
            builder.customAttributeMapperClassName(CUSTOM_ATTRIBUTE_MAPPER);
        }
        return builder.build();
    }
}
