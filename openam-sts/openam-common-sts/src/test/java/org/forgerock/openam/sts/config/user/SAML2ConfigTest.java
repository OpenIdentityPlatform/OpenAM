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
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SAML2ConfigTest {
    private static final String NAME_ID_FORMAT = "nameidformat";
    private static final long TOKEN_LIFETIME = 60 * 10;
    private static final String CUSTOM_SUBJECT_PROVIDER = "org.foo.MyCustomSubjectProvider";
    private static final String CUSTOM_CONDITIONS_PROVIDER = "org.foo.MyCustomConditionsProvider";
    private static final String CUSTOM_ATTRIBUTE_STATEMENTS_PROVIDER = "org.foo.MyCustomAttributeStatementsProvider";
    private static final String CUSTOM_ATTRIBUTE_MAPPER = "org.foo.MyCustomAttributeMapper";
    private static final String CUSTOM_AUTHN_CONTEXT_MAPPER = "org.foo.MyCustomAuthNContextMapper";
    private static final String CUSTOM_AUTHENTICATION_STATEMENTS_PROVIDER = "org.foo.MyCustomAuthenticationStatementsProvider";
    private static final String CUSTOM_AUTHZ_DECISION_STATEMENTS_PROVIDER = "org.foo.MyCustomAuthzDecisionStatementsProvider";
    private static final boolean WITH_ATTR_MAP = true;
    private static final boolean WITH_AUDIENCES = true;
    private static final boolean WITH_CUSTOM_PROVIDERS = true;
    private static final boolean SIGN_ASSERTION = true;

    @Test
    public void testEquals() {
        SAML2Config config1 = buildConfig(WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION);
        SAML2Config config2 = buildConfig(WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION);
        assertEquals(config1, config2);

        config1 = buildConfig(!WITH_ATTR_MAP, WITH_AUDIENCES, !WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION);
        config2 = buildConfig(!WITH_ATTR_MAP, WITH_AUDIENCES, !WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION);
        assertEquals(config1, config2);

        config1 = buildConfig(WITH_ATTR_MAP, !WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION);
        config2 = buildConfig(WITH_ATTR_MAP, !WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION);
        assertEquals(config1, config2);

        config1 = buildConfig(WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION);
        config2 = buildConfig(!WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION);
        assertNotEquals(config1, config2);

        config1 = buildConfig(!WITH_ATTR_MAP, !WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION);
        config2 = buildConfig(!WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION);
        assertNotEquals(config1, config2);

        config1 = buildConfig(WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION);
        config2 = buildConfig(WITH_ATTR_MAP, WITH_AUDIENCES, !WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION);
        assertNotEquals(config1, config2);

        config1 = buildConfig(WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION);
        config2 = buildConfig(WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION);
        assertNotEquals(config1, config2);
    }

    @Test
    public void testJsonRoundTrip1() {
        SAML2Config config = buildConfig(WITH_ATTR_MAP, WITH_AUDIENCES, !WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION);
        assertEquals(config, SAML2Config.fromJson(config.toJson()));
    }

    @Test
    public void testJsonRoundTrip2() {
        SAML2Config config = buildConfig(!WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION);
        assertEquals(config, SAML2Config.fromJson(config.toJson()));
    }

    @Test
    public void testJsonRoundTrip3() {
        SAML2Config config = buildConfig(WITH_ATTR_MAP, !WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION);
        assertEquals(config, SAML2Config.fromJson(config.toJson()));
    }

    @Test
    public void testJsonRoundTrip4() {
        SAML2Config config = buildConfig(!WITH_ATTR_MAP, !WITH_AUDIENCES, !WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION);
        assertEquals(config, SAML2Config.fromJson(config.toJson()));
    }

    @Test
    public void testToString() {
        //build a bunch of instances and call toString to insure no NPE results
        buildConfig(!WITH_ATTR_MAP, !WITH_AUDIENCES, !WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION).toString();
        buildConfig(WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION).toString();
        buildConfig(WITH_ATTR_MAP, !WITH_AUDIENCES, !WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION).toString();
        buildConfig(!WITH_ATTR_MAP, WITH_AUDIENCES, !WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION).toString();
        buildConfig(!WITH_ATTR_MAP, !WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION).toString();
        buildConfig(WITH_ATTR_MAP, WITH_AUDIENCES, !WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION).toString();
        buildConfig(!WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION).toString();
        buildConfig(!WITH_ATTR_MAP, WITH_AUDIENCES, !WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION).toString();
    }

    @Test
    public void testFieldPersistenceAfterRoundTrip() {
        SAML2Config config = buildConfig(!WITH_ATTR_MAP, !WITH_AUDIENCES, !WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION);
        SAML2Config reconsitutedConfig = SAML2Config.fromJson(config.toJson());
        assertEquals(NAME_ID_FORMAT, reconsitutedConfig.getNameIdFormat());
        assertEquals(TOKEN_LIFETIME, reconsitutedConfig.getTokenLifetimeInSeconds());

        config = buildConfig(WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION);
        reconsitutedConfig = SAML2Config.fromJson(config.toJson());
        assertEquals(SIGN_ASSERTION, reconsitutedConfig.signAssertion());
        assertEquals(NAME_ID_FORMAT, reconsitutedConfig.getNameIdFormat());
        assertEquals(TOKEN_LIFETIME, reconsitutedConfig.getTokenLifetimeInSeconds());
        assertEquals(CUSTOM_CONDITIONS_PROVIDER, reconsitutedConfig.getCustomConditionsProviderClassName());
        assertEquals(CUSTOM_SUBJECT_PROVIDER, reconsitutedConfig.getCustomSubjectProviderClassName());
        assertEquals(CUSTOM_AUTHENTICATION_STATEMENTS_PROVIDER, reconsitutedConfig.getCustomAuthenticationStatementsProviderClassName());
        assertEquals(CUSTOM_ATTRIBUTE_STATEMENTS_PROVIDER, reconsitutedConfig.getCustomAttributeStatementsProviderClassName());
        assertEquals(CUSTOM_ATTRIBUTE_MAPPER, reconsitutedConfig.getCustomAttributeMapperClassName());
        assertEquals(CUSTOM_AUTHN_CONTEXT_MAPPER, reconsitutedConfig.getCustomAuthNContextMapperClassName());
    }

    @Test
    public void testMapMarshalRoundTrip() {
        SAML2Config saml2Config = buildConfig(WITH_ATTR_MAP, WITH_AUDIENCES, !WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION);
        assertEquals(saml2Config, SAML2Config.marshalFromAttributeMap(saml2Config.marshalToAttributeMap()));

        saml2Config = buildConfig(!WITH_ATTR_MAP, !WITH_AUDIENCES, !WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION);
        assertEquals(saml2Config, SAML2Config.marshalFromAttributeMap(saml2Config.marshalToAttributeMap()));

        saml2Config = buildConfig(WITH_ATTR_MAP, WITH_AUDIENCES, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION);
        assertEquals(saml2Config, SAML2Config.marshalFromAttributeMap(saml2Config.marshalToAttributeMap()));

        saml2Config = buildConfig(!WITH_ATTR_MAP, !WITH_AUDIENCES, !WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION);
        assertEquals(saml2Config, SAML2Config.marshalFromAttributeMap(saml2Config.marshalToAttributeMap()));
    }

    private SAML2Config buildConfig(boolean withAttributeMap, boolean withAudiences, boolean withCustomProviders, boolean signAssertion) {
        SAML2Config.SAML2ConfigBuilder builder = SAML2Config.builder()
                .tokenLifetimeInSeconds(TOKEN_LIFETIME)
                .signAssertion(signAssertion)
                .nameIdFormat(NAME_ID_FORMAT);
        if (withAttributeMap) {
            Map<String, String> attrMap = new LinkedHashMap<String, String>();
            attrMap.put("saml_attr", "ldap_attr");
            attrMap.put("saml_attr1", "ldap_attr1");
            builder.attributeMap(attrMap);
        }
        if (withAudiences) {
            Set<String> audiences = new HashSet<String>();
            audiences.add("sp_acs1");
            audiences.add("sp_acs2");
            builder.audiences(audiences);
        }

        if (withCustomProviders) {
            builder.customConditionsProviderClassName(CUSTOM_CONDITIONS_PROVIDER);
            builder.customSubjectProviderClassName(CUSTOM_SUBJECT_PROVIDER);
            builder.customAuthenticationStatementsProviderClassName(CUSTOM_AUTHENTICATION_STATEMENTS_PROVIDER);
            builder.customAttributeStatementsProviderClassName(CUSTOM_ATTRIBUTE_STATEMENTS_PROVIDER);
            builder.customAuthzDecisionStatementsProviderClassName(CUSTOM_AUTHZ_DECISION_STATEMENTS_PROVIDER);
            builder.customAttributeMapperClassName(CUSTOM_ATTRIBUTE_MAPPER);
            builder.customAuthNContextMapperClassName(CUSTOM_AUTHN_CONTEXT_MAPPER);
        }
        return builder.build();
    }
}
