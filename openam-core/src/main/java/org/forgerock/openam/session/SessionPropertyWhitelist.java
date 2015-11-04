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
package org.forgerock.openam.session;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import java.security.AccessController;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * SessionPropertyWhitelist service that caches on a per-realm basis, and has listeners to
 * update cached map if config changes.
 **/
@Singleton
public class SessionPropertyWhitelist implements SessionPropertyList {

    /**
     * The name of the service implemented.
     */
    public static final String SERVICE_NAME = "SessionPropertyWhitelistService";

    /**
     * The version of the service implemented.
     */
    public static final String SERVICE_VERSION = "1.0";

    private static final Debug LOGGER = Debug.getInstance(SessionConstants.SESSION_DEBUG);

    private static final String WHITELIST_ATTRIBUTE_NAME = "forgerock-session-property-whitelist";
    private final Map<String, Set<String>> WHITELIST_REALM_MAP = new ConcurrentHashMap<>();
    private ServiceConfigManager serviceConfigManager;

    /**
     * Constructor (called by Guice), registers a listener for this class against all
     * SessionPropertyWhitelist changes.
     */
    @Inject
    public SessionPropertyWhitelist() {
        try {
            serviceConfigManager = new ServiceConfigManager(AccessController
                    .doPrivileged(AdminTokenAction.getInstance()), SERVICE_NAME, SERVICE_VERSION);

            serviceConfigManager.addListener(new SessionPropertyWhitelistListener());
        } catch (SMSException | SSOException e) {
            if (LOGGER.errorEnabled()) {
                LOGGER.error("Unable to load ServiceConfigManager for SessionPropertyWhitelist.", e);
            }
        }
    }

    @Override
    public Set<String> getAllListedProperties(SSOToken token, String realm) {

        final Set<String> allowed = getWhitelist(realm);

        Iterator<String> it = allowed.iterator();
        while (it.hasNext()) {
            String itN = it.next();
            try {
                SessionUtils.checkPermissionToSetProperty(token, itN, null);
            } catch (SessionException e) {
                LOGGER.message("Removed {} from list as protected property ", itN);
                it.remove();
            }
        }

        return allowed;
    }

    @Override
    public boolean isPropertyListed(SSOToken token, String realm, Set<String> propertyNames) {
        for (String prop : propertyNames) {
            try {
                SessionUtils.checkPermissionToSetProperty(token, prop, null);
            } catch (SessionException e) {
                return false;
            }
        }

        return getWhitelist(realm).containsAll(propertyNames);
    }

    private void installWhitelist(String realm) throws SSOException, SMSException {
        ServiceConfig scm = serviceConfigManager.getOrganizationConfig(realm, null);

        WHITELIST_REALM_MAP.put(realm.toLowerCase(),
                CollectionHelper.getServerMapAttrs(scm.getAttributes(), WHITELIST_ATTRIBUTE_NAME));
    }

    private Set<String> getWhitelist(String realm) {

        final String lowerRealm = realm.toLowerCase();

        if (WHITELIST_REALM_MAP.get(lowerRealm) == null) {
            try {
                installWhitelist(lowerRealm);
            } catch (SSOException | SMSException e) {
                LOGGER.error("Unable to load ServiceConfigManager for SessionPropertyWhitelist in realm {}", realm, e);
                return Collections.emptySet();
            }
        }

        return WHITELIST_REALM_MAP.get(lowerRealm);
    }

    /**
     * Our service config change listener.
     */
    private final class SessionPropertyWhitelistListener implements ServiceListener {

        /**
         * No-op for this impl.
         */
        @Override
        public void schemaChanged(String serviceName, String version) {
            //This section intentionally left blank
        }

        /**
         * No-op for this impl.
         */
        @Override
        public void globalConfigChanged(String serviceName, String version, String groupName,
                                        String serviceComponent, int type) {
            //This section intentionally left blank
        }

        @Override
        public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
                                              String serviceComponent, int type) {
            try {
                if (SERVICE_NAME.equals(serviceName) && SERVICE_VERSION.equals(version)) {
                    installWhitelist(DNMapper.orgNameToRealmName(orgName));
                }
            } catch (SSOException | SMSException e) {
                LOGGER.error("Unable to load ServiceConfigManager for SessionPropertyWhitelist in realm {}",
                        orgName, e);
            }
        }
    }

}
