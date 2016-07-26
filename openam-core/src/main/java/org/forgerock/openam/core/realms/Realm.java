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

package org.forgerock.openam.core.realms;

import java.security.AccessController;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.DNMapper;

/**
 * Models a valid realm within OpenAM.
 *
 * <p>On creation of a {@code Realm} instance the {@literal realm} {@code String} will be validated
 * and used to look up the realm.</p>
 *
 * <p>Note: Realms are <strong>case-insensitive</strong> and therefore are compared ignoring case.
 * </p>
 *
 * @since 14.0.0
 */
public final class Realm {

    /**
     * The root realm.
     */
    public static final Realm ROOT = new Realm(DNMapper.orgNameToDN("/"));

    /**
     * Uses the {@literal realm} {@code String} to lookup the Realm.
     *
     * @param realm The realm to lookup, in either path or DN format.
     * @return A {@code Realm} instance of the {@literal realm}.
     * @throws RealmLookupException If the provided {@literal realm} is not valid or failed to be
     * resolved.
     */
    public static Realm of(String realm) throws RealmLookupException {
        try {
            return new Realm(IdUtils.getOrganization(getAdminToken(), realm));
        } catch (IdRepoException | SSOException e) {
            throw new RealmLookupException("Failed to resolve realm: " + realm, e);
        }
    }

    private static SSOToken getAdminToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    private final String path;
    private final String realmDN;

    private Realm(String realmDN) {
        this.path = DNMapper.orgNameToRealmName(realmDN);
        this.realmDN = realmDN;
    }

    /**
     * Returns the realm in path format. e.g. '/realmA/realmB'.
     *
     * @return The realm path.
     */
    public String asPath() {
        return path;
    }

    /**
     * Returns the realm in DN format. e.g. 'o=realmA,o=realmB,ou=services,dc=example,dc=com'.
     *
     * @return The realm DN.
     */
    public String asDN() {
        return realmDN;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Realm realm = (Realm) o;
        return path.equalsIgnoreCase(realm.path);
    }

    @Override
    public int hashCode() {
        return path.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return path.toLowerCase();
    }
}
