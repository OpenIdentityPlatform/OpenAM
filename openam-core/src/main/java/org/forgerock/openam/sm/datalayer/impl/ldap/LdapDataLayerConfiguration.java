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

import org.forgerock.openam.cts.impl.LDAPConfig;
import org.forgerock.openam.sm.datalayer.api.StoreMode;
import org.forgerock.openam.utils.ModifiedProperty;

/**
 * An abstract LDAP set of configuration.
 */
public abstract class LdapDataLayerConfiguration extends LDAPConfig {

    protected LdapDataLayerConfiguration(String rootSuffix) {
        super(rootSuffix);
    }

    /**
     *
     */
    public abstract StoreMode getStoreMode();

        /**
         * Update the configuration of the LDAP connection details.
         * @param hosts The LDAP hosts.
         * @param username The LDAP username.
         * @param password The LDAP password.
         * @param maxConnections The maximum number of connections.
         * @param sslMode The SSL mode.
         * @param heartbeat The heartbeat interval.
         */
    public abstract void updateExternalLdapConfiguration(ModifiedProperty<String> hosts,
            ModifiedProperty<String> username, ModifiedProperty<String> password,
            ModifiedProperty<String> maxConnections, ModifiedProperty<Boolean> sslMode,
            ModifiedProperty<Integer> heartbeat);

}
