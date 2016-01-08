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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.uma;

import com.sun.identity.shared.Constants;
import javax.inject.Inject;
import javax.inject.Named;
import java.security.AccessController;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.openam.core.rest.UiRolePredicate;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.Config;
import org.forgerock.services.context.Context;

/**
 * Determines if the user can access the UMA pages within the UI.
 */
public class UmaUserUiRolePredicate implements UiRolePredicate {
    private final Config<SessionService> sessionService;
    private final Debug logger;

    @Inject
    public UmaUserUiRolePredicate(Config<SessionService> sessionService, @Named("frRest") Debug logger) {
        this.sessionService = sessionService;
        this.logger = logger;
    }
    @Override
    public String getRole() {
        return "ui-uma-user";
    }

    @Override
    public boolean apply(Context context) {
        try {
            String id = context.asContext(SSOTokenContext.class).getCallerSSOToken().getProperty(
                    Constants.UNIVERSAL_IDENTIFIER);
            if (sessionService.get().isSuperUser(id)) {
                return false;
            }
            String realm = context.asContext(RealmContext.class).getResolvedRealm();
            SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
            ServiceConfigManager serviceConfigManager = new ServiceConfigManager(token, UmaConstants.SERVICE_NAME,
                    UmaConstants.SERVICE_VERSION);
            return serviceConfigManager.getOrganizationConfig(realm, null).exists();
        } catch (Exception e) {
            logger.message("Could not access realm config", e);
            return false;
        }
    }
}
