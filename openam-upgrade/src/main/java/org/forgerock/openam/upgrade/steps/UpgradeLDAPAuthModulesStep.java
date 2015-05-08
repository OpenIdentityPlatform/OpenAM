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
package org.forgerock.openam.upgrade.steps;

import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

/**
 * This upgrade step upgrades LDAP and AD auth module instances.
 * <p/>
 * If module instances have value <code>true</code> set for property <code>iplanet-am-auth-ldap-ssl-enabled</code>,
 * the property <code>openam-auth-ldap-connection-mode</code> with value <code>LDAPS</code> will be set.
 * <p/>
 * If module instances have value <code>false</code> set for property <code>iplanet-am-auth-ldap-ssl-enabled</code>,
 * the property <code>openam-auth-ldap-connection-mode</code> with value <code>LDAP</code> will be set.
 * <p/>
 * If needed property <code>iplanet-am-auth-ldap-ssl-enabled</code> will be removed.
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class UpgradeLDAPAuthModulesStep extends AbstractUpgradeStep {

    private static final String SSL_ENABLED_PROPERTY = "iplanet-am-auth-ldap-ssl-enabled";
    private static final String CONNECTION_MODE_PROPERTY = "openam-auth-ldap-connection-mode";
    private static final String AUTH_INSTANCE_DATA = "%AUTH_INSTANCE_DATA%";
    private static final String SEPARATOR = ": ";
    private final Map<String, Map<String, Boolean>> instances = new HashMap<String, Map<String, Boolean>>();

    @Inject
    public UpgradeLDAPAuthModulesStep(final PrivilegedAction<SSOToken> adminTokenAction,
                                      @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory factory) {
        super(adminTokenAction, factory);
    }

    @Override
    public void initialize() throws UpgradeException {
        String realmName = null;
        String authInstanceName = null;
        try {
            for (final String realm : getRealmNames()) {
                realmName = realm;
                final AMAuthenticationManager mgr = new AMAuthenticationManager(getAdminToken(), realm);
                final Set<AMAuthenticationInstance> moduleInstances = mgr.getAuthenticationInstances();
                if (moduleInstances != null) {
                    for (final AMAuthenticationInstance moduleInstance : moduleInstances) {
                        if (moduleInstance.getType().equalsIgnoreCase("LDAP") ||
                                moduleInstance.getType().equalsIgnoreCase("AD")) {
                            authInstanceName = moduleInstance.getName();
                            if (DEBUG.messageEnabled()) {
                                DEBUG.message("ldap/ad auth module configuration found under realm: " + realm + " : "
                                        + authInstanceName);
                            }
                            final Map<String, Set<String>> configProperties =
                                    moduleInstance.getAttributeValues(asSet(SSL_ENABLED_PROPERTY));

                            if (configProperties != null && !configProperties.isEmpty()) {
                                final String sslEnabledProp = CollectionHelper.getMapAttr(configProperties,
                                        SSL_ENABLED_PROPERTY);
                                if (sslEnabledProp != null) {
                                    if (DEBUG.messageEnabled()) {
                                        DEBUG.message("ldap/ad auth module config " + authInstanceName + " in realm: "
                                                + realm + " " + SSL_ENABLED_PROPERTY + ":" + sslEnabledProp);
                                    }
                                    Map<String, Boolean> instanceMap = instances.get(realm);
                                    if (instanceMap == null) {
                                        instanceMap = new HashMap<String, Boolean>();
                                        instances.put(realm, instanceMap);
                                    }
                                    instanceMap.put(authInstanceName, Boolean.parseBoolean(sslEnabledProp));
                                }
                            }
                        }
                    }
                }
            }
        } catch (final Exception ex) {
            DEBUG.error("Unable to identify the configuration for the old ldap/ad auth module instance " + authInstanceName
                    + " in realm " + realmName, ex);
            throw new UpgradeException("An error occurred while trying to identify the configuration for the old " +
                    "ldap/ad auth module instance " + authInstanceName + " in realm " + realmName, ex);
        }
    }

    @Override
    public boolean isApplicable() {
        return !instances.isEmpty();
    }

    @Override
    public void perform() throws UpgradeException {
        UpgradeProgress.reportStart("upgrade.auth.instances.ldap.start");
        for (final Map.Entry<String, Map<String, Boolean>> entry : instances.entrySet()) {
            final String realm = entry.getKey();
            final Map<String, Boolean> instanceMap = entry.getValue();
            try {
                updateAttributes(realm, instanceMap);
            } catch (final Exception ex) {
                UpgradeProgress.reportEnd("upgrade.failed");
                DEBUG.error("An error occurred while upgrading service configs for auth module instances "
                        + " in realm " + realm, ex);
                throw new UpgradeException("Unable to upgrade ldap/ad auth module instance configurations "
                        + " in realm " + realm, ex);
            }
        }
        UpgradeProgress.reportEnd("upgrade.success");
    }

    private void updateAttributes(final String realm, final Map<String, Boolean> instanceMap)
            throws SMSException, AMConfigurationException, SSOException {
        final AMAuthenticationManager mgr = new AMAuthenticationManager(getAdminToken(), realm);
        for (final Map.Entry<String, Boolean> instance : instanceMap.entrySet()) {
            final String instanceName = instance.getKey();
            final String newValue = getNewValue(instance.getValue());
            final AMAuthenticationInstance authModuleInstance = mgr.getAuthenticationInstance(instanceName);
            final Map<String, Set<String>> moduleSettings = authModuleInstance.getAttributeValues();

            final ServiceConfig moduleConfig = authModuleInstance.getServiceConfig();

            Set<String> attributeValues = moduleSettings.get(SSL_ENABLED_PROPERTY);
            if (attributeValues != null && !attributeValues.isEmpty()) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Removing attribute " + SSL_ENABLED_PROPERTY + " from ldap/ad auth module instance <"
                            + instanceName + "> in realm: " + realm);
                }
                moduleConfig.removeAttribute(SSL_ENABLED_PROPERTY);
            }

            attributeValues = moduleSettings.get(CONNECTION_MODE_PROPERTY);
            if (attributeValues != null && !attributeValues.isEmpty()) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Upgrading attribute " + CONNECTION_MODE_PROPERTY + " for ldap/ad auth module instance <"
                            + instanceName + "> to <" + newValue
                            + "> in realm: " + realm);
                }
                final Map<String, Set<String>> newConnectionModeValues = new HashMap<String, Set<String>>();
                newConnectionModeValues.put(CONNECTION_MODE_PROPERTY, asSet(newValue));
                moduleConfig.setAttributes(newConnectionModeValues);
            }
        }
    }

    private String getNewValue(boolean value) {
        return value ? SetupConstants.LDAP_CONNECTION_MODE_LDAPS : SetupConstants.LDAP_CONNECTION_MODE_LDAP;
    }

    @Override
    public String getShortReport(String delimiter) {
        int count = 0;
        for (final Map<String, Boolean> changes : instances.values()) {
            count += changes.size();
        }

        return new StringBuilder(BUNDLE.getString("upgrade.auth.instances.ldap")).append(" (").append(count)
                .append(')').append(delimiter).toString();
    }

    @Override
    public String getDetailedReport(String delimiter) {
        final Map<String, String> tags = new HashMap<String, String>();
        tags.put(LF, delimiter);
        final StringBuilder sb = new StringBuilder();

        for (final Map.Entry<String, Map<String, Boolean>> entry : instances.entrySet()) {
            sb.append(BUNDLE.getString("upgrade.realm")).append(SEPARATOR).append(entry.getKey()).append(delimiter);
            for (final Map.Entry<String, Boolean> instance : entry.getValue().entrySet()) {
                final String authInstanceName = instance.getKey();
                sb.append(INDENT).append(BUNDLE.getString("upgrade.auth.instance")).append(SEPARATOR);
                sb.append(authInstanceName.substring(authInstanceName.lastIndexOf('/') + 1));
                sb.append(delimiter);
                sb.append(INDENT).append(BUNDLE.getString("upgrade.delattr")).append(SEPARATOR);
                sb.append(SSL_ENABLED_PROPERTY).append(delimiter);
                sb.append(INDENT).append(BUNDLE.getString("upgrade.addattr")).append(SEPARATOR);
                sb.append(CONNECTION_MODE_PROPERTY).append("=");
                sb.append(getNewValue(instance.getValue())).append(delimiter);
                sb.append(delimiter);
            }
        }

        tags.put(AUTH_INSTANCE_DATA, sb.toString());
        return tagSwapReport(tags, "upgrade.auth.instances.ldap.report");
    }
}
