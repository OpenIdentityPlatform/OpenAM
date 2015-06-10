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
package org.forgerock.openam.scripting.service;

import static org.forgerock.openam.scripting.ScriptConstants.ScriptContext.POLICY_CONDITION;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.*;
import static org.forgerock.openam.scripting.SupportedScriptingLanguage.JAVASCRIPT;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.scripting.ScriptException;
import org.forgerock.openam.scripting.datastore.ScriptingDataStore;
import org.forgerock.openam.scripting.datastore.ScriptingDataStoreFactory;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.security.Principal;

public class ScriptConfigurationServiceTest {

    private final String TEST_USER = "TestUser";
    private final Principal testUserPrinciple = new Principal() {
        @Override
        public String getName() {
            return TEST_USER;
        }
    };
    private ScriptConfigurationService service;
    private ScriptingDataStore<ScriptConfiguration> dataStore;

    @BeforeMethod
    public void setUp() throws ResourceException {
        Logger logger = mock(Logger.class);
        Subject subject = new Subject();
        subject.getPrincipals().add(testUserPrinciple);
        dataStore = mock(ScriptingDataStore.class);
        ScriptingDataStoreFactory<ScriptConfiguration> dataStoreFactory = mock(ScriptingDataStoreFactory.class);
        when(dataStoreFactory.create(any(Subject.class), anyString())).thenReturn(dataStore);
        service = new ScriptConfigurationService(logger, subject, "/", dataStoreFactory);
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
        when(dataStore.containsUuid(anyString())).thenReturn(false);
        when(dataStore.containsName(anyString())).thenReturn(false);

        // when
        sc = service.create(sc);

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
                .setName("MyJavaScript")
                .setDescription("This is a test script configuration")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION).build();
        when(dataStore.containsUuid(anyString())).thenReturn(false);
        when(dataStore.containsName(anyString())).thenReturn(true);

        // when
        try {
            service.create(sc);
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
                .generateId()
                .setName("MyJavaScript")
                .setDescription("This is a test script configuration")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION).build();
        when(dataStore.containsUuid(anyString())).thenReturn(true);
        when(dataStore.containsName(anyString())).thenReturn(false);

        // when
        try {
            service.create(sc);
            fail("shouldFailIfUuidExistsOnCreate");
        } catch (ScriptException e) {
            // then
            assertEquals(e.getScriptErrorCode(), SCRIPT_UUID_EXISTS);
        }

    }

    @Test
    public void shouldFailIfUuidDoesNotExistOnDelete() throws ScriptException {
        // given
        String uuid = "1234567890";
        when(dataStore.containsUuid(anyString())).thenReturn(false);

        // when
        try {
            service.delete(uuid);
            fail("shouldFailIfUuidDoesNotExistOnDelete");
        } catch (ScriptException e) {
            // then
            assertEquals(e.getScriptErrorCode(), SCRIPT_UUID_NOT_FOUND);
        }

    }

    @Test
    public void shouldFailIfUuidDoesNotExistOnGet() throws ScriptException {
        // given
        String uuid = "1234567890";
        when(dataStore.containsUuid(anyString())).thenReturn(false);

        // when
        try {
            service.delete(uuid);
            fail("shouldFailIfUuidDoesNotExistOnGet");
        } catch (ScriptException e) {
            // then
            assertEquals(e.getScriptErrorCode(), SCRIPT_UUID_NOT_FOUND);
        }

    }

    @Test
    public void shouldModifyMetaDataOnUpdate() throws ScriptException {
        // given
        ScriptConfiguration scOld = ScriptConfiguration.builder()
                .generateId()
                .setName("MyJavaScript")
                .setDescription("This is a test script configuration")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION)
                .setCreatedBy("TestUser")
                .setCreationDate(1422886484092l).build();
        ScriptConfiguration scNew = ScriptConfiguration.builder()
                .generateId()
                .setName("MyJavaScript")
                .setDescription("This is a test script configuration")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION).build();

        when(dataStore.get(anyString())).thenReturn(scOld);
        when(dataStore.containsUuid(anyString())).thenReturn(true);

        // when
        scNew = service.update(scNew);

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

        when(dataStore.containsUuid(anyString())).thenReturn(false);

        // when
        try {
            service.update(scNew);
            fail("shouldFailIfUuidDoesNotExistOnUpdate");
        } catch (ScriptException e) {
            // then
            assertEquals(e.getScriptErrorCode(), SCRIPT_UUID_NOT_FOUND);
        }
    }

    @Test
    public void shouldFailIfNameExistOnUpdate() throws ScriptException {
        // given
        ScriptConfiguration scOld = ScriptConfiguration.builder()
                .generateId()
                .setName("MyJavaScript")
                .setDescription("This is a test script configuration")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION)
                .setCreatedBy("TestUser")
                .setCreationDate(1422886484092l).build();
        ScriptConfiguration scNew = ScriptConfiguration.builder()
                .generateId()
                .setName("NewNameForMyJavaScript")
                .setDescription("This is a test script configuration")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION).build();

        when(dataStore.get(anyString())).thenReturn(scOld);
        when(dataStore.containsUuid(anyString())).thenReturn(true);
        when(dataStore.containsName(anyString())).thenReturn(true);

        // when
        try {
            service.update(scNew);
            fail("shouldFailIfNameExistOnUpdate");
        } catch (ScriptException e) {
            // then
            assertEquals(e.getScriptErrorCode(), SCRIPT_NAME_EXISTS);
        }
    }
}
