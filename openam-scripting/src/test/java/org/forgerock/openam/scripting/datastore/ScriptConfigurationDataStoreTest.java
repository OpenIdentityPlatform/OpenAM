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
package org.forgerock.openam.scripting.datastore;

import static org.forgerock.openam.scripting.ScriptConstants.ScriptContext.AUTHORIZATION_ENTITLEMENT_CONDITION;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.*;
import static org.forgerock.openam.scripting.SupportedScriptingLanguage.JAVASCRIPT;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.scripting.ScriptException;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.security.Principal;

public class ScriptConfigurationDataStoreTest {

    private final String TEST_USER = "TestUser";
    private final Principal testUserPrinciple = new Principal() {
        @Override
        public String getName() {
            return TEST_USER;
        }
    };
    private ScriptConfigurationDataStore dataStore;
    private ServiceConfigManager configManager;

    @BeforeMethod
    public void setUp() throws ResourceException {
        Logger logger = mock(Logger.class);
        Subject subject = new Subject();
        subject.getPrincipals().add(testUserPrinciple);
        configManager = mock(ServiceConfigManager.class);
        dataStore = new ScriptConfigurationDataStore(logger, subject, "/") {
            @Override
            protected ServiceConfigManager getConfigManager() throws SSOException, SMSException {
                return configManager;
            }
        };
    }

    @Test
    public void shouldFailIfConfigDoesNotExistOnSave() throws ScriptException {
        // given
        ScriptConfiguration sc = ScriptConfiguration.builder()
                .generateId()
                .setName("MyJavaScript")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(AUTHORIZATION_ENTITLEMENT_CONDITION).build();

        // when
        try {
            dataStore.save(sc);
            fail("shouldFailIfConfigDoesNotExistOnSave");
        } catch (ScriptException e) {
            // then
            assertEquals(e.getScriptErrorCode(), SAVE_FAILED);
        }

    }

    @Test
    public void shouldFailIfConfigDoesNotExistOnDelete() throws ScriptException {
        // given
        String uuid = "1234567890";

        // when
        try {
            dataStore.delete(uuid);
            fail("shouldFailIfConfigDoesNotExistOnDelete");
        } catch (ScriptException e) {
            // then
            assertEquals(e.getScriptErrorCode(), DELETE_FAILED);
        }

    }

    @Test
    public void shouldFailIfConfigDoesNotExistOnGetAll() throws ScriptException {
        // given

        // when
        try {
            dataStore.getAll();
            fail("shouldFailIfConfigDoesNotExistOnGetAll");
        } catch (ScriptException e) {
            // then
            assertEquals(e.getScriptErrorCode(), RETRIEVE_ALL_FAILED);
        }

    }

    @Test
    public void shouldFailIfConfigDoesNotExistOnGet() throws ScriptException {
        // given
        String uuid = "1234567890";

        // when
        try {
            dataStore.get(uuid);
            fail("shouldFailIfConfigDoesNotExistOnGet");
        } catch (ScriptException e) {
            // then
            assertEquals(e.getScriptErrorCode(), RETRIEVE_FAILED);
        }

    }

    @Test
    public void shouldFailIfConfigDoesNotExistOnContainsUuid() throws ScriptException {
        // given
        String uuid = "1234567890";

        // when
        try {
            dataStore.containsUuid(uuid);
            fail("shouldFailIfConfigDoesNotExistOnContainsUuid");
        } catch (ScriptException e) {
            // then
            assertEquals(e.getScriptErrorCode(), FIND_BY_UUID_FAILED);
        }

    }

    @Test
    public void shouldFailIfConfigDoesNotExistOnContainsName() throws ScriptException {
        // given
        String name = "MyJavaScript";

        // when
        try {
            dataStore.containsName(name);
            fail("shouldFailIfConfigDoesNotExistOnContainsName");
        } catch (ScriptException e) {
            // then
            assertEquals(e.getScriptErrorCode(), FIND_BY_NAME_FAILED);
        }

    }

}
