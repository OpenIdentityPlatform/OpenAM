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
import org.forgerock.openam.sts.config.user.AuthTargetMapping;

import org.forgerock.openam.sts.MapMarshallUtils;
import org.forgerock.util.Reject;

import java.util.Map;
import java.util.Set;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;


/**
 * This class represents the deployment configuration for an STS instance. This includes:
 * 1. the AuthTargetMapping instance - i.e. the REST authN context for each token type.
 * 2. The uri element (e.g. /realm1/accounting/bobo) at which the STS instance will be deployed
 * 3. The realm within which the STS is deployed.
 *
 */
public class RestDeploymentConfig {
    /*
    Define the names of fields to aid in json marshalling. Note that these names match the names of the AttributeSchema
    entries in restSTS.xml, as this aids in marshalling an instance of this class into the attribute map needed for
    SMS persistence.
     */
    private static final String REALM = "deployment-realm";
    private static final String URI_ELEMENT = "deployment-url-element";
    public static final String AUTH_TARGET_MAPPINGS = AuthTargetMapping.AUTH_TARGET_MAPPINGS;

    public static class RestDeploymentConfigBuilder {
        private String uriElement;
        private String realm = "/"; //default value
        private AuthTargetMapping authTargetMapping;

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

        public RestDeploymentConfig build() {
            return new RestDeploymentConfig(this);
        }
    }

    private final String uriElement;
    private final String realm;
    private final AuthTargetMapping authTargetMapping;

    private RestDeploymentConfig(RestDeploymentConfigBuilder builder) {
        uriElement = builder.uriElement;
        realm = builder.realm;
        authTargetMapping = builder.authTargetMapping;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RestDeploymentConfig instance: ").append('\n');
        sb.append('\t').append("Deployment uriElement: ").append(uriElement).append('\n');
        sb.append('\t').append("realm: ").append(realm).append('\n');
        sb.append('\t').append("authTargetMapping: ").append(authTargetMapping).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof RestDeploymentConfig) {
            RestDeploymentConfig otherConfig = (RestDeploymentConfig)other;
            return  uriElement.equals(otherConfig.getUriElement()) &&
                    realm.equals(otherConfig.getRealm()) &&
                    authTargetMapping.equals(otherConfig.getAuthTargetMapping());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (uriElement + realm + authTargetMapping).hashCode();
    }

    public JsonValue toJson() {
        return json(object(field(URI_ELEMENT, uriElement), field(REALM, realm),
                field(AuthTargetMapping.AUTH_TARGET_MAPPINGS, authTargetMapping.toJson())));
    }

    public static RestDeploymentConfig fromJson(JsonValue json) {
        return RestDeploymentConfig.builder()
                .authTargetMapping(AuthTargetMapping.fromJson(json.get(AUTH_TARGET_MAPPINGS)))
                .uriElement(json.get(URI_ELEMENT).asString())
                .realm(json.get(REALM).asString())
                .build();
    }

    public Map<String, Set<String>> marshalToAttributeMap() {
        Map<String, Set<String>> interimMap = MapMarshallUtils.toSmsMap(toJson().asMap());
        interimMap.remove(AUTH_TARGET_MAPPINGS);
        interimMap.putAll(authTargetMapping.marshalToAttributeMap());
        return interimMap;
    }

    public static RestDeploymentConfig marshalFromAttributeMap(Map<String, Set<String>> attributeMap) {
        AuthTargetMapping targetMapping = AuthTargetMapping.marshalFromAttributeMap(attributeMap);
        Map<String, Object> jsonMap = MapMarshallUtils.toJsonValueMap(attributeMap);
        jsonMap.put(AuthTargetMapping.AUTH_TARGET_MAPPINGS, targetMapping.toJson());
        return fromJson(new JsonValue(jsonMap));
    }
}
