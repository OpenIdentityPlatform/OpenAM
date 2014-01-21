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

package org.forgerock.openam.rest.router;

import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;

import javax.inject.Singleton;
import java.security.AccessController;

/**
 * Validates that realms are configured.
 *
 * @since 12.0.0
 */
@Singleton
public class RestRealmValidator {

    /**
     * Determines whether the given token is a valid configured realm.
     *
     * @param token The token to check.
     * @return <code>true</code> if the token is a realm.
     */
    public boolean isRealm(String token) {
        try {
            new OrganizationConfigManager(getSSOToken(), token);
        } catch (SMSException e) {
            // Cannot find realm
            return false;
        }
        return true;
    }

    /**
     * Gets the Admin SSO Token.
     *
     * @return The Admin SSO Token.
     */
    private SSOToken getSSOToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }
}
