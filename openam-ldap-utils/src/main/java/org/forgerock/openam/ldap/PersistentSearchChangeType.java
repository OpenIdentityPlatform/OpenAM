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

package org.forgerock.openam.ldap;

/**
 * Types of change that an LDAP persistent search will produce. The values are as defined in
 *  @see <a
 *      href="http://tools.ietf.org/html/draft-ietf-ldapext-psearch">draft-ietf-ldapext-psearch
 *      - Persistent Search: A Simple LDAP Change Notification Mechanism </a>
 */
public final class PersistentSearchChangeType {

    /**
     * Entry was added.
     */
    public static final int ADDED = 1;

    /**
     * Entry was removed.
     */
    public static final int REMOVED = 2;

    /**
     * Entry was modified.
     */
    public static final int MODIFIED = 4;

    /**
     * Entry was renamed/moved.
     */
    public static final int RENAMED = 8;

    /**
     * Constant for Entry being added, modified, removed or renamed.
     */
    public static final int ALL_OPERATIONS = ADDED | MODIFIED | REMOVED | RENAMED;

    private PersistentSearchChangeType() {
    }

}
