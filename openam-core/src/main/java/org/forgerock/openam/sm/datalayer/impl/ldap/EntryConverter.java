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

package org.forgerock.openam.sm.datalayer.impl.ldap;

import org.forgerock.opendj.ldap.Entry;

/**
 * An object to convert LDAP Entry objects to other types.
 * @param <T> The type to convert to.
 */
public interface EntryConverter<T> {
    /**
     * Converts an entry to the return type.
     * @param entry The LDAP entry.
     * @param requestedAttributes The list of requested attributes, or null.
     * @return The converted object.
     */
    T convert(Entry entry, String[] requestedAttributes);
}