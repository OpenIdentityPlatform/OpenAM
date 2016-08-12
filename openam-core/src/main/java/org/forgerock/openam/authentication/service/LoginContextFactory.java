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

package org.forgerock.openam.authentication.service;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginException;

import com.sun.identity.authentication.service.AMLoginContext;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.service.DSAMECallbackHandler;

/**
 * This Factory makes Login Context objects based on whether authentication is happening in pure JAAS modules and
 * whether there is a subject available.
 */
public class LoginContextFactory {

    private static LoginContextFactory instance = new LoginContextFactory();

    private LoginContextFactory() {}

    public static LoginContextFactory getInstance() {
        return instance;
    }

    /**
     * Creates an appropriate version of a login context object based on the provided parameters
     *
     * @param context the AMLoginContext that is requesting a login context
     * @param subject the subject that is attempting to log in - may be null
     * @param configName the config name for the JAAS object, must not be null.
     * @param isPureJAAS true if there are only pure JAAS
     * @param configuration the authentication configuration
     * @return a created login context
     * @throws LoginException if the login context could not be created
     */
    public LoginContext createLoginContext(final AMLoginContext context,
                                           final Subject subject,
                                           final String configName,
                                           final boolean isPureJAAS,
                                           final Configuration configuration) throws LoginException {
        if (configName == null) {
            throw new LoginException("Config name was null when creating LoginContext");
        }

        DSAMECallbackHandler dsameCallbackHandler = new DSAMECallbackHandler(context, isPureJAAS);

        if (isPureJAAS) {
            if (subject != null)  {
                return new LoginContextWrapper(
                        new javax.security.auth.login.LoginContext(configName, subject, dsameCallbackHandler));
            } else {
                return new LoginContextWrapper(
                        new javax.security.auth.login.LoginContext(configName, dsameCallbackHandler));
            }
        } else {
            AuthD.debug.message("Using non pure jaas mode.");
            AppConfigurationEntry[] entries = configuration.getAppConfigurationEntry(configName);
            if (subject != null)  {
                return new com.sun.identity.authentication.jaas.LoginContext(
                        entries, subject, dsameCallbackHandler);
            } else {
                return new com.sun.identity.authentication.jaas.LoginContext(
                        entries, dsameCallbackHandler);
            }
        }

    }
}
