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
import static org.forgerock.openam.scripting.SupportedScriptingLanguage.GROOVY;
import static org.forgerock.openam.scripting.SupportedScriptingLanguage.JAVASCRIPT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

import org.forgerock.openam.scripting.ScriptException;
import org.testng.annotations.Test;

import java.util.UUID;

public class ScriptConfigurationTest {

    @Test
    public void shouldBuildScriptConfiguration() throws ScriptException {
        //given
        ScriptConfiguration.Builder builder = ScriptConfiguration.builder()
                .generateId()
                .setName("MyJavaScript")
                .setDescription("This is a test script configuration")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION)
                .setCreatedBy("TestUser1")
                .setCreationDate(1422886484092l)
                .setLastModifiedBy("TestUser2")
                .setLastModifiedDate(1422886484098l);

        //when
        ScriptConfiguration scriptConfiguration = builder.build();
        System.out.println(scriptConfiguration.getId());

        //then
        assertEquals(scriptConfiguration.getName(), "MyJavaScript");
        assertEquals(scriptConfiguration.getDescription(), "This is a test script configuration");
        assertEquals(scriptConfiguration.getScript(), "var a = 123;var b = 456;");
        assertEquals(scriptConfiguration.getLanguage(), JAVASCRIPT);
        assertEquals(scriptConfiguration.getContext(), POLICY_CONDITION);
        assertEquals(scriptConfiguration.getCreatedBy(), "TestUser1");
        assertEquals(scriptConfiguration.getCreationDate(), 1422886484092l);
        assertEquals(scriptConfiguration.getLastModifiedBy(), "TestUser2");
        assertEquals(scriptConfiguration.getLastModifiedDate(), 1422886484098l);

    }

    @Test
    public void shouldCreateEqualScriptConfigurations() throws ScriptException {
        //given
        String uuid = UUID.randomUUID().toString();
        ScriptConfiguration.Builder builder1 = ScriptConfiguration.builder()
                .setId(uuid)
                .setName("MyJavaScript")
                .setDescription("This is a test script configuration")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION)
                .setCreatedBy("TestUser1")
                .setCreationDate(1422886484092l)
                .setLastModifiedBy("TestUser2")
                .setLastModifiedDate(1422886484098l);
        ScriptConfiguration.Builder builder2 = ScriptConfiguration.builder()
                .setId(uuid)
                .setName("MyJavaScript")
                .setDescription("This is a test script configuration")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION)
                .setCreatedBy("TestUser1")
                .setCreationDate(1422886484092l)
                .setLastModifiedBy("TestUser2")
                .setLastModifiedDate(1422886484098l);

        //when
        ScriptConfiguration rt1 = builder1.build();
        ScriptConfiguration rt2 = builder2.build();

        //then
        assertEquals(rt1, rt2);
        assertEquals(rt1.hashCode(), rt2.hashCode());

    }

    @Test
    public void shouldCreateDifferentScriptConfigurations() throws ScriptException {
        //given
        ScriptConfiguration.Builder builder1 = ScriptConfiguration.builder()
                .generateId()
                .setName("MyJavaScript")
                .setDescription("This is a test script configuration")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION)
                .setCreatedBy("TestUser1")
                .setCreationDate(1422886484092l)
                .setLastModifiedBy("TestUser2")
                .setLastModifiedDate(1422886484098l);
        ScriptConfiguration.Builder builder2 = ScriptConfiguration.builder()
                .generateId()
                .setName("MyGroovyScript")
                .setDescription("This is a test script configuration")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(GROOVY)
                .setContext(POLICY_CONDITION)
                .setCreatedBy("TestUser1")
                .setCreationDate(1422886484092l)
                .setLastModifiedBy("TestUser2")
                .setLastModifiedDate(1422886484098l);

        //when
        ScriptConfiguration rt1 = builder1.build();
        ScriptConfiguration rt2 = builder2.build();

        //then
        assertNotEquals(rt1, rt2);
        assertNotEquals(rt1.hashCode(), rt2.hashCode());

    }

    @Test
    public void shouldCreateEditableBuilderForMetaData() throws ScriptException {
        //given
        ScriptConfiguration rt1 = ScriptConfiguration.builder()
                .generateId()
                .setName("MyJavaScript")
                .setDescription("This is a test script configuration")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION)
                .setCreatedBy("TestUser1")
                .setCreationDate(1422886484092l)
                .setLastModifiedBy("TestUser2")
                .setLastModifiedDate(1422886484098l).build();

        //when
        ScriptConfiguration.Builder builder1 = rt1.populatedBuilder();
        ScriptConfiguration rt2 = builder1.setCreatedBy("TestUser3")
                .setCreationDate(1422886484093l)
                .setLastModifiedBy("TestUser4")
                .setLastModifiedDate(1422886484099l).build();

        //then
        assertEquals(rt2.getCreatedBy(), "TestUser3");
        assertEquals(rt2.getCreationDate(), 1422886484093l);
        assertEquals(rt2.getLastModifiedBy(), "TestUser4");
        assertEquals(rt2.getLastModifiedDate(), 1422886484099l);

    }

    @Test
    public void shouldCreateBuilderForClone() throws ScriptException {
        //given
        ScriptConfiguration rt1 = ScriptConfiguration.builder()
                .generateId()
                .setName("MyJavaScript")
                .setDescription("This is a test script configuration")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION)
                .setCreatedBy("TestUser1")
                .setCreationDate(1422886484092l)
                .setLastModifiedBy("TestUser2")
                .setLastModifiedDate(1422886484098l).build();

        //when
        ScriptConfiguration rt2 = rt1.populatedBuilder().build();

        //then
        assertEquals(rt1, rt2);
        assertEquals(rt1.hashCode(), rt2.hashCode());

    }

    @Test(expectedExceptions = ScriptException.class)
    public void shouldNotAllowNullUUID() throws ScriptException {
        //given
        ScriptConfiguration.Builder builder = ScriptConfiguration.builder()
                .setName("MyJavaScript")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION);

        //when
        builder.build();

    }

    @Test(expectedExceptions = ScriptException.class)
    public void shouldNotAllowNullName() throws ScriptException {
        //given
        ScriptConfiguration.Builder builder = ScriptConfiguration.builder()
                .generateId()
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION);

        //when
        builder.build();

    }

    @Test(expectedExceptions = ScriptException.class)
    public void shouldNotAllowNullScript() throws ScriptException {
        //given
        ScriptConfiguration.Builder builder = ScriptConfiguration.builder()
                .generateId()
                .setName("MyJavaScript")
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION);

        //when
        builder.build();

    }

    @Test(expectedExceptions = ScriptException.class)
    public void shouldNotAllowNullLanguage() throws ScriptException {
        //given
        ScriptConfiguration.Builder builder = ScriptConfiguration.builder()
                .generateId()
                .setName("MyJavaScript")
                .setScript("var a = 123;var b = 456;")
                .setContext(POLICY_CONDITION);

        //when
        builder.build();

    }

    @Test(expectedExceptions = ScriptException.class)
    public void shouldNotAllowNullContext() throws ScriptException {
        //given
        ScriptConfiguration.Builder builder = ScriptConfiguration.builder()
                .generateId()
                .setName("MyJavaScript")
                .setScript("var a = 123;var b = 456;")
                .setLanguage(JAVASCRIPT);

        //when
        builder.build();

    }
}
