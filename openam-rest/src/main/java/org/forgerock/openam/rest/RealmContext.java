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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.rest;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.services.context.AbstractContext;
import org.forgerock.services.context.Context;

/**
 * A CREST Context for holding realm information from the Request.
 *
 * @since 12.0.0
 */
public class RealmContext extends AbstractContext {

    private final Realm realm;
    private final boolean viaDns;

    /**
     * Constructs a new RealmContext for a realm determined from the request URI.
     *
     * @param parent The parent context.
     * @param realm The realm.
     * @since 14.0.0
     */
    public RealmContext(Context parent, Realm realm) {
        this(parent, realm, false);
    }

    /**
     * Constructs a new RealmContext.
     *
     * @param parent The parent context.
     * @param realm The realm.
     * @param viaDns {@code true} if the realm was determined from the DNS.
     * @since 14.0.0
     */
    RealmContext(Context parent, Realm realm, boolean viaDns) {
        super(parent, "realm");
        this.realm = realm;
        this.viaDns = viaDns;
    }

    /**
     * Determines if the resolved realm is the top level realm.
     *
     * @return True if the resolved realm is the top level realm, false otherwise.
     */
    public boolean isRootRealm() {
        return realm.equals(Realm.root());
    }

    /**
     * Returns {@code true} if the realm was determined from the DNS.
     *
     * @return {@code true} if the realm was determined from the DNS.
     */
    boolean isViaDns() {
        return viaDns;
    }

    /**
     * Gets the realm.
     *
     * @return The realm.
     * @since 14.0.0
     */
    public Realm getRealm() {
        return realm;
    }

    /**
     * Gets the realm for the given context.
     *
     * @param context The context.
     * @return The realm from the context.
     * @since 14.0.0
     */
    public static Realm getRealm(Context context) {
        RealmContext realmContext = context.asContext(RealmContext.class);
        return realmContext.getRealm();
    }
}
