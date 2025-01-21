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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.sm.datalayer.impl.ldap;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.utils.LdapTokenAttributeConversion;
import org.forgerock.opendj.ldap.Entry;

/**
 * A converter to convert an LDAP entry to a Token.
 */
@Singleton
public class EntryTokenConverter implements EntryConverter<Token> {

    private final LdapTokenAttributeConversion attributeConversion;

    @Inject
    public EntryTokenConverter(LdapTokenAttributeConversion attributeConversion) {
        this.attributeConversion = attributeConversion;
    }

    @Override
    public Token convert(Entry entry, String[] requestedAttributes) {
        return attributeConversion.tokenFromEntry(entry);
    }
}
