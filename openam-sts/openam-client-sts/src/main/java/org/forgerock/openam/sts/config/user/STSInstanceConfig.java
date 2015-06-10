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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.config.user;

import org.forgerock.guava.common.base.Objects;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.MapMarshallUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Base class encapsulating STS configuration state common to both the REST and SOAP STS. A builder builds this
 * base class, and is invoked by the builders in the REST/SOAP sublcasses.
 * For an explanation of what's going on with the builders in this class and its subclasses,
 * see https://weblogs.java.net/blog/emcmanus/archive/2010/10/25/using-builder-pattern-subclasses
 *
 * Also attempted to marshal the RestSTSInstanceConfig to/from json with the jackson ObjectMapper. But I was adding
 * {@code @JsonSerialize} and {@code @JsonDeserialize} annotations, and because builder-based classes don't expose ctors which
 * take the complete field set, I would have to create {@code @JsonCreator} instances which would have to pull all of the
 * values out of a map anyway, which is 75% of the way towards a hand-rolled json marshalling implementation based on
 * json-fluent. So a hand-rolled implementation it is. See toJson and fromJson methods.
 */
public class STSInstanceConfig {
    /*
    The following two constants define the key names for the json maps that encapsulate state for both rest and soap
    STS instances.
     */
    protected static final String SAML2_CONFIG = "saml2-config";
    protected static final String DEPLOYMENT_CONFIG = "deployment-config";
    protected static final String OIDC_ID_TOKEN_CONFIG = "oidc-id-token-config";

    public static abstract class STSInstanceConfigBuilderBase<T extends STSInstanceConfigBuilderBase<T>> {
        private SAML2Config saml2Config;
        private OpenIdConnectTokenConfig openIdConnectTokenConfig;

        protected abstract T self();

        public T saml2Config(SAML2Config saml2Config) {
            this.saml2Config = saml2Config;
            return self();
        }

        public T oidcIdTokenConfig(OpenIdConnectTokenConfig openIdConnectTokenConfig) {
            this.openIdConnectTokenConfig = openIdConnectTokenConfig;
            return self();
        }

        public STSInstanceConfig build() {
            return new STSInstanceConfig(this);
        }
    }

    public static class STSInstanceConfigBuilder extends STSInstanceConfigBuilderBase<STSInstanceConfigBuilder> {
        @Override
        protected STSInstanceConfigBuilder self() {
            return this;
        }
    }

    protected final SAML2Config saml2Config;
    protected final OpenIdConnectTokenConfig openIdConnectTokenConfig;

    protected STSInstanceConfig(STSInstanceConfigBuilderBase<?> builder) {
        saml2Config = builder.saml2Config;
        openIdConnectTokenConfig = builder.openIdConnectTokenConfig;
    }

    /**
     *
     * @return The SAML2Config object which specifies the state necessary for STS-instance-specific SAML2 assertions to
     * be generated. This state is used by the token generation service.
     */
    public SAML2Config getSaml2Config() {
        return saml2Config;
    }

    public OpenIdConnectTokenConfig getOpenIdConnectTokenConfig() {
        return openIdConnectTokenConfig;
    }

    public static STSInstanceConfigBuilderBase<?> builder() {
        return new STSInstanceConfigBuilder();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("STSInstanceConfig instance:\n");
        sb.append('\t').append("saml2Config: ").append(saml2Config).append('\n');
        sb.append('\t').append("OpenIdConnectTokenConfig: ").append(openIdConnectTokenConfig).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof STSInstanceConfig) {
            STSInstanceConfig otherConfig = (STSInstanceConfig)other;
            return  Objects.equal(saml2Config, otherConfig.getSaml2Config()) &&
                    Objects.equal(openIdConnectTokenConfig, otherConfig.getOpenIdConnectTokenConfig());
        }
        return false;
    }

    public JsonValue toJson() {
        JsonValue jsonValue =  new JsonValue(new HashMap<String, Object>());
        if (saml2Config != null) {
            jsonValue.add(SAML2_CONFIG, saml2Config.toJson());
        }
        if (openIdConnectTokenConfig != null) {
            jsonValue.add(OIDC_ID_TOKEN_CONFIG, openIdConnectTokenConfig.toJson());
        }
        return jsonValue;
    }

    public static STSInstanceConfig fromJson(JsonValue json) {
        STSInstanceConfigBuilderBase builder =  STSInstanceConfig.builder();
        final JsonValue samlConfig = json.get(SAML2_CONFIG);
        if (!samlConfig.isNull()) {
            builder.saml2Config(SAML2Config.fromJson(samlConfig));
        }

        final JsonValue oidcConfig = json.get(OIDC_ID_TOKEN_CONFIG);
        if (!oidcConfig.isNull()) {
            builder.oidcIdTokenConfig(OpenIdConnectTokenConfig.fromJson(oidcConfig));
        }
        return builder.build();
    }

    public Map<String, Set<String>> marshalToAttributeMap() {
        Map<String, Set<String>> attributes = MapMarshallUtils.toSmsMap(toJson().asMap());
        /*
        the Map<String, Object> expected by the SMS is a flat structure - so I need to flatten all
        nested elements.
         */
        if (saml2Config != null) {
            attributes.remove(SAML2_CONFIG);
            attributes.putAll(saml2Config.marshalToAttributeMap());
        }
        if (openIdConnectTokenConfig != null) {
            attributes.remove(OIDC_ID_TOKEN_CONFIG);
            attributes.putAll(openIdConnectTokenConfig.marshalToAttributeMap());
        }
        return attributes;
    }

    public static STSInstanceConfig marshalFromAttributeMap(Map<String, Set<String>> attributeMap) {
        Map<String, Object> jsonAttributes = MapMarshallUtils.toJsonValueMap(attributeMap);
        /*
        If SAML2Config state is not present, null will be returned. That will tell me that no SAML2Config
        had been present initially.
         */
        SAML2Config saml2Config = SAML2Config.marshalFromAttributeMap(attributeMap);
        if (saml2Config != null) {
            jsonAttributes.put(SAML2_CONFIG, saml2Config.toJson());
        }

        OpenIdConnectTokenConfig openIdConnectTokenConfig = OpenIdConnectTokenConfig.marshalFromAttributeMap(attributeMap);
        if (openIdConnectTokenConfig != null) {
            jsonAttributes.put(OIDC_ID_TOKEN_CONFIG, openIdConnectTokenConfig.toJson());
        }
        return fromJson(new JsonValue(jsonAttributes));
    }
}
