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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.scripting.service;

import static org.forgerock.openam.scripting.ScriptConstants.ScriptContext.POLICY_CONDITION;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.*;
import static org.forgerock.openam.scripting.SupportedScriptingLanguage.JAVASCRIPT;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.scripting.ScriptException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

public class ScriptConfigurationServiceTest {

    private final String TEST_USER = "TestUser";
    private final Principal testUserPrinciple = new Principal() {
        @Override
        public String getName() {
            return TEST_USER;
        }
    };
    private Subject subject;
    private ScriptConfigurationService service;
    @Mock
    private ServiceConfigManager serviceConfigManager;
    @Mock
    private ServiceConfig orgConfig;
    @Mock
    private ServiceConfig orgSubConfig;
    @Mock
    private ServiceConfig globalConfig;
    @Mock
    private ServiceConfig globalSubConfig;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Logger logger = mock(Logger.class);
        subject = new Subject();
        subject.getPrincipals().add(testUserPrinciple);
        CoreWrapper coreWrapper = mock(CoreWrapper.class);
        when(coreWrapper.getServiceConfigManager(anyString(), any(SSOToken.class))).thenReturn(serviceConfigManager);

        when(serviceConfigManager.getOrganizationConfig("/", null)).thenReturn(orgConfig);
        when(orgConfig.getSubConfig(anyString())).thenReturn(orgSubConfig);
        when(orgSubConfig.getSubConfigNames()).thenReturn(asSet("ExistingOrgScript"));
        ServiceConfig orgScript = mock(ServiceConfig.class);
        when(orgSubConfig.getSubConfig("ExistingOrgScript")).thenReturn(orgScript);
        when(orgScript.getAttributesForRead()).thenReturn(scriptAttributes("Existing Org Script"));

        when(serviceConfigManager.getGlobalConfig(null)).thenReturn(globalConfig);
        when(globalConfig.getSubConfig(anyString())).thenReturn(globalSubConfig);
        when(globalSubConfig.getSubConfigNames()).thenReturn(asSet("ExistingGlobalScript"));
        ServiceConfig globalScript = mock(ServiceConfig.class);
        when(globalSubConfig.getSubConfig("ExistingGlobalScript")).thenReturn(globalScript);
        when(globalScript.getAttributesForRead()).thenReturn(scriptAttributes("Existing Global Script"));

        service = new ScriptConfigurationService(logger, "/", coreWrapper, serviceConfigManager);
    }

    private Map<String, Set<String>> scriptAttributes(String name) {
        Map<String, Set<String>> result = new HashMap<>();
        result.put("name", asSet(name));
        result.put("description", asSet("This is a test script configuration"));
        result.put("script", asSet("var a = 123;var b = 456;"));
        result.put("language", asSet((JAVASCRIPT.name())));
        result.put("context", asSet(POLICY_CONDITION.name()));
        result.put("createdBy", asSet("TestUser"));
        result.put("creationDate", asSet("1422886484092"));
        return result;
    }

    @Test
    public void shouldModifyMetaDataOnCreate() throws ScriptException {
        // given
        ScriptConfiguration sc = ScriptConfiguration.builder()
                .generateId()
                .setName("MyJavaScript")
                .setDescription("This is a test script configuration")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION).build();

        // when
        sc = service.create(sc, subject);

        // then
        assertNotNull(sc.getCreatedBy());
        assertEquals(sc.getCreatedBy(), TEST_USER);
        assertNotNull(sc.getCreationDate());
        assertNotEquals(sc.getCreationDate(), 0);
        assertNotNull(sc.getLastModifiedBy());
        assertEquals(sc.getLastModifiedBy(), TEST_USER);
        assertNotNull(sc.getLastModifiedDate());
        assertNotEquals(sc.getLastModifiedDate(), 0);
    }

    @Test
    public void shouldFailIfNameExistsOnCreate() throws ScriptException {
        // given
        ScriptConfiguration sc = ScriptConfiguration.builder()
                .generateId()
                .setName("Existing Org Script")
                .setDescription("This is a test script configuration")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION).build();

        // when
        try {
            service.create(sc, subject);
            fail("shouldFailIfNameExistsOnCreate");
        } catch (ScriptException e) {
            // then
            assertEquals(e.getScriptErrorCode(), SCRIPT_NAME_EXISTS);
        }

    }

    @Test
    public void shouldFailIfUuidExistsOnCreate() throws ScriptException {
        // given
        ScriptConfiguration sc = ScriptConfiguration.builder()
                .setId("ExistingOrgScript")
                .setName("MyJavaScript")
                .setDescription("This is a test script configuration")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION).build();

        // when
        try {
            service.create(sc, subject);
            fail("shouldFailIfUuidExistsOnCreate");
        } catch (ScriptException e) {
            // then
            assertEquals(e.getScriptErrorCode(), SCRIPT_UUID_EXISTS);
        }

    }

    @Test
    public void shouldFailIfUuidDoesNotExistOnDelete() throws ScriptException {
        // when
        try {
            service.delete("1234567890");
            fail("shouldFailIfUuidDoesNotExistOnDelete");
        } catch (ScriptException e) {
            // then
            assertEquals(e.getScriptErrorCode(), SCRIPT_UUID_NOT_FOUND);
        }

    }

    @Test
    public void shouldFailIfUuidDoesNotExistOnGet() throws ScriptException {
        // when
        try {
            service.get("1234567890");
            fail("shouldFailIfUuidDoesNotExistOnGet");
        } catch (ScriptException e) {
            // then
            assertEquals(e.getScriptErrorCode(), SCRIPT_UUID_NOT_FOUND);
        }

    }

    @Test
    public void shouldModifyMetaDataOnUpdate() throws ScriptException {
        // given
        ScriptConfiguration scNew = ScriptConfiguration.builder()
                .setId("ExistingOrgScript")
                .setName("MyJavaScript")
                .setDescription("This is a test script configuration")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION).build();

        // when
        scNew = service.update(scNew, subject);

        // then
        assertNotNull(scNew.getCreatedBy());
        assertEquals(scNew.getCreatedBy(), TEST_USER);
        assertNotNull(scNew.getCreationDate());
        assertNotEquals(scNew.getCreationDate(), 0);
        assertNotNull(scNew.getLastModifiedBy());
        assertEquals(scNew.getLastModifiedBy(), TEST_USER);
        assertNotNull(scNew.getLastModifiedDate());
        assertNotEquals(scNew.getLastModifiedDate(), 0);
    }

    @Test
    public void shouldFailIfUuidDoesNotExistOnUpdate() throws ScriptException {
        // given
        ScriptConfiguration scNew = ScriptConfiguration.builder()
                .generateId()
                .setName("MyJavaScript")
                .setDescription("This is a test script configuration")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION).build();

        // when
        try {
            service.update(scNew, subject);
            fail("shouldFailIfUuidDoesNotExistOnUpdate");
        } catch (ScriptException e) {
            // then
            assertEquals(e.getScriptErrorCode(), SCRIPT_UUID_NOT_FOUND);
        }
    }

    @Test
    public void shouldFailIfNameExistOnUpdate() throws ScriptException {
        // given
        ScriptConfiguration scNew = ScriptConfiguration.builder()
                .setId("ExistingOrgScript")
                .setName("Existing Global Script")
                .setDescription("This is a test script configuration")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION).build();

        // when
        try {
            service.update(scNew, subject);
            fail("shouldFailIfNameExistOnUpdate");
        } catch (ScriptException e) {
            // then
            assertEquals(e.getScriptErrorCode(), SCRIPT_NAME_EXISTS);
        }
    }
}
