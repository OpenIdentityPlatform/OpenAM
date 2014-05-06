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
import org.forgerock.openam.sts.AuthTargetMapping;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.config.user.KeystoreConfig;
import org.forgerock.openam.sts.config.user.STSInstanceConfig;
import org.forgerock.util.Reject;

import java.util.*;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;


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
    private static final String DEPLOYMENT_CONFIG = "deploymentConfig";
    private static final String SUPPORTED_TOKEN_TRANSLATIONS = "supportedTokenTranslations";

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
                .amJsonRestBase(baseConfig.getJsonRestBase())
                .amDeploymentUrl(baseConfig.getAMDeploymentUrl())
                .amRestAuthNUriElement(baseConfig.getAMRestAuthNUriElement())
                .amRestLogoutUriElement(baseConfig.getAMRestLogoutUriElement())
                .amRestIdFromSessionUriElement(baseConfig.getAMRestIdFromSessionUriElement())
                .amSessionCookieName(baseConfig.getAMSessionCookieName())
                .keystoreConfig(baseConfig.getKeystoreConfig())
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
}
