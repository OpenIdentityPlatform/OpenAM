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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.rest;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.sun.identity.idsvcs.opensso.IdentityServicesImpl;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.forgerockrest.utils.MailServerLoader;
import org.forgerock.openam.services.RestSecurityProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.sm.config.ConsoleConfigHandler;

/**
 * Guice module for binding the Identity REST endpoints.
 *
 * @since 14.0.0
 */
public class CoreRestIdentityGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<UiRolePredicate> userUiRolePredicates = Multibinder.newSetBinder(binder(),
                UiRolePredicate.class);
        userUiRolePredicates.addBinding().to(SelfServiceUserUiRolePredicate.class);
        userUiRolePredicates.addBinding().to(GlobalAdminUiRolePredicate.class);
        userUiRolePredicates.addBinding().to(RealmAdminUiRolePredicate.class);
    }

    @Provides
    @Named("UsersResource")
    @Inject
    @Singleton
    public IdentityResourceV1 getUsersResourceV1(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider,
            ConsoleConfigHandler configHandler, Set<UiRolePredicate> uiRolePredicates) {
        return new IdentityResourceV1(IdentityResourceV1.USER_TYPE, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider, configHandler, uiRolePredicates);
    }

    @Provides
    @Named("UsersResource")
    @Inject
    @Singleton
    public IdentityResourceV2 getUsersResourceV2(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider,
            ConsoleConfigHandler configHandler, BaseURLProviderFactory baseURLProviderFactory, Set<UiRolePredicate> uiRolePredicates) {
        return new IdentityResourceV2(IdentityResourceV2.USER_TYPE, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider, configHandler, baseURLProviderFactory, uiRolePredicates);
    }

    @Provides
    @Named("UsersResource")
    @Inject
    @Singleton
    public IdentityResourceV3 getUsersResource(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider,
            ConsoleConfigHandler configHandler, BaseURLProviderFactory baseURLProviderFactory,
            @Named("PatchableUserAttributes") Set<String> patchableAttributes, Set<UiRolePredicate> uiRolePredicates) {
        return new IdentityResourceV3(IdentityResourceV2.USER_TYPE, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider, configHandler, baseURLProviderFactory, patchableAttributes, uiRolePredicates);
    }

    @Provides
    @Named("GroupsResource")
    @Inject
    @Singleton
    public IdentityResourceV1 getGroupsResourceV1(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider,
            ConsoleConfigHandler configHandler, Set<UiRolePredicate> uiRolePredicates) {
        return new IdentityResourceV1(IdentityResourceV1.GROUP_TYPE, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider, configHandler, uiRolePredicates);
    }

    @Provides
    @Named("GroupsResource")
    @Inject
    @Singleton
    public IdentityResourceV2 getGroupsResourceV2(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider,
            ConsoleConfigHandler configHandler, BaseURLProviderFactory baseURLProviderFactory, Set<UiRolePredicate> uiRolePredicates) {
        return new IdentityResourceV2(IdentityResourceV2.GROUP_TYPE, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider, configHandler, baseURLProviderFactory, uiRolePredicates);
    }

    @Provides
    @Named("GroupsResource")
    @Inject
    @Singleton
    public IdentityResourceV3 getGroupsResource(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider,
            ConsoleConfigHandler configHandler, BaseURLProviderFactory baseURLProviderFactory, Set<UiRolePredicate> uiRolePredicates) {
        return new IdentityResourceV3(IdentityResourceV2.GROUP_TYPE, mailServerLoader, identityServices,
                coreWrapper, restSecurityProvider, configHandler, baseURLProviderFactory, Collections.<String>emptySet(), uiRolePredicates);
    }

    @Provides
    @Named("AgentsResource")
    @Inject
    @Singleton
    public IdentityResourceV1 getAgentsResourceV1(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider,
            ConsoleConfigHandler configHandler, Set<UiRolePredicate> uiRolePredicates) {
        return new IdentityResourceV1(IdentityResourceV1.AGENT_TYPE, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider, configHandler, uiRolePredicates);
    }

    @Provides
    @Named("AgentsResource")
    @Inject
    @Singleton
    public IdentityResourceV2 getAgentsResourceV2(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider,
            ConsoleConfigHandler configHandler, BaseURLProviderFactory baseURLProviderFactory, Set<UiRolePredicate> uiRolePredicates) {
        return new IdentityResourceV2(IdentityResourceV2.AGENT_TYPE, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider, configHandler, baseURLProviderFactory, uiRolePredicates);
    }

    @Provides
    @Named("AgentsResource")
    @Inject
    @Singleton
    public IdentityResourceV3 getAgentsResource(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider,
            ConsoleConfigHandler configHandler, BaseURLProviderFactory baseURLProviderFactory, Set<UiRolePredicate> uiRolePredicates) {
        return new IdentityResourceV3(IdentityResourceV2.AGENT_TYPE, mailServerLoader, identityServices,
                coreWrapper, restSecurityProvider, configHandler, baseURLProviderFactory, Collections.<String>emptySet(), uiRolePredicates);
    }

    @Provides
    @Named("PatchableUserAttributes")
    public Set<String> getPatchableUserAttributes() {
        Set<String> patchableAttributes = new HashSet<>();
        patchableAttributes.add("userPassword");
        patchableAttributes.add("kbaInfo");
        return patchableAttributes;
    }
}