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
 * Copyright 2013-2015 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.soap.config.user;

import org.apache.cxf.common.util.CollectionUtils;
import org.forgerock.guava.common.base.Objects;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.DeploymentPathNormalizationImpl;
import org.forgerock.openam.sts.MapMarshallUtils;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.config.user.DeploymentConfig;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.config.user.STSInstanceConfig;
import org.forgerock.openam.sts.config.user.TokenTransformConfig;
import org.forgerock.openam.sts.token.UrlConstituentCatenatorImpl;
import org.forgerock.util.Reject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class which encapsulates all of the user-provided config information necessary to create an instance of the
 * STS.
 * It is an immutable object with getter methods to obtain all of the necessary information needed by the various
 * guice modules and providers to inject the object graph corresponding to a fully-configured STS instance.
 *
 */
public class SoapSTSInstanceConfig extends STSInstanceConfig {
    private static final String SOAP_KEYSTORE_CONFIG = "soap-keystore-config";
    private static final String SOAP_DELEGATION_CONFIG = "soap-delegation-config";
    /*
    The following three names correspond to entries defined in soapSTS.xml
     */
    static final String ISSUE_TOKEN_TYPES = "issued-token-types";
    static final String TRANSFORMED_TOKEN_TYPES = "transformed-token-types";
    static final String DELEGATION_RELATIONSHIP_SUPPORTED = "delegation-relationship-supported";

    public abstract static class SoapSTSInstanceConfigBuilderBase <T extends SoapSTSInstanceConfigBuilderBase<T>>
            extends STSInstanceConfig.STSInstanceConfigBuilderBase<T>  {
        private Set<TokenType> issueTokenTypes;

        private Set<TokenTransformConfig> validateTokenTranslations;

        private SoapDeploymentConfig deploymentConfig;
        private SoapSTSKeystoreConfig keystoreConfig;
        private SoapDelegationConfig soapDelegationConfig;

        /*
        Indicates whether this Soap STS instance supports OnBehalfOf and ActAs - necessary to issue SV SAML assertions.
        If this is set to true, then the SoapDelegationConfig must be non-null.
        */
        private boolean delegationRelationshipsSupported;

        private SoapSTSInstanceConfigBuilderBase() {
            issueTokenTypes = new HashSet<TokenType>();
            validateTokenTranslations = new HashSet<TokenTransformConfig>();
        }

        public T deploymentConfig(SoapDeploymentConfig deploymentConfig) {
            this.deploymentConfig = deploymentConfig;
            return self();
        }

        public T addValidateTokenTranslation(TokenType inputType, TokenType outputType, boolean invalidateInterimOpenAMSession) {
            validateTokenTranslations.add(new TokenTransformConfig(inputType, outputType, invalidateInterimOpenAMSession));
            return self();
        }

        public T setValidateTokenTranslations(Set<TokenTransformConfig> validateTokenTranslations) {
            this.validateTokenTranslations.addAll(validateTokenTranslations);
            return self();
        }

        public T addIssueTokenType(TokenType type) {
            if (!TokenType.SAML2.equals(type)) {
                throw new IllegalArgumentException("Only SAML2 tokens can be issued, not tokens of type " + type);
            }
            issueTokenTypes.add(type);
            return self();
        }

        public T soapSTSKeystoreConfig(SoapSTSKeystoreConfig keystoreConfig) {
            this.keystoreConfig = keystoreConfig;
            return self();
        }

        public T delegationRelationshipsSupported(boolean delegationRelationshipsSupported) {
            this.delegationRelationshipsSupported = delegationRelationshipsSupported;
            return self();
        }

        public T soapDelegationConfig(SoapDelegationConfig soapDelegationConfig) {
            this.soapDelegationConfig = soapDelegationConfig;
            return self();
        }

        public SoapSTSInstanceConfig build() {
            return new SoapSTSInstanceConfig(this);
        }
    }

    public static class SoapSTSInstanceConfigBuilder extends SoapSTSInstanceConfigBuilderBase<SoapSTSInstanceConfigBuilder> {
        public SoapSTSInstanceConfigBuilder self() {
            return this;
        }
    }

    private final Set<TokenType> issueTokenTypes;
    private final Set<TokenTransformConfig> validateTokenTranslations;

    private final SoapDeploymentConfig deploymentConfig;
    private final SoapSTSKeystoreConfig keystoreConfig;

    /*
    Indicates whether this Soap STS instance supports OnBehalfOf and ActAs - necessary to issue SV SAML assertions.
     */
    private final boolean delegationRelationshipsSupported;
    private final SoapDelegationConfig soapDelegationConfig;

    private SoapSTSInstanceConfig(SoapSTSInstanceConfigBuilderBase<?> builder) {
        super(builder);
        this.issueTokenTypes = Collections.unmodifiableSet(builder.issueTokenTypes);
        this.deploymentConfig = builder.deploymentConfig;
        this.keystoreConfig = builder.keystoreConfig;
        this.delegationRelationshipsSupported = builder.delegationRelationshipsSupported;
        this.validateTokenTranslations = (builder.validateTokenTranslations != null) ?
                Collections.unmodifiableSet(builder.validateTokenTranslations) : Collections.<TokenTransformConfig>emptySet();
        this.soapDelegationConfig = builder.soapDelegationConfig;
        //Keystore config can be null if we are dealing with an unprotected SecurityPolicy binding, or just the transport binding
        //not sure if the SecurityPolicy validator for the transport binding needs any crypto context, or if it just confirms container. TODO
        Reject.ifNull(issuerName, "Issuer name cannot be null");
        Reject.ifNull(deploymentConfig, "DeploymentConfig cannot be null");
        if (CollectionUtils.isEmpty(issueTokenTypes) && CollectionUtils.isEmpty(validateTokenTranslations)) {
            throw new IllegalArgumentException("One or both of the issue token types or validate token translations must be set.");
        }
        if (this.saml2Config == null) {
            for (TokenTransformConfig tokenTransformConfig : validateTokenTranslations) {
                if (TokenType.SAML2.equals(tokenTransformConfig.getOutputTokenType())) {
                    throw new IllegalStateException("A SAML2 token is a translation output, but no SAML2Config " +
                            "state has been specified to guide the production of SAML2 tokens.");
                }
            }
            for (TokenType tokenType : issueTokenTypes) {
                if (TokenType.SAML2.equals(tokenType)) {
                    throw new IllegalStateException("A SAML2 token is specified as an issued token type, but no SAML2Config " +
                            "state has been specified to guide the production of SAML2 tokens.");
                }
            }
        }
        if (delegationRelationshipsSupported && (soapDelegationConfig == null)) {
            throw new IllegalStateException("If the soap STS instance is configured to support delegation relationship, the " +
                    "SoapDelegationConfig instance must be non-null.");
        }
    }

    public static SoapSTSInstanceConfigBuilder builder() {
        return new SoapSTSInstanceConfigBuilder();
    }

    public SoapDeploymentConfig getDeploymentConfig() {
        return deploymentConfig;
    }

    public Set<TokenType> getIssueTokenTypes() {
        return issueTokenTypes;
    }

    public Set<TokenTransformConfig> getValidateTokenTranslations() {
        return validateTokenTranslations;
    }

    public SoapSTSKeystoreConfig getKeystoreConfig() {
        return keystoreConfig;
    }

    public boolean delegationRelationshipsSupported() {
        return delegationRelationshipsSupported;
    }

    public SoapDelegationConfig getSoapDelegationConfig() {
        return soapDelegationConfig;
    }

    /**
     * @return This method will return the sub-path at which the soap STS instance will be deployed (sub-path relative to the
     * path to the soap_sts servlet defined in the soap sts' web.xml (/sts/*)). This string serves to identify the soap
     * STS instance. This identifier
     * is passed to the TokenGenerationService so that the TGS can issue instance-specific tokens (i.e. reflecting the
     * KeystoreConfig and SAML2Config of the associated STS instance). This path is also the most specific element of the
     * DN identifying the config in the SMS.
     *
     * This method will be called to obtain the id under which the SoapSTSInstanceConfig state is stored in the SMS, and the
     * path to the sts instance published via the STSInstancePublisherImpl. Because this
     * resource will be accessed as a url, it cannot have a trailing slash, as this slash is removed in the http request.
     */
    public String getDeploymentSubPath() {
        String deploymentSubPath = new UrlConstituentCatenatorImpl().catenateUrlConstituents(
                getDeploymentConfig().getRealm(), getDeploymentConfig().getUriElement());
        return new DeploymentPathNormalizationImpl().normalizeDeploymentPath(deploymentSubPath);

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SoapSTSInstanceConfig instance:\n");
        sb.append('\t').append("STSInstanceConfig base: ").append(super.toString()).append('\n');
        sb.append('\t').append("KeyStoreConfig: ").append(keystoreConfig != null ? keystoreConfig : null).append('\n');
        sb.append('\t').append("issuerName: ").append(issuerName).append('\n');
        sb.append('\t').append("issueTokenTypes: ").append(issueTokenTypes).append('\n');
        sb.append('\t').append("validateTokenTranslations: ").append(validateTokenTranslations).append('\n');
        sb.append('\t').append("deploymentConfig: ").append(deploymentConfig).append('\n');
        sb.append('\t').append("delegationRelationshipsSupported: ").append(delegationRelationshipsSupported).append('\n');
        sb.append('\t').append("soapDelegationConfig: ").append(soapDelegationConfig).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SoapSTSInstanceConfig) {
            SoapSTSInstanceConfig otherConfig = (SoapSTSInstanceConfig)other;
            return  super.equals(otherConfig) &&
                    (delegationRelationshipsSupported == otherConfig.delegationRelationshipsSupported) &&
                    Objects.equal(soapDelegationConfig, otherConfig.getSoapDelegationConfig())  &&
                    Objects.equal(keystoreConfig, otherConfig.getKeystoreConfig()) &&
                    deploymentConfig.equals(otherConfig.getDeploymentConfig()) &&
                    Objects.equal(issueTokenTypes, otherConfig.getIssueTokenTypes()) &&
                    Objects.equal(validateTokenTranslations, otherConfig.getValidateTokenTranslations());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    /**
     * @return Provides the json representation of the SoapSTSInstanceConfig instance. The json representation is posted
     * to the sts-publish service to programmatically publish an instance of the soap-sts.
     */
    public JsonValue toJson() {
        JsonValue baseValue = super.toJson();
        baseValue.add(DEPLOYMENT_CONFIG, deploymentConfig.toJson());

        JsonValue supportedTranslations = new JsonValue(new ArrayList<Object>());
        List<Object> translationList = supportedTranslations.asList();
        Iterator<TokenTransformConfig> iter = validateTokenTranslations.iterator();
        while (iter.hasNext()) {
            translationList.add(iter.next().toJson());
        }
        baseValue.add(TRANSFORMED_TOKEN_TYPES, supportedTranslations);

        baseValue.add(SOAP_KEYSTORE_CONFIG, keystoreConfig != null ? keystoreConfig.toJson(): null);

        //cannot just add the issueTokenTypes set directly to the baseValue because the enclosing enums will not be quoted
        if (issueTokenTypes != null) {
            JsonValue issueTokens = new JsonValue(new HashSet<String>());
            Collection<String> issueCollection = issueTokens.asCollection(String.class);
            Iterator<TokenType> tokenTypeIter = issueTokenTypes.iterator();
            while (tokenTypeIter.hasNext()) {
                issueCollection.add(tokenTypeIter.next().name());
            }
            baseValue.add(ISSUE_TOKEN_TYPES, issueTokens);
        }

        baseValue.add(DELEGATION_RELATIONSHIP_SUPPORTED, String.valueOf(delegationRelationshipsSupported));

        baseValue.add(SOAP_DELEGATION_CONFIG, (soapDelegationConfig != null ? soapDelegationConfig.toJson() : null));

        return baseValue;
    }

    /**
     * @param json the json representation of an existing soap sts instance, usually emitted by the toJson method of this class.
     *             The JsonValue parameter cannot be null, or a NullPointerException will be thrown.
     * @return Returns a SoapSTSInstanceConfig instance if the json could be successfully marshaled. A NullPointerException
     * is thrown if the json is null, and an IllegalStateException is thrown if the json is mal-formed.
     */
    public static SoapSTSInstanceConfig fromJson(JsonValue json) {
        if (json == null) {
            throw new NullPointerException("JsonValue cannot be null!");
        }
        STSInstanceConfig baseConfig = STSInstanceConfig.fromJson(json);
        SoapSTSInstanceConfigBuilderBase<?> builder = SoapSTSInstanceConfig.builder()
                .issuerName(baseConfig.getIssuerName())
                .saml2Config(baseConfig.getSaml2Config())
                .deploymentConfig(SoapDeploymentConfig.fromJson(json.get(DEPLOYMENT_CONFIG)));

        JsonValue supportedTranslations = json.get(TRANSFORMED_TOKEN_TYPES);
        if (!supportedTranslations.isNull()) {
            if (!supportedTranslations.isList()) {
                throw new IllegalStateException("Unexpected value for the " + TRANSFORMED_TOKEN_TYPES + " field: "
                        + supportedTranslations.asString());
            }
            Set<TokenTransformConfig> transformConfigSet = new HashSet<TokenTransformConfig>();
            Iterator<Object> iter = supportedTranslations.asList().iterator();
            while (iter.hasNext()) {
                transformConfigSet.add(TokenTransformConfig.fromJson(new JsonValue(iter.next())));
            }
            builder.setValidateTokenTranslations(transformConfigSet);
        }

        builder.soapSTSKeystoreConfig(SoapSTSKeystoreConfig.fromJson(json.get(SOAP_KEYSTORE_CONFIG)));

        if (!json.get(ISSUE_TOKEN_TYPES).isNull()) {
            Iterator iter = json.get(ISSUE_TOKEN_TYPES).asCollection().iterator();
            while (iter.hasNext()) {
                builder.addIssueTokenType(TokenType.valueOf(iter.next().toString()));
            }
        }

        builder.delegationRelationshipsSupported(Boolean.valueOf(json.get(DELEGATION_RELATIONSHIP_SUPPORTED).asString()));

        if (!json.get(SOAP_DELEGATION_CONFIG).isNull()) {
            builder.soapDelegationConfig(SoapDelegationConfig.fromJson(json.get(SOAP_DELEGATION_CONFIG)));
        }
        return builder.build();
    }

    /**
     * @return The state of this instance marshaled into the <code>Map<String>, Set<String>> </code> format required for
     * persistence in the SMS.
     */
    public Map<String, Set<String>> marshalToAttributeMap() {
        /*
        The intent is to leverage the toJson functionality, as a JsonValue is essentially a map, with the following exceptions:
        1. the non-complex objects are not Set<String>, but rather <String>, and thus must be marshaled to a Set<String>. It seems
        like I could go through all of the values in the map, and if any entry is simply a String, I could marshal it to a Set<String>
        2. the complex objects (e.g. deploymentConfig, saml2Config, supportedTokenTranslations, etc) are themselves maps, and
       thus must be 'flattened' into a single map. This is done by calling each of these encapsulated objects to provide a
       map representation, and then insert these values into the top-level map.
         */
        Map<String, Set<String>> interimMap = MapMarshallUtils.toSmsMap(toJson().asMap());
        interimMap.remove(DEPLOYMENT_CONFIG);
        interimMap.putAll(deploymentConfig.marshalToAttributeMap());

        /*
        Here the values are already contained in a set. I want to remove the referenced complex-object, but
        then add each of the TokenTransformConfig instances in the supportTokenTranslationsSet to a Set<String>, obtaining
        a string representation for each TokenTransformConfig instance, and adding it to the Set<String>
         */
        if (validateTokenTranslations != null) {
            interimMap.remove(TRANSFORMED_TOKEN_TYPES);
            Set<String> supportedTransforms = new HashSet<String>();
            interimMap.put(TRANSFORMED_TOKEN_TYPES, supportedTransforms);
            for (TokenTransformConfig ttc : validateTokenTranslations) {
                supportedTransforms.add(ttc.toSMSString());
            }
        }
        if (issueTokenTypes != null) {
            interimMap.remove(ISSUE_TOKEN_TYPES);
            Set<String> tokenTypes = new HashSet<String>();
            interimMap.put(ISSUE_TOKEN_TYPES, tokenTypes);
            for (TokenType tt : issueTokenTypes) {
                tokenTypes.add(tt.toString());
            }
        }
        if (saml2Config != null) {
            interimMap.remove(SAML2_CONFIG);
            interimMap.putAll(saml2Config.marshalToAttributeMap());
        }

        if (keystoreConfig != null) {
            interimMap.remove(SOAP_KEYSTORE_CONFIG);
            interimMap.putAll(keystoreConfig.marshalToAttributeMap());
        }

        if (soapDelegationConfig != null) {
            interimMap.remove(SOAP_DELEGATION_CONFIG);
            interimMap.putAll(soapDelegationConfig.marshalToAttributeMap());
        }

        return interimMap;
    }

    /**
     *
     * @param attributeMap The attributeMap corresponding to the STS-persisted format of a soap-sts instance. Cannot be null.
     * @return the SoapSTSInstanceConfig instance corresponding to attributeMap state. An IllegalStateException will be thrown
     * if marshaling cannot be successfully performed.
     */
    public static SoapSTSInstanceConfig marshalFromAttributeMap(Map<String, Set<String>> attributeMap) {
        /*
        When we are marshaling back from a Map<String, Set<String>>, this Map contains all of the values, also those
        contributed by encapsulated complex objects. So the structure must be 'un-flattened', where the top-level map
        is passed to encapsulated complex-objects, so that they may re-constitute themselves, and then the top-level json entry
        key is set to point at these re-constituted complex objects.

        Not that the marshalToAttributeMap first calls toJson to obtain the map representation, albeit with hierarchical
        elements, which must be subsequently flattened. The 'flattening' performed by the marshalToAttributeMap must then
        be 'inverted' by this method, where all complex objects are re-constituted, using the state in the flattened map.

        */
        Map<String, Object> jsonAttributes = MapMarshallUtils.toJsonValueMap(attributeMap);

        DeploymentConfig deploymentConfig = SoapDeploymentConfig.marshalFromAttributeMap(attributeMap);
        jsonAttributes.remove(DEPLOYMENT_CONFIG);
        jsonAttributes.put(DEPLOYMENT_CONFIG, deploymentConfig.toJson());

        SAML2Config saml2Config = SAML2Config.marshalFromAttributeMap(attributeMap);
        if (saml2Config != null) {
            jsonAttributes.remove(SAML2_CONFIG);
            jsonAttributes.put(SAML2_CONFIG, saml2Config.toJson());
        }

        SoapSTSKeystoreConfig keystoreConfig = SoapSTSKeystoreConfig.marshalFromAttributeMap(attributeMap);
        if (keystoreConfig != null) {
            jsonAttributes.remove(SOAP_KEYSTORE_CONFIG);
            jsonAttributes.put(SOAP_KEYSTORE_CONFIG, keystoreConfig.toJson());
        }

        /*
         The TRANSFORMED_TOKEN_TYPES are currently each in a String representation in the Set<String> map entry corresponding
         to the TRANSFORMED_TOKEN_TYPES key. I need to marshal each back into a TokenTransformConfig instance, and then
         call toJson on each, and put them in a JsonValue wrapping a list.
         */
        ArrayList<JsonValue> jsonTranslationsList = new ArrayList<JsonValue>();
        JsonValue jsonTranslations = new JsonValue(jsonTranslationsList);
        jsonAttributes.remove(TRANSFORMED_TOKEN_TYPES);
        jsonAttributes.put(TRANSFORMED_TOKEN_TYPES, jsonTranslations);
        Set<String> stringTokenTranslations = attributeMap.get(TRANSFORMED_TOKEN_TYPES);
        for (String translation : stringTokenTranslations) {
            jsonTranslationsList.add(TokenTransformConfig.fromSMSString(translation).toJson());
        }

        /*
        Ultimately, the ISSUE_TOKEN_TYPES is a set, but it's set type gets stripped by the MapMarshalUtils.toJsonValueMap
        method. Thus it is a 'complex' object, which must be reconstituted in this method.
         */
        Set<String> jsonIssueSet = new HashSet<String>();
        JsonValue jsonIssueTypes = new JsonValue(jsonIssueSet);
        jsonAttributes.remove(ISSUE_TOKEN_TYPES);
        jsonAttributes.put(ISSUE_TOKEN_TYPES, jsonIssueTypes);
        Set<String> issueTypes = attributeMap.get(ISSUE_TOKEN_TYPES);
        for (String issueType : issueTypes) {
            jsonIssueSet.add(issueType);
        }

        SoapDelegationConfig delegationConfig = SoapDelegationConfig.marshalFromAttributeMap(attributeMap);
        if (delegationConfig != null) {
            jsonAttributes.remove(SOAP_DELEGATION_CONFIG);
            jsonAttributes.put(SOAP_DELEGATION_CONFIG, delegationConfig.toJson());
        }

        return fromJson(new JsonValue(jsonAttributes));
    }

    /*
    When the SaopSecurityTokenServiceViewBean harvests the configurations input by the user, it attempts to publish the
    JsonValue wrapping this Map<String, Set<String>>. It cannot directly attempt to marshal these configuration properties
    in the ViewBean class, as this would introduce a dependency on the rest-sts into the openam-console module. Thus the
    RestSecurityTokenServiceViewBean can only invoke the rest-sts-publish service with a JsonValue wrapping the
    Map<String, Set<String>> (or the Set<String> has to be turned into List<String> as JsonValue#toString does not currently
    turn Set values into json arrays - TODO: is this still true, or can this logic be changed?).
    This method will be invoked with the JsonValue generated by wrapping a Map<String, List<String>>
    containing the user's rest-sts-configurations. It will turn the Map<String, List<String>> wrapped by the JsonValue back into
    a raw Map<String, Set<String>>, and call marshalFromAttributeMap.
     */
    public static SoapSTSInstanceConfig marshalFromJsonAttributeMap(JsonValue jsonValue) throws IllegalStateException {
        if (jsonValue ==  null) {
            throw new IllegalStateException("JsonValue cannot be null!");
        }
        if (!jsonValue.isMap()) {
            throw new IllegalStateException("In SoapSTSInstanceConfig#marshalFromJsonAttributeMap, Passed-in JsonValue " +
                    "is not a map. The JsonValue instance: " + jsonValue.toString());
        }
        Map<String, Set<String>> smsMap = new HashMap<String, Set<String>>();
        for (String key : jsonValue.keys()) {
            final JsonValue value = jsonValue.get(key);
            if (value.isNull()) {
                smsMap.put(key, Collections.EMPTY_SET);
            } else if(!value.isList()) {
                throw new IllegalStateException("In SoapSTSInstanceConfig#marshalFromJsonAttributeMap, value " +
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
