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
package org.forgerock.openam.entitlement.constraints;

import static org.fest.assertions.Assertions.assertThat;

import com.sun.identity.entitlement.URLResourceName;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.utils.CollectionUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

/**
 * Unit test for the constraint validator.
 *
 * @since 13.0.0
 */
public class ConstraintValidatorImplTest {

    private ConstraintValidator validator;

    @BeforeMethod
    public void setUp() {
        validator = new ConstraintValidatorImpl();
    }

    @Test
    public void exactActionsPass() {
        // Given
        ResourceType resourceType = ResourceType
                .builder("test", "/")
                .setUUID("abc")
                .addAction("GET", true)
                .addAction("POST", true)
                .build();

        // When
        Set<String> actions = CollectionUtils
                .asSet("GET", "POST");
        boolean successful = validator
                .verifyActions(actions)
                .against(resourceType)
                .isSuccessful();

        // Then
        assertThat(successful).isTrue();
    }

    @Test
    public void subsetActionsPass() {
        // Given
        ResourceType resourceType = ResourceType
                .builder("test", "/")
                .setUUID("abc")
                .addAction("GET", true)
                .addAction("POST", true)
                .addAction("DELETE", true)
                .addAction("PATCH", true)
                .build();

        // When
        Set<String> actions = CollectionUtils
                .asSet("GET", "POST");
        boolean successful = validator
                .verifyActions(actions)
                .against(resourceType)
                .isSuccessful();

        // Then
        assertThat(successful).isTrue();
    }

    @Test
    public void additionalActionsFail() {
        // Given
        ResourceType resourceType = ResourceType
                .builder("test", "/")
                .setUUID("abc")
                .addAction("GET", true)
                .addAction("POST", true)
                .build();

        // When
        Set<String> actions = CollectionUtils
                .asSet("GET", "POST", "DELETE");
        boolean successful = validator
                .verifyActions(actions)
                .against(resourceType)
                .isSuccessful();

        // Then
        assertThat(successful).isFalse();
    }

    @Test(expectedExceptions = ConstraintFailureException.class,
            expectedExceptionsMessageRegExp = "Invalid value DELETE defined for property actionValues")
    public void throwsExceptionWhenActionsFail() throws ConstraintFailureException {
        // Given
        ResourceType resourceType = ResourceType
                .builder("test", "/")
                .setUUID("abc")
                .addAction("GET", true)
                .addAction("POST", true)
                .build();

        // When
        Set<String> actions = CollectionUtils
                .asSet("GET", "POST", "DELETE");
        validator
                .verifyActions(actions)
                .against(resourceType)
                .throwExceptionIfFailure();
    }

    @Test
    public void validResourcesPass() {
        // Given
        ResourceType resourceType = ResourceType
                .builder("test", "/")
                .setUUID("abc")
                .addPattern("a://b:c/*")
                .addPattern("d://*:*/*")
                .build();

        // When
        Set<String> resources = CollectionUtils
                .asSet("a://b:c/def/hij", "d://fried:egg/test/home");
        boolean successful = validator
                .verifyResources(resources)
                .using(new URLResourceName())
                .against(resourceType)
                .isSuccessful();

        // Then
        assertThat(successful).isTrue();
    }

    @Test
    public void invalidResourcesFail() {
        // Given
        ResourceType resourceType = ResourceType
                .builder("test", "/")
                .setUUID("abc")
                .addPattern("a://b:c/*")
                .addPattern("d://*:*/*")
                .build();

        // When
        Set<String> resources = CollectionUtils
                .asSet("a://b:c/def/hij", "fail://uri:blah/goodbye");
        boolean successful = validator
                .verifyResources(resources)
                .using(new URLResourceName())
                .against(resourceType)
                .isSuccessful();

        // Then
        assertThat(successful).isFalse();
    }

    @Test(expectedExceptions = ConstraintFailureException.class,
            expectedExceptionsMessageRegExp = "Invalid value fail://uri:blah/goodbye defined for property resources")
    public void throwsExceptionWhenResourcesFail() throws ConstraintFailureException {
        // Given
        ResourceType resourceType = ResourceType
                .builder("test", "/")
                .setUUID("abc")
                .addPattern("a://b:c/*")
                .addPattern("d://*:*/*")
                .build();

        // When
        Set<String> resources = CollectionUtils
                .asSet("a://b:c/def/hij", "fail://uri:blah/goodbye");
        validator
                .verifyResources(resources)
                .using(new URLResourceName())
                .against(resourceType)
                .throwExceptionIfFailure();
    }

}