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

package org.forgerock.openam.sts.config.user;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.Reject;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * Base class encapsulating STS configuration state common to both the REST and SOAP STS. A builder builds this
 * base class, and is invoked by the builders in the REST/SOAP sublcasses.
 * For an explanation of what's going on with the builders in this class and its subclasses,
 * see https://weblogs.java.net/blog/emcmanus/archive/2010/10/25/using-builder-pattern-subclasses
 *
 * Also attempted to marshal the RestSTSInstanceConfig to/from json with the jackson ObjectMapper. But I was adding
 * @JsonSerialize and @JsonDeserialize annotations, and because builder-based classes don't expose ctors which
 * take the complete field set, I would have to create @JsonCreator instances which would have to pull all of the
 * values out of a map anyway, which is 75% of the way towards a hand-rolled json marshalling implementation based on
 * json-fluent. So a hand-rolled implementation it is. See toJson and fromJson methods.
 */
public class STSInstanceConfig {
    /*
    Define the names of fields to aid in json marshalling.
     */
    protected static final String AM_JSON_REST_BASE = "amJsonRestBase";
    protected static final String AM_DEPLOYMENT_URL = "amDeploymentUrl";
    protected static final String AM_REST_AUTHN_URI_ELEMENT = "amRestAuthNUriElement";
    protected static final String AM_REST_LOGOUT_URI_ELEMENT = "amRestLogoutUriElement";
    protected static final String AM_REST_ID_FROM_SESSION_URI_ELEMENT = "amRestIdFromSessionUriElement";
    protected static final String AM_REST_TOKEN_GENERATION_SERVICE_URI_ELEMENT = "amRestTokenGenerationServiceUriElement";
    protected static final String AM_SESSION_COOKIE_NAME = "amSessionCookieName";
    protected static final String KEYSTORE_CONFIG =  "keystoreConfig";
    protected static final String ISSUER_NAME = "issuerName";
    protected static final String SAML2_CONFIG = "saml2config";

    protected final String amJsonRestBase;
    protected final String amDeploymentUrl;
    protected final String amRestAuthNUriElement;
    protected final String amRestLogoutUriElement;
    protected final String amRestIdFromSessionUriElement;
    protected final String amRestTokenGenerationServiceUriElement;
    protected final String amSessionCookieName;
    protected final KeystoreConfig keystoreConfig;
    protected final String issuerName;
    protected final SAML2Config saml2Config;

    public static abstract class STSInstanceConfigBuilderBase<T extends STSInstanceConfigBuilderBase<T>> {
        private String amJsonRestBase;
        private String amDeploymentUrl;
        private String amRestAuthNUriElement;
        private String amRestLogoutUriElement;
        private String amRestIdFromSessionUriElement;
        protected String amRestTokenGenerationServiceUriElement;
        private String amSessionCookieName;
        private KeystoreConfig keystoreConfig;
        private String issuerName;
        private SAML2Config saml2Config;


        protected abstract T self();

        public T amJsonRestBase(String jsonRestBase) {
            this.amJsonRestBase = jsonRestBase;
            return self();
        }

        public T amDeploymentUrl(String url) {
            this.amDeploymentUrl = url;
            return self();
        }

        public T amRestAuthNUriElement(String uri) {
            this.amRestAuthNUriElement = uri;
            return self();
        }

        public T amRestLogoutUriElement(String uri) {
            this.amRestLogoutUriElement = uri;
            return self();
        }

        public T amRestIdFromSessionUriElement(String uri) {
            this.amRestIdFromSessionUriElement = uri;
            return self();
        }

        public T amRestTokenGenerationServiceUriElement(String uri) {
            this.amRestTokenGenerationServiceUriElement = uri;
            return self();
        }

        public T amSessionCookieName(String amSessionCookieName) {
            this.amSessionCookieName = amSessionCookieName;
            return self();
        }

        public T keystoreConfig(KeystoreConfig keystoreConfig) {
            this.keystoreConfig = keystoreConfig;
            return self();
        }

        public T issuerName(String issuerName) {
            this.issuerName = issuerName;
            return self();
        }

        public T saml2Config(SAML2Config saml2Config) {
            this.saml2Config = saml2Config;
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

    protected STSInstanceConfig(STSInstanceConfigBuilderBase<?> builder) {
        amJsonRestBase = builder.amJsonRestBase;
        amDeploymentUrl = builder.amDeploymentUrl;
        amRestAuthNUriElement = builder.amRestAuthNUriElement;
        amRestLogoutUriElement = builder.amRestLogoutUriElement;
        amRestIdFromSessionUriElement = builder.amRestIdFromSessionUriElement;
        amRestTokenGenerationServiceUriElement = builder.amRestTokenGenerationServiceUriElement;
        amSessionCookieName = builder.amSessionCookieName;
        keystoreConfig = builder.keystoreConfig;
        issuerName = builder.issuerName;
        //can be null if STS does not issue SAML tokens - but if SAML2 tokens only output, this must be non-null. TODO:
        saml2Config = builder.saml2Config;
        Reject.ifNull(keystoreConfig, "KeystoreConfig cannot be null");
        Reject.ifNull(issuerName, "Issuer name cannot be null");
        Reject.ifNull(amDeploymentUrl, "AM deployment url cannot be null");
        Reject.ifNull(amRestAuthNUriElement, "AM REST authN url element cannot be null");
        Reject.ifNull(amRestLogoutUriElement, "AM REST logout url element cannot be null");
        Reject.ifNull(amRestIdFromSessionUriElement, "AM REST id from Session url element cannot be null");
        Reject.ifNull(amRestTokenGenerationServiceUriElement, "AM REST Token Generation Service url element cannot be null");
        Reject.ifNull(amSessionCookieName, "AM session cookie name cannot be null");
        Reject.ifNull(amJsonRestBase, "AM json rest base cannot be null");
    }

    public String getJsonRestBase() {
        return amJsonRestBase;
    }
    /**
     * @return  The crypto-context necessary to decrypt/trust/sign/encrypt the tokens validated and generated by this
     *          STS instance.
     */
    public KeystoreConfig getKeystoreConfig() {
        return keystoreConfig;
    }

    /**
     * @return  The issuerName in tokens (e.g. SAML2) generated by the STS.
     */
    public String getIssuerName() {
        return issuerName;
    }

    /**
     * @return  The String corresponding to the OpenAM deployment url. (May go away, as the REST-STS will likely be
     *          co-deployed with OpenAM).
     */
    public String getAMDeploymentUrl() {
        return amDeploymentUrl;
    }

    /**
     * @return  The String corresponding to the path to the OpenAM rest authN context (relative to the root AM deployment) -
     *          e.g. /json/authenticate
     */
    public String getAMRestAuthNUriElement() {
        return amRestAuthNUriElement;
    }

    /**
     * @return  The String corresponding to the path to the OpenAM rest authN logout context (relative to the root AM deployment) -
     *          e.g. /json/sessions/?_action=logout
     */
    public String getAMRestLogoutUriElement() {
        return amRestLogoutUriElement;
    }

    /**
     * @return  The String corresponding to the path to the OpenAM rest context which allows a user Id to be obtained
     *          from a session (relative to the root AM deployment) - e.g. /json/users/?_action=idFromSession
     */
    public String getAMRestIdFromSessionUriElement() {
        return amRestIdFromSessionUriElement;
    }

    /**
     * @return  The String corresponding to the path to the OpenAM rest context where the TokenGenerationService is exposed -
     * e.g. /sts_tokengen/issue?_action=issue
     */
    public String getAmRestTokenGenerationServiceUriElement() {
        return amRestTokenGenerationServiceUriElement;
    }

    /**
     * @return  The String corresponding to identifier for the name of the AM Session cookie (e.g. iPlanetDirectoryPro).
     * Necessary in call to obtain a user Id from session. (May well go away as the REST STS will be co-deployed with
     * OpenAM, and thus this information can be obtained from OpenAM config state)
     */
    public String getAMSessionCookieName() {
        return amSessionCookieName;
    }

    /**
     *
     * @return The SAML2Config object which specifies the state necessary for STS-instance-specific SAML2 assertions to
     * be generated. This state is used by the token generation service.
     */
    public SAML2Config getSaml2Config() {
        return saml2Config;
    }

    public static STSInstanceConfigBuilderBase<?> builder() {
        return new STSInstanceConfigBuilder();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("STSInstanceConfig instance:\n");
        sb.append('\t').append("KeyStoreConfig: ").append(keystoreConfig).append('\n');
        sb.append('\t').append("issuerName: ").append(issuerName).append('\n');
        sb.append('\t').append("amDeploymentUrl: ").append(amDeploymentUrl).append('\n');
        sb.append('\t').append("amRestAuthNUriElement: ").append(amRestAuthNUriElement).append('\n');
        sb.append('\t').append("amRestLogoutUriElement: ").append(amRestLogoutUriElement).append('\n');
        sb.append('\t').append("amRestAMTokenValidationUriElement: ").append(amRestIdFromSessionUriElement).append('\n');
        sb.append('\t').append("amRestTokenGenerationServiceUriElement: ").append(amRestTokenGenerationServiceUriElement).append('\n');
        sb.append('\t').append("amSessionCookieName: ").append(amSessionCookieName).append('\n');
        sb.append('\t').append("saml2Config: ").append(saml2Config).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof STSInstanceConfig) {
            STSInstanceConfig otherConfig = (STSInstanceConfig)other;
            return keystoreConfig.equals(otherConfig.getKeystoreConfig()) &&
                    issuerName.equals(otherConfig.getIssuerName()) &&
                    amDeploymentUrl.equals(otherConfig.getAMDeploymentUrl()) &&
                    amRestAuthNUriElement.equals(otherConfig.getAMRestAuthNUriElement()) &&
                    amRestIdFromSessionUriElement.equals(otherConfig.getAMRestIdFromSessionUriElement()) &&
                    amRestTokenGenerationServiceUriElement.equals(otherConfig.getAmRestTokenGenerationServiceUriElement()) &&
                    amSessionCookieName.equals(otherConfig.getAMSessionCookieName()) &&
                    amRestLogoutUriElement.equals(otherConfig.getAMRestLogoutUriElement()) &&
                    amJsonRestBase.equals(otherConfig.getJsonRestBase()) &&
                    ((saml2Config != null ? saml2Config.equals(otherConfig.getSaml2Config()) : (otherConfig.getSaml2Config() == null)));
        }
        return false;
    }

    public JsonValue toJson() {
        JsonValue jsonValue =  json(object(field(AM_JSON_REST_BASE, amJsonRestBase), field(AM_DEPLOYMENT_URL, amDeploymentUrl),
                field(AM_REST_AUTHN_URI_ELEMENT, amRestAuthNUriElement), field(AM_REST_LOGOUT_URI_ELEMENT, amRestLogoutUriElement),
                field(AM_REST_ID_FROM_SESSION_URI_ELEMENT, amRestIdFromSessionUriElement), field(AM_SESSION_COOKIE_NAME, amSessionCookieName),
                field(KEYSTORE_CONFIG, keystoreConfig.toJson()), field(ISSUER_NAME, issuerName),
                field(AM_REST_TOKEN_GENERATION_SERVICE_URI_ELEMENT, amRestTokenGenerationServiceUriElement)));
        if (saml2Config == null) {
            return jsonValue;
        } else {
            jsonValue.add(SAML2_CONFIG, saml2Config.toJson());
            return jsonValue;
        }
    }

    public static STSInstanceConfig fromJson(JsonValue json) {
        STSInstanceConfigBuilderBase builder =  STSInstanceConfig.builder()
                .amJsonRestBase(json.get(AM_JSON_REST_BASE).asString())
                .amDeploymentUrl(json.get(AM_DEPLOYMENT_URL).asString())
                .amRestAuthNUriElement(json.get(AM_REST_AUTHN_URI_ELEMENT).asString())
                .amRestLogoutUriElement(json.get(AM_REST_LOGOUT_URI_ELEMENT).asString())
                .amRestIdFromSessionUriElement(json.get(AM_REST_ID_FROM_SESSION_URI_ELEMENT).asString())
                .amRestTokenGenerationServiceUriElement(json.get(AM_REST_TOKEN_GENERATION_SERVICE_URI_ELEMENT).asString())
                .amSessionCookieName(json.get(AM_SESSION_COOKIE_NAME).asString())
                .keystoreConfig(KeystoreConfig.fromJson(json.get(KEYSTORE_CONFIG)))
                .issuerName(json.get(ISSUER_NAME).asString());
        final JsonValue samlConfig = json.get(SAML2_CONFIG);
        if (samlConfig.isNull()) {
            return builder.build();
        } else {
            return builder.saml2Config(SAML2Config.fromJson(samlConfig)).build();
        }
    }
}
