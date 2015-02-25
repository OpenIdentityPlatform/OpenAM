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

package org.forgerock.openam.entitlement.utils;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;

/**
 * Unit test to exercise the behaviour of {@link EntitlementUtils}.
 */
public class EntitlementUtilsTest {

    @Test
    public void shouldRetrieveDescriptionFromDataWhenItIsPresent() {
        //Given
        String expectedDescription = "A description";

        Map<String,Set<String>> data = new HashMap<String, Set<String>>();
        data.put("attributeName",
                new HashSet<String>(Arrays.asList(new String[]{"Attribute value 1", "Attribute value 2"})));
        data.put("description",
                new HashSet<String>(Arrays.asList(new String[]{expectedDescription})));
        data.put("anotherAttributeName",
                new HashSet<String>(Arrays.asList(new String[]{"Another attribute value"})));

        //When
        Set<String> actualDescription = EntitlementUtils.getDescription(data);

        //Then
        assertEquals(actualDescription.size(), 1, "Actual description expected to only contain one item.");
        assertEquals(actualDescription.iterator().next(), expectedDescription,
                "Actual description found was not as expected.");
    }

    @Test
    public void shouldNotRetrieveDescriptionFromDataWhenItIsNotPresent() {
        //Given
        Map<String,Set<String>> data = new HashMap<String, Set<String>>();
        data.put("attributeName",
                new HashSet<String>(Arrays.asList(new String[]{"Attribute value 1", "Attribute value 2"})));
        data.put("anotherAttributeName",
                new HashSet<String>(Arrays.asList(new String[]{"Another attribute value"})));

        //When
        Set<String> actualDescription = EntitlementUtils.getDescription(data);

        //Then
        assertEquals(actualDescription, null, "Actual description expected to be null.");
    }
}
