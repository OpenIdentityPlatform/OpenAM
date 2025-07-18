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

package org.forgerock.openam.authentication.service;

import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Singleton;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.authentication.service.AMAuthErrorCode;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

/**
 * This Class is responsible for looking up whether any used modules are non JAAS Modules.
 */
@Singleton
public class JAASModuleDetector {

    private final boolean enforceJAASThread = SystemProperties.getAsBoolean(Constants.ENFORCE_JAAS_THREAD);

    private final Debug debug = Debug.getInstance("amAuthUtils");

    private final ConcurrentHashMap<String, Boolean> categoryClassNames = new ConcurrentHashMap<>();

    /**
     * Returns whether the auth module is or the auth chain contains pure JAAS module(s).
     *
     * @return true for pure JAAS module; false for module(s) provided by IS only.
     */
    public boolean isPureJAASModulePresent(final String configName, final Configuration configuration)
            throws AuthLoginException {
        if (enforceJAASThread) {
            return true;
        }
        if (null == configuration) {
            return true;
        }
        final AppConfigurationEntry[] entries = configuration.getAppConfigurationEntry(configName);

        if (entries == null) {
            throw new AuthLoginException("amAuth", AMAuthErrorCode.AUTH_CONFIG_NOT_FOUND, null);
        }

        for (AppConfigurationEntry entry : entries) {
            String className = entry.getLoginModuleName();
            if (debug.messageEnabled()) {
                debug.message("config entry: " + className);
            }
            if (isPureJAASModule(className)) {
                return true;
            } else if (!isISModule(className)) {
                categoriseModuleClassFromClassname(className);
                if (isPureJAASModule(className)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void categoriseModuleClassFromClassname(String className) {
        try {
            boolean isAMLoginModuleChild = AMLoginModule.class.isAssignableFrom(
                    Class.forName(className, true, Thread.currentThread().getContextClassLoader()));
            if (isAMLoginModuleChild) {
                debug.message("%s is instance of AMLoginModule", className);
                addClassnameToModule(className, false);
            } else {
                debug.message("%s is a pure jaas module", className);
                addClassnameToModule(className, true);
            }
        } catch (Exception e) {
            debug.message("fail to instantiate class for {}", className);
            addClassnameToModule(className, true);
        }
    }

    private boolean isPureJAASModule(String classname) {
        Boolean isPureJAAS = categoryClassNames.get(classname);
        return isPureJAAS != null && isPureJAAS;
    }

    private boolean isISModule(String classname) {
        Boolean isPureJAAS = categoryClassNames.get(classname);
        return isPureJAAS != null && !isPureJAAS;
    }

    private void addClassnameToModule(String classname, boolean isPureJAAS) {
        categoryClassNames.putIfAbsent(classname, isPureJAAS); // Side effect: assumes class cannot be both JAAS and IS.
    }
}