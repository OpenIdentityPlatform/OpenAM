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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.forgerock.openam.cts.utils.LdapTokenAttributeConversion;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.opendj.ldap.Entry;

/**
 * A converter to convert an LDAP entry to a PartialToken.
 */
@Singleton
public class EntryPartialTokenConverter implements EntryConverter<PartialToken> {

    private final LdapTokenAttributeConversion attributeConversion;

    @Inject
    public EntryPartialTokenConverter(LdapTokenAttributeConversion attributeConversion) {
        this.attributeConversion = attributeConversion;
    }

    @Override
    public PartialToken convert(Entry entry, String[] requestedAttributes) {
        return new PartialToken(attributeConversion.mapFromEntry(entry));
    }
}
