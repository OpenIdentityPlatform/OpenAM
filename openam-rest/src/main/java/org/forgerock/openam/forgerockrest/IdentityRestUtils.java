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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.forgerockrest;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;

public final class IdentityRestUtils {

    private static final Debug debug = Debug.getInstance("frRest");

    private IdentityRestUtils() {
    }

    public static void changePassword(ServerContext serverContext, String realm, String username, String oldPassword,
            String newPassword) throws ResourceException {
        try {
            SSOToken token = serverContext.asContext(SSOTokenContext.class).getCallerSSOToken();
            AMIdentity userIdentity = new AMIdentity(token, username, IdType.USER, realm, null);
            userIdentity.changePassword(oldPassword, newPassword);
        } catch (SSOException ssoe) {
            debug.warning("IdentityRestUtils.changePassword() :: SSOException occurred while changing "
                    + "the password for user: " + username, ssoe);
            throw new PermanentException(401, "An error occurred while trying to change the password", ssoe);
        } catch (IdRepoException ire) {
            if (IdRepoBundle.ACCESS_DENIED.equals(ire.getErrorCode())) {
                throw new ForbiddenException("The user is not authorized to change the password");
            } else {
                debug.warning("IdentityRestUtils.changePassword() :: IdRepoException occurred while "
                        + "changing the password for user: " + username, ire);
                throw new InternalServerErrorException("An error occurred while trying to change the password", ire);
            }
        }
    }
}
