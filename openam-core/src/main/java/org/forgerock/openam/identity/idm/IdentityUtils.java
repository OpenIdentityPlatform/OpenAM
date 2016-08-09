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
 * Portions Copyrighted 2005 Sun Microsystems Inc.
 */
package org.forgerock.openam.identity.idm;

import com.sun.identity.idm.AMIdentity;

/**
 * Collection of helper functions for {@link AMIdentity}.
 */
public final class IdentityUtils {

    /**
     * Private constructor.
     */
    private IdentityUtils() {
    }

    /**
     * Returns the matching DN from the AM SDK for this entry. This utility is
     * required by auth.
     *
     * @param id  {@code AMIdentity} object.
     * @return {@code DN} of the object, as represented in the datastore.
     */
    public static String getDN(AMIdentity id) {
        if (id.getDN() != null) {
            return id.getDN();
        } else {
            return id.getUniversalId();
        }
    }
}
