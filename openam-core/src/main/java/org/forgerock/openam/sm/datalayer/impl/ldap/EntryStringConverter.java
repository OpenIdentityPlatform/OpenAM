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

import javax.inject.Singleton;

import org.forgerock.opendj.ldap.Entry;
import org.forgerock.util.Reject;

/**
 * An entry converter that extracts a single string value from the entry.
 */
@Singleton
public class EntryStringConverter implements EntryConverter<String> {
    @Override
    public String convert(Entry entry, String[] requestedAttributes) {
        Reject.ifTrue(requestedAttributes == null || requestedAttributes.length != 1);
        return entry.getAttribute(requestedAttributes[0]).firstValueAsString();
    }
}
