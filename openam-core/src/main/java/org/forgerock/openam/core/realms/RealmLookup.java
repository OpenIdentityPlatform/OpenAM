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

/**
 * API for looking up realms and determining if they are active or not.
 *
 * <p>Example usage:
 * <pre>
 *     Realms realms = ...;
 *     Realm realm = realms.lookup("/realm/A");
 *     boolean active = realms.isActive(realm);
 * </pre></p>
 *
 * @since 14.0.0
 */
public interface RealmLookup {

    /**
     * Looks up the provided {@literal realm}, in either its path or DN format.
     *
     * @param realm The realm to lookup, in either path or DN format.
     * @return A non-{@code null} {@link Realm} instance that matches the {@literal realm}.
     * @throws RealmLookupException If the provided {@literal realm} cannot be found.
     */
    Realm lookup(String realm) throws RealmLookupException;

    /**
     * Determines if the provided {@literal realm} is active or inactive.
     *
     * @param realm The realm to determine if is active or inactive.
     * @return {@code true} if the {@literal realm} is active, {@code false} otherwise.
     * @throws RealmLookupException If the provided {@literal realm} cannot be found.
     */
    boolean isActive(Realm realm) throws RealmLookupException;
}
