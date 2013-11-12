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

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertFalse;

public class ComparisonResultTest {

    @Test
    public void shouldCreateComparisonResultWithZeroPenaltyPoints() {

        //Given

        //When
        ComparisonResult comparisonResult = new ComparisonResult();

        //Then
        assertEquals(comparisonResult.getPenaltyPoints(), (Long) 0L);
        assertFalse(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCreateComparisonResultWithZeroPenaltyPointsUsingConstant() {

        //Given

        //When
        ComparisonResult comparisonResult = ComparisonResult.ZERO_PENALTY_POINTS;

        //Then
        assertEquals(comparisonResult.getPenaltyPoints(), (Long) 0L);
        assertFalse(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCreateComparisonResultWithPenaltyPointsAndAdditionalInfo() {

        //Given

        //When
        ComparisonResult comparisonResult = new ComparisonResult(111L, true);

        //Then
        assertEquals(comparisonResult.getPenaltyPoints(), (Long) 111L);
        assertTrue(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCreateComparisonResultWithPenaltyPoints() {

        //Given

        //When
        ComparisonResult comparisonResult = new ComparisonResult(111L);

        //Then
        assertEquals(comparisonResult.getPenaltyPoints(), (Long) 111L);
        assertFalse(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCreateComparisonResultWithAdditionalInfo() {

        //Given

        //When
        ComparisonResult comparisonResult = new ComparisonResult(true);

        //Then
        assertEquals(comparisonResult.getPenaltyPoints(), (Long) 0L);
        assertTrue(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldAddComparisonResult() {

        //Given
        ComparisonResult comparisonResult = new ComparisonResult(111L);
        ComparisonResult anotherComparisonResult = new ComparisonResult(321L);

        //When
        comparisonResult.addComparisonResult(anotherComparisonResult);

        //Then
        assertEquals(comparisonResult.getPenaltyPoints(), (Long) 432L);
        assertFalse(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldAddComparisonResultWithAdditionalInfo() {

        //Given
        ComparisonResult comparisonResult = new ComparisonResult(111L);
        ComparisonResult anotherComparisonResult = new ComparisonResult(321L, true);

        //When
        comparisonResult.addComparisonResult(anotherComparisonResult);

        //Then
        assertEquals(comparisonResult.getPenaltyPoints(), (Long) 432L);
        assertTrue(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldGetComparisonResultSuccessful() {

        //Given
        ComparisonResult comparisonResult = new ComparisonResult(0L);

        //When
        boolean isSuccessful = comparisonResult.isSuccessful();

        //Then
        assertTrue(isSuccessful);
    }

    @Test
    public void shouldGetComparisonResultUnsuccessful() {

        //Given
        ComparisonResult comparisonResult = new ComparisonResult(1L);

        //When
        boolean isSuccessful = comparisonResult.isSuccessful();

        //Then
        assertFalse(isSuccessful);
    }

    @Test
    public void shouldCompareComparisonResultsEqually() {

        //Given
        ComparisonResult comparisonResult = new ComparisonResult(111L);
        ComparisonResult anotherComparisonResult = new ComparisonResult(111L);

        //When
        int result = comparisonResult.compareTo(anotherComparisonResult);

        //Then
        assertEquals(result, 0);
    }

    @Test
    public void shouldCompareComparisonResultsNotEqualPenaltyPoints() {

        //Given
        ComparisonResult comparisonResult = new ComparisonResult(110L);
        ComparisonResult anotherComparisonResult = new ComparisonResult(111L);

        //When
        int result = comparisonResult.compareTo(anotherComparisonResult);

        //Then
        assertEquals(result, -1);
    }

    @Test
    public void shouldCompareComparisonResultsWithEqualPenaltyPointsButOneWithAdditionalInfo() {

        //Given
        ComparisonResult comparisonResult = new ComparisonResult(111L, true);
        ComparisonResult anotherComparisonResult = new ComparisonResult(111L);

        //When
        int result = comparisonResult.compareTo(anotherComparisonResult);

        //Then
        assertEquals(result, 1);
    }

    @Test
    public void shouldCompareComparisonResultsWithNull() {

        //Given
        ComparisonResult comparisonResult = new ComparisonResult(111L);
        ComparisonResult anotherComparisonResult = null;

        //When
        int result = comparisonResult.compareTo(anotherComparisonResult);

        //Then
        assertEquals(result, 1);
    }
}
