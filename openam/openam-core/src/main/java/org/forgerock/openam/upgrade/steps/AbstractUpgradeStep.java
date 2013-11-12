/*
 * Copyright 2013 ForgeRock AS.
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
package org.forgerock.openam.upgrade.steps;

import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import java.security.AccessController;
import java.util.ResourceBundle;
import java.util.Set;
import org.forgerock.openam.guice.InjectorHolder;
import org.forgerock.openam.sm.DataLayerConnectionFactory;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ErrorResultException;

/**
 * An abstract class that provides utility methods for upgrade steps.
 *
 * @author Peter Major
 */
public abstract class AbstractUpgradeStep implements UpgradeStep {

    protected static final String BULLET = "* ";
    protected static final String INDENT = "\t";
    protected static final Debug DEBUG = Debug.getInstance("amUpgrade");
    protected static ResourceBundle BUNDLE = ResourceBundle.getBundle("amUpgrade");
    private final DataLayerConnectionFactory connFactory =
            InjectorHolder.getInstance(DataLayerConnectionFactory.class);

    /**
     * Returns a valid admin SSOToken.
     *
     * @return A valid admin SSOToken.
     */
    protected final SSOToken getAdminToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
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
    protected final Connection getConnection() throws ErrorResultException {
        return connFactory.getConnection();
    }
}
