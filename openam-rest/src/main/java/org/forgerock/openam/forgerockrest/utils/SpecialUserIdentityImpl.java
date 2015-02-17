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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.utils;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.shared.debug.Debug;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @see org.forgerock.openam.forgerockrest.utils.SpecialUserIdentity
 */
public class SpecialUserIdentityImpl implements SpecialUserIdentity {
    private final Debug debug;

    @Inject
    SpecialUserIdentityImpl(@Named("frRest") Debug debug) {
        this.debug = debug;
    }
    @Override
    public boolean isSpecialUser(SSOToken token) {
        try {
            final String userId = token.getPrincipal().getName();
            return AuthD.getAuth().isSpecialUser(userId);
        } catch (SSOException e) {
            if (debug.messageEnabled()) {
                debug.message("SpecialUserIdentityImpl :: Unable to authorize as Special user using SSO Token.", e);
            }
            return false;
        }
    }
}
