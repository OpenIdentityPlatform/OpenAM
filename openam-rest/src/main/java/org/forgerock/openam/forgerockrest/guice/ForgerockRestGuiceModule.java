/*
 * Copyright 2014-2016 ForgeRock AS.
 *
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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.forgerockrest.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationEvaluatorImpl;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.openam.entitlement.EntitlementRegistry;
import org.forgerock.openam.forgerockrest.utils.AgentIdentity;
import org.forgerock.openam.forgerockrest.utils.AgentIdentityImpl;
import org.forgerock.openam.forgerockrest.utils.RestLog;
import org.forgerock.openam.forgerockrest.utils.SpecialUserIdentity;
import org.forgerock.openam.forgerockrest.utils.SpecialUserIdentityImpl;
import org.forgerock.openam.rest.RestConstants;
import org.forgerock.openam.authz.PrivilegeDefinition;
import org.forgerock.openam.rest.router.DelegationEvaluatorProxy;
import org.forgerock.openam.utils.AMKeyProvider;
import org.forgerock.util.SignatureUtil;

/**
 * Guice Module for configuring bindings for the AuthenticationRestService classes.
 */
@GuiceModule
public class ForgerockRestGuiceModule extends AbstractModule {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure() {
        bind(AMKeyProvider.class).in(Singleton.class);
        bind(SignatureUtil.class).toProvider(new Provider<SignatureUtil>() {
            public SignatureUtil get() {
                return SignatureUtil.getInstance();
            }
        });

        bind(EntitlementRegistry.class).toInstance(EntitlementRegistry.load());

        bind(DelegationEvaluatorImpl.class).in(Singleton.class);
        bind(DelegationEvaluator.class).to(DelegationEvaluatorProxy.class).in(Singleton.class);

        bind(SpecialUserIdentity.class).to(SpecialUserIdentityImpl.class);
        bind(AgentIdentity.class).to(AgentIdentityImpl.class);
    }

    @Provides
    @Singleton
    public RestLog getRestLog() {
        return new RestLog();
    }

    @Provides
    @Named("CrestPrivilegeDefinitions")
    @Singleton
    public Map<String, PrivilegeDefinition> getCrestPrivilegeDefinitions() {
        final Map<String, PrivilegeDefinition> definitions = new HashMap<>();

        final PrivilegeDefinition evaluateDefinition = PrivilegeDefinition
                .getInstance("evaluate", PrivilegeDefinition.Action.READ);
        definitions.put("evaluate", evaluateDefinition);
        definitions.put("evaluateTree", evaluateDefinition);
        definitions.put("copy",
                PrivilegeDefinition.getInstance("modify", PrivilegeDefinition.Action.MODIFY));
        definitions.put("move",
                PrivilegeDefinition.getInstance("modify", PrivilegeDefinition.Action.MODIFY));
        definitions.put("schema",
                PrivilegeDefinition.getInstance("schema", PrivilegeDefinition.Action.READ));
        definitions.put("validate",
                PrivilegeDefinition.getInstance("validate", PrivilegeDefinition.Action.READ));
        definitions.put(RestConstants.TEMPLATE,
                PrivilegeDefinition.getInstance(RestConstants.TEMPLATE, PrivilegeDefinition.Action.READ));
        definitions.put(RestConstants.GET_ALL_TYPES,
                PrivilegeDefinition.getInstance(RestConstants.GET_ALL_TYPES, PrivilegeDefinition.Action.READ));
        definitions.put(RestConstants.GET_CREATABLE_TYPES,
                PrivilegeDefinition.getInstance(RestConstants.GET_CREATABLE_TYPES, PrivilegeDefinition.Action.READ));
        definitions.put(RestConstants.NEXT_DESCENDENTS,
                PrivilegeDefinition.getInstance(RestConstants.NEXT_DESCENDENTS, PrivilegeDefinition.Action.READ));
        definitions.put(RestConstants.GET_TYPE,
                PrivilegeDefinition.getInstance(RestConstants.GET_TYPE, PrivilegeDefinition.Action.READ));
        definitions.put("getPropertyNames",
                PrivilegeDefinition.getInstance("getPropertyNames", PrivilegeDefinition.Action.READ));
        definitions.put("getProperty",
                PrivilegeDefinition.getInstance("getProperty", PrivilegeDefinition.Action.READ));
        definitions.put("setProperty",
                PrivilegeDefinition.getInstance("setProperty", PrivilegeDefinition.Action.MODIFY));
        definitions.put("deleteProperty",
                PrivilegeDefinition.getInstance("deleteProperty", PrivilegeDefinition.Action.MODIFY));
        definitions.put("clone",
                PrivilegeDefinition.getInstance("clone", PrivilegeDefinition.Action.MODIFY));

        return definitions;
    }

    @Provides
    @Named("HttpPrivilegeDefinitions")
    @Singleton
    public Map<String, PrivilegeDefinition> getHttpPrivilegeDefinitions() {
        Map<String, PrivilegeDefinition> definitions = new HashMap<>();
        definitions.put("GET", PrivilegeDefinition.getInstance("GET", PrivilegeDefinition.Action.READ));
        definitions.put("HEAD", PrivilegeDefinition.getInstance("HEAD", PrivilegeDefinition.Action.READ));
        definitions.put("POST", PrivilegeDefinition.getInstance("POST", PrivilegeDefinition.Action.MODIFY));
        definitions.put("PUT", PrivilegeDefinition.getInstance("PUT", PrivilegeDefinition.Action.MODIFY));
        definitions.put("DELETE", PrivilegeDefinition.getInstance("DELETE", PrivilegeDefinition.Action.MODIFY));
        definitions.put("PATCH", PrivilegeDefinition.getInstance("PATCH", PrivilegeDefinition.Action.MODIFY));
        return definitions;
    }
}
