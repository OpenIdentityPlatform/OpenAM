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
package org.forgerock.openam.upgrade.steps;

import com.iplanet.sso.SSOToken;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.Mockito.mock;

public class UpgradeServerDefaultsStepTest {

    private UpgradeServerDefaultsStep upgradeStep;
    private PrivilegedAction<SSOToken> adminTokenAction;
    private ConnectionFactory connectionFactory;

    @BeforeMethod
    public void setUp() throws Exception {
        adminTokenAction = mock(PrivilegedAction.class);
        connectionFactory = mock(ConnectionFactory.class);
        upgradeStep = new UpgradeServerDefaultsStep(adminTokenAction, connectionFactory);
    }

    @Test
    public void testAddedDefaults() throws Exception {
        Map<String, String> addedDefaults = upgradeStep.calculateAddedServerDefaults(getNewDefaults(),
                getExistingDefaults());
        assertThat(addedDefaults).isNotNull().hasSize(1);
        assertThat(addedDefaults.get("b")).isNotNull().isEqualTo("c");
    }

    @Test
    public void testModifiedDefaults() throws Exception {
        Map<String, String> modifiedDefaults = upgradeStep.calculateModifiedServerDefaults(getNewDefaults(),
                getExistingDefaults(), asSet("a"));
        assertThat(modifiedDefaults).isNotNull().hasSize(1);
        assertThat(modifiedDefaults.get("a")).isNotNull().isEqualTo("b");
    }

    @Test
    public void testDeletedDefaults() throws Exception {
        Set<String> deletedDefaults = upgradeStep.calculateDeletedServerDefaults(getExistingDefaults(),
                loadProperties("/validserverconfig.properties"));
        assertThat(deletedDefaults).isNotNull().hasSize(1).contains("x");
    }

    @SuppressWarnings("rawtypes")
    private Map<String, String> loadProperties(String fileName) throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getResourceAsStream(fileName));
        return new HashMap(properties);
    }

    private Map<String, String> getExistingDefaults() throws Exception {
        return loadProperties("/oldserverdefaults.properties");
    }

    private Map<String, String> getNewDefaults() throws Exception {
        return loadProperties("/newserverdefaults.properties");
    }
}
