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

package org.forgerock.openam.sm.datalayer.impl;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.StoreMode;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapDataLayerConfiguration;
import org.forgerock.openam.utils.ModifiedProperty;
import org.forgerock.opendj.ldap.DN;

/**
 * A data layer configuration for use with resource set tokens.
 * @see org.forgerock.openam.sm.datalayer.api.ConnectionType#RESOURCE_SETS
 */
public class ResourceSetDataLayerConfiguration extends LdapDataLayerConfiguration {

    @Inject
    public ResourceSetDataLayerConfiguration(@Named(DataLayerConstants.ROOT_DN_SUFFIX) String rootSuffix) {
        super(rootSuffix);
    }

    @Override
    public void updateExternalLdapConfiguration(ModifiedProperty<String> hosts, ModifiedProperty<String> username, ModifiedProperty<String> password, ModifiedProperty<String> maxConnections, ModifiedProperty<Boolean> sslMode, ModifiedProperty<Integer> heartbeat) {
        // TODO
    }

    @Override
    public StoreMode getStoreMode() {
        return StoreMode.DEFAULT; // TODO
    }

    @Override
    protected DN setDefaultTokenDNPrefix(DN root) {
        return root.child("ou=resource_sets");
    }

    @Override
    protected String getCustomTokenRootSuffixProperty() {
        return null;
    }
}
