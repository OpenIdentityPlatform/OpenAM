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

import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.shared.locale.L10NMessageImpl;

/**
 * Signals that the {@literal realm} {@code String} used to lookup a realm failed due to it being
 * an invalid realm identifier or the lookup operation failed.
 *
 * @since 14.0.0
 */
public class RealmLookupException extends L10NMessageImpl {

    private final String realm;

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param errorCode Key to the {@link IdRepoBundle#BUNDLE_NAME} resource bundle.
     * @param realm The realm.
     */
    public RealmLookupException(String errorCode, String realm) {
        super(IdRepoBundle.BUNDLE_NAME, errorCode, new Object[]{realm});
        this.realm = realm;
    }

    /**
     * Constructs a new exception with the specified {@code Throwable}.
     *
     * @param cause The cause.
     */
    public RealmLookupException(Throwable cause) {
        super(cause);
        this.realm = null;
    }

    /**
     * Gets the realm that could not be found.
     *
     * @return The realm.
     */
    public String getRealm() {
        return realm;
    }
}
