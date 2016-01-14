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
package org.forgerock.openam.sso.providers.stateless;

import java.security.Principal;

import javax.inject.Inject;

import org.forgerock.util.Reject;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.service.AuthD;

/**
 * Centralised logic to coordinate the decision around Stateless Sessions.
 *
 * In particular it has been decided that administrator users will not use Stateless Sessions.
 */
public class StatelessAdminRestriction {
    private final SuperUserDelegate delegate;
    private final StatelessSessionFactory factory;

    @Inject
    public StatelessAdminRestriction(SuperUserDelegate delegate, StatelessSessionFactory factory) {
        this.delegate = delegate;
        this.factory = factory;
    }

    /**
     * Indicates if a given SSOToken should be restricted when used in the
     * context of a Stateless Session.
     *
     * @param token Non null SSOToken.
     *
     * @return True if the the SSOToken should be restricted in the context of Stateless Sessions.
     *
     * @throws SessionException If there was an error whilst attempting to verify
     * if the SSOToken represented a Stateless Session then this exception will
     * be thrown.
     */
    public boolean isRestricted(SSOToken token) throws SessionException {
        Reject.ifNull(token);

        if (!factory.containsJwt(token.toString())) {
            throw new SessionException("Not a Stateless Session");
        }

        try {
            return isRestricted(token.getPrincipal().getName());
        } catch (SSOException e) {
            throw new SessionException(e);
        }
    }

    /**
     * Indicates if the given User DN should be restricted when used in the
     * context of Stateless Sessions.
     *
     * @see SSOToken#getPrincipal()
     * @see Principal#getName()
     *
     * @param userDN Non null user DN.
     * @return True if the userDN should be restricted.
     */
    public boolean isRestricted(String userDN) {
        Reject.ifNull(userDN);
        return delegate.isSuperUser(userDN) || delegate.isSpecialUser(userDN);
    }

    /**
     * Used to generate a singleton SuperUserDelegate whose AuthD instance is lazily loaded
     * and which is used to verify the administrative nature of users passed in.
     *
     * @return A new SuperUserDelegate, ready for use.
     */
    public static SuperUserDelegate createAuthDDelegate() {
        return new SuperUserDelegate() {
            private AuthD authD;

            // Deliberate lazy initialisation.
            private AuthD getAuthD() {
                if (authD == null) {
                    authD = AuthD.getAuth();
                }
                return authD;
            }

            @Override
            public boolean isSuperUser(String userDN) {
                return getAuthD().isSuperUser(userDN);
            }

            @Override
            public boolean isSpecialUser(String userDN) {
                return getAuthD().isSpecialUser(userDN);
            }
        };
    }

    /**
     * Responsible for answering the question of whether some token represents an administrator user.
     */
    public interface SuperUserDelegate {
        boolean isSuperUser(String userDN);
        boolean isSpecialUser(String userDN);
    }
}
