/**
 * Copyright 2013 ForgeRock, AS.
 *
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
 */
package com.sun.identity.sm.ldap.impl;

import com.google.inject.Inject;
import com.sun.identity.sm.ldap.api.CoreTokenConstants;
import com.sun.identity.sm.ldap.utils.LDAPDataConversion;
import com.sun.identity.sm.ldap.utils.TokenAttributeConversion;
import org.forgerock.openam.sm.DataLayerConnectionFactory;

/**
 * Responsible for generating instances of a QueryBuilder to perform queries against
 * the LDAP connection.
 *
 * @author robert.wapshott@forgerock.com
 */
public class QueryFactory {
    // Injected
    private final CoreTokenConstants constants;
    private final DataLayerConnectionFactory connectionFactory;

    @Inject
    public QueryFactory(CoreTokenConstants constants, DataLayerConnectionFactory connectionFactory) {
        this.constants = constants;
        this.connectionFactory = connectionFactory;
    }

    /**
     * Generate an instance of the QueryBuilder.
     *
     * @return A non null instance of the QueryBuilder.
     */
    public QueryBuilder createInstance() {
        LDAPDataConversion conversion = new LDAPDataConversion();
        TokenAttributeConversion attributeConversion = new TokenAttributeConversion(constants, conversion);
        return new QueryBuilder(connectionFactory, attributeConversion, constants);
    }

    /**
     * Generate an instance of the QueryFilter for use with the QueryBuilder.
     *
     * @return A non null QueryFilter instance.
     */
    public QueryFilter createFilter() {
        return new QueryFilter(new LDAPDataConversion());
    }
}
