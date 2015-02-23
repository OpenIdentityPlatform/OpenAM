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
package org.forgerock.openam.entitlement.service;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

import com.sun.identity.entitlement.EntitlementException;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.configuration.ResourceTypeConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.security.Principal;

public class ResourceTypeServiceTest {

    private ResourceTypeConfiguration configuration;
    private ResourceTypeService service;
    private Subject subject;

    private final String TEST_USER = "TestUser";
    private final Principal testUserPrinciple = new Principal() {
        @Override
        public String getName() {
            return TEST_USER;
        }
    };

    @BeforeMethod
    public void setup() {
        configuration = mock(ResourceTypeConfiguration.class);
        service = new ResourceTypeServiceImpl(configuration);
        subject = new Subject();
        subject.getPrincipals().add(testUserPrinciple);
    }

    @Test
    public void shouldModifyResourceTypeMetaData() throws EntitlementException {
        // given
        ResourceType resourceType = ResourceType.builder("URL", "/testRealm").generateUUID()
                .setDescription("This is a URL resource type")
                .addPattern("*://*:*/*")
                .addPattern("*://*:*/*?*")
                .addAction("GET", true)
                .addAction("PUT", false).build();

        // when
        resourceType = service.saveResourceType(subject, resourceType);

        // then
        assertNotNull(resourceType.getCreatedBy());
        assertEquals(resourceType.getCreatedBy(), TEST_USER);
        assertNotNull(resourceType.getCreationDate());
        assertNotEquals(resourceType.getCreationDate(), 0);
        assertNotNull(resourceType.getLastModifiedBy());
        assertEquals(resourceType.getLastModifiedBy(), TEST_USER);
        assertNotNull(resourceType.getLastModifiedDate());
        assertNotEquals(resourceType.getLastModifiedDate(), 0);
    }
}
