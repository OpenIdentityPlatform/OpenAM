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

package com.sun.identity.sm;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Manager for maintaining list of REST endpoints that cannot be used as realm names.
 *
 * @since 13.0.0
 */
public final class InvalidRealmNameManager {

    /**
     * Set of forbidden realm names due to clashes with REST endpoints or other reasons.
     */
    private static final Set<String> INVALID_REALM_NAMES = new CopyOnWriteArraySet<String>();

    private InvalidRealmNameManager() {
        //Private utility constructor.
    }

    /**
     * Returns a <em>mutable</em> set of realm names that should be black-listed to prevent conflicts with REST
     * endpoints or other functionality. The returned set can be safely modified from concurrent threads.
     *
     * @return the set of invalid realm names.
     */
    public static Set<String> getInvalidRealmNames() {
        return INVALID_REALM_NAMES;
    }
}
