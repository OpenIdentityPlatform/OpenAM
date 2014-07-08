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

import org.apache.ws.security.saml.ext.builder.SAML2Constants;
import org.apache.xml.security.c14n.Canonicalizer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.MapMarshallUtils;
import org.forgerock.util.Reject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * TODO: Ambiguity in the context of setting the customAttributeStatementsProviderClassName
 * and the customAttributeMapperClassName. As it currently stands, the customAttributeStatementsProvider will be passed
 * an instance of the customAttributeMapper if both are specified. The usual case will simply to set the customAttributeMapper,
 * as this allows custom attributes to be set in the AttributeStatement.
 *
 * TODO: do I want a name-qualifier in addition to a nameIdFormat?
 */
public class SAML2Config {
    private static final String BAR = "|";
    public static class SAML2ConfigBuilder {
        /*
        use the ws-security constant, instead of the SAML2Constants defined in openam-federation, as this dependency
        introduces a dependency on openam-core, which pulls the ws-* dependencies into the soap-sts, which I don't want.
         */
        private String nameIdFormat = SAML2Constants.NAMEID_FORMAT_UNSPECIFIED;
        private Map<String, String> attributeMap;
        private long tokenLifetimeInSeconds = 60 * 10; //default token lifetime is 10 minutes
        /**
         * This list contains the EntityIds (URLs) of the SPs. It will be used to populate the Audiences of the AudienceRestriction
         * element of the Conditions element, as required for bearer tokens.
         * http://docs.oasis-open.org/security/saml/v2.0/saml-profiles-2.0-os.pdf
         */
        private List<String> audiences;
        private String customConditionsProviderClassName;
        private String customSubjectProviderClassName;
        private String customAuthenticationStatementsProviderClassName;
        private String customAttributeStatementsProviderClassName;
        private String customAuthzDecisionStatementsProviderClassName;
        private String customAttributeMapperClassName;
        private String customAuthNContextMapperClassName;
        private String canonicalizationAlgorithm = Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS;
        private String signatureAlgorithm;
        private boolean signAssertion = true;

        public SAML2ConfigBuilder nameIdFormat(String nameIdFormat) {
            //TODO - test to see if it matches one of the allowed values?
            this.nameIdFormat = nameIdFormat;
            return this;
        }

        public SAML2ConfigBuilder attributeMap(Map<String, String> attributeMap) {
            this.attributeMap = Collections.unmodifiableMap(attributeMap);
            return this;
        }

        public SAML2ConfigBuilder tokenLifetimeInSeconds(long lifetimeInSeconds) {
            this.tokenLifetimeInSeconds = lifetimeInSeconds;
            return this;
        }

        public SAML2ConfigBuilder audiences(List<String> audiences) {
            this.audiences = new ArrayList<String>(audiences);
            return this;
        }

        public SAML2ConfigBuilder customConditionsProviderClassName(String customConditionsProviderClassName) {
            this.customConditionsProviderClassName = customConditionsProviderClassName;
            return this;
        }

        public SAML2ConfigBuilder customSubjectProviderClassName(String customSubjectProviderClassName) {
            this.customSubjectProviderClassName = customSubjectProviderClassName;
            return this;
        }

        public SAML2ConfigBuilder customAuthenticationStatementsProviderClassName(String customAuthenticationStatementsProviderClassName) {
            this.customAuthenticationStatementsProviderClassName = customAuthenticationStatementsProviderClassName;
            return this;
        }

        public SAML2ConfigBuilder customAttributeStatementsProviderClassName(String customAttributeStatementsProviderClassName) {
            this.customAttributeStatementsProviderClassName = customAttributeStatementsProviderClassName;
            return this;
        }

        public SAML2ConfigBuilder customAuthzDecisionStatementsProviderClassName(String customAuthzDecisionStatementsProviderClassName) {
            this.customAuthzDecisionStatementsProviderClassName = customAuthzDecisionStatementsProviderClassName;
            return this;
        }

        public SAML2ConfigBuilder customAttributeMapperClassName(String customAttributeMapperClassName) {
            this.customAttributeMapperClassName = customAttributeMapperClassName;
            return this;
        }

        public SAML2ConfigBuilder customAuthNContextMapperClassName(String customAuthNContextMapperClassName) {
            this.customAuthNContextMapperClassName = customAuthNContextMapperClassName;
            return this;
        }

        public SAML2ConfigBuilder canonicalizationAlgorithm(String canonicalizationAlgorithm) {
            this.canonicalizationAlgorithm = canonicalizationAlgorithm;
            return this;
        }

        /*
        See comments in SAML2AssertionSigner class about the ultimate validation of the correctness of this specification.
        One issue with maintaining a static map of valid values is that the xmlsec package allows for the dynamic registration
        of handlers for new signature specification types. It could make sense to new up an instance of the
        org.apache.xml.security.algorithms.SignatureAlgorithm class with the specified algorithm, but this is a bit hacky,
        and requires a Document parameter as well. The allowed set of algorithms is probably best handled in documentation.
        If no algorithm is set, either http://www.w3.org/2000/09/xmldsig#dsa-sha1 or http://www.w3.org/2000/09/xmldsig#rsa-sha1
        is used, depending upon the type of the private key.
         */
        public SAML2ConfigBuilder signatureAlgorithm(String signatureAlgorithm) {
            this.signatureAlgorithm = signatureAlgorithm;
            return this;
        }

        public SAML2ConfigBuilder signAssertion(boolean signAssertion) {
            this.signAssertion = signAssertion;
            return this;
        }


        public SAML2Config build() {
            return new SAML2Config(this);
        }
    }

    /*
    Define the names of fields to aid in json marshalling. Note that these names match the names of the AttributeSchema
    entries in restSTS.xml, as this aids in marshalling an instance of this class into the attribute map needed for
    SMS persistence.
     */
    private static final String NAME_ID_FORMAT = "saml2-name-id-format";
    private static final String ATTRIBUTE_MAP = "saml2-attribute-map";
    private static final String TOKEN_LIFETIME = "saml2-token-lifetime-seconds";
    private static final String AUDIENCES = "saml2-audiences";
    private static final String CUSTOM_CONDITIONS_PROVIDER_CLASS = "saml2-custom-conditions-provider-class-name";
    private static final String CUSTOM_SUBJECT_PROVIDER_CLASS = "saml2-custom-subject-provider-class-name";
    private static final String CUSTOM_ATTRIBUTE_STATEMENTS_PROVIDER_CLASS = "saml2-custom-attribute-statements-provider-class-name";
    private static final String CUSTOM_AUTHENTICATION_STATEMENTS_PROVIDER_CLASS = "saml2-custom-authentication-statements-provider-class-name";
    private static final String CUSTOM_AUTHZ_DECISION_STATEMENTS_PROVIDER_CLASS = "saml2-custom-authz-decision-statements-provider-class-name";
    private static final String CUSTOM_ATTRIBUTE_MAPPER_CLASS = "saml2-custom-attribute-mapper-class-name";
    private static final String CUSTOM_AUTHN_CONTEXT_MAPPER_CLASS = "saml2-custom-authn-context-mapper-class-name";
    private static final String SIGNATURE_ALGORITHM = "saml2-signature-algorithm";
    private static final String CANONICALIZATION_ALGORITHM = "saml2-canonicalization-algorithm";
    private static final String SIGN_ASSERTION = "saml2-sign-assertion";

    private final String nameIdFormat;
    private final Map<String, String> attributeMap;
    private final long tokenLifetimeInSeconds;
    private final List<String> audiences;
    private final String customConditionsProviderClassName;
    private final String customSubjectProviderClassName;
    private final String customAuthenticationStatementsProviderClassName;
    private final String customAttributeStatementsProviderClassName;
    private final String customAuthzDecisionStatementsProviderClassName;
    private final String customAttributeMapperClassName;
    private final String customAuthNContextMapperClassName;
    private final String signatureAlgorithm;
    private final String canonicalizationAlgorithm;
    private final boolean signAssertion;

    private SAML2Config(SAML2ConfigBuilder builder) {
        this.nameIdFormat = builder.nameIdFormat; //not required so don't reject if null
        if (builder.attributeMap != null) {
            this.attributeMap = Collections.unmodifiableMap(builder.attributeMap);
        } else {
            attributeMap = Collections.emptyMap();
        }
        if (builder.audiences != null) {
            this.audiences = Collections.unmodifiableList(builder.audiences);
        } else {
            audiences = Collections.emptyList();
        }
        tokenLifetimeInSeconds = builder.tokenLifetimeInSeconds; //will be set to default if not explicitly set
        customConditionsProviderClassName = builder.customConditionsProviderClassName;
        customSubjectProviderClassName = builder.customSubjectProviderClassName;
        customAuthenticationStatementsProviderClassName = builder.customAuthenticationStatementsProviderClassName;
        customAuthzDecisionStatementsProviderClassName = builder.customAuthzDecisionStatementsProviderClassName;
        customAttributeStatementsProviderClassName = builder.customAttributeStatementsProviderClassName;
        customAttributeMapperClassName = builder.customAttributeMapperClassName;
        customAuthNContextMapperClassName = builder.customAuthNContextMapperClassName;
        signatureAlgorithm = builder.signatureAlgorithm;
        canonicalizationAlgorithm = builder.canonicalizationAlgorithm;
        this.signAssertion = builder.signAssertion;

        Reject.ifNull(canonicalizationAlgorithm, "CanonicalizationAlgorithm must be set.");
        if (!Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS.equals(canonicalizationAlgorithm) && !Canonicalizer.ALGO_ID_C14N_EXCL_WITH_COMMENTS.equals(canonicalizationAlgorithm)) {
            throw new IllegalArgumentException("Canonicalization algorithm must be set to either " +
                    Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS + " or " + Canonicalizer.ALGO_ID_C14N_EXCL_WITH_COMMENTS + ". See" +
                    "section 5.4.3 or 5.4.4 of http://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf for details.");
        }
    }

    public static SAML2ConfigBuilder builder() {
        return new SAML2ConfigBuilder();
    }

    public String getNameIdFormat() {
        return nameIdFormat;
    }

    public long getTokenLifetimeInSeconds() {
        return tokenLifetimeInSeconds;
    }

    public List<String> getAudiences() {
        return audiences;
    }

    public Map<String, String> getAttributeMap() {
        return attributeMap;
    }

    public String getCustomConditionsProviderClassName() {
        return customConditionsProviderClassName;
    }

    public String getCustomSubjectProviderClassName() {
        return customSubjectProviderClassName;
    }

    public String getCustomAuthenticationStatementsProviderClassName() {
        return customAuthenticationStatementsProviderClassName;
    }

    public String getCustomAttributeMapperClassName() {
        return customAttributeMapperClassName;
    }

    public String getCustomAuthNContextMapperClassName() {
        return customAuthNContextMapperClassName;
    }

    public String getCustomAttributeStatementsProviderClassName() {
        return customAttributeStatementsProviderClassName;
    }

    public String getCustomAuthzDecisionStatementsProviderClassName() {
        return customAuthzDecisionStatementsProviderClassName;
    }

    public String getCanonicalizationAlgorithm() {
        return canonicalizationAlgorithm;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public boolean signAssertion() {
        return signAssertion;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SAML2Config instance:").append('\n');
        sb.append('\t').append("nameIDFormat: ").append(nameIdFormat).append('\n');
        sb.append('\t').append("attributeMap: ").append(attributeMap).append('\n');
        sb.append('\t').append("audiences: ").append(audiences).append('\n');
        sb.append('\t').append("tokenLifetimeInSeconds: ").append(tokenLifetimeInSeconds).append('\n');
        sb.append('\t').append("customConditionsProviderClassName: ").append(customConditionsProviderClassName).append('\n');
        sb.append('\t').append("customSubjectProviderClassName: ").append(customSubjectProviderClassName).append('\n');
        sb.append('\t').append("customAttributeStatementsProviderClassName: ").append(customAttributeStatementsProviderClassName).append('\n');
        sb.append('\t').append("customAttributeMapperClassName: ").append(customAttributeMapperClassName).append('\n');
        sb.append('\t').append("customAuthNContextMapperClassName: ").append(customAuthNContextMapperClassName).append('\n');
        sb.append('\t').append("customAuthenticationStatementsProviderClassName: ").append(customAuthenticationStatementsProviderClassName).append('\n');
        sb.append('\t').append("customAuthzDecisionStatementsProviderClassName: ").append(customAuthzDecisionStatementsProviderClassName).append('\n');
        sb.append('\t').append("signatureAlgorithm: ").append(signatureAlgorithm).append('\n');
        sb.append('\t').append("canonicalizationAlgorithm: ").append(canonicalizationAlgorithm).append('\n');
        sb.append('\t').append("Sign assertion ").append(signAssertion).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SAML2Config) {
            SAML2Config otherConfig = (SAML2Config)other;
            return nameIdFormat.equals(otherConfig.getNameIdFormat()) &&
                    tokenLifetimeInSeconds == otherConfig.tokenLifetimeInSeconds &&
                    attributeMap.equals(otherConfig.attributeMap) &&
                    audiences.equals(otherConfig.audiences) &&
                    signAssertion == otherConfig.signAssertion() &&
                    (customConditionsProviderClassName != null
                            ? customConditionsProviderClassName.equals(otherConfig.getCustomConditionsProviderClassName())
                            : otherConfig.getCustomConditionsProviderClassName() == null) &&
                    (customSubjectProviderClassName != null
                            ? customSubjectProviderClassName.equals(otherConfig.getCustomSubjectProviderClassName())
                            : otherConfig.getCustomSubjectProviderClassName() == null) &&
                    (customAttributeStatementsProviderClassName != null
                        ? customAttributeStatementsProviderClassName.equals(otherConfig.getCustomAttributeStatementsProviderClassName())
                        : otherConfig.getCustomAttributeStatementsProviderClassName() == null) &&
                    (customAuthzDecisionStatementsProviderClassName != null
                            ? customAuthzDecisionStatementsProviderClassName.equals(otherConfig.getCustomAuthzDecisionStatementsProviderClassName())
                            : otherConfig.getCustomAuthzDecisionStatementsProviderClassName() == null) &&
                    (customAttributeMapperClassName != null
                            ? customAttributeMapperClassName.equals(otherConfig.getCustomAttributeMapperClassName())
                            : otherConfig.getCustomAttributeMapperClassName() == null) &&
                    (customAuthNContextMapperClassName != null
                            ? customAuthNContextMapperClassName.equals(otherConfig.getCustomAuthNContextMapperClassName())
                            : otherConfig.getCustomAuthNContextMapperClassName() == null) &&
                    (customAuthenticationStatementsProviderClassName != null
                        ? customAuthenticationStatementsProviderClassName.equals(otherConfig.getCustomAuthenticationStatementsProviderClassName())
                        : otherConfig.getCustomAuthenticationStatementsProviderClassName() == null) &&
                    (signatureAlgorithm != null
                        ? signatureAlgorithm.equals(otherConfig.getSignatureAlgorithm())
                        : otherConfig.getSignatureAlgorithm() == null) &&
                    (canonicalizationAlgorithm != null
                            ? canonicalizationAlgorithm.equals(otherConfig.getCanonicalizationAlgorithm())
                            : otherConfig.getCanonicalizationAlgorithm() == null);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (nameIdFormat + attributeMap + audiences + canonicalizationAlgorithm + Long.toString(tokenLifetimeInSeconds)).hashCode();
    }

    /*
    Because toJson will be used to produce the map that will also be used to marshal to the SMS attribute map format, and
    because the SMS attribute map format represents all values as Set<String>, I need to represent all of the json values
    as strings as well.
     */
    public JsonValue toJson() {
        JsonValue jsonValue = json(object(
                field(NAME_ID_FORMAT, nameIdFormat),
                field(TOKEN_LIFETIME, String.valueOf(tokenLifetimeInSeconds)),
                field(CUSTOM_CONDITIONS_PROVIDER_CLASS, customConditionsProviderClassName),
                field(CUSTOM_SUBJECT_PROVIDER_CLASS, customSubjectProviderClassName),
                field(CUSTOM_ATTRIBUTE_STATEMENTS_PROVIDER_CLASS, customAttributeStatementsProviderClassName),
                field(CUSTOM_ATTRIBUTE_MAPPER_CLASS, customAttributeMapperClassName),
                field(CUSTOM_AUTHN_CONTEXT_MAPPER_CLASS, customAuthNContextMapperClassName),
                field(CUSTOM_AUTHENTICATION_STATEMENTS_PROVIDER_CLASS, customAuthenticationStatementsProviderClassName),
                field(CUSTOM_AUTHZ_DECISION_STATEMENTS_PROVIDER_CLASS, customAuthzDecisionStatementsProviderClassName),
                field(SIGNATURE_ALGORITHM, signatureAlgorithm),
                field(SIGN_ASSERTION, String.valueOf(signAssertion)),
                field(CANONICALIZATION_ALGORITHM, canonicalizationAlgorithm)));

        JsonValue jsonValueAttributeMap = new JsonValue(new HashMap<String, Object>());
        Map<String, Object> jsonAttributeMap = jsonValueAttributeMap.asMap();
        jsonAttributeMap.putAll(attributeMap);
        jsonValue.add(ATTRIBUTE_MAP, jsonAttributeMap);

        JsonValue jsonAudiences = new JsonValue(new ArrayList<Object>());
        List<Object> audienceList = jsonAudiences.asList();
        audienceList.addAll(audiences);
        jsonValue.add(AUDIENCES, jsonAudiences);
        return jsonValue;
    }

    public static SAML2Config fromJson(JsonValue json) throws IllegalStateException {
        SAML2ConfigBuilder builder = SAML2Config.builder()
                .nameIdFormat(json.get(NAME_ID_FORMAT).asString())
                //because we have to go to the SMS Map representation, where all values are Set<String>, I need to
                // pull the value from Json as a string, and then parse out a Long.
                .tokenLifetimeInSeconds(Long.valueOf(json.get(TOKEN_LIFETIME).asString()))
                .customConditionsProviderClassName(json.get(CUSTOM_CONDITIONS_PROVIDER_CLASS).asString())
                .customSubjectProviderClassName(json.get(CUSTOM_SUBJECT_PROVIDER_CLASS).asString())
                .customAttributeStatementsProviderClassName(json.get(CUSTOM_ATTRIBUTE_STATEMENTS_PROVIDER_CLASS).asString())
                .customAttributeMapperClassName(json.get(CUSTOM_ATTRIBUTE_MAPPER_CLASS).asString())
                .customAuthNContextMapperClassName(json.get(CUSTOM_AUTHN_CONTEXT_MAPPER_CLASS).asString())
                .customAuthenticationStatementsProviderClassName(json.get(CUSTOM_AUTHENTICATION_STATEMENTS_PROVIDER_CLASS).asString())
                .customAuthzDecisionStatementsProviderClassName(json.get(CUSTOM_AUTHZ_DECISION_STATEMENTS_PROVIDER_CLASS).asString())
                .signatureAlgorithm(json.get(SIGNATURE_ALGORITHM).asString())
                .signAssertion(Boolean.valueOf(json.get(SIGN_ASSERTION).asString()))
                .canonicalizationAlgorithm(json.get(CANONICALIZATION_ALGORITHM).asString());

        JsonValue jsonAttributes = json.get(ATTRIBUTE_MAP);
        if (!jsonAttributes.isMap()) {
            throw new IllegalStateException("Unexpected value for the " + ATTRIBUTE_MAP + " field: "
                    + jsonAttributes);
        }
        Map<String, String> toBeSetAttrMap = new HashMap<String, String>();
        Map<String, Object> jsonAttrMap = jsonAttributes.asMap();
        for (Map.Entry<String, Object> entry : jsonAttrMap.entrySet()) {
            toBeSetAttrMap.put(entry.getKey(), entry.getValue().toString());
        }
        builder.attributeMap(toBeSetAttrMap);

        JsonValue jsonAudiences = json.get(AUDIENCES);
        if (!jsonAudiences.isList()) {
            throw new IllegalStateException("Unexpected value for the " + AUDIENCES + " field: "
                    + jsonAudiences);
        }
        List<String> toBeSetAudiences = new ArrayList<String>();
        Iterator<Object> iter = jsonAudiences.asList().iterator();
        while (iter.hasNext()) {
            toBeSetAudiences.add(iter.next().toString());
        }
        builder.audiences(toBeSetAudiences);

        return builder.build();
    }

    /*
    We need to marshal the SAML2Config instance to a Map<String, Object>. The JsonValue of toJson gets us there,
    except for the complex types for the audiences and attribute map. These need to be marshaled into a Set<String>, and
    these entries included in the top-level map, replacing the existing complex entries.
     */
    public Map<String, Set<String>> marshalToAttributeMap() {
        Map<String, Object> preMap = toJson().asMap();
        Map<String, Set<String>> finalMap = MapMarshallUtils.toSmsMap(preMap);
        Object attributesObject = preMap.get(ATTRIBUTE_MAP);
        if (attributesObject instanceof Map) {
            finalMap.remove(ATTRIBUTE_MAP);
            Set<String> attributeValues = new LinkedHashSet<String>();
            finalMap.put(ATTRIBUTE_MAP, attributeValues);
            for (Map.Entry<String, String> entry : ((Map<String, String>)attributesObject).entrySet()) {
                attributeValues.add(entry.getKey() + BAR + entry.getValue());
            }
        } else {
            throw new IllegalStateException("Type corresponding to " + ATTRIBUTE_MAP + " key unexpected. Type: "
                    + (attributesObject != null ? attributesObject.getClass().getName() :" null"));
        }

        Object audiencesObject = preMap.get(AUDIENCES);
        if ((audiencesObject instanceof JsonValue) && ((JsonValue) audiencesObject).isList()) {
            finalMap.remove(AUDIENCES);
            Set<String> audienceValues = new LinkedHashSet<String>();
            finalMap.put(AUDIENCES, audienceValues);
            for (Object obj : ((JsonValue)audiencesObject).asList()) {
                audienceValues.add(obj.toString());
            }
        } else {
            throw new IllegalStateException("Type corresponding to " + AUDIENCES + " key unexpected. Type: "
                    + (audiencesObject != null ? audiencesObject.getClass().getName() :" null"));
        }
        return finalMap;
    }

    /*
    Here we have to modify the ATTRIBUTE_MAP and AUDIENCES entries to match the JsonValue format expected by
    fromJson, and then call the static fromJson. This method must marshal between the Json representation of a complex
    object, and the representation expected by the SMS
     */
    public static SAML2Config marshalFromAttributeMap(Map<String, Set<String>> smsAttributeMap) {
        Set<String> attributes = smsAttributeMap.get(ATTRIBUTE_MAP);
        /*
        The STSInstanceConfig may not have SAML2Config, if there are no defined token transformations that result
        in a SAML2 assertion. So if we have null attributes, this means that STSInstanceConfig.marshalFromAttributeMap
        was called. Note that we cannot check for isEmpty, as this will be the case if SAML2Config has been defined, but
        simply without any attributes.
         */
        if (attributes == null) {
            return null;
        }
        Map<String, Object> jsonAttributes = MapMarshallUtils.toJsonValueMap(smsAttributeMap);
        jsonAttributes.remove(ATTRIBUTE_MAP);
        Map<String, Object> jsonAttributeMap = new LinkedHashMap<String, Object>();
        for (String entry : attributes) {
            StringTokenizer st = new StringTokenizer(entry, BAR);
            jsonAttributeMap.put(st.nextToken(), st.nextToken());
        }
        jsonAttributes.put(ATTRIBUTE_MAP, new JsonValue(jsonAttributeMap));

        /*
        AUDIENCES is a Set<String> in the smsAttributeMap, but fromJson expects a List.
         */
        jsonAttributes.remove(AUDIENCES);
        JsonValue jsonAudiences = new JsonValue(new ArrayList<Object>());
        List<Object> audienceList = jsonAudiences.asList();
        audienceList.addAll(smsAttributeMap.get(AUDIENCES));
        jsonAttributes.put(AUDIENCES, jsonAudiences);

        return fromJson(new JsonValue(jsonAttributes));
    }
}
