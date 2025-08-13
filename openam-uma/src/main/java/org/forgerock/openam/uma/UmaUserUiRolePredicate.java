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
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.uma;

import com.sun.identity.shared.Constants;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.security.AccessController;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
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
    private volatile ServiceConfigManager serviceConfigManager;
    private static Map<String, ServiceConfig> configMap = new ConcurrentHashMap<String, ServiceConfig>();

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
            String realm = context.asContext(RealmContext.class).getRealm().asPath();
            return checkIfUmaConfigExists(realm);
        } catch (SMSException | SSOException e) {
            serviceConfigManager = null;
            logger.message("Could not access realm config", e);
            return false;
        }
    }
    
    private boolean checkIfUmaConfigExists(String realm) throws SMSException, SSOException {
        ServiceConfig orgConfig = configMap.get(realm);
        if (orgConfig != null && orgConfig.isValid()) {
            return orgConfig.exists();
        } else {
            if (serviceConfigManager == null) {
                synchronized(this) {
                    SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
                    if (serviceConfigManager == null) {
                        serviceConfigManager = new ServiceConfigManager(token, UmaConstants.SERVICE_NAME, 
                                UmaConstants.SERVICE_VERSION);
                    }
                }
            }
            orgConfig = serviceConfigManager.getOrganizationConfig(realm, null);
            if (orgConfig != null && orgConfig.isValid()) {
                configMap.put(realm, orgConfig);
                return orgConfig.exists();
            }
        }
        return false;
    }
}
