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

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.util.promise.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit test for {@link org.forgerock.openam.upgrade.steps.DelegationConfigUpgradeStep}
 *
 * @since 12.0.0
 */
public class DelegationConfigUpgradeStepTest {

    private DelegationConfigUpgradeStep step;

    @Mock
    private ServiceConfigManager configManager;
    @Mock
    private ServiceConfig delegationConfig;
    @Mock
    private ServiceConfig permissions;
    @Mock
    private ServiceConfig privileges;
    @Mock
    private ServiceConfig privilege1;
    @Mock
    private ServiceConfig privilege2;

    @Mock
    private Function<String, String, NeverThrowsException> tagSwapFunc;
    @Mock
    private PrivilegedAction<SSOToken> adminTokenAction;
    @Mock
    private ConnectionFactory connectionFactory;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        step = new SafeDelegationConfigUpgradeStep(configManager, tagSwapFunc, adminTokenAction, connectionFactory);
    }

    @Test
    public void noChangesToProcess() throws UpgradeException, SSOException, SMSException {
        // Given...
        given(configManager.getGlobalConfig(null)).willReturn(delegationConfig);
        given(delegationConfig.getSubConfig("Permissions")).willReturn(permissions);
        given(delegationConfig.getSubConfig("Privileges")).willReturn(privileges);

        given(permissions.getSubConfig("TestPerm1")).willReturn(mock(ServiceConfig.class));
        given(permissions.getSubConfig("TestPerm2")).willReturn(mock(ServiceConfig.class));

        final Map<String, Set<String>> privilege1Attributes = new HashMap<String, Set<String>>();
        privilege1Attributes.put("listOfPermissions", new HashSet<String>(Arrays.asList("TestPerm1", "TestPerm2")));
        final Map<String, Set<String>> privilege2Attributes = new HashMap<String, Set<String>>();
        privilege2Attributes.put("listOfPermissions", new HashSet<String>(Arrays.asList("TestPerm2")));

        given(privilege1.getAttributes()).willReturn(privilege1Attributes);
        given(privilege2.getAttributes()).willReturn(privilege2Attributes);
        given(privileges.getSubConfig("TestPrivilege1")).willReturn(privilege1);
        given(privileges.getSubConfig("TestPrivilege2")).willReturn(privilege2);

        // When...
        step.initialize();

        // Then...
        assertThat(step.isApplicable()).isFalse();
        verify(configManager).getGlobalConfig(null);
        verify(delegationConfig).getSubConfig("Permissions");
        verify(delegationConfig).getSubConfig("Privileges");
        verify(permissions).getSubConfig("TestPerm1");
        verify(permissions).getSubConfig("TestPerm2");
        verify(privileges).getSubConfig("TestPrivilege1");
        verify(privileges).getSubConfig("TestPrivilege2");
        verify(privilege1).getAttributes();
        verify(privilege2).getAttributes();
        verifyNoMoreInteractions(configManager, delegationConfig, permissions, privileges,
                privilege1, privilege2, tagSwapFunc, adminTokenAction, connectionFactory);
    }

    @Test
    public void processNewPermissionsOnly() throws UpgradeException, SSOException, SMSException {
        // Given...
        given(configManager.getGlobalConfig(null)).willReturn(delegationConfig);
        given(delegationConfig.getSubConfig("Permissions")).willReturn(permissions);
        given(delegationConfig.getSubConfig("Privileges")).willReturn(privileges);

        given(permissions.getSubConfig("TestPerm1")).willReturn(mock(ServiceConfig.class));
        // Return of null implies a new entry.
        given(permissions.getSubConfig("TestPerm2")).willReturn(null);

        final Map<String, Set<String>> privilege1Attributes = new HashMap<String, Set<String>>();
        privilege1Attributes.put("listOfPermissions", new HashSet<String>(Arrays.asList("TestPerm1", "TestPerm2")));
        final Map<String, Set<String>> privilege2Attributes = new HashMap<String, Set<String>>();
        privilege2Attributes.put("listOfPermissions", new HashSet<String>(Arrays.asList("TestPerm2")));

        given(privilege1.getAttributes()).willReturn(privilege1Attributes);
        given(privilege2.getAttributes()).willReturn(privilege2Attributes);
        given(privileges.getSubConfig("TestPrivilege1")).willReturn(privilege1);
        given(privileges.getSubConfig("TestPrivilege2")).willReturn(privilege2);

        given(tagSwapFunc.apply("/test/perm/2")).willReturn("/swapped/test/perm/2");

        // When...
        step.initialize();
        // Before performing the upgrade verify there are changes to be made.
        assertThat(step.isApplicable()).isTrue();
        step.perform();

        // Then...
        verify(configManager, times(2)).getGlobalConfig(null);
        verify(delegationConfig, times(2)).getSubConfig("Permissions");
        verify(delegationConfig, times(2)).getSubConfig("Privileges");
        verify(permissions).getSubConfig("TestPerm1");
        verify(permissions).getSubConfig("TestPerm2");
        verify(privileges).getSubConfig("TestPrivilege1");
        verify(privileges).getSubConfig("TestPrivilege2");
        verify(privilege1).getAttributes();
        verify(privilege2).getAttributes();
        verify(tagSwapFunc).apply("/test/perm/2");

        final Map<String, Set<String>> permissionAttributes = new HashMap<String, Set<String>>();
        permissionAttributes.put("resource", new HashSet<String>(Arrays.asList("/swapped/test/perm/2")));
        permissionAttributes.put("actions", new HashSet<String>(Arrays.asList("READ")));
        verify(permissions).addSubConfig(eq("TestPerm2"), eq("Permission"), eq(0), eq(permissionAttributes));

        verifyNoMoreInteractions(configManager, delegationConfig, permissions, privileges,
                privilege1, privilege2, tagSwapFunc, adminTokenAction, connectionFactory);

    }

    @Test
    public void processNewPrivilegeOnly() throws UpgradeException, SSOException, SMSException {
        // Given...
        given(configManager.getGlobalConfig(null)).willReturn(delegationConfig);
        given(delegationConfig.getSubConfig("Permissions")).willReturn(permissions);
        given(delegationConfig.getSubConfig("Privileges")).willReturn(privileges);

        given(permissions.getSubConfig("TestPerm1")).willReturn(mock(ServiceConfig.class));
        given(permissions.getSubConfig("TestPerm2")).willReturn(mock(ServiceConfig.class));

        final Map<String, Set<String>> privilege1Attributes = new HashMap<String, Set<String>>();
        privilege1Attributes.put("listOfPermissions", new HashSet<String>(Arrays.asList("TestPerm1", "TestPerm2")));

        given(privilege1.getAttributes()).willReturn(privilege1Attributes);
        given(privileges.getSubConfig("TestPrivilege1")).willReturn(privilege1);
        // Return of null implies a new entry.
        given(privileges.getSubConfig("TestPrivilege2")).willReturn(null);

        // When...
        step.initialize();
        // Before performing the upgrade verify there are changes to be made.
        assertThat(step.isApplicable()).isTrue();
        step.perform();

        // Then...
        verify(configManager, times(2)).getGlobalConfig(null);
        verify(delegationConfig, times(2)).getSubConfig("Permissions");
        verify(delegationConfig, times(2)).getSubConfig("Privileges");
        verify(permissions).getSubConfig("TestPerm1");
        verify(permissions).getSubConfig("TestPerm2");
        verify(privileges).getSubConfig("TestPrivilege1");
        verify(privileges).getSubConfig("TestPrivilege2");
        verify(privilege1).getAttributes();

        final Map<String, Set<String>> privilege2Attributes = new HashMap<String, Set<String>>();
        privilege2Attributes.put("listOfPermissions", new HashSet<String>(Arrays.asList("TestPerm2")));
        verify(privileges).addSubConfig(eq("TestPrivilege2"), eq("Privilege"), eq(0), eq(privilege2Attributes));

        verifyNoMoreInteractions(configManager, delegationConfig, permissions, privileges,
                privilege1, privilege2, tagSwapFunc, adminTokenAction, connectionFactory);

    }

    @Test
    public void processPrivilegeUpdatesOnly() throws UpgradeException, SSOException, SMSException {
        // Given...
        given(configManager.getGlobalConfig(null)).willReturn(delegationConfig);
        given(delegationConfig.getSubConfig("Permissions")).willReturn(permissions);
        given(delegationConfig.getSubConfig("Privileges")).willReturn(privileges);

        given(permissions.getSubConfig("TestPerm1")).willReturn(mock(ServiceConfig.class));
        given(permissions.getSubConfig("TestPerm2")).willReturn(mock(ServiceConfig.class));

        final Map<String, Set<String>> privilege1Attributes = new HashMap<String, Set<String>>();
        privilege1Attributes.put("listOfPermissions", new HashSet<String>(Arrays.asList("TestPerm1")));
        final Map<String, Set<String>> privilege2Attributes = new HashMap<String, Set<String>>();
        privilege2Attributes.put("listOfPermissions", new HashSet<String>(Arrays.asList("TestPerm2")));

        given(privilege1.getAttributes()).willReturn(privilege1Attributes);
        given(privilege2.getAttributes()).willReturn(privilege2Attributes);
        given(privileges.getSubConfig("TestPrivilege1")).willReturn(privilege1);
        given(privileges.getSubConfig("TestPrivilege2")).willReturn(privilege2);

        given(privileges.getSubConfig("TestPrivilege1")).willReturn(privilege1);

        // When...
        step.initialize();
        // Before performing the upgrade verify there are changes to be made.
        assertThat(step.isApplicable()).isTrue();
        step.perform();

        // Then...
        verify(configManager, times(2)).getGlobalConfig(null);
        verify(delegationConfig, times(2)).getSubConfig("Permissions");
        verify(delegationConfig, times(2)).getSubConfig("Privileges");
        verify(permissions).getSubConfig("TestPerm1");
        verify(permissions).getSubConfig("TestPerm2");
        verify(privileges, times(2)).getSubConfig("TestPrivilege1");
        verify(privileges).getSubConfig("TestPrivilege2");
        verify(privilege1).getAttributes();
        verify(privilege2).getAttributes();
        verify(privilege1).addAttribute("listOfPermissions", new HashSet<String>(Arrays.asList("TestPerm2")));
        verifyNoMoreInteractions(configManager, delegationConfig, permissions, privileges,
                privilege1, privilege2, tagSwapFunc, adminTokenAction, connectionFactory);
    }

    @Test
    public void processPermissionAndPrivilegeChanges() throws UpgradeException, SSOException, SMSException {
        // Given...
        given(configManager.getGlobalConfig(null)).willReturn(delegationConfig);
        given(delegationConfig.getSubConfig("Permissions")).willReturn(permissions);
        given(delegationConfig.getSubConfig("Privileges")).willReturn(privileges);

        given(permissions.getSubConfig("TestPerm1")).willReturn(mock(ServiceConfig.class));
        // Return of null implies a new entry.
        given(permissions.getSubConfig("TestPerm2")).willReturn(null);

        final Map<String, Set<String>> privilege1Attributes = new HashMap<String, Set<String>>();
        privilege1Attributes.put("listOfPermissions", new HashSet<String>(Arrays.asList("TestPerm1")));

        given(privilege1.getAttributes()).willReturn(privilege1Attributes);
        given(privileges.getSubConfig("TestPrivilege1")).willReturn(privilege1);
        // Return of null implies a new entry.
        given(privileges.getSubConfig("TestPrivilege2")).willReturn(null);

        given(tagSwapFunc.apply("/test/perm/2")).willReturn("/swapped/test/perm/2");
        given(privileges.getSubConfig("TestPrivilege1")).willReturn(privilege1);

        // When...
        step.initialize();
        // Before performing the upgrade verify there are changes to be made.
        assertThat(step.isApplicable()).isTrue();
        step.perform();

        // Then...
        verify(configManager, times(2)).getGlobalConfig(null);
        verify(delegationConfig, times(2)).getSubConfig("Permissions");
        verify(delegationConfig, times(2)).getSubConfig("Privileges");
        verify(permissions).getSubConfig("TestPerm1");
        verify(permissions).getSubConfig("TestPerm2");
        verify(privileges, times(2)).getSubConfig("TestPrivilege1");
        verify(privileges).getSubConfig("TestPrivilege2");
        verify(privilege1).getAttributes();
        verify(tagSwapFunc).apply("/test/perm/2");

        // Creates new permission.
        final Map<String, Set<String>> permissionAttributes = new HashMap<String, Set<String>>();
        permissionAttributes.put("resource", new HashSet<String>(Arrays.asList("/swapped/test/perm/2")));
        permissionAttributes.put("actions", new HashSet<String>(Arrays.asList("READ")));
        verify(permissions).addSubConfig(eq("TestPerm2"), eq("Permission"), eq(0), eq(permissionAttributes));

        // Creates new privilege.
        final Map<String, Set<String>> privilege2Attributes = new HashMap<String, Set<String>>();
        privilege2Attributes.put("listOfPermissions", new HashSet<String>(Arrays.asList("TestPerm2")));
        verify(privileges).addSubConfig(eq("TestPrivilege2"), eq("Privilege"), eq(0), eq(privilege2Attributes));

        // Updates existing privilege.
        verify(privilege1).addAttribute("listOfPermissions", new HashSet<String>(Arrays.asList("TestPerm2")));

        verifyNoMoreInteractions(configManager, delegationConfig, permissions, privileges,
                privilege1, privilege2, tagSwapFunc, adminTokenAction, connectionFactory);
    }

    private static final class SafeDelegationConfigUpgradeStep extends DelegationConfigUpgradeStep {

        public SafeDelegationConfigUpgradeStep(ServiceConfigManager configManager,
                                               Function<String, String, NeverThrowsException> tagSwapFunc,
                                               PrivilegedAction<SSOToken> adminTokenAction,
                                               ConnectionFactory connectionFactory) {
            super(configManager, tagSwapFunc, adminTokenAction, connectionFactory);
        }

        @Override
        protected Document getDelegationDocument() throws UpgradeException {
            try {
                return XMLUtils.getXMLDocument(ClassLoader.getSystemResourceAsStream("test-delegation.xml"));
            } catch (Exception e) {
                throw new UpgradeException(e);
            }
        }

    }

}