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
package org.forgerock.openam.sm;

import org.forgerock.openam.ldap.LDAPURL;
import org.forgerock.openam.ldap.LDAPUtils;

import java.util.Set;

/**
 * Represents the details required to establish a Connection to an LDAP server.
 *
 * This configuration is expected to be used with {@link LDAPUtils}
 */
public interface ConnectionConfig {
    /**
     * The LDAPURLs that are to be used for the connection. This may be more than
     * one to allow for automatic fail-over connections.
     *
     * @return Non null collection with at least one entry present.
     */
    Set<LDAPURL> getLDAPURLs();

    /**
     * The Bind DN to use for the connection.
     *
     * @return Non null.
     */
    String getBindDN();

    /**
     * The Bind password to use for the connection.
     *
     * @return Non null.
     */
    char[] getBindPassword();

    /**
     * The maximum number of connections that may be established to the server
     * by the connection pool which corresponds to this configuration.
     *
     * @return A positive number.
     */
    int getMaxConnections();

    /**
     * The timeout in seconds to use for the heartbeat detection in the connection.
     *
     * @return -1 if not used, or a positive number.
     */
    int getLdapHeartbeat();
}
