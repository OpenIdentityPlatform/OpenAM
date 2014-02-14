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

import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.config.user.KeystoreConfig;
import org.forgerock.openam.sts.config.user.STSInstanceConfig;
import org.forgerock.util.Reject;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * Class which encapsulates all of the user-provided config information necessary to create an instance of the
 * STS.
 * It is an immutable object with getter methods to obtain all of the necessary information needed by the various
 * guice modules and providers to inject the object graph corresponding to a fully-configured STS instance.
 *
 * For an explanation of what's going on with the builders in this class,
 * see https://weblogs.java.net/blog/emcmanus/archive/2010/10/25/using-builder-pattern-subclasses
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
    private RestSTSInstanceConfig(RestSTSInstanceConfigBuilderBase<?> builder) {
        super(builder);
        this.supportedTokenTranslations = Collections.unmodifiableSet(builder.supportedTokenTranslations);
        this.deploymentConfig = builder.deploymentConfig;
        Reject.ifNull(keystoreConfig, "KeystoreConfig cannot be null");
        Reject.ifNull(issuerName, "Issuer name cannot be null");
        Reject.ifNull(supportedTokenTranslations, "Supported token translations cannot be null");
        Reject.ifNull(deploymentConfig, "DeploymentConfig cannot be null");
        Reject.ifNull(amDeploymentUrl, "AM deployment url cannot be null");
        Reject.ifNull(amRestAuthNUriElement, "AM REST authN url element cannot be null");
        Reject.ifNull(amRestLogoutUriElement, "AM REST logout url element cannot be null");
        Reject.ifNull(amRestIdFromSessionUriElement, "AM REST id from Session url element cannot be null");
        Reject.ifNull(amSessionCookieName, "AM session cookie name cannot be null");
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
        StringBuilder sb = new StringBuilder("STSInstanceConfig instance:\n");
        sb.append('\t').append("KeyStoreConfig: ").append(keystoreConfig).append('\n');
        sb.append('\t').append("issuerName: ").append(issuerName).append('\n');
        sb.append('\t').append("validateTokenTransformTypes: ").append(supportedTokenTranslations).append('\n');
        sb.append('\t').append("deploymentConfig: ").append(deploymentConfig).append('\n');
        sb.append('\t').append("amDeploymentUrl: ").append(amDeploymentUrl).append('\n');
        sb.append('\t').append("amRestAuthNUriElement: ").append(amRestAuthNUriElement).append('\n');
        sb.append('\t').append("amRestLogoutUriElement: ").append(amRestLogoutUriElement).append('\n');
        sb.append('\t').append("amRestAMTokenValidationUriElement: ").append(amRestIdFromSessionUriElement).append('\n');
        sb.append('\t').append("amSessionCookieName: ").append(amSessionCookieName).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof RestSTSInstanceConfig) {
            RestSTSInstanceConfig otherConfig = (RestSTSInstanceConfig)other;
            return keystoreConfig.equals(otherConfig.getKeystoreConfig()) &&
                    issuerName.equals(otherConfig.getIssuerName()) &&
                    supportedTokenTranslations.equals(otherConfig.getSupportedTokenTranslations())  &&
                    deploymentConfig.equals(otherConfig.getDeploymentConfig()) &&
                    amDeploymentUrl.equals(otherConfig.getAMDeploymentUrl()) &&
                    amRestAuthNUriElement.equals(otherConfig.getAMRestAuthNUriElement()) &&
                    amRestIdFromSessionUriElement.equals(otherConfig.getAMRestIdFromSessionUriElement()) &&
                    amSessionCookieName.equals(otherConfig.getAMSessionCookieName()) &&
                    amRestLogoutUriElement.equals(otherConfig.getAMRestLogoutUriElement());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
