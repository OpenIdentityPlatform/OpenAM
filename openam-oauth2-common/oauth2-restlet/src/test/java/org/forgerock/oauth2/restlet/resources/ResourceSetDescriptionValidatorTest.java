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

package org.forgerock.oauth2.restlet.resources;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ResourceSetDescriptionValidatorTest {

    private ResourceSetDescriptionValidator validator;

    @BeforeMethod
    public void setup() {
        validator = new ResourceSetDescriptionValidator();
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldFailToValidateResourceSetDescriptionWithMissingName() throws Exception {

        //Given
        Map<String, Object> resourceSetDescription = new HashMap<String, Object>();

        resourceSetDescription.put(OAuth2Constants.ResourceSets.SCOPES, "SCOPES");

        //When
        validator.validate(resourceSetDescription);

        //Then
        //Expected BadRequestException
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldFailToValidateResourceSetDescriptionWithInvalidName() throws Exception {

        //Given
        Map<String, Object> resourceSetDescription = new HashMap<String, Object>();

        resourceSetDescription.put(OAuth2Constants.ResourceSets.NAME, 123);
        resourceSetDescription.put(OAuth2Constants.ResourceSets.SCOPES, "SCOPES");

        //When
        validator.validate(resourceSetDescription);

        //Then
        //Expected BadRequestException
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldFailToValidateResourceSetDescriptionWithMissingScope() throws Exception {

        //Given
        Map<String, Object> resourceSetDescription = new HashMap<String, Object>();

        resourceSetDescription.put(OAuth2Constants.ResourceSets.NAME, "NAME");

        //When
        validator.validate(resourceSetDescription);

        //Then
        //Expected BadRequestException
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldFailToValidateResourceSetDescriptionWithInvalidScope() throws Exception {

        //Given
        Map<String, Object> resourceSetDescription = new HashMap<String, Object>();

        resourceSetDescription.put(OAuth2Constants.ResourceSets.NAME, "NAME");
        resourceSetDescription.put(OAuth2Constants.ResourceSets.SCOPES, "SCOPES");

        //When
        validator.validate(resourceSetDescription);

        //Then
        //Expected BadRequestException
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldFailToValidateResourceSetDescriptionWithInvalidURI() throws Exception {

        //Given
        Map<String, Object> resourceSetDescription = new HashMap<String, Object>();

        resourceSetDescription.put(OAuth2Constants.ResourceSets.NAME, "NAME");
        resourceSetDescription.put(OAuth2Constants.ResourceSets.URI, "NOT_A^^^_URI");
        resourceSetDescription.put(OAuth2Constants.ResourceSets.SCOPES, Collections.singleton("SCOPES"));

        //When
        validator.validate(resourceSetDescription);

        //Then
        //Expected BadRequestException
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldFailToValidateResourceSetDescriptionWithInvalidType() throws Exception {

        //Given
        Map<String, Object> resourceSetDescription = new HashMap<String, Object>();

        resourceSetDescription.put(OAuth2Constants.ResourceSets.NAME, "NAME");
        resourceSetDescription.put(OAuth2Constants.ResourceSets.TYPE, 123);
        resourceSetDescription.put(OAuth2Constants.ResourceSets.SCOPES, Collections.singleton("SCOPES"));

        //When
        validator.validate(resourceSetDescription);

        //Then
        //Expected BadRequestException
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldFailToValidateResourceSetDescriptionWithInvalidIconUri() throws Exception {

        //Given
        Map<String, Object> resourceSetDescription = new HashMap<String, Object>();

        resourceSetDescription.put(OAuth2Constants.ResourceSets.NAME, "NAME");
        resourceSetDescription.put(OAuth2Constants.ResourceSets.SCOPES, Collections.singleton("SCOPES"));
        resourceSetDescription.put(OAuth2Constants.ResourceSets.ICON_URI, "NOT_A^^^_URI");

        //When
        validator.validate(resourceSetDescription);

        //Then
        //Expected BadRequestException
    }

    @Test
    public void shouldValidateResourceSetDescription() throws Exception {

        //Given
        Map<String, Object> resourceSetDescription = new HashMap<String, Object>();

        resourceSetDescription.put(OAuth2Constants.ResourceSets.NAME, "NAME");
        resourceSetDescription.put(OAuth2Constants.ResourceSets.SCOPES, Collections.singleton("SCOPES"));

        //When
        Map<String, Object> validated = validator.validate(resourceSetDescription);

        //Then
        assertThat(validated).isEqualTo(resourceSetDescription);
    }

    @Test
    public void shouldValidateAllResourceSetDescription() throws Exception {

        //Given
        Map<String, Object> resourceSetDescription = new HashMap<String, Object>();

        resourceSetDescription.put(OAuth2Constants.ResourceSets.NAME, "NAME");
        resourceSetDescription.put(OAuth2Constants.ResourceSets.URI, "/URI");
        resourceSetDescription.put(OAuth2Constants.ResourceSets.TYPE, "TYPE");
        resourceSetDescription.put(OAuth2Constants.ResourceSets.SCOPES, Collections.singleton("SCOPES"));
        resourceSetDescription.put(OAuth2Constants.ResourceSets.ICON_URI, "/ICON_URI");

        //When
        Map<String, Object> validated = validator.validate(resourceSetDescription);

        //Then
        assertThat(validated).isEqualTo(resourceSetDescription);
    }
}
