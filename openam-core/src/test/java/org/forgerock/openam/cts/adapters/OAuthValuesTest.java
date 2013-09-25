/**
 * Copyright 2013 ForgeRock, AS.
 *
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
 */
package org.forgerock.openam.cts.adapters;

import org.forgerock.openam.cts.adapters.OAuthValues;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.testng.Assert.assertEquals;

/**
 * @author robert.wapshott@forgerock.com
 */
public class OAuthValuesTest {
    @Test
    public void shouldStoreDateValue() {
        // Given
        String date = "1370259234252";
        OAuthValues values = new OAuthValues();

        // When
        Calendar calendar = values.getDateValue(Arrays.asList(date));
        Collection<String> result = values.fromDateValue(calendar);

        // Then
        assertEquals(1, result.size());
        assertThat(result, hasItem(date));
    }

    @Test
    public void shouldStoreCollectionOfStrings() {
        // Given
        String one = "Badger";
        String two = "Weasel";
        String three = "Ferret";

        OAuthValues values = new OAuthValues();

        // When
        String value = values.getSingleValue(Arrays.asList(one, two, three));
        Collection<String> result = values.fromSingleValue(value);

        // Then
        assertEquals(3, result.size());
        assertThat(result, hasItems(one, two, three));
    }

    @Test
    public void shouldHandleEmptyCollection() {
        // Given
        OAuthValues values = new OAuthValues();

        // When
        String value = values.getSingleValue(Collections.<String>emptyList());
        Collection<String> result = values.fromSingleValue(value);

        // Then
        assertEquals(0, result.size());
    }
}
