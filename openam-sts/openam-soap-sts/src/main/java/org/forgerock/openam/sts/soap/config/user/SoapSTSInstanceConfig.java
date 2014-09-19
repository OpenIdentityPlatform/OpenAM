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

package org.forgerock.openam.sts.soap.config.user;

import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.config.user.STSInstanceConfig;
import org.forgerock.util.Reject;

import java.util.*;

/**
 * Class which encapsulates all of the user-provided config information necessary to create an instance of the
 * STS.
 * It is an immutable object with getter methods to obtain all of the necessary information needed by the various
 * guice modules and providers to inject the object graph corresponding to a fully-configured STS instance.
 *
 */
public class SoapSTSInstanceConfig extends STSInstanceConfig {
    public abstract static class SOAPSTSInstanceConfigBuilderBase <T extends SOAPSTSInstanceConfigBuilderBase<T>> extends STSInstanceConfig.STSInstanceConfigBuilderBase<T>  {
        private Set<TokenType> issueTokenTypes;
        /*
        Token validation can simply validate the status (valid|invalid) of a token, or transform an
        input token into another token type. The validateTokenTypes contains the set of tokens for which
        status can be returned.
         */
        private Set<TokenType> validateTokenStatusTypes;
        /*
        The validateTokenTransformTypes contains the Mapping of allowable token transformations. In other words, the
         validate operation can be invoked with 'tokenType' of STSConstants.STATUS, in which case only the status is returned,
         or some other tokenType (e.g. SAML2, X509), in which case the input token type will be transformed into the output
         token type. This Map contains the valid set of token transformations supported by this STS instance.
         */
        private Map<TokenType, TokenType> validateTokenTransformTypes;
        private Set<TokenType> renewTokenTypes;

        private DeploymentConfig deploymentConfig;
        private SoapSTSKeystoreConfig keystoreConfig;

        private SOAPSTSInstanceConfigBuilderBase() {
            issueTokenTypes = new HashSet<TokenType>();
            validateTokenStatusTypes = new HashSet<TokenType>();
            validateTokenTransformTypes = new HashMap<TokenType, TokenType>();
            renewTokenTypes = new HashSet<TokenType>();
        }

        public T deploymentConfig(DeploymentConfig deploymentConfig) {
            this.deploymentConfig = deploymentConfig;
            return self();
        }

        public T addValidateTokenStatusType(TokenType type) {
            validateTokenStatusTypes.add(type);
            return self();
        }

        public T addValidateTokenTransformation(TokenType inputType, TokenType outputType) {
            validateTokenTransformTypes.put(inputType, outputType);
            return self();
        }

        public T addRenewTokenType(TokenType type) {
            renewTokenTypes.add(type);
            return self();
        }

        public T addIssueTokenType(TokenType type) {
            issueTokenTypes.add(type);
            return self();
        }

        public T soapSTSKeystoreConfig(SoapSTSKeystoreConfig keystoreConfig) {
            this.keystoreConfig = keystoreConfig;
            return self();
        }

        public SoapSTSInstanceConfig build() {
            return new SoapSTSInstanceConfig(this);
        }
    }

    public static class SOAPSTSInstanceConfigBuilder extends SOAPSTSInstanceConfigBuilderBase<SOAPSTSInstanceConfigBuilder> {
        public SOAPSTSInstanceConfigBuilder self() {
            return this;
        }
    }

    /*
    The three lists will be immutable views, and the mutable backing list will never be published, so references
    to the lists below can be returned without compromising immutability
     */
    private final Set<TokenType> issueTokenTypes; //just SAML
    private final Set<TokenType> validateTokenStatusTypes; //SAML and OPENAM
    private final Map<TokenType,TokenType> validateTokenTransformTypes;
    private final Set<TokenType> renewTokenTypes;

    private final DeploymentConfig deploymentConfig;
    private final SoapSTSKeystoreConfig keystoreConfig;

    private SoapSTSInstanceConfig(SOAPSTSInstanceConfigBuilderBase<?> builder) {
        super(builder);
        this.issueTokenTypes = Collections.unmodifiableSet(builder.issueTokenTypes);
        this.validateTokenStatusTypes = Collections.unmodifiableSet(builder.validateTokenStatusTypes);
        this.validateTokenTransformTypes = Collections.unmodifiableMap(builder.validateTokenTransformTypes);
        this.renewTokenTypes = Collections.unmodifiableSet(builder.renewTokenTypes);
        this.deploymentConfig = builder.deploymentConfig;
        this.keystoreConfig = builder.keystoreConfig;
        Reject.ifNull(keystoreConfig, "KeystoreConfig cannot be null");
        Reject.ifNull(issuerName, "Issuer name cannot be null");
        Reject.ifNull(deploymentConfig, "DeploymentConfig cannot be null");

    }

    public static SOAPSTSInstanceConfigBuilderBase<?> builder() {
        return new SOAPSTSInstanceConfigBuilder();
    }

    public DeploymentConfig getDeploymentConfig() {
        return deploymentConfig;
    }

    public Set<TokenType> getIssueTokenTypes() {
        return issueTokenTypes;
    }

    public Set<TokenType> getValidateTokenStatusTypes() {
        return validateTokenStatusTypes;
    }

    public Map<TokenType, TokenType> getValidateTokenTransformTypes() {
        return validateTokenTransformTypes;
    }

    public Set<TokenType> getRenewTokenTypes() {
        return renewTokenTypes;
    }

    public SoapSTSKeystoreConfig getKeystoreConfig() {
        return keystoreConfig;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("STSInstanceConfig instance:\n");
        sb.append('\t').append("KeyStoreConfig: ").append(keystoreConfig).append('\n');
        sb.append('\t').append("issuerName: ").append(issuerName).append('\n');
        sb.append('\t').append("issueTokenTypes: ").append(issueTokenTypes).append('\n');
        sb.append('\t').append("validateTokenStatusTypes: ").append(validateTokenStatusTypes).append('\n');
        sb.append('\t').append("validateTokenTransformTypes: ").append(validateTokenTransformTypes).append('\n');
        sb.append('\t').append("renewTokenTypes: ").append(renewTokenTypes).append('\n');
        sb.append('\t').append("deploymentConfig: ").append(deploymentConfig).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SoapSTSInstanceConfig) {
            SoapSTSInstanceConfig otherConfig = (SoapSTSInstanceConfig)other;
            return keystoreConfig.equals(otherConfig.getKeystoreConfig()) &&
                    issuerName.equals(otherConfig.getIssuerName()) &&
                    issueTokenTypes.equals(otherConfig.getIssueTokenTypes()) &&
                    validateTokenStatusTypes.equals(otherConfig.getValidateTokenStatusTypes()) &&
                    validateTokenTransformTypes.equals(otherConfig.getValidateTokenTransformTypes())  &&
                    renewTokenTypes.equals(otherConfig.getRenewTokenTypes()) &&
                    deploymentConfig.equals(otherConfig.getDeploymentConfig());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
