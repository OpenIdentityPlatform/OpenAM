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

package org.forgerock.openam.uma;

import static org.forgerock.openam.uma.UmaConstants.*;

import javax.inject.Inject;
import java.net.URI;
import java.security.AccessController;
import java.util.HashSet;
import java.util.Set;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.oauth2.OAuthProblemException;
import org.forgerock.openam.utils.OpenAMSettingsImpl;

/**
 * Implementation of the {@link UmaSettings}.
 *
 * @since 13.0.0
 */
public class UmaSettingsImpl extends OpenAMSettingsImpl implements UmaSettings {

    private final Debug logger = Debug.getInstance("UmaProvider");
    private final String realm;
    private static final Set<String> supportedGrantTypes = new HashSet<String>();

    static {
        supportedGrantTypes.add("authorization_code");
        supportedGrantTypes.add("implicit");
        supportedGrantTypes.add("password");
        supportedGrantTypes.add("client_credentials");
    }

    @Inject
    public UmaSettingsImpl(@Assisted String realm) throws NotFoundException {
        super(UmaConstants.SERVICE_NAME, UmaConstants.SERVICE_VERSION);
        this.realm = realm;
        if (!exists()) {
            throw new NotFoundException("No UMA provider for realm " + realm);
        }
        addServiceListener();
    }

    private void addServiceListener() {
        try {
            final SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
            final ServiceConfigManager serviceConfigManager = new ServiceConfigManager(token, SERVICE_NAME,
                    SERVICE_VERSION);
            if (serviceConfigManager.addListener(new UmaProviderSettingsChangeListener()) == null) {
                logger.error("Could not add listener to ServiceConfigManager instance. UMA provider service " +
                        "changes will not be dynamically updated for realm " + realm);
            }
        } catch (Exception e) {
            String message = "Unable to construct ServiceConfigManager: " + e;
            logger.error(message, e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, message);
        }
    }

    private boolean exists() {
        try {
            return hasConfig(realm);
        } catch (Exception e) {
            logger.message("Could not access realm config", e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion() {
        return "1.0";
    }

    private Set<String> getSetSetting(String realm, String attributeName) throws ServerException {
        try {
            return getSetting(realm, attributeName);
        } catch (SMSException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        } catch (SSOException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getSupportedPATProfiles() throws ServerException {
        return getSetSetting(realm, SUPPORTED_PAT_PROFILES_ATTR_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getSupportedAATProfiles() throws ServerException {
        return getSetSetting(realm, SUPPORTED_AAT_PROFILES_ATTR_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getSupportedRPTProfiles() throws ServerException {
        return getSetSetting(realm, SUPPORTED_RPT_PROFILES_ATTR_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuditLogConfig() throws SMSException, SSOException {
        return this.getStringSetting(realm, AUDIT_CONNECTION_CONFIG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getSupportedPATGrantTypes() throws ServerException {
        return supportedGrantTypes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getSupportedAATGrantTypes() throws ServerException {
        return supportedGrantTypes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getSupportedClaimTokenProfiles() throws ServerException {
        return getSetSetting(realm, SUPPORTED_CLAIM_TOKEN_PROFILES_ATTR_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<URI> getSupportedUmaProfiles() throws ServerException {
        Set<URI> supportedProfiles = new HashSet<URI>();
        for (String profile : getSetSetting(realm, SUPPORTED_UMA_PROFILES_ATTR_NAME)) {
            supportedProfiles.add(URI.create(profile));
        }
        return supportedProfiles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getRPTLifetime() throws ServerException {
        try {
            return getLongSetting(realm, RPT_LIFETIME_ATTR_NAME);
        } catch (SMSException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        } catch (SSOException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPermissionTicketLifetime() throws ServerException {
        try {
            return getLongSetting(realm, PERMISSION_TIKCET_LIFETIME_ATTR_NAME);
        } catch (SMSException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        } catch (SSOException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onDeleteResourceServerDeletePolicies() throws ServerException {
        try {
            return getBooleanSetting(realm, DELETE_POLICIES_ON_RESOURCE_SERVER_DELETION);
        } catch (SMSException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        } catch (SSOException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onDeleteResourceServerDeleteResourceSets() throws ServerException {
        try {
            return getBooleanSetting(realm, DELETE_RESOURCE_SETS_ON_RESOURCE_SERVER_DELETION);
        } catch (SMSException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        } catch (SSOException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * ServiceListener implementation to clear cache when it changes.
     */
    private final class UmaProviderSettingsChangeListener implements ServiceListener {

        public void schemaChanged(String serviceName, String version) {
            logger.warning("The schemaChanged ServiceListener method was invoked for service " + serviceName
                    + ". This is unexpected.");
        }

        public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent,
                int type) {
            logger.warning("The globalConfigChanged ServiceListener method was invoked for service " + serviceName);
            //if the global config changes, all organizationalConfig change listeners are invoked as well.
        }

        public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
                String serviceComponent, int type) {
            if (currentRealmTargetedByOrganizationUpdate(serviceName, version, orgName, type)) {
                if (logger.messageEnabled()) {
                    logger.message("Updating UMA service configuration state for realm " + realm);
                }
            } else {
                if (logger.messageEnabled()) {
                    logger.message("Got service update message, but update did not target UmaProvider in " +
                            realm + " realm. ServiceName: " + serviceName + " version: " + version + " orgName: " +
                            orgName + " groupName: " + groupName + " serviceComponent: " + serviceComponent +
                            " type (modified=4, delete=2, add=1): " + type + " realm as DN: "
                            + DNMapper.orgNameToDN(realm));
                }
            }
        }

        /*
        The listener receives updates for all changes for each service instance in a given realm. We want to be sure
        that we only pull updates as necessary if the update pertains to this particular realm.
         */
        private boolean currentRealmTargetedByOrganizationUpdate(String serviceName, String version, String orgName,
                int type) {
            return SERVICE_NAME.equals(serviceName) && SERVICE_VERSION.equals(version)
                    && ((ServiceListener.MODIFIED == type) || (ServiceListener.ADDED == type)
                    || (ServiceListener.REMOVED == type))
                    && (orgName != null) && orgName.equals(DNMapper.orgNameToDN(realm));
        }
    }
}
