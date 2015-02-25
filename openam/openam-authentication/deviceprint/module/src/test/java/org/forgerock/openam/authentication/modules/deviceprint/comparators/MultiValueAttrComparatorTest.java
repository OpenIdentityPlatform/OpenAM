/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
/*
 * Portions Copyrighted 2013 Syntegrity.
 * Portions Copyrighted 2013-2014 ForgeRock AS.
 * Portions Copyrighted 2014 Nomura Research Institute, Ltd.
 */

package org.forgerock.openam.authentication.modules.deviceprint.comparators;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class MultiValueAttrComparatorTest {

    private MultiValueAttributeComparator multiValueAttrComparator;

    @BeforeClass
    public void setUp() {
        multiValueAttrComparator = new MultiValueAttributeComparator();
    }

    @Test
    public void shouldCompareMultiValueStringsWhenStoredValueIsNullAndCurrentValueIsEmpty() {

        //Given
        String currentValue = "";
        String storedValue = null;

        //When
        ComparisonResult comparisonResult = multiValueAttrComparator.compare(currentValue, storedValue, 20, 1, 111L);

        //Then
        assertTrue(comparisonResult.isSuccessful());
        assertEquals(comparisonResult.getPenaltyPoints(), (Long) 0L);
        assertTrue(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCompareMultiValueStringsWhenBothAreEmpty() {

        //Given
        String currentValue = "";
        String storedValue = "";

        //When
        ComparisonResult comparisonResult = multiValueAttrComparator.compare(currentValue, storedValue, 20, 1, 111L);

        //Then
        assertTrue(comparisonResult.isSuccessful());
        assertEquals(comparisonResult.getPenaltyPoints(), (Long) 0L);
        assertFalse(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCompareMultiValueStringsWhenBothAreEqual() {

        //Given
        String currentValue = "VALUE_A; VALUE_B; VALUE_C; VALUE_D; VALUE_E";
        String storedValue = "VALUE_A; VALUE_B; VALUE_C; VALUE_D; VALUE_E";

        //When
        ComparisonResult comparisonResult = multiValueAttrComparator.compare(currentValue, storedValue, 20, 1, 111L);

        //Then
        assertTrue(comparisonResult.isSuccessful());
        assertEquals(comparisonResult.getPenaltyPoints(), (Long) 0L);
        assertFalse(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCompareMultiValueStringWhenThereAreLessDifferencesThanMax() {

        //Given
        String currentValue = "VALUE_AA; VALUE_B; VALUE_C; VALUE_D; VALUE_E";
        String storedValue = "VALUE_B; VALUE_C; VALUE_D; VALUE_E";

        //When
        ComparisonResult comparisonResult = multiValueAttrComparator.compare(currentValue, storedValue, 20, 1, 111L);

        //Then
        assertTrue(comparisonResult.isSuccessful());
        assertEquals(comparisonResult.getPenaltyPoints(), (Long) 0L);
        assertTrue(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCompareMultiValueStringWhenThereAreMoreDifferencesThanMax() {

        //Given
        String currentValue = "VALUE_AA; VALUE_BB; VALUE_C; VALUE_D; VALUE_E";
        String storedValue = "VALUE_B; VALUE_C; VALUE_D; VALUE_E";

        //When
        ComparisonResult comparisonResult = multiValueAttrComparator.compare(currentValue, storedValue, 20, 1, 111L);

        //Then
        assertFalse(comparisonResult.isSuccessful());
        assertEquals(comparisonResult.getPenaltyPoints(), (Long) 111L);
        assertFalse(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCompareMultiValueStringWhenThereIsLessPercentageDiffThanMax() {

        //Given
        String currentValue = "VALUE_AA; VALUE_B; VALUE_C; VALUE_D; VALUE_E";
        String storedValue = "VALUE_B; VALUE_C; VALUE_D; VALUE_E";

        //When
        ComparisonResult comparisonResult = multiValueAttrComparator.compare(currentValue, storedValue, 20, 1, 111L);

        //Then
        assertTrue(comparisonResult.isSuccessful());
        assertEquals(comparisonResult.getPenaltyPoints(), (Long) 0L);
        assertTrue(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCompareMultiValueStringWhenThereIsMorePercentageDiffThanMax() {

        //Given
        String currentValue = "VALUE_AA; VALUE_BB; VALUE_C; VALUE_D; VALUE_E";
        String storedValue = "VALUE_B; VALUE_C; VALUE_D; VALUE_E";

        //When
        ComparisonResult comparisonResult = multiValueAttrComparator.compare(currentValue, storedValue, 20, 1, 111L);

        //Then
        assertFalse(comparisonResult.isSuccessful());
        assertEquals(comparisonResult.getPenaltyPoints(), (Long) 111L);
        assertFalse(comparisonResult.getAdditionalInfoInCurrentValue());
    }
}
