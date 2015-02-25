/*
 * Copyright 2013 ForgeRock AS.
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
 */
package org.forgerock.agents.jetty.v7;

import com.sun.identity.agents.arch.IModuleAccess;
import com.sun.identity.agents.realm.AmRealmAuthenticationResult;
import com.sun.identity.agents.realm.AmRealmManager;
import com.sun.identity.agents.realm.IAmRealm;

import java.util.Collections;
import java.util.Set;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.security.Password;

/**
 *
 * @author Peter Major
 */
public class AgentCredential extends Credential {

    private String userName;
    private Set<String> roles;
    private static IAmRealm amRealmInstance = AmRealmManager.getAmRealmInstance();
    private static IModuleAccess moduleAccess = AmRealmManager.getModuleAccess();

    public AgentCredential(String userName) {
        this.userName = userName;
    }

    @Override
    public boolean check(Object credentials) {

        String transportString;
        if (credentials instanceof char[]) {
            transportString = new String((char[]) credentials);
        } else if (credentials instanceof String || credentials instanceof Password) {
            transportString = credentials.toString();
        } else {
            logMessage("Unknown credential type: " + credentials.getClass().getCanonicalName());
            return false;
        }

        AmRealmAuthenticationResult authResult = amRealmInstance.authenticate(userName, transportString);
        if (authResult.isValid()) {
            roles = authResult.getAttributes();
            return true;
        }

        return false;
    }

    /**
     * Returns the list of roles that the user has. This is actually retrieved during the authentication process.
     * @return The list of rolenames assigned to this user.
     */
    Set<String> getRoles() {

        if (roles == null) {
            return Collections.EMPTY_SET;
        }

        return roles;
    }

    private void logMessage(String message) {

        if (moduleAccess != null && moduleAccess.isLogMessageEnabled()) {
            moduleAccess.logMessage(message);
        }
    }
}
