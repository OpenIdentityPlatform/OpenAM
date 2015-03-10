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
package org.forgerock.openam.upgrade.steps.policy;

import static org.forgerock.openam.entitlement.utils.EntitlementUtils.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.upgrade.UpgradeException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UpgradeResourceTypeStepTest {

    private final Document document = mock(Document.class);
    private final Set<String> realms = Collections.singleton("/");
    private final Set<String> policies = new HashSet<String>();
    private final Map<String, Set<String>> appData = new HashMap<String, Set<String>>();
    private final Map<String, Set<String>> appTypeData = new HashMap<String, Set<String>>();

    private PrivilegedAction privilegedAction;
    private ResourceTypeService resourceTypeService;
    private UpgradeResourceTypeStep upgradeResourceTypeStep;
    private ConnectionFactory connectionFactory;
    private ServiceConfigManager configManager;

    @BeforeMethod
    public void setUp() throws Exception {
        privilegedAction = mock(PrivilegedAction.class);
        resourceTypeService = mock(ResourceTypeService.class);
        connectionFactory = mock(ConnectionFactory.class);
        configManager = mock(ServiceConfigManager.class);
        upgradeResourceTypeStep =
                new UpgradeResourceTypeStep(configManager, resourceTypeService, privilegedAction, connectionFactory) {
                    @Override
                    protected Document getEntitlementXML() throws UpgradeException {
                        return document;
                    }

                    @Override
                    protected Set<String> getRealmNamesFromParent() throws UpgradeException {
                        return realms;
                    }

                    @Override
                    protected Set<String> policiesEligibleForUpgrade(String appName, String realm)
                            throws UpgradeException {
                        return policies;
                    }
                };

        when(document.getElementsByTagName(anyString())).thenReturn(new NodeList() {
            @Override
            public Node item(int i) {
                return null;
            }

            @Override
            public int getLength() {
                return 0;
            }
        });

        // Mock global and application type service configuration
        ServiceConfig globalConfig = mock(ServiceConfig.class);
        when(configManager.getGlobalConfig(anyString())).thenReturn(globalConfig);
        ServiceConfig appTypesConfig = mock(ServiceConfig.class);
        when(globalConfig.getSubConfig(anyString())).thenReturn(appTypesConfig);

        // Mock organisation and application service configuration
        ServiceConfig orgConfig = mock(ServiceConfig.class);
        when(configManager.getOrganizationConfig(anyString(), anyString())).thenReturn(orgConfig);
        ServiceConfig appsConfig = mock(ServiceConfig.class);
        when(orgConfig.getSubConfig(anyString())).thenReturn(appsConfig);

        // Mock application names
        when(appsConfig.getSubConfigNames()).thenReturn(Collections.singleton("MyApplication"));

        // Mock application data
        ServiceConfig appConfig = mock(ServiceConfig.class);
        when(appsConfig.getSubConfig("MyApplication")).thenReturn(appConfig);
        when(appConfig.getAttributes()).thenReturn(appData);

        // Mock application type on application and application type data
        ServiceConfig appTypeConfig = mock(ServiceConfig.class);
        when(appTypesConfig.getSubConfig("MyApplicationType")).thenReturn(appTypeConfig);
        when(appTypeConfig.getAttributes()).thenReturn(appTypeData);

        setupDataStructures();
    }

    private void setupDataStructures() {
        policies.clear();
        appData.clear();
        appTypeData.clear();

        appData.put(APPLICATION_TYPE, Collections.singleton("MyApplicationType"));
        appData.put(CONFIG_RESOURCES, Collections.singleton("http://localhost:80/*"));
        appData.put(CONFIG_ACTIONS, Collections.singleton("DELETE"));
    }

    @Test
    public void shouldBeApplicableWhenApplicationHasNoResourceType() throws UpgradeException {
        // given
        setupDataStructures();
        appData.put(CONFIG_RESOURCE_TYPE_UUIDS, null);

        // when
        upgradeResourceTypeStep.initialize();

        // then
        assertEquals(upgradeResourceTypeStep.isApplicable(), true);
    }

    @Test
    public void shouldBeApplicableWhenPolicyHasNoResourceType() throws UpgradeException {
        // given
        setupDataStructures();
        policies.add("PolicyWithoutResourceType");

        // when
        upgradeResourceTypeStep.initialize();

        // then
        assertEquals(upgradeResourceTypeStep.isApplicable(), true);
    }

    @Test
    public void shouldNotBeApplicableWhenApplicationHasResourceType() throws UpgradeException {
        // given
        setupDataStructures();
        appData.put(CONFIG_RESOURCE_TYPE_UUIDS, Collections.singleton("123456789"));

        // when
        upgradeResourceTypeStep.initialize();

        // then
        assertEquals(upgradeResourceTypeStep.isApplicable(), false);
    }

    @Test
    public void shouldReportCorrectAmountOfUpgradableObjects() throws UpgradeException {
        // given
        setupDataStructures();
        appData.put(CONFIG_RESOURCE_TYPE_UUIDS, null);
        policies.add("PolicyWithoutResourceTypeOne");
        policies.add("PolicyWithoutResourceTypeTwo");
        policies.add("PolicyWithoutResourceTypeThree");

        // when
        upgradeResourceTypeStep.initialize();
        String shortReport = upgradeResourceTypeStep.getShortReport("");

        // then
        assertEquals(shortReport, "New entitlement resource types (1)Modified applications (1)Modified policies (3)");
    }

    @Test
    public void shouldReportCorrectUpgradableObjectNames() throws UpgradeException {
        // given
        setupDataStructures();
        appData.put(CONFIG_RESOURCE_TYPE_UUIDS, null);
        policies.add("PolicyWithoutResourceTypeOne");
        policies.add("PolicyWithoutResourceTypeTwo");
        policies.add("PolicyWithoutResourceTypeThree");

        // when
        upgradeResourceTypeStep.initialize();
        String detailedReport = upgradeResourceTypeStep.getDetailedReport("");

        // then
        assertEquals(detailedReport.contains("MyApplication"), true);
        assertEquals(detailedReport.contains("MyApplicationResourceType"), true);
        assertEquals(detailedReport.contains("PolicyWithoutResourceTypeOne"), true);
        assertEquals(detailedReport.contains("PolicyWithoutResourceTypeTwo"), true);
        assertEquals(detailedReport.contains("PolicyWithoutResourceTypeThree"), true);
    }

}
