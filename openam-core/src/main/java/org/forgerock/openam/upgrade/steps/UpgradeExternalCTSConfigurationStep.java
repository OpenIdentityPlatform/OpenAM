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

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.utils.StringUtils;

import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ServerConfiguration;

/**
 * Detects if changes are needed to the ExternalCTS properties and upgrades accordingly. Currently it detects if the
 * following update is required: 1) Updating the org.forgerock.services.cts.store.directory.name property. In version
 * 11.0 to 11.0.3 org.forgerock.services.cts.store.directory.name and org.forgerock.services.cts.store.port were
 * separately defined values. In version 12 and 11.0.3+ these have been combined to single Connection String stored in
 * the store.directory.name attribute e.g opendj.example.com:389
 * The server default values will need to be re-applied later by the server default upgrade step.
 */
@UpgradeStepInfo
public class UpgradeExternalCTSConfigurationStep extends AbstractUpgradeStep {

    private static final String COLON_CHAR = ":";
    private static final String MOD_ATTRS = "%MOD_ATTRS%";

    // Value may not be in current versions
    private String CTS_STORE_PORT = "org.forgerock.services.cts.store.port";

    private String ctsDefaultLocation;
    private String ctsDefaultHostName;
    private String ctsDefaultHostPort;
    private final Map<String, String> propertiesToModify = new HashMap<String, String>();

    @Inject
    public UpgradeExternalCTSConfigurationStep(final PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
    }

    @Override
    public boolean isApplicable() {
        return !propertiesToModify.isEmpty();
    }

    @Override
    public void initialize() throws UpgradeException {
        try {
            // get server instances config
            Set<String> serverNames = ServerConfiguration.getServers(getAdminToken());
            // get the default server values and store values
            Properties props = ServerConfiguration.getServerInstance(getAdminToken(),
                    ServerConfiguration.DEFAULT_SERVER_CONFIG);
            setDefaultValues(props);
            checkCTSStoreConnections(ServerConfiguration.DEFAULT_SERVER_CONFIG, props);
            // check values for each instance
            for (String serverName : serverNames) {
                checkCTSStoreConnections(serverName, ServerConfiguration.getServerInstance(getAdminToken(), serverName));
            }
        } catch (Exception ex) {
            DEBUG.error("Unable to upgrade External CTS Configuration", ex);
            throw new UpgradeException(ex);
        }
    }

    /**
     * Set the default values from the server-default values.
     * 
     * @param props the properties from the server
     */
    protected void setDefaultValues(Properties props) {
        ctsDefaultLocation = props.getProperty(CoreTokenConstants.CTS_STORE_LOCATION);
        ctsDefaultHostName = props.getProperty(CoreTokenConstants.CTS_STORE_HOSTNAME);
        ctsDefaultHostPort = props.getProperty(CTS_STORE_PORT);

        DEBUG.message("Setting External CTS Store properties defaults: " + "\n ctsDefaultHostName: " + ctsDefaultHostName
                + " ctsDefaultHostPort: " + ctsDefaultHostPort + " ctsDefaultLocation:" + ctsDefaultLocation);
    }

    /**
     * Will check if {@link CoreTokenConstant.CTS_STORE_HOSTNAME} property will need to be updated and need the host
     * port appended to the connection string.
     * 
     * @param serverName the name of the server Instance.
     * @param serverProperties the configuration of the server Instance.
     */
    protected void checkCTSStoreConnections(String serverName, Properties serverProperties) {
        String ctsHostName = serverProperties.getProperty(CoreTokenConstants.CTS_STORE_HOSTNAME);
        String ctsPort = serverProperties.getProperty(CTS_STORE_PORT);
        String ctsLocation = serverProperties.getProperty(CoreTokenConstants.CTS_STORE_LOCATION);

        DEBUG.message("Checking CTS Store properties for instance: " + serverName + " \n properties: " + "ctsHostName: "
                + ctsHostName + " ctsPort: " + ctsPort + " ctsLocation: " + ctsLocation);
        // if no host and port values then return
        if (StringUtils.isBlank(ctsHostName) && StringUtils.isBlank(ctsPort)) {
            return;
        }

        // check current instance values and set to default if needed
        if (StringUtils.isBlank(ctsHostName)) {
            ctsHostName = ctsDefaultHostName;
            DEBUG.message("ctsHostName defaulting to : " + ctsDefaultHostName);
        }
        if (StringUtils.isBlank(ctsPort)) {
            ctsPort = ctsDefaultHostPort;
            DEBUG.message("ctsPort defaulting to : " + ctsDefaultHostPort);
        }
        if (StringUtils.isBlank(ctsLocation)) {
            ctsLocation = ctsDefaultLocation;
            DEBUG.message("ctsLocation defaulting to : " + ctsDefaultLocation);
        }

        // if the configuration is default, the host or port is blank from defaults or if there is already a colon in
        // CTS host name then skip
        if (!"external".equals(ctsLocation) || StringUtils.isBlank(ctsHostName) || ctsHostName.contains(COLON_CHAR)
                || StringUtils.isBlank(ctsPort)) {
            return;
        }
        // store properties to be updated
        propertiesToModify.put(serverName, ctsHostName + COLON_CHAR + ctsPort);
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            DEBUG.message("External CTS Configuration upgrading: " + propertiesToModify);
            UpgradeProgress.reportStart("upgrade.cts.property");
            for (Entry<String, String> serverProperty : propertiesToModify.entrySet()) {
                // get existing values
                Map<String, String> existingServerProperties = new HashMap(ServerConfiguration.getServerInstance(
                        getAdminToken(), serverProperty.getKey()));
                // add new values to existing values
                existingServerProperties.put(CoreTokenConstants.CTS_STORE_HOSTNAME, serverProperty.getValue());
                existingServerProperties.keySet().remove(CTS_STORE_PORT);
                ServerConfiguration.upgradeServerInstance(getAdminToken(), serverProperty.getKey(), null,
                        existingServerProperties);
            }
            UpgradeProgress.reportEnd("upgrade.success");
        } catch (Exception ex) {
            DEBUG.error("Unable to upgrade External CTS properties", ex);
            throw new UpgradeException(ex);
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        StringBuilder sb = new StringBuilder();
        sb.append(BUNDLE.getString("upgrade.cts.property")).append(": ");
        if (!propertiesToModify.isEmpty()) {
            sb.append(BUNDLE.getString("upgrade.updated")).append(" (").append(propertiesToModify.size()).append(')');
        }
        sb.append(delimiter);
        return sb.toString();
    }

    @Override
    public String getDetailedReport(String delimiter) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(LF, delimiter);

        if (!propertiesToModify.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Entry<String, String> entry : propertiesToModify.entrySet()) {
                sb.append(BULLET).append(BUNDLE.getString("upgrade.instance")).append(": ").append(entry.getKey())
                        .append(' ');
                sb.append(BUNDLE.getString("upgrade.modattr")).append(": ").append(entry.getValue());
                if (entry.getKey().equals(ServerConfiguration.DEFAULT_SERVER_CONFIG)) {
                    sb.append(' ').append(BUNDLE.getString("upgrade.default.update"));
                }
                sb.append(delimiter);
            }
            sb.append(BULLET).append(BUNDLE.getString("upgrade.delattr")).append(": ").append(CTS_STORE_PORT).append(delimiter);
            tags.put(MOD_ATTRS, sb.toString());          

        } else {
            tags.put(MOD_ATTRS, BUNDLE.getString("upgrade.none"));
        }
        return tagSwapReport(tags, "upgrade.cts.propertiesreport");
    }
}
