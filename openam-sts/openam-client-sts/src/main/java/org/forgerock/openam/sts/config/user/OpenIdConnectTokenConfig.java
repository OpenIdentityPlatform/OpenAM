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

import org.forgerock.guava.common.base.Objects;
import org.forgerock.guava.common.collect.Sets;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithmType;
import org.forgerock.openam.shared.sts.SharedSTSConstants;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.MapMarshallUtils;
import org.forgerock.openam.utils.CollectionUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * Encapsulates the configuration state necessary to produce OpenId Connect Id Tokens.
 *
 * Each published rest-sts instance will encapsulate state to allow it to issue OIDC Id Tokens for a single
 * OIDC RP as a OIDC OP. The OP iss, as well as the RP aud and azp are specified in this class.
 * The signatureAlias corresponds to the OP's signing key.
 *
 */
public class OpenIdConnectTokenConfig {
    private static final String EQUALS = "=";

    public static class OIDCIdTokenConfigBuilder {
        private String issuer;
        private long tokenLifetimeInSeconds = 60 * 10; //default token lifetime is 10 minutes
        private Map<String, String> claimMap;
        private JwsAlgorithm signatureAlgorithm;
        private List<String> audience;
        private String authorizedParty;
        private String keystoreLocation;
        private byte[] keystorePassword;
        //corresponds to the key used to sign the assertion.
        private String signatureKeyAlias;
        private byte[] signatureKeyPassword;
        private byte[] clientSecret;
        private String customClaimMapperClass;
        private String customAuthenticationContextMapper;
        private String customAuthenticationMethodReferencesMapper;

        private OIDCIdTokenConfigBuilder() {
            audience = new ArrayList<>();
        }
        /**
         * Corresponds to the manner in which the public key corresponding to the private signing key is
         * referenced in the jwk. Valid types described here: https://tools.ietf.org/html/draft-ietf-jose-json-web-signature-41#section-4.1
         * Currently, only none and jwk will be supported.
         */
        private OpenIdConnectTokenPublicKeyReferenceType publicKeyReferenceType = OpenIdConnectTokenPublicKeyReferenceType.NONE;


        public OIDCIdTokenConfigBuilder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public OIDCIdTokenConfigBuilder publicKeyReferenceType(String publicKeyReferenceType) {
            this.publicKeyReferenceType = OpenIdConnectTokenPublicKeyReferenceType.valueOf(publicKeyReferenceType);
            return this;
        }

        public OIDCIdTokenConfigBuilder publicKeyReferenceType(OpenIdConnectTokenPublicKeyReferenceType publicKeyReferenceType) {
            this.publicKeyReferenceType = publicKeyReferenceType;
            return this;
        }

        public OIDCIdTokenConfigBuilder authorizedParty(String authorizedParty) {
            this.authorizedParty = authorizedParty;
            return this;
        }

        public OIDCIdTokenConfigBuilder addAudience(String audience) {
            this.audience.add(audience);
            return this;
        }

        public OIDCIdTokenConfigBuilder setAudience(List<String> audience) {
            this.audience.addAll(audience);
            return this;
        }

        public OIDCIdTokenConfigBuilder signatureAlgorithm(String signatureAlgorithm) {
            this.signatureAlgorithm = JwsAlgorithm.valueOf(signatureAlgorithm);
            return this;
        }

        public OIDCIdTokenConfigBuilder signatureAlgorithm(JwsAlgorithm signatureAlgorithm) {
            this.signatureAlgorithm = signatureAlgorithm;
            return this;
        }

        /**
         * Contains the mapping of OIDC token claim names (Map keys) to local OpenAM attributes (Map values)
         * in configured data stores. The keys in the map will be claim entries in the issued OIDC token, and the
         * value of these claims will be the principal attribute state resulting from LDAP datastore lookup of the map values.
         * If no values are returned from the the LDAP datastore lookup of the attribute corresponding to the map value,
         * no claim will be set in the issued OIDC token
         * @param claimMap the map specifying the state corresponding to the description above
         * @return the builder
         */
        public OIDCIdTokenConfigBuilder claimMap(Map<String, String> claimMap) {
            this.claimMap = Collections.unmodifiableMap(claimMap);
            return this;
        }

        public OIDCIdTokenConfigBuilder tokenLifetimeInSeconds(long lifetimeInSeconds) {
            this.tokenLifetimeInSeconds = lifetimeInSeconds;
            return this;
        }

        public OIDCIdTokenConfigBuilder signatureKeyAlias(String signatureKeyAlias) {
            this.signatureKeyAlias = signatureKeyAlias;
            return this;
        }

        public OIDCIdTokenConfigBuilder signatureKeyPassword(byte[] signatureKeyPassword) {
            this.signatureKeyPassword = signatureKeyPassword;
            return this;
        }

        public OIDCIdTokenConfigBuilder keystoreLocation(String keystoreLocation) {
            this.keystoreLocation = keystoreLocation;
            return this;
        }

        public OIDCIdTokenConfigBuilder keystorePassword(byte[] keystorePassword) {
            this.keystorePassword = keystorePassword;
            return this;
        }

        public OIDCIdTokenConfigBuilder clientSecret(byte[] clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        /**
         * Sets a custom implementation of the org.forgerock.openam.sts.tokengeneration.oidc.OpenIdConnectTokenClaimMapper
         * interface, which will be referenced when performing the claim mapping for custom claims to be included in issued
         * OIDC tokens. If not set, the org.forgerock.openam.sts.tokengeneration.oidc.DefaultOpenIdConnectTokenClaimMapper
         * will be referenced.
         * @param customClaimMapperClass custom implementation of the org.forgerock.openam.sts.tokengeneration.oidc.OpenIdConnectTokenClaimMapper
         *                               interface
         * @return the builder
         */
        public OIDCIdTokenConfigBuilder customClaimMapperClass(String customClaimMapperClass) {
            this.customClaimMapperClass = customClaimMapperClass;
            return this;
        }

        /**
         * Sets a custom implementation of the org.forgerock.openam.sts.rest.token.provider.oidc.OpenIdConnectTokenAuthnContextMapper
         * interface, to be consulted when obtaining the optional acr claim to be included in issued OIDC tokens. If not set, the
         * org.forgerock.openam.sts.rest.token.provider.oidc.DefaultOpenIdConnectTokenAuthnContextMapper will be used, which
         * simply returns null.
         * @param customAuthenticationContextMapperClass the class name of the
         *                                               org.forgerock.openam.sts.rest.token.provider.oidc.OpenIdConnectTokenAuthnContextMapper
         *                                               interface
         * @return the builder
         */
        public OIDCIdTokenConfigBuilder customAuthenticationContextMapperClass(String customAuthenticationContextMapperClass) {
            this.customAuthenticationContextMapper = customAuthenticationContextMapperClass;
            return this;
        }

        /**
         * Sets a custom implementation of the org.forgerock.openam.sts.rest.token.provider.oidc.OpenIdConnectTokenAuthModeReferencesMapper
         * interface, to be consulted when obtaining the optional acr claim to be included in issued OIDC tokens. If not set, the
         * org.forgerock.openam.sts.rest.token.provider.oidc.DefaultOpenIdConnectTokenAuthModeReferencesMapper will be used, which
         * simply returns null.
         * @param customAuthenticationMethodReferencesMapperClass the class name of the
         *                                                      org.forgerock.openam.sts.rest.token.provider.oidc.OpenIdConnectTokenAuthModeReferencesMapper
         *                                                      interface
         * @return the builder
         */
        public OIDCIdTokenConfigBuilder customAuthenticationMethodReferencesMapperClass(String customAuthenticationMethodReferencesMapperClass) {
            this.customAuthenticationMethodReferencesMapper = customAuthenticationMethodReferencesMapperClass;
            return this;
        }

        public OpenIdConnectTokenConfig build() {
            return new OpenIdConnectTokenConfig(this);
        }
    }

    /*
    Define the names of fields to aid in json marshalling. Note that these names match the names of the AttributeSchema
    entries in restSTS.xml and soapSTS.xml, as this aids in marshalling an instance of this class into the attribute map needed for
    SMS persistence.
     */
    static final String ISSUER = SharedSTSConstants.OIDC_ISSUER;
    static final String CLAIM_MAP = SharedSTSConstants.OIDC_CLAIM_MAP;
    static final String TOKEN_LIFETIME = SharedSTSConstants.OIDC_TOKEN_LIFETIME;
    static final String KEYSTORE_LOCATION = SharedSTSConstants.OIDC_KEYSTORE_LOCATION;
    static final String KEYSTORE_PASSWORD = SharedSTSConstants.OIDC_KEYSTORE_PASSWORD;
    static final String SIGNATURE_KEY_ALIAS = SharedSTSConstants.OIDC_SIGNATURE_KEY_ALIAS;
    static final String SIGNATURE_KEY_PASSWORD = SharedSTSConstants.OIDC_SIGNATURE_KEY_PASSWORD;
    static final String SIGNATURE_ALGORITHM = SharedSTSConstants.OIDC_SIGNATURE_ALGORITHM;
    static final String AUDIENCE = SharedSTSConstants.OIDC_AUDIENCE;
    static final String AUTHORIZED_PARTY = "oidc-authorized-party";
    static final String CLIENT_SECRET = SharedSTSConstants.OIDC_CLIENT_SECRET;
    static final String PUBLIC_KEY_REFERENCE_TYPE = "oidc-public-key-reference-type";
    static final String CUSTOM_CLAIM_MAPPER_CLASS = "oidc-custom-claim-mapper-class";
    static final String CUSTOM_AUTHN_CONTEXT_MAPPER_CLASS = "oidc-custom-authn-context-mapper-class";
    static final String CUSTOM_AUTHN_METHOD_REFERENCES_MAPPER_CLASS = "oidc-custom-authn-method-references-mapper-class";

    private final String issuer;
    private final Map<String, String> claimMap;
    private final JwsAlgorithm signatureAlgorithm;
    private final List<String> audience;
    private final String authorizedParty;

    private final long tokenLifetimeInSeconds;
    private final String keystoreLocation;
    private final byte[] keystorePassword;
    private final String signatureKeyAlias;
    private final byte[] signatureKeyPassword;
    private final byte[] clientSecret;
    private final OpenIdConnectTokenPublicKeyReferenceType publicKeyReferenceType;
    private final String customClaimMapperClass;
    private final String customAuthenticationContextMapper;
    private final String customAuthenticationMethodReferencesMapper;

    private OpenIdConnectTokenConfig(OIDCIdTokenConfigBuilder builder) {
        this.issuer = builder.issuer;
        if (this.issuer == null) {
            throw new IllegalArgumentException("An OIDC issuer must be specified.");
        }
        if (builder.claimMap != null) {
            this.claimMap = Collections.unmodifiableMap(builder.claimMap);
        } else {
            claimMap = Collections.emptyMap();
        }
        this.tokenLifetimeInSeconds = builder.tokenLifetimeInSeconds;
        this.keystoreLocation = builder.keystoreLocation;
        this.keystorePassword = builder.keystorePassword;
        this.signatureKeyAlias = builder.signatureKeyAlias;
        this.signatureKeyPassword = builder.signatureKeyPassword;
        this.audience = Collections.unmodifiableList(builder.audience);
        this.signatureAlgorithm = builder.signatureAlgorithm;
        this.clientSecret = builder.clientSecret;
        this.authorizedParty = builder.authorizedParty;
        this.publicKeyReferenceType = builder.publicKeyReferenceType;
        this.customClaimMapperClass = builder.customClaimMapperClass;
        this.customAuthenticationContextMapper = builder.customAuthenticationContextMapper;
        this.customAuthenticationMethodReferencesMapper = builder.customAuthenticationMethodReferencesMapper;

        if (this.signatureAlgorithm == null) {
            throw new IllegalArgumentException("Signature algorithm must be set, or set to NONE if jwt should not be signed");
        }

        if (CollectionUtils.isEmpty(this.audience)) {
            throw new IllegalArgumentException("An audience must be specified.");
        }

        if (JwsAlgorithmType.RSA.equals(signatureAlgorithm.getAlgorithmType())) {
            if ((keystoreLocation == null) || (keystorePassword == null) || (signatureKeyAlias == null) || (signatureKeyPassword == null)) {
                throw new IllegalArgumentException("For a signing algorithm of "+ signatureAlgorithm + " the keystore location, " +
                        "password, and signature key alias and password values must be specified.");
            }
        }

        if (JwsAlgorithmType.HMAC.equals(signatureAlgorithm.getAlgorithmType())) {
            if (clientSecret == null) {
                throw new IllegalArgumentException("The client secret must be set for HMAC family of signing algorithms.");
            }
        }

        if (publicKeyReferenceType == null) {
            throw new IllegalArgumentException("A OpenIdConnectTokenPublicKeyReferenceType must be specified.");
        }
    }

    public static OIDCIdTokenConfigBuilder builder() {
        return new OIDCIdTokenConfigBuilder();
    }

    public String getIssuer() {
        return issuer;
    }

    public long getTokenLifetimeInSeconds() {
        return tokenLifetimeInSeconds;
    }

    public Map<String, String> getClaimMap() {
        return claimMap;
    }

    public String getKeystoreLocation() {
        return keystoreLocation;
    }

    public byte[] getKeystorePassword() {
        return keystorePassword;
    }

    public String getSignatureKeyAlias() {
        return signatureKeyAlias;
    }

    public byte[] getSignatureKeyPassword() {
        return signatureKeyPassword;
    }

    public JwsAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public List<String> getAudience() {
        return audience;
    }

    public String getAuthorizedParty() {
        return authorizedParty;
    }

    public OpenIdConnectTokenPublicKeyReferenceType getPublicKeyReferenceType() {
        return publicKeyReferenceType;
    }

    public byte[] getClientSecret() {
        return clientSecret;
    }

    public String getCustomClaimMapperClass() {
        return customClaimMapperClass;
    }

    public String getCustomAuthnContextMapperClass() {
        return customAuthenticationContextMapper;
    }

    public String getCustomAuthnMethodReferencesMapperClass() {
        return customAuthenticationMethodReferencesMapper;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OpenIdConnectTokenConfig instance:").append('\n');
        sb.append('\t').append("issuer: ").append(issuer).append('\n');
        sb.append('\t').append("audience: ").append(audience).append('\n');
        sb.append('\t').append("authorizedParty: ").append(authorizedParty).append('\n');
        sb.append('\t').append("signature algorithm: ").append(signatureAlgorithm).append('\n');
        sb.append('\t').append("claimMap: ").append(claimMap).append('\n');
        sb.append('\t').append("tokenLifetimeInSeconds: ").append(tokenLifetimeInSeconds).append('\n');
        sb.append('\t').append("Keystore File ").append(keystoreLocation).append('\n');
        sb.append('\t').append("Public key reference type ").append(publicKeyReferenceType).append('\n');
        sb.append('\t').append("Signature key alias").append(signatureKeyAlias).append('\n');
        sb.append('\t').append("Custom claim mapper class").append(customClaimMapperClass).append('\n');
        sb.append('\t').append("Custom authn context mapper class").append(customAuthenticationContextMapper).append('\n');
        sb.append('\t').append("Custom authn method references mapper class").append(customAuthenticationMethodReferencesMapper).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof OpenIdConnectTokenConfig) {
            OpenIdConnectTokenConfig otherConfig = (OpenIdConnectTokenConfig)other;
            return  issuer.equals(otherConfig.issuer) &&
                    signatureAlgorithm.equals(otherConfig.signatureAlgorithm) &&
                    Objects.equal(authorizedParty, otherConfig.authorizedParty) &&
                    audience.equals(otherConfig.audience) &&
                    (tokenLifetimeInSeconds == otherConfig.tokenLifetimeInSeconds) &&
                    claimMap.equals(otherConfig.claimMap) &&
                    Objects.equal(keystoreLocation, otherConfig.keystoreLocation) &&
                    Arrays.equals(keystorePassword, otherConfig.keystorePassword) &&
                    Objects.equal(signatureKeyAlias, otherConfig.signatureKeyAlias) &&
                    Objects.equal(customClaimMapperClass, otherConfig.customClaimMapperClass) &&
                    Objects.equal(customAuthenticationContextMapper, otherConfig.customAuthenticationContextMapper) &&
                    Objects.equal(customAuthenticationMethodReferencesMapper, otherConfig.customAuthenticationMethodReferencesMapper) &&
                    Objects.equal(publicKeyReferenceType, otherConfig.publicKeyReferenceType) &&
                    Arrays.equals(signatureKeyPassword, otherConfig.signatureKeyPassword) &&
                    Arrays.equals(clientSecret, otherConfig.clientSecret);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (claimMap + Long.toString(tokenLifetimeInSeconds) + issuer).hashCode();
    }

    /*
    Because toJson will be used to produce the map that will also be used to marshal to the SMS attribute map format, and
    because the SMS attribute map format represents all values as Set<String>, I need to represent all of the json values
    as strings as well.
     */
    public JsonValue toJson() {
        try {
            return json(object(
                    field(ISSUER, issuer),
                    field(PUBLIC_KEY_REFERENCE_TYPE, publicKeyReferenceType.name()),
                    field(TOKEN_LIFETIME, String.valueOf(tokenLifetimeInSeconds)),
                    field(AUTHORIZED_PARTY, authorizedParty),
                    field(AUDIENCE, audience),
                    field(SIGNATURE_ALGORITHM, signatureAlgorithm.name()),
                    field(CLAIM_MAP, claimMap),
                    field(CUSTOM_CLAIM_MAPPER_CLASS, customClaimMapperClass),
                    field(CUSTOM_AUTHN_CONTEXT_MAPPER_CLASS, customAuthenticationContextMapper),
                    field(CUSTOM_AUTHN_METHOD_REFERENCES_MAPPER_CLASS, customAuthenticationMethodReferencesMapper),
                    field(KEYSTORE_LOCATION, keystoreLocation),
                    field(KEYSTORE_PASSWORD,
                            keystorePassword != null ? new String(keystorePassword, AMSTSConstants.UTF_8_CHARSET_ID) : null),
                    field(CLIENT_SECRET,
                            clientSecret != null ? new String(clientSecret, AMSTSConstants.UTF_8_CHARSET_ID) : null),
                    field(SIGNATURE_KEY_ALIAS, signatureKeyAlias),
                    field(SIGNATURE_KEY_PASSWORD,
                            signatureKeyPassword != null ? new String(signatureKeyPassword, AMSTSConstants.UTF_8_CHARSET_ID) : null)));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Unsupported encoding when marshalling from String to to byte[]: " + e, e);
        }
    }

    public static OpenIdConnectTokenConfig fromJson(JsonValue json) throws IllegalStateException {
        try {
            return OpenIdConnectTokenConfig.builder()
                    //because we have to go to the SMS Map representation, where all values are Set<String>, I need to
                    // pull the value from Json as a string, and then parse out a Long.
                    .tokenLifetimeInSeconds(Long.valueOf(json.get(TOKEN_LIFETIME).asString()))
                    .issuer(json.get(ISSUER).asString())
                    .publicKeyReferenceType(json.get(PUBLIC_KEY_REFERENCE_TYPE).isString()
                            ? OpenIdConnectTokenPublicKeyReferenceType.valueOf(json.get(PUBLIC_KEY_REFERENCE_TYPE).asString()) : OpenIdConnectTokenPublicKeyReferenceType.NONE)
                    .claimMap(json.get(CLAIM_MAP).asMap(String.class))
                    .keystoreLocation(json.get(KEYSTORE_LOCATION).asString())
                    .keystorePassword(json.get(KEYSTORE_PASSWORD).isString()
                            ? json.get(KEYSTORE_PASSWORD).asString().getBytes(AMSTSConstants.UTF_8_CHARSET_ID) : null)
                    .signatureKeyPassword(json.get(SIGNATURE_KEY_PASSWORD).isString()
                            ? json.get(SIGNATURE_KEY_PASSWORD).asString().getBytes(AMSTSConstants.UTF_8_CHARSET_ID) : null)
                    .clientSecret(json.get(CLIENT_SECRET).isString()
                            ? json.get(CLIENT_SECRET).asString().getBytes(AMSTSConstants.UTF_8_CHARSET_ID) : null)
                    .signatureKeyAlias(json.get(SIGNATURE_KEY_ALIAS).isString() ? json.get(SIGNATURE_KEY_ALIAS).asString() : null)
                    .customClaimMapperClass(json.get(CUSTOM_CLAIM_MAPPER_CLASS).isString() ? json.get(CUSTOM_CLAIM_MAPPER_CLASS).asString() : null)
                    .customAuthenticationContextMapperClass(json.get(CUSTOM_AUTHN_CONTEXT_MAPPER_CLASS).isString()
                            ? json.get(CUSTOM_AUTHN_CONTEXT_MAPPER_CLASS).asString() : null)
                    .customAuthenticationMethodReferencesMapperClass(json.get(CUSTOM_AUTHN_METHOD_REFERENCES_MAPPER_CLASS).isString()
                            ? json.get(CUSTOM_AUTHN_METHOD_REFERENCES_MAPPER_CLASS).asString() : null)
                    .authorizedParty(json.get(AUTHORIZED_PARTY).isString() ? json.get(AUTHORIZED_PARTY).asString() : null)
                    .setAudience(json.get(AUDIENCE).asList(String.class))
                    .signatureAlgorithm(json.get(SIGNATURE_ALGORITHM).asString())
                    .build();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Unsupported encoding when marshalling from String to to byte[]: " + e, e);
        }
    }

    /*
    We need to marshal the OIDCTokenIdConfig instance to a Map<String, Object>. The JsonValue of toJson gets us there,
    except for the complex types for the audience and attribute map. These need to be marshaled into the appropriate complex
    type, and these entries included in the top-level map, replacing the existing complex entries.
     */
    public Map<String, Set<String>> marshalToAttributeMap() {
        Map<String, Object> preMap = toJson().asMap();
        Map<String, Set<String>> finalMap = MapMarshallUtils.toSmsMap(preMap);
        Object attributesObject = preMap.get(CLAIM_MAP);
        if (attributesObject instanceof Map) {
            finalMap.remove(CLAIM_MAP);
            Set<String> attributeValues = new LinkedHashSet<>();
            finalMap.put(CLAIM_MAP, attributeValues);
            for (Map.Entry<String, String> entry : ((Map<String, String>)attributesObject).entrySet()) {
                attributeValues.add(entry.getKey() + EQUALS + entry.getValue());
            }
        } else {
            throw new IllegalStateException("Type corresponding to " + CLAIM_MAP + " key unexpected. Type: "
                    + (attributesObject != null ? attributesObject.getClass().getName() :" null"));
        }

        Object audienceObject = preMap.get(AUDIENCE);
        if (audienceObject instanceof List) {
            finalMap.remove(AUDIENCE);
            finalMap.put(AUDIENCE, Sets.newHashSet((List)audienceObject));
        } else {
            throw new IllegalStateException("Type corresponding to " + AUDIENCE + " claim type unexpected: " +
                    (audienceObject != null ? audienceObject.getClass().getCanonicalName() : null));
        }
        return finalMap;
    }

    /*
    Here we have to modify the CLAIM_MAP and AUDIENCES entries to match the JsonValue format expected by
    fromJson, and then call the static fromJson. This method must marshal between the Json representation of a complex
    object, and the representation expected by the SMS
     */
    public static OpenIdConnectTokenConfig marshalFromAttributeMap(Map<String, Set<String>> smsAttributeMap) {
        Set<String> attributes = smsAttributeMap.get(CLAIM_MAP);
        /*
        The STSInstanceConfig may not have OpenIdConnectTokenConfig, if there are no defined token transformations that result
        in a OIDC Id Token. So if we have null attributes, this means that STSInstanceConfig.marshalFromAttributeMap
        was called. Note that we cannot check for isEmpty, as this will be the case if OpenIdConnectTokenConfig has been defined, but
        simply without any attributes.
         */
        if (attributes == null) {
            return null;
        }
        Map<String, Object> jsonAttributes = MapMarshallUtils.toJsonValueMap(smsAttributeMap);
        jsonAttributes.remove(CLAIM_MAP);
        Map<String, Object> jsonAttributeMap = new LinkedHashMap<>();
        for (String entry : attributes) {
            StringTokenizer st = new StringTokenizer(entry, EQUALS);
            jsonAttributeMap.put(st.nextToken(), st.nextToken());
        }
        jsonAttributes.put(CLAIM_MAP, new JsonValue(jsonAttributeMap));

        jsonAttributes.put(AUDIENCE, new JsonValue(smsAttributeMap.get(AUDIENCE)));

        return fromJson(new JsonValue(jsonAttributes));
    }
}
