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
 * Portions Copyrighted 2013 ForgeRock Inc.
 */

package org.forgerock.openam.authentication.modules.deviceprint.comparators;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ColocationComparatorTest {

    private ColocationComparator colocationComparator;

    @BeforeClass
    public void setUp() {
        colocationComparator = new ColocationComparator();
    }

    @Test
    public void shouldCompareLocationWhenBothXsAreNull() {

        //Given

        //When
        ComparisonResult comparisonResult = colocationComparator.compare(null, 2.0, null, 2.0, 100, 111L);

        //Then
        assertTrue(comparisonResult.isSuccessful());
    }

    @Test
    public void shouldCompareLocationWhenBothYsAreNull() {

        //Given

        //When
        ComparisonResult comparisonResult = colocationComparator.compare(2.0, null, 2.0, null, 100, 111L);

        //Then
        assertTrue(comparisonResult.isSuccessful());
    }

    @Test
    public void shouldCompareLocationWhenCurrentXIsNull() {

        //Given

        //When
        ComparisonResult comparisonResult = colocationComparator.compare(null, 2.0, 2.0, 2.0, 100, 111L);

        //Then
        assertFalse(comparisonResult.isSuccessful());
        assertEquals(comparisonResult.getPenaltyPoints(), (Long) 111L);
        assertFalse(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCompareLocationWhenCurrentYIsNull() {

        //Given

        //When
        ComparisonResult comparisonResult = colocationComparator.compare(2.0, null, 2.0, 2.0, 100, 111L);

        //Then
        assertFalse(comparisonResult.isSuccessful());
        assertEquals(comparisonResult.getPenaltyPoints(), (Long) 111L);
        assertFalse(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCompareLocationWhenStoredXIsNull() {

        //Given

        //When
        ComparisonResult comparisonResult = colocationComparator.compare(2.0, 2.0, null, 2.0, 100, 111L);

        //Then
        assertFalse(comparisonResult.isSuccessful());
        assertEquals(comparisonResult.getPenaltyPoints(), (Long) 111L);
        assertTrue(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCompareLocationWhenStoredYIsNull() {

        //Given

        //When
        ComparisonResult comparisonResult = colocationComparator.compare(2.0, 2.0, 2.0, null, 100, 111L);

        //Then
        assertFalse(comparisonResult.isSuccessful());
        assertEquals(comparisonResult.getPenaltyPoints(), (Long) 111L);
        assertTrue(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCompareLocationsThatAreEqual() {

        //Given

        //When
        ComparisonResult comparisonResult = colocationComparator.compare(2.0, 2.0, 2.0, 2.0, 100, 111L);

        //Then
        assertTrue(comparisonResult.isSuccessful());
        assertFalse(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCompareLocationsThatAreWithinTolerableRange() {

        //Given

        //When
        ComparisonResult comparisonResult = colocationComparator.compare(3.0, 3.0, 2.0, 2.0, 100, 111L);

        //Then
        assertTrue(comparisonResult.isSuccessful());
        assertTrue(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCompareLocationsThatAreOutsideTolerableRange() {

        //Given

        //When
        ComparisonResult comparisonResult = colocationComparator.compare(20.0, 20.0, 2.0, 2.0, 100, 111L);

        //Then
        assertFalse(comparisonResult.isSuccessful());
        assertEquals(comparisonResult.getPenaltyPoints(), (Long) 111L);
        assertFalse(comparisonResult.getAdditionalInfoInCurrentValue());
    }
}
