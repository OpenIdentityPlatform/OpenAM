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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.sm;

import com.iplanet.dpro.session.service.SessionConstants;
import com.iplanet.services.ldap.LDAPUser;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.sm.exceptions.ConnectionCredentialsNotFound;
import org.forgerock.openam.sm.exceptions.ServerConfigurationNotFound;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.IllegalStateException;
import java.util.Arrays;

/**
 * Resolves which ServerConfiguration to retrieve from the SMS configuration.
 */
public class SMSConfigurationFactory {
    private final Debug debug;
    private final ServerConfigurationFactory parser;

    @Inject
    public SMSConfigurationFactory(ServerConfigurationFactory parser,
                                   @Named(SessionConstants.SESSION_DEBUG) Debug debug) {
        this.parser = parser;
        this.debug = debug;
    }

    /**
     * Try the various known configurations for appropriate connection credentials.
     *
     * @return Non null ServerGroupConfiguration.
     *
     * @throws IllegalStateException If there were no configuration details defined.
     */
    public synchronized ServerGroupConfiguration getSMSConfiguration() {
        LDAPUser.Type type = LDAPUser.Type.AUTH_ADMIN;

        for (String group : Arrays.asList("sms", "default")) {
            /**
             * Fetch configuration for the named Group. If the group fails to
             * retrieve valid credentials, this is not necessarily an error
             * as there are a number of groups to try.
             */
            try {
                return parser.getServerConfiguration(group, type);
            } catch (ServerConfigurationNotFound e) {
                debug.message("No Server Configuration found for " + group);
            } catch (ConnectionCredentialsNotFound e) {
                debug.message("Server Configuration missing " + type.toString() + " credentials for Group " + group);
            }
        }
        throw new IllegalStateException("No server configurations found");
    }
}
