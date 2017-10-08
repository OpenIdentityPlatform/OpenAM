/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file at legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package com.forgerock.openam.functionaltest.sts.frmwk.rest;

import org.forgerock.openam.sts.config.user.CustomTokenOperation;
import org.forgerock.openam.sts.rest.config.user.TokenTransformConfig;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Encapsulates specification of custom token operations, for use in publishing rest-sts instances.
 */
public class CustomTokenOperationContext {
    public static class CustomTokenOperationContextBuilder {
        private final HashSet<CustomTokenOperation> customValidators;
        private final HashSet<CustomTokenOperation> customProviders;
        private final HashSet<TokenTransformConfig> customTransforms;

        private CustomTokenOperationContextBuilder() {
            customValidators = new HashSet<>();
            customProviders = new HashSet<>();
            customTransforms = new HashSet<>();
        }

        public CustomTokenOperationContextBuilder addCustomTokenValidator(String customTokenId, String restTokenValidatorImplClass) {
            customValidators.add(new CustomTokenOperation(customTokenId, restTokenValidatorImplClass));
            return this;
        }

        public CustomTokenOperationContextBuilder addCustomTokenProvider(String customTokenId, String restTokenValidatorImplClass) {
            customProviders.add(new CustomTokenOperation(customTokenId, restTokenValidatorImplClass));
            return this;
        }

        public CustomTokenOperationContextBuilder addCustomTokenTransformation(String inputTokenType, String outputTokenType,
                                                                               boolean invalidateInterimOpenAMSession) {
            customTransforms.add(new TokenTransformConfig(inputTokenType, outputTokenType, invalidateInterimOpenAMSession));
            return this;
        }

        public CustomTokenOperationContext build() {
            return new CustomTokenOperationContext(this);
        }
    }

    private final Set<CustomTokenOperation> customValidators;
    private final Set<CustomTokenOperation> customProviders;
    private final Set<TokenTransformConfig> customTransforms;

    private CustomTokenOperationContext(CustomTokenOperationContextBuilder builder) {
        this.customValidators = Collections.unmodifiableSet(builder.customValidators);
        this.customProviders = Collections.unmodifiableSet(builder.customProviders);
        this.customTransforms = Collections.unmodifiableSet(builder.customTransforms);
    }

    public static CustomTokenOperationContextBuilder builder() {
        return new CustomTokenOperationContextBuilder();
    }

    public Set<CustomTokenOperation> getCustomValidators() { return customValidators; }

    public Set<CustomTokenOperation> getCustomProviders() { return customProviders; }

    public Set<TokenTransformConfig> getCustomTransforms() { return customTransforms; }
}