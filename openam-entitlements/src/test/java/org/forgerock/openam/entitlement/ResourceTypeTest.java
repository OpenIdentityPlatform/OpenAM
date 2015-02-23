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
package org.forgerock.openam.entitlement;

import org.testng.annotations.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class ResourceTypeTest {

    @Test
    public void shouldBuildResourceType() {
        //given
        ResourceType.Builder builder = ResourceType.builder("URL", "/testRealm").generateUUID()
                .setDescription("This is a URL resource type")
                .addPattern("*://*:*/*")
                .addPattern("*://*:*/*?*")
                .addAction("GET", true)
                .addAction("PUT", false)
                .setCreatedBy("TestUser1")
                .setCreationDate(1422886484092l)
                .setLastModifiedBy("TestUser2")
                .setLastModifiedDate(1422886484098l);

        //when
        ResourceType resourceType = builder.build();

        //then
        assertEquals(resourceType.getName(), "URL");
        assertEquals(resourceType.getRealm(), "/testRealm");
        assertEquals(resourceType.getDescription(), "This is a URL resource type");
        assertEquals(resourceType.getPatterns().contains("*://*:*/*"), true);
        assertEquals(resourceType.getPatterns().contains("*://*:*/*?*"), true);
        assertEquals(resourceType.getActions().containsKey("GET"), true);
        assertEquals(resourceType.getActions().get("GET"), Boolean.TRUE);
        assertEquals(resourceType.getActions().containsKey("PUT"), true);
        assertEquals(resourceType.getActions().get("PUT"), Boolean.FALSE);
        assertEquals(resourceType.getCreatedBy(), "TestUser1");
        assertEquals(resourceType.getCreationDate(), 1422886484092l);
        assertEquals(resourceType.getLastModifiedBy(), "TestUser2");
        assertEquals(resourceType.getLastModifiedDate(), 1422886484098l);

    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void shouldNotAllowPatternSetModification() {
        //given
        ResourceType resourceType = ResourceType.builder("URL", "/testRealm").generateUUID()
                .addPattern("*://*:*/*")
                .addPattern("*://*:*/*?*")
                .build();

        //when
        Set<String> patterns = resourceType.getPatterns();
        patterns.add("*.*.*");

    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void shouldNotAllowActionMapModification() {
        //given
        ResourceType resourceType = ResourceType.builder("URL", "/testRealm").generateUUID()
                .addAction("GET", true)
                .addAction("PUT", false)
                .build();

        //when
        Map<String, Boolean> actions = resourceType.getActions();
        actions.put("POST", true);

    }

    @Test
    public void shouldCreateEqualResourceTypes() {
        //given
        String uuid = UUID.randomUUID().toString();
        ResourceType.Builder builder1 = ResourceType.builder("URL", "/testRealm").setUUID(uuid)
                .setDescription("This is a URL resource type")
                .addPattern("*://*:*/*")
                .addPattern("*://*:*/*?*")
                .addAction("GET", true)
                .addAction("PUT", false)
                .setCreatedBy("TestUser1")
                .setCreationDate(1422886484092l)
                .setLastModifiedBy("TestUser2")
                .setLastModifiedDate(1422886484098l);
        ResourceType.Builder builder2 = ResourceType.builder("URL", "/testRealm").setUUID(uuid)
                .setDescription("This is a URL resource type")
                .addPattern("*://*:*/*")
                .addPattern("*://*:*/*?*")
                .addAction("GET", true)
                .addAction("PUT", false)
                .setCreatedBy("TestUser1")
                .setCreationDate(1422886484092l)
                .setLastModifiedBy("TestUser2")
                .setLastModifiedDate(1422886484098l);

        //when
        ResourceType rt1 = builder1.build();
        ResourceType rt2 = builder2.build();

        //then
        assertEquals(rt1, rt2);
        assertEquals(rt1.hashCode(), rt2.hashCode());

    }

    @Test
    public void shouldCreateDifferentResourceTypes() {
        //given
        ResourceType.Builder builder1 = ResourceType.builder("URL", "/testRealm").generateUUID()
                .setDescription("This is a URL resource type")
                .addPattern("*://*:*/realm1")
                .addPattern("*://*:*/*?realm1")
                .addAction("GET", true)
                .addAction("PUT", false)
                .setCreatedBy("TestUser1")
                .setCreationDate(1422886484092l)
                .setLastModifiedBy("TestUser2")
                .setLastModifiedDate(1422886484098l);
        ResourceType.Builder builder2 = ResourceType.builder("URL", "/testRealm").generateUUID()
                .setDescription("This is a URL resource type")
                .addPattern("*://*:*/realm2")
                .addPattern("*://*:*/*?realm2")
                .addAction("GET", false)
                .addAction("PUT", true)
                .setCreatedBy("TestUser3")
                .setCreationDate(1422886484093l)
                .setLastModifiedBy("TestUser4")
                .setLastModifiedDate(1422886484099l);

        //when
        ResourceType rt1 = builder1.build();
        ResourceType rt2 = builder2.build();

        //then
        assertNotEquals(rt1, rt2);
        assertNotEquals(rt1.hashCode(), rt2.hashCode());

    }

    @Test
    public void shouldCreateEditableBuilderForMetaData() {
        //given
        ResourceType rt1 = ResourceType.builder("URL", "/testRealm").generateUUID()
                .setDescription("This is a URL resource type")
                .addPattern("*://*:*/realm1")
                .addPattern("*://*:*/*?realm1")
                .addAction("GET", true)
                .addAction("PUT", false)
                .setCreatedBy("TestUser1")
                .setCreationDate(1422886484092l)
                .setLastModifiedBy("TestUser2")
                .setLastModifiedDate(1422886484098l).build();

        //when
        ResourceType.Builder builder1 = rt1.builder();
        ResourceType rt2 = builder1.setCreatedBy("TestUser3")
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
    public void shouldCreateBuilderForClone() {
        //given
        ResourceType rt1 = ResourceType.builder("URL", "/testRealm").generateUUID()
                .setDescription("This is a URL resource type")
                .addPattern("*://*:*/realm1")
                .addPattern("*://*:*/*?realm1")
                .addAction("GET", true)
                .addAction("PUT", false)
                .setCreatedBy("TestUser1")
                .setCreationDate(1422886484092l)
                .setLastModifiedBy("TestUser2")
                .setLastModifiedDate(1422886484098l).build();

        //when
        ResourceType rt2 = rt1.builder().build();

        //then
        assertEquals(rt1, rt2);
        assertEquals(rt1.hashCode(), rt2.hashCode());

    }


    @Test(expectedExceptions = NullPointerException.class)
    public void shouldNotAllowNullUUID() {
        //given
        ResourceType.Builder builder = ResourceType.builder("URL", "/testRealm");

        //when
        builder.build();

    }
}
