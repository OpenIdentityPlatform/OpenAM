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
 * Copyright 2013-2015 ForgeRock AS.
 */
package org.forgerock.openam.upgrade.steps;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ResourceBundle;
import java.util.Set;

import javax.inject.Inject;
import javax.security.auth.Subject;

import com.sun.identity.entitlement.opensso.SubjectUtils;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ErrorResultException;

import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;

/**
 * An abstract class that provides utility methods for upgrade steps.
 */
public abstract class AbstractUpgradeStep implements UpgradeStep {

    protected static final String BULLET = "* ";
    protected static final String INDENT = "\t";
    protected static final Debug DEBUG = Debug.getInstance("amUpgrade");
    protected static ResourceBundle BUNDLE = ResourceBundle.getBundle("amUpgrade");
    private final PrivilegedAction<SSOToken> adminTokenAction;
    private final ConnectionFactory<Connection> connectionFactory;

    @Inject
    public AbstractUpgradeStep(final PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory connectionFactory) {
        this.adminTokenAction = adminTokenAction;
        this.connectionFactory = connectionFactory;
    }

    /**
     * Returns a valid admin SSOToken.
     *
     * @return A valid admin SSOToken.
     */
    protected final SSOToken getAdminToken() {
        return AccessController.doPrivileged(adminTokenAction);
    }

    /**
     * Return an admin subject built out of the admin token.
     *
     * @return an admin subject.
     */
    protected final Subject getAdminSubject() {
        final SSOToken adminToken = getAdminToken();
        if (adminToken == null) {
            return null;
        }
        return SubjectUtils.createSubject(adminToken);
    }

    /**
     * Returns the names of the realms available in the OpenAM configuration. The returned set is ordered, so the realm
     * hierarchy maintained (i.e. subrealm precedes sub-subrealm).
     *
     * @return The set of realmnames available in OpenAM.
     * @throws UpgradeException In case retrieving the realmnames was not successful.
     */
    protected final Set<String> getRealmNames() throws UpgradeException {
        try {
            OrganizationConfigManager ocm = new OrganizationConfigManager(getAdminToken(), "/");
            Set<String> realms = CollectionUtils.asOrderedSet("/");
            realms.addAll(ocm.getSubOrganizationNames("*", true));
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Discovered realms in the configuration: " + realms);
            }
            return realms;
        } catch (SMSException smse) {
            DEBUG.error("An error occurred while trying to retrieve the list of realms", smse);
            throw new UpgradeException("Unable to retrieve realms from SMS");
        }
    }

    /**
     * Acquires an LDAP connection against the configuration store.
     *
     * @return An LDAP connection object set up based on the configuration store settings.
     * @throws ErrorResultException If there was a problem establishing a connection to a valid server.
     */
    protected final Connection getConnection() throws DataLayerException {
        return connectionFactory.create();
    }
}
