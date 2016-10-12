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
 */

package org.forgerock.openam.core.rest.session;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.iplanet.sso.SSOTokenManager;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import org.forgerock.openam.rest.authz.AdminOnlyAuthzModule;
import org.forgerock.openam.rest.authz.CrestPrivilegeAuthzModule;

/**
 * Guice module for binding the Session REST endpoints.
 *
 * @since 14.0.0
 */
public class CoreRestSessionGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides
    @Inject
    @Named("SessionResourceAuthzModule")
    public AnyOfAuthzModule getSessionResourceAuthzModule(SSOTokenManager ssoTokenManager,
            CrestPrivilegeAuthzModule privilegeAuthzModule,
            AdminOnlyAuthzModule adminOnlyAuthzModule) {
        List<CrestAuthorizationModule> authzList = initializeAuthzList(privilegeAuthzModule, adminOnlyAuthzModule);
        authzList.add(new SessionResourceAuthzModule(ssoTokenManager));
        return new AnyOfAuthzModule(authzList);
    }

    private List<CrestAuthorizationModule> initializeAuthzList(CrestPrivilegeAuthzModule privilegeAuthzModule,
            AdminOnlyAuthzModule adminOnlyAuthzModule) {
        List<CrestAuthorizationModule> authzList = new ArrayList<>(3);
        authzList.add(adminOnlyAuthzModule);
        authzList.add(privilegeAuthzModule);
        return authzList;
    }
}