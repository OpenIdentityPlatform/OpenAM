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
 * Copyright © 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.config.user;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.shared.sts.SharedSTSConstants;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.MapMarshallUtils;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.config.user.STSInstanceConfig;
import org.forgerock.openam.sts.token.UrlConstituentCatenatorImpl;
import org.forgerock.util.Reject;

import java.util.*;


/**
 * Class which encapsulates all of the user-provided config information necessary to create an instance of the
 * STS.
 * It is an immutable object with getter methods to obtain all of the necessary information needed by the various
 * guice modules and providers to inject the object graph corresponding to a fully-configured STS instance.
 *
 * For an explanation of what's going on with the builders in this class,
 * see https://weblogs.java.net/blog/emcmanus/archive/2010/10/25/using-builder-pattern-subclasses
 *
 * Also attempted to marshal the RestSTSInstanceConfig to/from json with the jackson ObjectMapper. But I was adding
 * @JsonSerialize and @JsonDeserialize annotations, and because builder-based classes don't expose ctors which
 * take the complete field set, I would have to create @JsonCreator instances which would have to pull all of the
 * values out of a map anyway, which is 75% of the way towards a hand-rolled json marshalling implementation based on
 * json-fluent. So a hand-rolled implementation it is.
 */
public class RestSTSInstanceConfig extends STSInstanceConfig {
    public abstract static class RestSTSInstanceConfigBuilderBase<T extends RestSTSInstanceConfigBuilderBase<T>> extends STSInstanceConfig.STSInstanceConfigBuilderBase<T>  {
        private Set<TokenTransformConfig> supportedTokenTranslations;
        private RestDeploymentConfig deploymentConfig;

        private RestSTSInstanceConfigBuilderBase() {
            supportedTokenTranslations = new HashSet<TokenTransformConfig>();
        }

        public T deploymentConfig(RestDeploymentConfig deploymentConfig) {
            this.deploymentConfig = deploymentConfig;
            return self();
        }

        public T addSupportedTokenTranslation(
                TokenType inputType,
                TokenType outputType,
                boolean invalidateInterimOpenAMSession) {
            supportedTokenTranslations.add(new TokenTransformConfig(inputType,
                    outputType, invalidateInterimOpenAMSession));
            return self();
        }

        public T setSupportedTokenTranslations(Collection<TokenTransformConfig> transforms) {
            supportedTokenTranslations.addAll(transforms);
            return self();
        }

        public RestSTSInstanceConfig build() {
            return new RestSTSInstanceConfig(this);
        }
    }

    public static class RestSTSInstanceConfigBuilder extends RestSTSInstanceConfigBuilderBase<RestSTSInstanceConfigBuilder> {
        @Override
        protected RestSTSInstanceConfigBuilder self() {
            return this;
        }
    }

    private final Set<TokenTransformConfig> supportedTokenTranslations;
    private final RestDeploymentConfig deploymentConfig;

    /*
    Define the names of fields to aid in json marshalling. Note that these names match the names of the AttributeSchema
    entries in restSTS.xml, as this aids in marshalling an instance of this class into the attribute map needed for
    SMS persistence.
     */
    private static final String DEPLOYMENT_CONFIG = "deployment-config";
    private static final String SUPPORTED_TOKEN_TRANSLATIONS = SharedSTSConstants.SUPPORTED_TOKEN_TRANSFORMS;

    private RestSTSInstanceConfig(RestSTSInstanceConfigBuilderBase<?> builder) {
        super(builder);
        this.supportedTokenTranslations = Collections.unmodifiableSet(builder.supportedTokenTranslations);
        this.deploymentConfig = builder.deploymentConfig;
        Reject.ifNull(supportedTokenTranslations, "Supported token translations cannot be null");
        Reject.ifNull(deploymentConfig, "DeploymentConfig cannot be null");
        /*
        throw an exception if no SAML2Config is set, but a SAML token is specified as
        output in one of the token transformations.
         */
        if (this.saml2Config == null) {
            for (TokenTransformConfig tokenTransformConfig : supportedTokenTranslations) {
                if (TokenType.SAML2.equals(tokenTransformConfig.getOutputTokenType())) {
                    throw new IllegalStateException("A SAML2 token is a transformation output, but no Saml2Config " +
                            "state has been specified to guide the production of SAML2 tokens.");
                }
            }
        }
    }

    public static RestSTSInstanceConfigBuilderBase<?> builder() {
        return new RestSTSInstanceConfigBuilder();
    }

    /**
     * @return  The RestDeploymentConfig instance which specifies the url of the deployed STS instance, its realm,
     *          and its OpenAM authN context for each validated token type.
     */
    public RestDeploymentConfig getDeploymentConfig() {
        return deploymentConfig;
    }

    /**
     * @return  The set of token transformation operations supported by this STS instance.
     */
    public Set<TokenTransformConfig> getSupportedTokenTranslations() {
        return supportedTokenTranslations;
    }

    /**
     * @return This method will return the sub-path at which the rest STS instance will be deployed (sub-path relative to the
     * path of the Crest service fronting the STS). This string serves to identify the rest STS instance. This identifier
     * is passed to the TokenGenerationService so that the TGS can issue instance-specific tokens (i.e. reflecting the
     * KeystoreConfig and SAML2Config of the associated STS instance). This path is also the most specific element of the
     * DN identifying the config in the SMS.
     *
     * This method will be called to obtain the id under which the RestSTSInstanceConfig state is stored in the SMS, and
     * the caching of the Route entry added to the Crest Router (needs to be cached to be later removed). Because this
     * resource will be accessed as a url, it cannot have a trailing slash, as this slash is removed in the http request.
     */
    public String getDeploymentSubPath() {
        String deploymentSubPath = new UrlConstituentCatenatorImpl().catenateUrlConstituents(
                getDeploymentConfig().getRealm(), getDeploymentConfig().getUriElement());
        if (deploymentSubPath.endsWith(AMSTSConstants.FORWARD_SLASH)) {
            return deploymentSubPath.substring(0, deploymentSubPath.lastIndexOf(AMSTSConstants.FORWARD_SLASH));
        }

        if (deploymentSubPath.startsWith(AMSTSConstants.FORWARD_SLASH)) {
            deploymentSubPath = deploymentSubPath.substring(1, deploymentSubPath.length());
        }

        return deploymentSubPath;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RestSTSInstanceConfig instance:\n");
        sb.append('\t').append("STSInstanceConfig: ").append(super.toString()).append('\n');
        sb.append('\t').append("supportedTokenTranslations: ").append(supportedTokenTranslations).append('\n');
        sb.append('\t').append("deploymentConfig: ").append(deploymentConfig).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof RestSTSInstanceConfig) {
            RestSTSInstanceConfig otherConfig = (RestSTSInstanceConfig)other;
            return  super.equals(otherConfig) &&
                    supportedTokenTranslations.equals(otherConfig.getSupportedTokenTranslations())  &&
                    deploymentConfig.equals(otherConfig.getDeploymentConfig());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    public JsonValue toJson() {
        JsonValue baseValue = super.toJson();
        baseValue.add(DEPLOYMENT_CONFIG, deploymentConfig.toJson());
        JsonValue supportedTranslations = new JsonValue(new ArrayList<Object>());
        List<Object> translationList = supportedTranslations.asList();
        Iterator<TokenTransformConfig> iter = supportedTokenTranslations.iterator();
        while (iter.hasNext()) {
            translationList.add(iter.next().toJson());
        }
        baseValue.add(SUPPORTED_TOKEN_TRANSLATIONS, supportedTranslations);
        return baseValue;
    }

    public static RestSTSInstanceConfig fromJson(JsonValue json) {
        if (json == null) {
            throw new NullPointerException("JsonValue cannot be null!");
        }
        STSInstanceConfig baseConfig = STSInstanceConfig.fromJson(json);
        RestSTSInstanceConfigBuilderBase<?> builder = RestSTSInstanceConfig.builder()
                .issuerName(baseConfig.getIssuerName())
                .saml2Config(baseConfig.getSaml2Config())
                .deploymentConfig(RestDeploymentConfig.fromJson(json.get(DEPLOYMENT_CONFIG)));
        JsonValue supportedTranslations = json.get(SUPPORTED_TOKEN_TRANSLATIONS);
        if (!supportedTranslations.isList()) {
            throw new IllegalStateException("Unexpected value for the " + SUPPORTED_TOKEN_TRANSLATIONS + " field: "
                    + supportedTranslations.asString());
        }
        List<TokenTransformConfig> transformConfigList = new ArrayList<TokenTransformConfig>();
        Iterator<Object> iter = supportedTranslations.asList().iterator();
        while (iter.hasNext()) {
            transformConfigList.add(TokenTransformConfig.fromJson(new JsonValue(iter.next())));
        }
        builder.setSupportedTokenTranslations(transformConfigList);
        return builder.build();
    }

    /*
    This method will marshal this state into the Map<String>, Set<String>> required for persistence in the SMS. The intent
    is to leverage the toJson functionality, as a JsonValue is essentially a map, with the following exceptions:
    1. the non-complex objects are not Set<String>, but rather <String>, and thus must be marshaled to a Set<String>. It seems
    like I could go through all of the values in the map, and if any entry is simply a String, I could marshal it to a Set<String>
    2. the complex objects (e.g. deploymentConfig, saml2Config, supportedTokenTranslations, etc) are themselves maps, and
    thus must be 'flattened' into a single map. This is done by calling each of these encapsulated objects to provide a
    map representation, and then insert these values into the top-level map.
     */
    public Map<String, Set<String>> marshalToAttributeMap() {
        Map<String, Set<String>> interimMap = MapMarshallUtils.toSmsMap(toJson().asMap());
        interimMap.remove(DEPLOYMENT_CONFIG);
        interimMap.putAll(deploymentConfig.marshalToAttributeMap());

        /*
        Here the values are already contained in a set. I want to remove the referenced complex-object, but
        then add each of the TokenTransformConfig instances in the supportTokenTranslationsSet to a Set<String>, obtaining
        a string representation for each TokenTransformConfig instance, and adding it to the Set<String>
         */
        interimMap.remove(SUPPORTED_TOKEN_TRANSLATIONS);
        Set<String> supportedTransforms = new HashSet<String>();
        interimMap.put(SUPPORTED_TOKEN_TRANSLATIONS, supportedTransforms);
        for (TokenTransformConfig ttc : supportedTokenTranslations) {
            supportedTransforms.add(ttc.toSMSString());
        }

        if (saml2Config != null) {
            interimMap.remove(SAML2_CONFIG);
            interimMap.putAll(saml2Config.marshalToAttributeMap());
        }

        return interimMap;
    }

    /*
    When we are marshaling back from a Map<String, Set<String>>, this Map contains all of the values, also those
    contributed by encapsulated complex objects. So the structure must be 'un-flattened', where the top-level map
    is passed to encapsulated complex-objects, so that they may re-constitute themselves, and then the top-level json entry
    key is set to point at these re-constituted complex objects.

    Not that the marshalToAttributeMap first calls toJson to obtain the map representation, albeit with hierarchical
    elements, which must be subsequently flattened. The 'flattening' performed by the marshalToAttributeMap must then
     be 'inverted' by this method, where all complex objects are re-constituted, using the state in the flattened map.

     */
    public static RestSTSInstanceConfig marshalFromAttributeMap(Map<String, Set<String>> attributeMap) {
        RestDeploymentConfig restDeploymentConfig = RestDeploymentConfig.marshalFromAttributeMap(attributeMap);
        Map<String, Object> jsonAttributes = MapMarshallUtils.toJsonValueMap(attributeMap);
        jsonAttributes.remove(DEPLOYMENT_CONFIG);
        jsonAttributes.put(DEPLOYMENT_CONFIG, restDeploymentConfig.toJson());

        SAML2Config saml2Config = SAML2Config.marshalFromAttributeMap(attributeMap);
        if (saml2Config != null) {
            jsonAttributes.remove(SAML2_CONFIG);
            jsonAttributes.put(SAML2_CONFIG, saml2Config.toJson());
        }

        /*
         The SUPPORTED_TOKEN_TRANSLATIONS are currently each in a String representation in the Set<String> map entry corresponding
         to the SUPPORTED_TOKEN_TRANSLATIONS key. I need to marshal each back into a TokenTransformConfig instance, and then
         call toJson on each, and put them in a JsonValue wrapping a list.
         */
        ArrayList<JsonValue> jsonTranslationsList = new ArrayList<JsonValue>();
        JsonValue jsonTranslations = new JsonValue(jsonTranslationsList);
        jsonAttributes.remove(SUPPORTED_TOKEN_TRANSLATIONS);
        jsonAttributes.put(SUPPORTED_TOKEN_TRANSLATIONS, jsonTranslations);
        Set<String> stringTokenTranslations = attributeMap.get(SUPPORTED_TOKEN_TRANSLATIONS);
        for (String translation : stringTokenTranslations) {
            jsonTranslationsList.add(TokenTransformConfig.fromSMSString(translation).toJson());
        }

        return fromJson(new JsonValue(jsonAttributes));
    }

    /*
    When the RestSecurityTokenServiceViewBean harvests the configurations input by the user, it attempts to publish the
    JsonValue wrapping this Map<String, Set<String>>. It cannot directly attempt to marshal these configuration properties
    in the ViewBean class, as this would introduce a dependency on the rest-sts into the openam-console module. Thus the
    RestSecurityTokenServiceViewBean can only invoke the rest-sts-publish service with a JsonValue wrapping the
    Map<String, Set<String>> (or the Set<String> has to be turned into List<String> as JsonValue#toString does not currently
    turn Set values into json arrays). This method will be invoked with the JsonValue generated by wrapping a Map<String, List<String>>
    containing the user's rest-sts-configurations. It will turn the Map<String, List<String>> wrapped by the JsonValue back into
    a raw Map<String, Set<String>>, and call marshalFromAttributeMap.
     */
    public static RestSTSInstanceConfig marshalFromJsonAttributeMap(JsonValue jsonValue) throws IllegalStateException {
        if (jsonValue ==  null) {
            throw new IllegalStateException("JsonValue cannot be null!");
        }
        if (!jsonValue.isMap()) {
            throw new IllegalStateException("In RestSTSInstanceConfig#marshalFromJsonAttributeMap, Passed-in JsonValue " +
                    "is not a map. The JsonValue instance: " + jsonValue.toString());
        }
        Map<String, Set<String>> smsMap = new HashMap<String, Set<String>>();
        for (String key : jsonValue.keys()) {
            final JsonValue value = jsonValue.get(key);
            if (value.isNull()) {
                smsMap.put(key, Collections.EMPTY_SET);
            } else if(!value.isList()) {
                throw new IllegalStateException("In RestSTSInstanceConfig#marshalFromJsonAttributeMap, value " +
                        "corresponding to key " + key + " is not a list. The value: " + value);
            } else {
                List<String> stringList = value.asList(String.class);
                Set<String> stringSet = new HashSet<String>(stringList);
                smsMap.put(key, stringSet);
            }
        }
        return marshalFromAttributeMap(smsMap);
    }
}
