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

package org.forgerock.openam.sts.rest.config.user;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.shared.sts.SharedSTSConstants;
import org.forgerock.openam.sts.DeploymentPathNormalizationImpl;
import org.forgerock.openam.sts.MapMarshallUtils;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.config.user.CustomTokenOperation;
import org.forgerock.openam.sts.config.user.DeploymentConfig;
import org.forgerock.openam.sts.config.user.OpenIdConnectTokenConfig;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.config.user.STSInstanceConfig;
import org.forgerock.openam.sts.token.UrlConstituentCatenatorImpl;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.Reject;

import java.util.*;


/**
 * Class which encapsulates all of the user-provided config information necessary to create an instance of the rest
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
        private Set<TokenTransformConfig> supportedTokenTransforms;
        private DeploymentConfig deploymentConfig;
        private Set<CustomTokenOperation> customTokenValidators;
        private Set<CustomTokenOperation> customTokenProviders;
        private Set<TokenTransformConfig> customTokenTransforms;

        private RestSTSInstanceConfigBuilderBase() {
            supportedTokenTransforms = new HashSet<>();
            customTokenProviders = new HashSet<>();
            customTokenValidators = new HashSet<>();
            customTokenTransforms =  new HashSet<>();
        }

        public T deploymentConfig(DeploymentConfig deploymentConfig) {
            this.deploymentConfig = deploymentConfig;
            return self();
        }

        public T addSupportedTokenTransform(
                TokenType inputType,
                TokenType outputType,
                boolean invalidateInterimOpenAMSession) {
            supportedTokenTransforms.add(new TokenTransformConfig(inputType,
                    outputType, invalidateInterimOpenAMSession));
            return self();
        }

        public T setSupportedTokenTransforms(Collection<TokenTransformConfig> transforms) {
            supportedTokenTransforms.addAll(transforms);
            return self();
        }

        public T setCustomTokenTransforms(Collection<TokenTransformConfig> transforms) {
            customTokenTransforms.addAll(transforms);
            return self();
        }

        public T setCustomValidators(Collection<CustomTokenOperation> validators) {
            customTokenValidators.addAll(validators);
            return self();
        }

        public T setCustomProviders(Collection<CustomTokenOperation> providers) {
            customTokenProviders.addAll(providers);
            return self();
        }
        
        public T addCustomTokenValidator(String customTokenType, String restTokenValidatorImplClassName) {
            customTokenValidators.add(new CustomTokenOperation(customTokenType, restTokenValidatorImplClassName));
            return self();
        }

        public T addCustomTokenProvider(String customTokenType, String restTokenProviderImplClassName) {
            customTokenProviders.add(new CustomTokenOperation(customTokenType, restTokenProviderImplClassName));
            return self();
        }

        public T addCustomTokenTransform(String inputTokenType, String outputTokenType, boolean invalidateInterimOpenAMSession) {
            customTokenTransforms.add(new TokenTransformConfig(inputTokenType, outputTokenType, invalidateInterimOpenAMSession));
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

    private final Set<TokenTransformConfig> supportedTokenTransforms;
    private final DeploymentConfig deploymentConfig;
    private final Set<CustomTokenOperation> customTokenValidators;
    private final Set<CustomTokenOperation> customTokenProviders;
    private final Set<TokenTransformConfig> customTokenTransforms;

    /*
    Define the names of fields to aid in json marshalling. Note that these names match the names of the AttributeSchema
    entries in restSTS.xml, as this aids in marshalling an instance of this class into the attribute map needed for
    SMS persistence.
     */
    public static final String SUPPORTED_TOKEN_TRANSFORMS = SharedSTSConstants.SUPPORTED_TOKEN_TRANSFORMS;
    public static final String CUSTOM_TOKEN_PROVIDERS = SharedSTSConstants.CUSTOM_TOKEN_PROVIDERS;
    public static final String CUSTOM_TOKEN_VALIDATORS = SharedSTSConstants.CUSTOM_TOKEN_VALIDATORS;
    public static final String CUSTOM_TOKEN_TRANSFORMS = SharedSTSConstants.CUSTOM_TOKEN_TRANSFORMS;

    private RestSTSInstanceConfig(RestSTSInstanceConfigBuilderBase<?> builder) {
        super(builder);
        this.supportedTokenTransforms = Collections.unmodifiableSet(builder.supportedTokenTransforms);
        this.deploymentConfig = builder.deploymentConfig;
        Reject.ifNull(deploymentConfig, "DeploymentConfig cannot be null");
        this.customTokenValidators = Collections.unmodifiableSet(builder.customTokenValidators);
        this.customTokenProviders = Collections.unmodifiableSet(builder.customTokenProviders);
        this.customTokenTransforms = Collections.unmodifiableSet(builder.customTokenTransforms);
        
        if (CollectionUtils.isEmpty(supportedTokenTransforms) && CollectionUtils.isEmpty(customTokenTransforms)) {
            throw new IllegalStateException("Neither standard nor custom token transforms have been specified.");        
        }
        
        boolean foundValidator, foundProvider;
        for (TokenTransformConfig tokenTransformConfig : customTokenTransforms) {
            foundValidator = false;
            foundProvider = false;
            for (CustomTokenOperation customTokenOperation : customTokenValidators) {
                if (customTokenOperation.getCustomTokenName().equals(tokenTransformConfig.getInputTokenType().getId())) {
                    foundValidator = true;
                    break;
                }
            }
            for (CustomTokenOperation customTokenOperation : customTokenProviders) {
                if (customTokenOperation.getCustomTokenName().equals(tokenTransformConfig.getOutputTokenType().getId())) {
                    foundProvider = true;
                    break;
                }
            }
            /*
            custom token transforms can reference non-custom tokens - only if neither a custom token validator or
            custom token provider is referenced, is the configuration incorrect.
             */
            if (!foundProvider && !foundValidator) {
                throw new IllegalStateException("No custom token provider or custom validator found to realize the " +
                        "custom token transform: " + tokenTransformConfig);
            }
        }
        
        /*
        throw an exception if no SAML2Config is set, but a SAML token is specified as
        output in one of the token transformations. Likewise for OIDC.
         */
        if (this.saml2Config == null) {
            for (TokenTransformConfig tokenTransformConfig : supportedTokenTransforms) {
                if (TokenType.SAML2.getId().equals(tokenTransformConfig.getOutputTokenType().getId())) {
                    throw new IllegalStateException("A SAML2 token is a transformation output, but no Saml2Config " +
                            "state has been specified to guide the production of SAML2 tokens.");
                }
            }
        }
        if (this.openIdConnectTokenConfig == null) {
            for (TokenTransformConfig tokenTransformConfig : supportedTokenTransforms) {
                if (TokenType.OPENIDCONNECT.getId().equals(tokenTransformConfig.getOutputTokenType().getId())) {
                    throw new IllegalStateException("A OPENIDCONNECT token is a transformation output, but no OIDCTokenConfig " +
                            "state has been specified to guide the production of OIDC Id Tokens.");
                }
            }
        }
    }

    public static RestSTSInstanceConfigBuilder builder() {
        return new RestSTSInstanceConfigBuilder();
    }

    /**
     * @return  The RestDeploymentConfig instance which specifies the url of the deployed STS instance, its realm,
     *          and its OpenAM authN context for each validated token type.
     */
    public DeploymentConfig getDeploymentConfig() {
        return deploymentConfig;
    }

    /**
     * @return  The set of token transformation operations supported by this STS instance.
     */
    public Set<TokenTransformConfig> getSupportedTokenTransforms() {
        return supportedTokenTransforms;
    }

    /**
     * @return  The set of custom token transformation operations supported by this STS instance.
     */
    public Set<TokenTransformConfig> getCustomTokenTransforms() {
        return customTokenTransforms;
    }

    /**
     *
     * @return the set of custom token validators supported by this STS instance.
     */
    public Set<CustomTokenOperation> getCustomTokenValidators() {
        return customTokenValidators;
    }

    /**
     *
     * @return the set of custom token providers supported by this STS instance.
     */
    public Set<CustomTokenOperation> getCustomTokenProviders() {
        return customTokenProviders;
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
        return new DeploymentPathNormalizationImpl().normalizeDeploymentPath(deploymentSubPath);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RestSTSInstanceConfig instance:\n");
        sb.append('\t').append("STSInstanceConfig: ").append(super.toString()).append('\n');
        sb.append('\t').append("supportedTokenTransforms: ").append(supportedTokenTransforms).append('\n');
        sb.append('\t').append("deploymentConfig: ").append(deploymentConfig).append('\n');
        sb.append('\t').append("customTokenValidators: ").append(customTokenValidators).append('\n');
        sb.append('\t').append("customTokenProviders: ").append(customTokenProviders).append('\n');
        sb.append('\t').append("customTokenTransforms: ").append(customTokenTransforms).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof RestSTSInstanceConfig) {
            RestSTSInstanceConfig otherConfig = (RestSTSInstanceConfig)other;
            return  super.equals(otherConfig) &&
                    supportedTokenTransforms.equals(otherConfig.supportedTokenTransforms)  &&
                    Objects.equals(customTokenValidators, otherConfig.customTokenValidators) &&
                    Objects.equals(customTokenProviders, otherConfig.customTokenProviders) &&
                    Objects.equals(customTokenTransforms, otherConfig.customTokenTransforms) &&
                    deploymentConfig.equals(otherConfig.deploymentConfig);
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

        if (!supportedTokenTransforms.isEmpty()) {
            List<JsonValue> translationList = new ArrayList<>(supportedTokenTransforms.size());
            for (TokenTransformConfig tokenTransformConfig : supportedTokenTransforms) {
                translationList.add(tokenTransformConfig.toJson());
            }
            JsonValue supportedTranslations = new JsonValue(translationList);
            baseValue.add(SUPPORTED_TOKEN_TRANSFORMS, supportedTranslations);
        }
        
        if (!customTokenValidators.isEmpty()) {
            List<JsonValue> customValidatorsList = new ArrayList<>(customTokenValidators.size());
            for (CustomTokenOperation customTokenOperation : customTokenValidators) {
                customValidatorsList.add(customTokenOperation.toJson());
            }
            JsonValue customValidators = new JsonValue(customValidatorsList);
            baseValue.add(CUSTOM_TOKEN_VALIDATORS, customValidators);
        }

        if (!customTokenProviders.isEmpty()) {
            List<JsonValue> customProvidersList = new ArrayList<>(customTokenProviders.size());
            for (CustomTokenOperation customTokenOperation : customTokenProviders) {
                customProvidersList.add(customTokenOperation.toJson());
            }
            JsonValue customProviders = new JsonValue(customProvidersList);
            baseValue.add(CUSTOM_TOKEN_PROVIDERS, customProviders);
        }

        if (!customTokenTransforms.isEmpty()) {
            List<JsonValue> customTranslationsList = new ArrayList<>(customTokenTransforms.size());
            for (TokenTransformConfig tokenTransformConfig : customTokenTransforms) {
                customTranslationsList.add(tokenTransformConfig.toJson());
            }
            JsonValue customTranslations = new JsonValue(customTranslationsList);
            baseValue.add(CUSTOM_TOKEN_TRANSFORMS, customTranslations);
        }

        return baseValue;
    }

    public static RestSTSInstanceConfig fromJson(JsonValue json) {
        if (json == null) {
            throw new NullPointerException("JsonValue cannot be null!");
        }
        STSInstanceConfig baseConfig = STSInstanceConfig.fromJson(json);
        RestSTSInstanceConfigBuilderBase<?> builder = RestSTSInstanceConfig.builder()
                .saml2Config(baseConfig.getSaml2Config())
                .oidcIdTokenConfig(baseConfig.getOpenIdConnectTokenConfig())
                .persistIssuedTokensInCTS(baseConfig.persistIssuedTokensInCTS())
                .deploymentConfig(DeploymentConfig.fromJson(json.get(DEPLOYMENT_CONFIG)));
        
        JsonValue supportedTranslations = json.get(SUPPORTED_TOKEN_TRANSFORMS);
        if (!supportedTranslations.isNull()) {
            if (!supportedTranslations.isList()) {
                throw new IllegalStateException("Unexpected value for the " + SUPPORTED_TOKEN_TRANSFORMS + " field: "
                        + supportedTranslations.asString());
            }
            List<TokenTransformConfig> transformConfigList = new ArrayList<>();
            for (Object translation : supportedTranslations.asList()) {
                transformConfigList.add(TokenTransformConfig.fromJson(new JsonValue(translation)));
            }
            builder.setSupportedTokenTransforms(transformConfigList);
        }

        JsonValue customTranslations = json.get(CUSTOM_TOKEN_TRANSFORMS);
        if (!customTranslations.isNull()) {
            if (!customTranslations.isList()) {
                throw new IllegalStateException("Unexpected value for the " + CUSTOM_TOKEN_TRANSFORMS + " field: "
                        + customTranslations.asString());
            }
            List<TokenTransformConfig> transformConfigList = new ArrayList<>();
            for (Object translation : customTranslations.asList()) {
                transformConfigList.add(TokenTransformConfig.fromJson(new JsonValue(translation)));
            }
            builder.setCustomTokenTransforms(transformConfigList);
        }

        JsonValue customValidators = json.get(CUSTOM_TOKEN_VALIDATORS);
        if (!customValidators.isNull()) {
            if (!customValidators.isList()) {
                throw new IllegalStateException("Unexpected value for the " + CUSTOM_TOKEN_VALIDATORS + " field: "
                        + customValidators.asString());
            }
            List<CustomTokenOperation> customValidatorsList = new ArrayList<>();
            for (Object translation : customValidators.asList()) {
                customValidatorsList.add(CustomTokenOperation.fromJson(new JsonValue(translation)));
            }
            builder.setCustomValidators(customValidatorsList);
        }

        JsonValue customProviders = json.get(CUSTOM_TOKEN_PROVIDERS);
        if (!customProviders.isNull()) {
            if (!customProviders.isList()) {
                throw new IllegalStateException("Unexpected value for the " + CUSTOM_TOKEN_PROVIDERS + " field: "
                        + customProviders.asString());
            }
            List<CustomTokenOperation> customProvidersList = new ArrayList<>();
            for (Object translation : customProviders.asList()) {
                customProvidersList.add(CustomTokenOperation.fromJson(new JsonValue(translation)));
            }
            builder.setCustomProviders(customProvidersList);
        }

        return builder.build();
    }

    /*
    This method will marshal this state into the Map<String>, Set<String>> required for persistence in the SMS. The intent
    is to leverage the toJson functionality, as a JsonValue is essentially a map, with the following exceptions:
    1. the non-complex objects are not Set<String>, but rather <String>, and thus must be marshaled to a Set<String>. It seems
    like I could go through all of the values in the map, and if any entry is simply a String, I could marshal it to a Set<String>
    2. the complex objects (e.g. deploymentConfig, saml2Config, supportedTokenTransforms, etc) are themselves maps, and
    thus must be 'flattened' into a single map. This is done by calling each of these encapsulated objects to provide a
    map representation, and then insert these values into the top-level map.
    Note also, that the SMS Map<String, Set<String>> representations of optional, null objects should be set to the empty
    values. This is to support the update operation invoked from the Admin UI when an existing rest-sts instance is
    edited. In this case, it could be that the SAML2Config of a published rest-sts instance is removed, as it should no
    longer issue SAML2 assertions. When the updated RestSTSInstanceConfig is marshalled from the Map<String, Set<String>>
    dispatched from the AdminUI (necessary to generate good error messages, and necessary to create the Injector necessary
    for rest-sts instance creation), the SAML2Config instance will be null, and thus when this method is called, to get
    the SMS persistence state, no SAML2-related attributes will be written, thereby leaving the previous, non-empty values
    unchanged. Thus this method should be sure to create empty Set<String> entries for all attributes defined for all
    complex, optional, but null objects. This applies to the SAML2Config and OpenIdConnectTokenConfig objects.
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
        interimMap.remove(SUPPORTED_TOKEN_TRANSFORMS);
        Set<String> supportedTransforms = new HashSet<>();
        interimMap.put(SUPPORTED_TOKEN_TRANSFORMS, supportedTransforms);
        for (TokenTransformConfig ttc : supportedTokenTransforms) {
            supportedTransforms.add(ttc.toSMSString());
        }

        interimMap.remove(CUSTOM_TOKEN_TRANSFORMS);
        Set<String> customTransforms = new HashSet<>();
        interimMap.put(CUSTOM_TOKEN_TRANSFORMS, customTransforms);
        for (TokenTransformConfig ttc : customTokenTransforms) {
            customTransforms.add(ttc.toSMSString());
        }

        interimMap.remove(CUSTOM_TOKEN_VALIDATORS);
        Set<String> customValidators = new HashSet<>();
        interimMap.put(CUSTOM_TOKEN_VALIDATORS, customValidators);
        for (CustomTokenOperation cto : customTokenValidators) {
            customValidators.add(cto.toSMSString());
        }

        interimMap.remove(CUSTOM_TOKEN_PROVIDERS);
        Set<String> customProviders = new HashSet<>();
        interimMap.put(CUSTOM_TOKEN_PROVIDERS, customProviders);
        for (CustomTokenOperation cto : customTokenProviders) {
            customProviders.add(cto.toSMSString());
        }

        interimMap.remove(SAML2_CONFIG);
        if (saml2Config != null) {
            interimMap.putAll(saml2Config.marshalToAttributeMap());
        } else {
            /*
            Generate empty values for all of the SAML2Config attribute keys, in case this method is called as part of
            an update, and previous values need to be over-written.
             */
            interimMap.putAll(SAML2Config.getEmptySMSAttributeState());
        }

        interimMap.remove(OIDC_ID_TOKEN_CONFIG);
        if (openIdConnectTokenConfig != null) {
            interimMap.putAll(openIdConnectTokenConfig.marshalToAttributeMap());
        } else {
            /*
            Generate empty values for all of the OpenIdConnectTokenConfig attribute keys, in case this method is called as part of
            an update, and previous values need to be over-written.
             */
            interimMap.putAll(OpenIdConnectTokenConfig.getEmptySMSAttributeState());
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
        DeploymentConfig deploymentConfig = DeploymentConfig.marshalFromAttributeMap(attributeMap);
        Map<String, Object> jsonAttributes = MapMarshallUtils.toJsonValueMap(attributeMap);
        jsonAttributes.remove(DEPLOYMENT_CONFIG);
        jsonAttributes.put(DEPLOYMENT_CONFIG, deploymentConfig.toJson());

        SAML2Config saml2Config = SAML2Config.marshalFromAttributeMap(attributeMap);
        if (saml2Config != null) {
            jsonAttributes.remove(SAML2_CONFIG);
            jsonAttributes.put(SAML2_CONFIG, saml2Config.toJson());
        }

        OpenIdConnectTokenConfig openIdConnectTokenConfig = OpenIdConnectTokenConfig.marshalFromAttributeMap(attributeMap);
        if (openIdConnectTokenConfig != null) {
            jsonAttributes.remove(OIDC_ID_TOKEN_CONFIG);
            jsonAttributes.put(OIDC_ID_TOKEN_CONFIG, openIdConnectTokenConfig.toJson());
        }

        /*
         The SUPPORTED_TOKEN_TRANSFORMS, CUSTOM_TOKEN_TRANSFORMS, CUSTOM_TOKEN_VALIDATORS, and CUSTOM_TOKEN_PROVIDERS
          are currently each in a String representation in the Set<String> map entry corresponding
         to their respective key. I need to marshal each back into a TokenTransformConfig instance, and then
         call toJson on each, and put them in a JsonValue wrapping a list.
         */
        ArrayList<JsonValue> jsonTranslationsList = new ArrayList<>();
        JsonValue jsonTranslations = new JsonValue(jsonTranslationsList);
        jsonAttributes.remove(SUPPORTED_TOKEN_TRANSFORMS);
        jsonAttributes.put(SUPPORTED_TOKEN_TRANSFORMS, jsonTranslations);
        Set<String> stringTokenTranslations = attributeMap.get(SUPPORTED_TOKEN_TRANSFORMS);
        for (String translation : stringTokenTranslations) {
            jsonTranslationsList.add(TokenTransformConfig.fromSMSString(translation).toJson());
        }

        ArrayList<JsonValue> jsonCustomTranslationsList = new ArrayList<>();
        JsonValue jsonCustomTranslations = new JsonValue(jsonCustomTranslationsList);
        jsonAttributes.remove(CUSTOM_TOKEN_TRANSFORMS);
        jsonAttributes.put(CUSTOM_TOKEN_TRANSFORMS, jsonCustomTranslations);
        Set<String> stringCustomTranslations = attributeMap.get(CUSTOM_TOKEN_TRANSFORMS);
        for (String translation : stringCustomTranslations) {
            jsonCustomTranslationsList.add(TokenTransformConfig.fromSMSString(translation).toJson());
        }

        ArrayList<JsonValue> jsonCustomValidatorsList = new ArrayList<>();
        JsonValue jsonCustomValidators = new JsonValue(jsonCustomValidatorsList);
        jsonAttributes.remove(CUSTOM_TOKEN_VALIDATORS);
        jsonAttributes.put(CUSTOM_TOKEN_VALIDATORS, jsonCustomValidators);
        Set<String> stringCustomValidators = attributeMap.get(CUSTOM_TOKEN_VALIDATORS);
        for (String validator : stringCustomValidators) {
            jsonCustomValidatorsList.add(CustomTokenOperation.fromSMSString(validator).toJson());
        }

        ArrayList<JsonValue> jsonCustomProvidersList = new ArrayList<>();
        JsonValue jsonCustomProviders = new JsonValue(jsonCustomProvidersList);
        jsonAttributes.remove(CUSTOM_TOKEN_PROVIDERS);
        jsonAttributes.put(CUSTOM_TOKEN_PROVIDERS, jsonCustomProviders);
        Set<String> stringCustomProviders = attributeMap.get(CUSTOM_TOKEN_PROVIDERS);
        for (String provider : stringCustomProviders) {
            jsonCustomProvidersList.add(CustomTokenOperation.fromSMSString(provider).toJson());
        }
        return fromJson(new JsonValue(jsonAttributes));
    }

    /*
    When the RestSecurityTokenServiceViewBean harvests the configurations input by the user, it attempts to publish the
    JsonValue wrapping this Map<String, Set<String>>. It cannot directly attempt to marshal these configuration properties
    in the ViewBean class, as this would introduce a dependency on the rest-sts into the openam-console module. Thus the
    RestSecurityTokenServiceViewBean can only invoke the rest-sts-publish service with a JsonValue wrapping the
    Map<String, Set<String>>.
    This method will be invoked with the JsonValue generated by wrapping a Map<String, Set<String>>
    containing the user's rest-sts-configurations. This JsonValue is generated by the RestSTSModelImpl class, which is invoked from the
    rest-sts ViewBean classes to compose the POST to the publish-service with the ViewBean PropertySheet yield. It would
    be possible to simply call jsonValue.asMap, and then cast the result, but the approach in the method below is a bit
    safer, and avoids the blind cast.
     */
    public static RestSTSInstanceConfig marshalFromJsonAttributeMap(JsonValue jsonValue) throws IllegalStateException {
        if (jsonValue ==  null) {
            throw new IllegalStateException("JsonValue cannot be null!");
        }
        if (!jsonValue.isMap()) {
            throw new IllegalStateException("In RestSTSInstanceConfig#marshalFromJsonAttributeMap, Passed-in JsonValue " +
                    "is not a map. The JsonValue instance: " + jsonValue.toString());
        }
        Map<String, Set<String>> smsMap = new HashMap<>();
        for (String key : jsonValue.keys()) {
            final JsonValue value = jsonValue.get(key);
            if (value.isNull()) {
                smsMap.put(key, Collections.EMPTY_SET);
            } else if(!value.isCollection()) {
                throw new IllegalStateException("In RestSTSInstanceConfig#marshalFromJsonAttributeMap, value " +
                        "corresponding to key " + key + " is not a collection. The value: " + value);
            } else {
                Collection<String> stringCollection = value.asCollection(String.class);
                Set<String> stringSet = new HashSet<>(stringCollection);
                smsMap.put(key, stringSet);
            }
        }
        return marshalFromAttributeMap(smsMap);
    }
}
