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
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.util.Reject;

import java.io.UnsupportedEncodingException;

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
    protected static final String AM_SESSION_COOKIE_NAME = "amSessionCookieName";
    protected static final String KEYSTORE_CONFIG =  "keystoreConfig";
    protected static final String ISSUER_NAME = "issuerName";

    protected final String amJsonRestBase;
    protected final String amDeploymentUrl;
    protected final String amRestAuthNUriElement;
    protected final String amRestLogoutUriElement;
    protected final String amRestIdFromSessionUriElement;
    protected final String amSessionCookieName;
    protected final KeystoreConfig keystoreConfig;
    protected final String issuerName;

    public static abstract class STSInstanceConfigBuilderBase<T extends STSInstanceConfigBuilderBase<T>> {
        private String amJsonRestBase;
        private String amDeploymentUrl;
        private String amRestAuthNUriElement;
        private String amRestLogoutUriElement;
        private String amRestIdFromSessionUriElement;
        private String amSessionCookieName;
        private KeystoreConfig keystoreConfig;
        private String issuerName;


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
        amSessionCookieName = builder.amSessionCookieName;
        keystoreConfig = builder.keystoreConfig;
        issuerName = builder.issuerName;
        Reject.ifNull(keystoreConfig, "KeystoreConfig cannot be null");
        Reject.ifNull(issuerName, "Issuer name cannot be null");
        Reject.ifNull(amDeploymentUrl, "AM deployment url cannot be null");
        Reject.ifNull(amRestAuthNUriElement, "AM REST authN url element cannot be null");
        Reject.ifNull(amRestLogoutUriElement, "AM REST logout url element cannot be null");
        Reject.ifNull(amRestIdFromSessionUriElement, "AM REST id from Session url element cannot be null");
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
     * @return  The String corresponding to identifier for the name of the AM Session cookie (e.g. iPlanetDirectoryPro).
     * Necessary in call to obtain a user Id from session. (May well go away as the REST STS will be co-deployed with
     * OpenAM, and thus this information can be obtained from OpenAM config state)
     */
    public String getAMSessionCookieName() {
        return amSessionCookieName;
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
        sb.append('\t').append("amSessionCookieName: ").append(amSessionCookieName).append('\n');
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
                    amSessionCookieName.equals(otherConfig.getAMSessionCookieName()) &&
                    amRestLogoutUriElement.equals(otherConfig.getAMRestLogoutUriElement()) &&
                    amJsonRestBase.equals(otherConfig.getJsonRestBase());
        }
        return false;
    }

    public JsonValue toJson() {
        return json(object(field(AM_JSON_REST_BASE, amJsonRestBase), field(AM_DEPLOYMENT_URL, amDeploymentUrl),
                field(AM_REST_AUTHN_URI_ELEMENT, amRestAuthNUriElement), field(AM_REST_LOGOUT_URI_ELEMENT, amRestLogoutUriElement),
                field(AM_REST_ID_FROM_SESSION_URI_ELEMENT, amRestIdFromSessionUriElement), field(AM_SESSION_COOKIE_NAME, amSessionCookieName),
                field(KEYSTORE_CONFIG, keystoreConfig.toJson()), field(ISSUER_NAME, issuerName)));
    }

    public static STSInstanceConfig fromJson(JsonValue json) {
        return STSInstanceConfig.builder()
                .amJsonRestBase(json.get(AM_JSON_REST_BASE).asString())
                .amDeploymentUrl(json.get(AM_DEPLOYMENT_URL).asString())
                .amRestAuthNUriElement(json.get(AM_REST_AUTHN_URI_ELEMENT).asString())
                .amRestLogoutUriElement(json.get(AM_REST_LOGOUT_URI_ELEMENT).asString())
                .amRestIdFromSessionUriElement(json.get(AM_REST_ID_FROM_SESSION_URI_ELEMENT).asString())
                .amSessionCookieName(json.get(AM_SESSION_COOKIE_NAME).asString())
                .keystoreConfig(KeystoreConfig.fromJson(json.get(KEYSTORE_CONFIG)))
                .issuerName(json.get(ISSUER_NAME).asString())
                .build();
    }
}
