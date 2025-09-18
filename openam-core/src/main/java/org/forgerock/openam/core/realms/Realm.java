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
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.realms;

import jakarta.inject.Inject;

import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.util.Reject;

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
public class Realm {

    @Inject
    private static RealmLookup realmLookup;
    @Inject
    private static CoreWrapper coreWrapper;

    private static Realm root;

    /**
     * Gets the root realm.
     *
     * @return The root realm.
     */
    public static Realm root() {
        // Ok if this creates more than one instance. Intended as best effort.
        if (root == null) {
            root = new Realm(convertRealmPathToDN("/"));
        }
        return root;
    }

    /**
     * Returns realm in DN format for the provided realm in path format.
     *
     * @param realmPath realm in path format.
     * @return The realm in DN format.
     */
    static String convertRealmPathToDN(String realmPath) {
        return coreWrapper.convertRealmNameToOrgName(realmPath);
    }

    /**
     * Returns realm in path format for the provided realm in DN format.
     *
     * @param realmDN realm in DN format.
     * @return The realm in path format.
     */
    static String convertRealmDNToPath(String realmDN) {
        return coreWrapper.convertOrgNameToRealmName(realmDN);
    }

    /**
     * Uses the {@literal realm} {@code String} to lookup the Realm.
     *
     * @param realm The realm to lookup, in either path or DN format.
     * @return A {@code Realm} instance of the {@literal realm}.
     * @throws RealmLookupException If the provided {@literal realm} is not valid or failed to be
     * resolved.
     */
    public static Realm of(String realm) throws RealmLookupException {
        return realmLookup.lookup(realm);
    }

    /**
     * Uses the {@literal realm} as the parent realm and the {@literal subRealm} as sub-realm to
     * lookup the Realm.
     *
     * @param realm The parent realm.
     * @param subRealm The sub-realm.
     * @return A {@code Realm} instance of the concatenation of {@literal realm} and
     * {@literal subRealm}.
     * @throws RealmLookupException If the provided {@literal realm} and {@literal subRealm} do not
     * constitute a valid realm or failed to be resolved/
     */
    public static Realm of(Realm realm, String subRealm) throws RealmLookupException {
        return realmLookup.lookup(concatenateRealmElements(realm, subRealm));
    }

    private static String concatenateRealmElements(Realm parentRealm, String realmElement) {
        Reject.ifNull(parentRealm, realmElement);
        Reject.ifTrue(realmElement.contains("/"), "realmElement cannot contain '/'");
        if (parentRealm.equals(root())) {
            return root().asPath() + realmElement;
        } else {
            return parentRealm.asPath() + "/" + realmElement;
        }
    }

    private final String path;
    private final String realmDN;

    Realm(String realmDN) {
        this.path = convertRealmDNToPath(realmDN);
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
