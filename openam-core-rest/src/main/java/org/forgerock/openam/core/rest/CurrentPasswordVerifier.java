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
 * Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.openam.core.rest;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.json.resource.BadRequestException;
import org.forgerock.openam.rest.RestUtils;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.debug.Debug;

/**
 * Confirms that a password is the current password of an identity, as the identity endpoints require before a
 * protected attribute is changed.
 * <p>
 * This asks the identity repositories directly and never reaches the authentication service, so the two things
 * that service does around a password comparison have to be done here.
 * <p>
 * The identity type of the endpoint is passed to the repositories rather than leaving the type open. An untyped
 * call offers the credentials to every repository of the realm in turn, so an agent whose name and password
 * happen to match would confirm the change of a user of the same name.
 * <p>
 * The status of the identity is checked as well, because a repository compares the password and nothing else. It
 * is {@code LoginState} that refuses an identity which is switched off, and nothing in this path consults it, so
 * without the check the holder of a deactivated account could still confirm its own password.
 */
class CurrentPasswordVerifier {

    private static final Debug debug = Debug.getInstance("frRest");

    /** The identity type the endpoint serves, as {@code IdentityResourceV2} names it. */
    private final String objectType;

    CurrentPasswordVerifier(String objectType) {
        this.objectType = objectType;
    }

    /**
     * Whether the password is the current password of an identity that is allowed to use it.
     *
     * @param username the identity that has to confirm the password.
     * @param password the password offered as that identity's current one.
     * @param realm the realm the identity lives in.
     * @return {@code true} where the password belongs to the identity and the identity is active.
     * @throws BadRequestException where no user name or no password was given.
     */
    boolean isCurrentPasswordOf(String username, char[] password, String realm) throws BadRequestException {
        if (username == null || password == null) {
            throw new BadRequestException("Invalid Username or Password");
        }
        final IdType identityType = identityType();
        try {
            Callback[] callbacks = new Callback[2];
            NameCallback nc = new NameCallback("dummy");
            nc.setName(username);
            callbacks[0] = nc;
            PasswordCallback pc = new PasswordCallback("dummy", false);
            pc.setPassword(password);
            callbacks[1] = pc;

            if (!authenticateIdentity(realm, identityType, callbacks)) {
                return false;
            }
            if (!isIdentityActive(realm, identityType, username)) {
                /*
                 * Not "is inactive": DJLDAPv3Repo.isActive() answers false for an identity it could
                 * not read the status of, without logging why, so this is as much as can be said.
                 */
                debug.error("CurrentPasswordVerifier :: identity={} in realm={} was not confirmed active",
                        username, realm);
                return false;
            }
            return true;
        } catch (AuthLoginException ex) {
            /*
             * The repositories refused the credentials, which is what a wrong password looks like
             * here: DJLDAPv3Repo.authenticate() throws InvalidPasswordException and IdServicesImpl
             * rethrows it. Expected, and driven by whoever is calling the endpoint, so it is not an
             * error of this server's.
             */
            debug.message("CurrentPasswordVerifier :: identity={} in realm={} was refused its password",
                    username, realm, ex);
            return false;
        } catch (Exception ex) {
            debug.error("CurrentPasswordVerifier :: could not verify the password of identity={} in realm={}",
                    username, realm, ex);
            return false;
        }
    }

    /**
     * Whether the identity repositories of the realm accept the credentials for that identity type. Overridable
     * so that {@link #isCurrentPasswordOf} can be tested without a repository.
     */
    protected boolean authenticateIdentity(String realm, IdType identityType, Callback[] credentials)
            throws IdRepoException, AuthLoginException {
        return identityRepository(realm).authenticate(identityType, credentials);
    }

    /**
     * The identity repository of the realm. Broken out from {@link #authenticateIdentity} so a test can pin the
     * typed {@code authenticate} call by overriding this seam alone and running the real
     * {@link #authenticateIdentity} against a mock repository.
     */
    protected AMIdentityRepository identityRepository(String realm) {
        return new AMIdentityRepository(realm, RestUtils.getToken());
    }

    /**
     * Whether the identity is active. Overridable so that {@link #isCurrentPasswordOf} can be tested without a
     * repository.
     */
    protected boolean isIdentityActive(String realm, IdType identityType, String name)
            throws IdRepoException, SSOException {
        return identity(realm, identityType, name).isActive();
    }

    /**
     * The identity whose status {@link #isIdentityActive} reads. Broken out so a test can pin the type the status
     * is read with by overriding this seam alone and running the real {@link #isIdentityActive}.
     */
    protected AMIdentity identity(String realm, IdType identityType, String name) {
        return new AMIdentity(RestUtils.getToken(), name, identityType, realm, null);
    }

    /**
     * The identity type of the endpoint. Mapped here rather than through {@code IdUtils}, which reads the
     * supported types out of the IdRepo service configuration. Package visible so a test can pin the type the
     * endpoint wires the verifier up with.
     */
    IdType identityType() throws BadRequestException {
        if (IdentityResourceV2.USER_TYPE.equals(objectType)) {
            return IdType.USER;
        }
        if (IdentityResourceV2.AGENT_TYPE.equals(objectType)) {
            return IdType.AGENT;
        }
        /*
         * GROUP is deliberately absent. A group holds no password of its own: DJLDAPv3Repo.authenticate()
         * looks up the DN of a *user* by the name it is given whatever type it is asked for, and its
         * isActive() throws IdRepoUnsupportedOpException for every type but USER. So a confirmation on
         * /json/groups could only ever have succeeded by binding a different identity that shares the
         * group's name. The endpoint is refused here, rather than through an exception swallowed below.
         */
        throw new BadRequestException("Cannot verify the password of an identity of type " + objectType);
    }
}
