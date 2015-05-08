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

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.security.PrivilegedAction;
import java.util.Properties;

import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.sm.datalayer.api.StoreMode;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ServerConfiguration;

/**
 * Unit test to exercise the behaviour of {@link UpgradeExternalCTSConfigurationStep}.
 */
public class UpgradeExternalCTSConfigurationStepTest {

    private static final String PORT_OVERRIDE_VALUE = "9999";
    private static final String PORT_DEFAULT_VALUE = "1234";
    private static final String HOST_DEFAULT_VALUE = "opendj-cts.default";
    private static final String HOST_OVERRIDE_VALUE = "opendj-cts.override.org";
    private static final String COLON_CHAR = ":";
    
    // Value may not be in current versions
    String CTS_STORE_PORT = "org.forgerock.services.cts.store.port";
    private UpgradeExternalCTSConfigurationStep step;
    private PrivilegedAction<SSOToken> adminTokenAction;
    private ConnectionFactory connectionFactory;
    private static final String TXT_LF = "\n";

    @BeforeMethod
    public void setUp() throws Exception {
        adminTokenAction = mock(PrivilegedAction.class);
        connectionFactory = mock(ConnectionFactory.class);
        step = new UpgradeExternalCTSConfigurationStep(adminTokenAction, connectionFactory);
        step.setDefaultValues(getServerDefaults());
    }

    @Test
    public void testEmptyHostAndPortNameShouldReturnEmpty() throws Exception {
        Properties properties = new Properties();
        step.checkCTSStoreConnections(ServerConfiguration.DEFAULT_SERVER_CONFIG, properties);
        assertThat(step.isApplicable()).isFalse();
    }

    @Test
    public void testLocationIsDefaultShouldReturnEmpty() throws Exception {
        step.checkCTSStoreConnections(ServerConfiguration.DEFAULT_SERVER_CONFIG, getLocationIsDefault());
        assertThat(step.isApplicable()).isFalse();
    }

    @Test
    public void testDefaultContainsColonShouldReturnEmpty() throws Exception {
        step.setDefaultValues(getServerDefaultsWithColon());
        step.checkCTSStoreConnections(ServerConfiguration.DEFAULT_SERVER_CONFIG, getHostAndPort());
        assertThat(step.isApplicable()).isTrue();
    }

    @Test
    public void testHostContainsColonShouldReturnEmpty() throws Exception {
        step.checkCTSStoreConnections(ServerConfiguration.DEFAULT_SERVER_CONFIG, getHostWithColon());
        assertThat(step.isApplicable()).isFalse();
    }

    @Test
    public void testOverrideHostAndPort() throws Exception {
        step.checkCTSStoreConnections(ServerConfiguration.DEFAULT_SERVER_CONFIG, getHostAndPort());
        assertThat(step.isApplicable()).isTrue();
        assertThat(step.getDetailedReport(TXT_LF).contains(HOST_OVERRIDE_VALUE + COLON_CHAR + PORT_OVERRIDE_VALUE));
    }

    @Test
    public void testOverrideHostUseDefaultPort() throws Exception {
        step.checkCTSStoreConnections(ServerConfiguration.DEFAULT_SERVER_CONFIG, getHostnameOnly());
        assertThat(step.isApplicable()).isTrue();
        assertThat(step.getDetailedReport(TXT_LF).contains(HOST_OVERRIDE_VALUE + COLON_CHAR + PORT_DEFAULT_VALUE));
    }

    @Test
    public void testOverridePortUseDefaultHost() throws Exception {
        step.checkCTSStoreConnections(ServerConfiguration.DEFAULT_SERVER_CONFIG, getPortOnly());
        assertThat(step.isApplicable()).isTrue();
        assertThat(step.getDetailedReport(TXT_LF).contains(HOST_DEFAULT_VALUE + COLON_CHAR + PORT_OVERRIDE_VALUE));
    }

    @Test
    public void testNoDefaultOrOverridePortShouldReturnEmpty() throws Exception {
        step.setDefaultValues(getServerDefaultsNoPort());
        step.checkCTSStoreConnections(ServerConfiguration.DEFAULT_SERVER_CONFIG, getHostnameOnly());
        assertThat(step.isApplicable()).isFalse();
    }


    private Properties getHostAndPort() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(CoreTokenConstants.CTS_STORE_HOSTNAME, HOST_OVERRIDE_VALUE);
        properties.setProperty(CTS_STORE_PORT, PORT_OVERRIDE_VALUE);
        properties.setProperty(CoreTokenConstants.CTS_STORE_LOCATION, "external");
        return properties;
    }

    private Properties getLocationIsDefault() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(CoreTokenConstants.CTS_STORE_HOSTNAME, HOST_OVERRIDE_VALUE);
        properties.setProperty(CTS_STORE_PORT, PORT_OVERRIDE_VALUE);
        properties.setProperty(CoreTokenConstants.CTS_STORE_LOCATION, StoreMode.DEFAULT.toString().toLowerCase());
        return properties;
    }

    private Properties getHostnameOnly() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(CoreTokenConstants.CTS_STORE_HOSTNAME, HOST_OVERRIDE_VALUE);
        return properties;
    }

    private Properties getPortOnly() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(CTS_STORE_PORT, PORT_OVERRIDE_VALUE);
        return properties;
    }

    private Properties getHostWithColon() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(CoreTokenConstants.CTS_STORE_HOSTNAME, HOST_OVERRIDE_VALUE + COLON_CHAR
                + PORT_OVERRIDE_VALUE);
        properties.setProperty(CTS_STORE_PORT, "7666786");
        return properties;
    }

    private Properties getServerDefaults() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(CoreTokenConstants.CTS_STORE_HOSTNAME, HOST_DEFAULT_VALUE);
        properties.setProperty(CoreTokenConstants.CTS_STORE_LOCATION, StoreMode.EXTERNAL.toString().toLowerCase());
        properties.setProperty(CTS_STORE_PORT, PORT_DEFAULT_VALUE);
        return properties;
    }

    private Properties getServerDefaultsWithColon() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(CoreTokenConstants.CTS_STORE_HOSTNAME, HOST_DEFAULT_VALUE + COLON_CHAR
                + PORT_DEFAULT_VALUE);
        properties.setProperty(CoreTokenConstants.CTS_STORE_LOCATION, StoreMode.EXTERNAL.toString().toLowerCase());
        return properties;
    }

    private Properties getServerDefaultsNoPort() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(CoreTokenConstants.CTS_STORE_HOSTNAME, HOST_DEFAULT_VALUE);
        properties.setProperty(CoreTokenConstants.CTS_STORE_LOCATION, StoreMode.EXTERNAL.toString().toLowerCase());
        return properties;
    }

}
