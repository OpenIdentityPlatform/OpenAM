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
* Portions Copyrighted 2025 3A Systems LLC.
*/
package org.forgerock.openam.session;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.delegation.DelegationPermissionFactory;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * SessionPropertyWhitelist service that caches on a per-realm basis, and has listeners to
 * update cached map if config changes.
 **/
@Singleton
public class SessionPropertyWhitelist {

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

    private final DelegationEvaluator delegationEvaluator;
    private final DelegationPermissionFactory delegationPermissionFactory;

    /**
     * Constructor (called by Guice), registers a listener for this class against all
     * SessionPropertyWhitelist changes.
     */
    @Inject
    public SessionPropertyWhitelist(DelegationEvaluator delegationEvaluator,
                                    DelegationPermissionFactory delegationPermissionFactory) {
        this.delegationEvaluator = delegationEvaluator;
        this.delegationPermissionFactory = delegationPermissionFactory;

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

    /**
     * Get the properties listed for the provided realm, using the caller token to check
     * they have permission to see this result. This will return all properties that have been whitelisted,
     * even those which may not be settable due to being protected properties.
     *
     * @param realm The realm in which this operation is taking place.
     * @return The set of allowed listed properties.
     */
    public Set<String> getAllListedProperties(String realm) {
        return getWhitelist(realm);
    }

    /**
     * Returns true if the user is an administrator, or if it has delegated permissions to perform this
     * request.
     *
     * @param token SSOToken performing the request.
     * @param realm in which the request is taking place.
     */
    public boolean userHasReadAdminPrivs(SSOToken token, String realm)
            throws DelegationException, SSOException {

        DelegationPermission dp = delegationPermissionFactory
                .newInstance(realm, "rest", "1.0", "sessions", "getProperty", Collections.singleton("READ"),
                        Collections.<String, String>emptyMap());
        return delegationEvaluator.isAllowed(token, dp, Collections.<String, Set<String>>emptyMap());
    }

    /**
     * Whether or not the property is listed in the whitelist. If the caller has permission to see a protected
     * property they will, otherwise protected properties are removed from the returned set before being returned.
     *
     * @param caller The user checking their permission.
     * @param realm The realm in which this request is occurring.
     * @param propertyNames The names they wish for a response to.
     * @return true if all requested properties are whitelisted.
     */
    public boolean isPropertyListed(SSOToken caller, String realm, Collection<String> propertyNames)
            throws DelegationException, SSOException {
        return userHasReadAdminPrivs(caller, realm) || getWhitelist(realm).containsAll(propertyNames);
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

        return Collections.unmodifiableSet(WHITELIST_REALM_MAP.get(lowerRealm));
    }

    /**
     * Queries whether all the properties in the properties set are settable. Recording the key of the attempted
     * setting. Returns true if all  properties are settable, returns false if any one of them is not.
     *
     * @param caller The user checking their permission.
     * @param properties The property names they wish to set.
     * @return true if all requested properties are settable.
     */
    public boolean isPropertySetSettable(SSOToken caller, Collection<String> properties) {
        for (String property : properties) {
            try {
                SessionUtils.checkPermissionToSetProperty(caller, property, null);
            } catch (SessionException e) {
                return false;
            }
        }

        return true;
    }

    /**
     * Queries whether all the properties in the properties map are settable, recording the
     * key and value of the attempted setting. Returns true if all properties are settable, returns false if any one
     * of them is not.
     *
     * @param caller The user checking their permission.
     * @param properties Map of property to value it wishes to be set to.
     * @return true if all requested properties are settable.
     */
    public boolean isPropertyMapSettable(SSOToken caller, Map<String, String> properties) {
        for (Map.Entry<String, String> property : properties.entrySet()) {
            try {
                SessionUtils.checkPermissionToSetProperty(caller, property.getKey(), property.getValue());
            } catch (SessionException e) {
                return false;
            }
        }

        return true;
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
