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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.rest.resource;

import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.ServerContext;

/**
 * A CREST Context for holding realm information from the Request.
 *
 * @since 12.0.0
 */
public class RealmContext extends ServerContext {

    private String realm;

    /**
     * Constructs a new RealmContext instance with the realm.
     *
     * @param parent The parent context.
     * @param realm The realm.
     */
    public RealmContext(Context parent, String realm) {
        super(parent);
        this.realm = realm;
    }

    /**
     * Gets the realm.
     *
     * @return The realm.
     */
    public String getRealm() {
        return realm;
    }

    /**
     * <p>Adds the sub-realm portion to the realm in the context.</p>
     *
     * <p>If the sub-realm is {@code null} or empty, no action is taken. If the sub-realm contains a leading or trailing
     * backslash, they will be stripped before appending to the current realm value.</p>
     *
     * @param subrealm The sub-realm to add to the realm context.
     */
    void addSubRealm(String subrealm) {
        if (subrealm == null || subrealm.isEmpty()) {
            return;
        }
        if (subrealm.startsWith("/")) {
            subrealm = subrealm.substring(1);
        }
        if (subrealm.endsWith("/")) {
            subrealm = subrealm.substring(0, subrealm.length() - 1);
        }

        realm = realm.equals("/") ? realm + subrealm : realm + "/" + subrealm;
    }
}
