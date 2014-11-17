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
package org.forgerock.openam.security.whitelist;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import java.security.AccessController;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;

/**
 * Extracts the valid goto URL domains from the Authentication service configuration and caches the result. If the
 * configuration changes, a service listener should clear the corresponding cache entry resulting in a config reload.
 */
public class ValidGotoUrlExtractor implements ValidDomainExtractor<String> {

    private static final ValidDomainExtractor<String> INSTANCE = new ValidGotoUrlExtractor();
    private static final String VALIDATION_SERVICE = "validationService";
    private static final String VALID_GOTO_RESOURCES = "openam-auth-valid-goto-resources";
    private static final Debug DEBUG = Debug.getInstance("patternMatching");
    private static final Map<String, Set<String>> CACHE =
            Collections.synchronizedMap(new HashMap<String, Set<String>>());
    private static volatile boolean isListenerRegistered = false;

    private ValidGotoUrlExtractor() {
    }

    public static ValidDomainExtractor<String> getInstance() {
        return INSTANCE;
    }

    @Override
    public Collection<String> extractValidDomains(String realm) {
        final String realmDN = normalizeRealm(realm);
        Set<String> validGotoUrlPatterns = CACHE.get(realmDN);
        if (validGotoUrlPatterns == null) {
            synchronized (CACHE) {
                validGotoUrlPatterns = CACHE.get(realmDN);
                if (validGotoUrlPatterns == null) {
                    try {
                        validGotoUrlPatterns = getValidGotoUrlPatterns(realmDN);
                        CACHE.put(realmDN, validGotoUrlPatterns);
                    } catch (final SMSException smse) {
                        DEBUG.error("An error occurred while retrieving the valid goto URLs for realm "
                                + realmDN, smse);
                        return null;
                    }
                }
            }
        }

        return validGotoUrlPatterns;
    }

    protected Set<String> getValidGotoUrlPatterns(final String realm) throws SMSException {
        final OrganizationConfigManager ocm = new OrganizationConfigManager(getAdminToken(), realm);
        final ServiceConfig serviceConfig = ocm.getServiceConfig(VALIDATION_SERVICE);
        final Map<String, Set<String>> attrs = serviceConfig.getAttributes();
        final Set<String> ret = attrs.get(VALID_GOTO_RESOURCES);
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Valid goto URLs in realm " + realm + ":\n" + ret);
        }

        if (!isListenerRegistered) {
            try {
                final ServiceConfigManager scm = new ServiceConfigManager(VALIDATION_SERVICE, getAdminToken());
                scm.addListener(new ConfigListener());
                isListenerRegistered = true;
            } catch (SMSException smse) {
                DEBUG.error("An error occurred while registering SMS listener", smse);
            } catch (SSOException ssoe) {
                DEBUG.error("An error occurred while registering SMS listener", ssoe);
            }
        }

        return ret == null ? Collections.EMPTY_SET : Collections.unmodifiableSet(ret);
    }

    private String normalizeRealm(String realm) {
        return DNMapper.orgNameToDN(realm);
    }

    private SSOToken getAdminToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    /**
     * A service listener that clears the cached goto URL domains if the Authentication service settings changes.
     */
    private class ConfigListener implements ServiceListener {

        @Override
        public void schemaChanged(String serviceName, String version) {
        }

        @Override
        public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent,
                int type) {
        }

        @Override
        public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
                String serviceComponent, int type) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Auth Service organization configuration has changed"
                        + "\nserviceName: " + serviceName
                        + "\nversion: " + version
                        + "\norgName: " + orgName
                        + "\ngroupName: " + groupName
                        + "\nserviceComponent: " + serviceComponent
                        + "\ntype: " + type);
            }
            CACHE.remove(normalizeRealm(orgName));
        }
    }
}
