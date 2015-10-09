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

import javax.inject.Inject;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.common.configuration.ServerConfigXML;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.sm.SMSException;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.impl.queue.config.CTSQueueConfiguration;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.api.StoreMode;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapDataLayerConfiguration;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.VersionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

/**
 * Detects if changes are needed to the CTS Max Connections property.
 *
 * Performs the following logic to determine if the property needs updating on which servers.
 * <ul>
 *     <li>
 *         If all the servers directory config max connection pool sizes are the same then use that value
 *         and do the following:
 *         <ul>
 *             <li>Only update the server default CTS max connection setting if the store mode is DEFAULT
 *             and it has no max connection value set</li>
 *             <li>Never update each server instance CTS max connection setting as if its not set then it
 *             will be inherited otherwise if it is set then it should be left as it is.</li>
 *         </ul>
 *     </li>
 *     <li>Otherwise pick the most used value of the server directory config max connection pool sizes
 *     and set that as the default CTS max connections value and then for each server which has a
 *     different directory config max connection pool size update the CTS max connections value (if not
 *     already set)</li>
 * </ul>
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeExternalCTSConfigurationStep")
public class UpgradeCTSMaxConnectionsConfigurationStep extends AbstractUpgradeStep {

    private static final String PROGRESS_REPORT = "upgrade.ctsmaxconnections.progress";
    private static final String PROGRESS_SUCCESS = "upgrade.success";
    private static final String SHORT_REPORT = "upgrade.ctsmaxconnections.short";
    private static final String DETAIL_REPORT_SUMMARY = "upgrade.ctsmaxconnections.detail.summary";
    private static final String DETAIL_REPORT_SERVER = "upgrade.ctsmaxconnections.detail.server";
    private static final String DETAIL_REPORT = "upgrade.ctsmaxconnections.detail";
    private static final int AM_13 = 1300;

    private final ConnectionCount connectionCount;
    private final Helper helper;
    private final Map<String, String> serversToUpdate = new HashMap<>();

    @Inject
    public UpgradeCTSMaxConnectionsConfigurationStep(final PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory connectionFactory,
            ConnectionCount connectionCount, Helper helper) {
        super(adminTokenAction, connectionFactory);
        this.connectionCount = connectionCount;
        this.helper = helper;
    }

    @Override
    public void initialize() throws UpgradeException {
        if (VersionUtils.isCurrentVersionLessThan(AM_13, true)) {
            try {
                if (isSingleDirectoryConfigMaxConnectionsValueUsed()) {
                    if (helper.getDefaultServerConfig(getAdminToken()).isDefaultStoreMode()
                            && helper.getDefaultServerConfig(getAdminToken()).getCTSMaxConnections() == null) {
                        int directoryConfigMaxConnections = -1;
                        for (ServerInstanceConfig serverInstanceConfig :
                                helper.getServerConfigs(getAdminToken()).values()) {
                            directoryConfigMaxConnections = serverInstanceConfig.getDirectoryConfigMaxConnections();
                            if (directoryConfigMaxConnections > 0) {
                                break;
                            }
                        }
                        serversToUpdate.put(ServerConfiguration.DEFAULT_SERVER_CONFIG,
                                Integer.toString(calculateCTSMaxConnections(directoryConfigMaxConnections)));
                    }
                } else {
                    int mostUsedDirectoryConfigMaxConnectionsValue = getMostUsedDirectoryConfigMaxConnectionsValue();
                    serversToUpdate.put(ServerConfiguration.DEFAULT_SERVER_CONFIG,
                            Integer.toString(calculateCTSMaxConnections(mostUsedDirectoryConfigMaxConnectionsValue)));

                    for (Map.Entry<String, ServerInstanceConfig> serverInstanceConfig :
                            helper.getServerConfigs(getAdminToken()).entrySet()) {
                        int directoryConfigMaxConnections = serverInstanceConfig.getValue()
                                .getDirectoryConfigMaxConnections();
                        if (mostUsedDirectoryConfigMaxConnectionsValue != directoryConfigMaxConnections
                                && serverInstanceConfig.getValue().getCTSMaxConnections() == null) {
                            serversToUpdate.put(serverInstanceConfig.getKey(),
                                    Integer.toString(calculateCTSMaxConnections(directoryConfigMaxConnections)));
                        }
                    }
                }
            } catch (Exception ex) {
                DEBUG.error("Unable to upgrade External CTS Configuration", ex);
                throw new UpgradeException(ex);
            }
        }
    }

    @Override
    public boolean isApplicable() {
        return !serversToUpdate.isEmpty();
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            DEBUG.message("External CTS Configuration upgrading: {}", serversToUpdate);
            helper.clearState();
            for (Entry<String, String> ctsMaxConnectionsProperty : serversToUpdate.entrySet()) {
                UpgradeProgress.reportStart(PROGRESS_REPORT, ctsMaxConnectionsProperty.getKey());
                if (ServerConfiguration.DEFAULT_SERVER_CONFIG.equals(ctsMaxConnectionsProperty.getKey())) {
                    helper.getDefaultServerConfig(getAdminToken())
                            .setCTSMaxConnections(ctsMaxConnectionsProperty.getValue());
                } else {
                    helper.getServerConfigs(getAdminToken()).get(ctsMaxConnectionsProperty.getKey())
                            .setCTSMaxConnections(ctsMaxConnectionsProperty.getValue());
                }
                UpgradeProgress.reportEnd(PROGRESS_SUCCESS);
            }
            UpgradeProgress.reportEnd(PROGRESS_SUCCESS);
        } catch (Exception ex) {
            DEBUG.error("Unable to upgrade External CTS properties", ex);
            throw new UpgradeException(ex);
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        return MessageFormat.format(BUNDLE.getString(SHORT_REPORT), serversToUpdate.size()) + delimiter;
    }

    @Override
    public String getDetailedReport(String delimiter) {
        StringBuilder content = new StringBuilder();
        content.append(delimiter);
        content.append(DETAIL_REPORT_SUMMARY);
        content.append(delimiter);
        for (Map.Entry<String, String> serverEntry : serversToUpdate.entrySet()) {
            content.append(MessageFormat.format(BUNDLE.getString(DETAIL_REPORT_SERVER), serverEntry.getKey(),
                    serverEntry.getValue()));
            content.append(delimiter);
        }
        Map<String, String> tags = new HashMap<>();
        tags.put(LF, delimiter);
        tags.put("%CONTENT%", content.toString());
        return tagSwapReport(tags, DETAIL_REPORT);
    }

    private boolean isSingleDirectoryConfigMaxConnectionsValueUsed() throws Exception {
        int maxConnections = -1;
        for (ServerInstanceConfig serverInstanceConfig : helper.getServerConfigs(getAdminToken()).values()) {
            if (maxConnections < 0) {
                maxConnections = serverInstanceConfig.getDirectoryConfigMaxConnections();
            } else if (maxConnections != serverInstanceConfig.getDirectoryConfigMaxConnections()) {
                return false;
            }
        }
        return true;
    }

    private int getMostUsedDirectoryConfigMaxConnectionsValue() throws Exception {
        List<Integer> maxConnectionsCount = new ArrayList<>();
        for (Map.Entry<String, ServerInstanceConfig> serverInstanceConfig :
                helper.getServerConfigs(getAdminToken()).entrySet()) {
            maxConnectionsCount.add(serverInstanceConfig.getValue().getDirectoryConfigMaxConnections());
        }
        int mostUsedMaxConnectionsValue = -1;
        int count = -1;
        for (Integer maxConnections : maxConnectionsCount) {
            int frequency = Collections.frequency(maxConnectionsCount, maxConnections);
            if (frequency > count) {
                count = frequency;
                mostUsedMaxConnectionsValue = maxConnections;
            }
        }
        return mostUsedMaxConnectionsValue;
    }

    private int calculateCTSMaxConnections(int maxConnections) {
        return connectionCount.getConnectionCount(maxConnections, ConnectionType.CTS_ASYNC);
    }

    static class Helper {

        private ServerInstanceConfig defaultServerInstance;
        private Map<String, ServerInstanceConfig> serverInstances;

        ServerInstanceConfig getDefaultServerConfig(SSOToken adminToken) throws SMSException, IOException,
                SSOException {
            if (defaultServerInstance == null) {
                defaultServerInstance = new ServerInstanceConfig(adminToken,
                        ServerConfiguration.DEFAULT_SERVER_CONFIG, null, ServerConfiguration.getServerInstance(
                        adminToken, ServerConfiguration.DEFAULT_SERVER_CONFIG));
            }
            return defaultServerInstance;
        }

        Map<String, ServerInstanceConfig> getServerConfigs(SSOToken adminToken) throws Exception {
            if (serverInstances == null) {
                serverInstances = new HashMap<>();
                for (String serverName : ServerConfiguration.getServers(adminToken)) {
                    int smsMaxConnectionPoolSize = new ServerConfigXML(
                            ServerConfiguration.getServerConfigXML(adminToken, serverName))
                            .getSMSServerGroup().maxPool;
                    serverInstances.put(serverName, new ServerInstanceConfig(adminToken, serverName,
                            smsMaxConnectionPoolSize,
                            ServerConfiguration.getServerInstance(adminToken, serverName)));
                }
            }
            return serverInstances;
        }

        private void clearState() {
            defaultServerInstance = null;
            serverInstances = null;
        }
    }

    static class ServerInstanceConfig {

        private final SSOToken adminToken;
        private final String serverName;
        private final Integer smsMaxConnectionPoolSize;
        private final Properties properties;

        private ServerInstanceConfig(SSOToken adminToken, String serverName, Integer smsMaxConnectionPoolSize,
                Properties properties) {
            this.adminToken = adminToken;
            this.serverName = serverName;
            this.smsMaxConnectionPoolSize = smsMaxConnectionPoolSize;
            this.properties = properties;
        }

        Integer getDirectoryConfigMaxConnections() {
            if (smsMaxConnectionPoolSize == null) {
                DEBUG.warning("Default server does not have a SMS Directory Config max connection pool size.");
                throw new IllegalStateException("Default server does not have a SMS Directory "
                        + "Config max connection pool size.");
            }
            return smsMaxConnectionPoolSize;
        }

        boolean isDefaultStoreMode() {
            return "default".equalsIgnoreCase(properties.getProperty(CoreTokenConstants.CTS_STORE_LOCATION));
        }

        Integer getCTSMaxConnections() {
            String property = properties.getProperty(CoreTokenConstants.CTS_STORE_MAX_CONNECTIONS);
            if (StringUtils.isEmpty(property)) {
                return null;
            }
            return Integer.parseInt(property);
        }

        void setCTSMaxConnections(String maxConnections) throws SMSException, IOException, SSOException,
                ConfigurationException {
            // get existing values
            Map<String, String> existingServerProperties = new HashMap(ServerConfiguration.getServerInstance(
                    adminToken, serverName));
            // add new values to existing values
            existingServerProperties.put(CoreTokenConstants.CTS_STORE_MAX_CONNECTIONS, maxConnections);
            ServerConfiguration.upgradeServerInstance(adminToken, serverName, null,
                    existingServerProperties);
        }
    }

    /**
     * Logic to resolve the number of connections used by the three main users of the Service Management layer.
     *
     * @see ConnectionType
     */
    static class ConnectionCount {
        static final int MINIMUM_CONNECTIONS = 6;
        private final Map<ConnectionType, LdapDataLayerConfiguration> dataLayerConfiguration;

        /**
         * Guice initialised constructor.
         *
         * @param dataLayerConfiguration Configuration object from which the StoreMode (required for calculating
         *                               connections) can be obtained.
         */
        @Inject
        public ConnectionCount(Map<ConnectionType, LdapDataLayerConfiguration> dataLayerConfiguration) {
            this.dataLayerConfiguration = dataLayerConfiguration;
        }

        /**
         * Returns the number of connections that should be allocated to for each ConnectionFactory type.
         * <p/>
         * When used in embedded mode, all three types are applicable. When used in External mode only
         * the CTS Async and CTS Reaper modes are applicable.
         *
         * @param max  Non negative maximum number of connections allowed.
         * @param type The non null type of ConnectionFactory to be created.
         * @return A non negative integer.
         * @throws IllegalArgumentException If the maximum is less than {@link #MINIMUM_CONNECTIONS}.
         * @throws IllegalStateException    If the type was unknown.
         */
        int getConnectionCount(int max, ConnectionType type) {
            Reject.ifTrue(max < MINIMUM_CONNECTIONS);
            switch (type) {
                case CTS_ASYNC:
                    if (dataLayerConfiguration.get(type).getStoreMode() == StoreMode.DEFAULT) {
                        max = max / 2;
                    } else {
                        max = max - 2;
                    }
                    return CTSQueueConfiguration.findPowerOfTwo(max);
                default:
                    throw new IllegalStateException();
            }
        }
    }
}
