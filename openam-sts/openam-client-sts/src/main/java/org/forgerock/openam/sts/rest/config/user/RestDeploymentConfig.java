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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.config.user;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.shared.sts.SharedSTSConstants;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;

import org.forgerock.openam.sts.MapMarshallUtils;
import org.forgerock.util.Reject;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;


/**
 * This class represents the deployment configuration for an STS instance. This includes:
 * 1. the AuthTargetMapping instance - i.e. the REST authN context for each token type.
 * 2. The uri element at which the STS instance will be deployed
 * 3. The realm within which the STS is deployed.
 *
 */
public class RestDeploymentConfig {
    /*
    Define the names of fields to aid in json marshalling. Note that these names match the names of the AttributeSchema
    entries in restSTS.xml, as this aids in marshalling an instance of this class into the attribute map needed for
    SMS persistence.
     */
    private static final String REALM = SharedSTSConstants.DEPLOYMENT_REALM;
    private static final String URI_ELEMENT = SharedSTSConstants.DEPLOYMENT_URL_ELEMENT;
    public static final String AUTH_TARGET_MAPPINGS = AuthTargetMapping.AUTH_TARGET_MAPPINGS;
    private static final String OFFLOADED_TWO_WAY_TLS_HEADER_KEY = SharedSTSConstants.OFFLOADED_TWO_WAY_TLS_HEADER_KEY;
    private static final String TLS_OFFLOAD_ENGINE_HOSTS = SharedSTSConstants.TLS_OFFLOAD_ENGINE_HOSTS;

    public static class RestDeploymentConfigBuilder {
        private String uriElement;
        private String realm = "/"; //default value
        private AuthTargetMapping authTargetMapping;
        private String offloadedTwoWayTLSHeaderKey;
        private Set<String> tlsOffloadEngineHostIpAddrs;

        public RestDeploymentConfigBuilder uriElement(String uriElement)  {
            this.uriElement = uriElement;
            return this;
        }

        public RestDeploymentConfigBuilder realm(String realm)  {
            this.realm = realm;
            return this;
        }

        public RestDeploymentConfigBuilder authTargetMapping(AuthTargetMapping authTargetMapping)  {
            this.authTargetMapping = authTargetMapping;
            return this;
        }

        public RestDeploymentConfigBuilder offloadedTwoWayTLSHeaderKey(String offloadedTLSHeaderKey)  {
            this.offloadedTwoWayTLSHeaderKey = offloadedTLSHeaderKey;
            return this;
        }

        public RestDeploymentConfigBuilder tlsOffloadEngineHostIpAddrs(Set<String> tlsOffloadEngineHostIpAddrs) {
            if (tlsOffloadEngineHostIpAddrs != null) {
                this.tlsOffloadEngineHostIpAddrs = new LinkedHashSet<String>(tlsOffloadEngineHostIpAddrs);
            }
            return this;
        }

        public RestDeploymentConfig build() {
            return new RestDeploymentConfig(this);
        }
    }

    private final String uriElement;
    private final String realm;
    private final AuthTargetMapping authTargetMapping;
    private final String offloadedTwoWayTLSHeaderKey;
    private final Set<String> tlsOffloadEngineHostIpAddrs;

    private RestDeploymentConfig(RestDeploymentConfigBuilder builder) {
        uriElement = builder.uriElement;
        realm = builder.realm;
        authTargetMapping = builder.authTargetMapping;
        offloadedTwoWayTLSHeaderKey = builder.offloadedTwoWayTLSHeaderKey; //can be null
        if (builder.tlsOffloadEngineHostIpAddrs != null) {
            tlsOffloadEngineHostIpAddrs = Collections.unmodifiableSet(builder.tlsOffloadEngineHostIpAddrs);
        } else {
            tlsOffloadEngineHostIpAddrs = Collections.emptySet();
        }
        if (((offloadedTwoWayTLSHeaderKey != null) && (tlsOffloadEngineHostIpAddrs.isEmpty())) ||
                ((offloadedTwoWayTLSHeaderKey == null) && (!tlsOffloadEngineHostIpAddrs.isEmpty()))) {
            throw new IllegalStateException("If the offloadedTwoWayTLSHeaderKey is set, so too must the list of " +
                    "tlsOffloadEngineHostIpAddrs.");
        }
        Reject.ifNull(uriElement, "UriElement String cannot be null");
        Reject.ifNull(realm, "Realm String cannot be null");
        Reject.ifNull(authTargetMapping, "AuthTargetMapping cannot be null");

    }

    public static RestDeploymentConfigBuilder builder() {
        return new RestDeploymentConfigBuilder();
    }

    public String getUriElement() {
        return uriElement;
    }

    public String getRealm() {
        return realm;
    }

    public AuthTargetMapping getAuthTargetMapping() {
        return authTargetMapping;
    }

    public String getOffloadedTwoWayTlsHeaderKey() {
        return offloadedTwoWayTLSHeaderKey;
    }

    public Set<String> getTlsOffloadEngineHostIpAddrs() {
        return tlsOffloadEngineHostIpAddrs;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RestDeploymentConfig instance: ").append('\n');
        sb.append('\t').append("Deployment uriElement: ").append(uriElement).append('\n');
        sb.append('\t').append("realm: ").append(realm).append('\n');
        sb.append('\t').append("authTargetMapping: ").append(authTargetMapping).append('\n');
        sb.append('\t').append("offloadedTwoWayTLSHeaderKey: ").append(offloadedTwoWayTLSHeaderKey).append('\n');
        sb.append('\t').append("tlsOffloadEngineHostIpAddrs: ").append(tlsOffloadEngineHostIpAddrs).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof RestDeploymentConfig) {
            RestDeploymentConfig otherConfig = (RestDeploymentConfig)other;
            return  uriElement.equals(otherConfig.getUriElement()) &&
                    realm.equals(otherConfig.getRealm()) &&
                    authTargetMapping.equals(otherConfig.getAuthTargetMapping()) &&
                    (offloadedTwoWayTLSHeaderKey != null
                            ? offloadedTwoWayTLSHeaderKey.equals(otherConfig.getOffloadedTwoWayTlsHeaderKey())
                            : otherConfig.getOffloadedTwoWayTlsHeaderKey() == null) &&
                    (tlsOffloadEngineHostIpAddrs != null
                            ? tlsOffloadEngineHostIpAddrs.equals(otherConfig.getTlsOffloadEngineHostIpAddrs())
                            : otherConfig.getTlsOffloadEngineHostIpAddrs() == null);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (uriElement + realm + authTargetMapping).hashCode();
    }

    public JsonValue toJson() {

        return json(object(field(URI_ELEMENT, uriElement), field(REALM, realm),
                field(AuthTargetMapping.AUTH_TARGET_MAPPINGS, authTargetMapping.toJson()),
                field(OFFLOADED_TWO_WAY_TLS_HEADER_KEY, offloadedTwoWayTLSHeaderKey),
                field(TLS_OFFLOAD_ENGINE_HOSTS, tlsOffloadEngineHostIpAddrs)));
    }

    public static RestDeploymentConfig fromJson(JsonValue json) {
        return RestDeploymentConfig.builder()
                .authTargetMapping(AuthTargetMapping.fromJson(json.get(AUTH_TARGET_MAPPINGS)))
                .uriElement(json.get(URI_ELEMENT).asString())
                .realm(json.get(REALM).asString())
                .offloadedTwoWayTLSHeaderKey(json.get(OFFLOADED_TWO_WAY_TLS_HEADER_KEY).asString())
                .tlsOffloadEngineHostIpAddrs(json.get(TLS_OFFLOAD_ENGINE_HOSTS).isCollection() ?
                        new HashSet<String>(json.get(TLS_OFFLOAD_ENGINE_HOSTS).asCollection(String.class)) : null)
                .build();
    }

    public Map<String, Set<String>> marshalToAttributeMap() {
        Map<String, Object> preMap = toJson().asMap();
        Map<String, Set<String>> interimMap = MapMarshallUtils.toSmsMap(preMap);
        interimMap.remove(AUTH_TARGET_MAPPINGS);
        interimMap.putAll(authTargetMapping.marshalToAttributeMap());

        /*
        Ultimately, I have to re-constitute the list constituents because the toSmsMap will simply call toString on the
        Map values. I have considered refactoring the toSmsMap to take a JsonValue, so the type of the Values can
        be preserved, but this generic method cannot know what the type of the Set/List constituents is, and often,
        as in the Set<TokenTransformConfig> maintained by the RestSTSInstanceConfig, this type itself has a custom
        marshalling structure. So I have to go through the overhead of this conversion, as the toSmsMap will simply
        call toString on the Set entry (which works for Sets of Strings, but more complex types, with custom marshalling,
        won't be addressed by this generic approach).
         */
        Object tlsOffloadHostsObject = preMap.get(TLS_OFFLOAD_ENGINE_HOSTS);
        if (tlsOffloadHostsObject instanceof Set) {
            interimMap.put((TLS_OFFLOAD_ENGINE_HOSTS), (Set)tlsOffloadHostsObject);
        } else {
            throw new IllegalStateException("Type corresponding to " + (TLS_OFFLOAD_ENGINE_HOSTS) + " key unexpected. Type: "
                    + (tlsOffloadHostsObject != null ? tlsOffloadHostsObject.getClass().getName() :" null"));
        }
        return interimMap;


    }

    public static RestDeploymentConfig marshalFromAttributeMap(Map<String, Set<String>> attributeMap) {
        AuthTargetMapping targetMapping = AuthTargetMapping.marshalFromAttributeMap(attributeMap);
        Map<String, Object> jsonMap = MapMarshallUtils.toJsonValueMap(attributeMap);
        jsonMap.put(AuthTargetMapping.AUTH_TARGET_MAPPINGS, targetMapping.toJson());

        jsonMap.put(TLS_OFFLOAD_ENGINE_HOSTS, new JsonValue(attributeMap.get(TLS_OFFLOAD_ENGINE_HOSTS)));

        return fromJson(new JsonValue(jsonMap));
    }
}
