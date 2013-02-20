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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.matcher;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.ComparisonResult;
import org.testng.annotations.Test;

public class ColocationMatcherTest {
	
	private static final long PENALTY_POINTS = 100L;
	private static final long NO_PENALTY_POINTS = 0L;
	
	@Test
	public void testBothNullExactMatch() throws JsonParseException, JsonMappingException, IOException {
		
		Double firstLatitude = null;
		Double firstLongitude = null;
		Double secundLatitude = null;
		Double secondLongitude = null;
		Long TOLERANCE = 0L;
		
		ComparisonResult comparationResult = ColocationMatcher.getComparationResult(firstLatitude,firstLongitude, secundLatitude, secondLongitude, TOLERANCE , PENALTY_POINTS);
		
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testCurrentNullStoredNotNullNotExactMatch() throws JsonParseException, JsonMappingException, IOException {
		
		Double firstLatitude = 51.107887;
		Double firstLongitude = 17.038525;
		Double secundLatitude = null;
		Double secondLongitude = null;
		Long TOLERANCE = 0L;
		
		ComparisonResult comparationResult = ColocationMatcher.getComparationResult(firstLatitude,firstLongitude, secundLatitude, secondLongitude, TOLERANCE , PENALTY_POINTS);
		
		assertEquals((long)comparationResult.getPenaltyPoints(), PENALTY_POINTS);
		assertFalse(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testStoredNullCurrentNotNullNotExactMatch() throws JsonParseException, JsonMappingException, IOException {
		
		Double firstLatitude = null;
		Double firstLongitude = null;
		Double secundLatitude = 52.229745;
		Double secondLongitude = 21.012118;
		Long TOLERANCE = 0L;
		
		ComparisonResult comparationResult = ColocationMatcher.getComparationResult(firstLatitude,firstLongitude, secundLatitude, secondLongitude, TOLERANCE , PENALTY_POINTS);
		
		assertEquals((long)comparationResult.getPenaltyPoints(), PENALTY_POINTS);
		assertFalse(comparationResult.isSuccessful());
		assertTrue(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testSameExactMatch() throws JsonParseException, JsonMappingException, IOException {
		
		Double firstLatitude = 51.107887;
		Double firstLongitude = 17.038525;
		Double secundLatitude = 51.107887;
		Double secondLongitude =17.038525;
		Long TOLERANCE = 0L;
		
		ComparisonResult comparationResult = ColocationMatcher.getComparationResult(firstLatitude,firstLongitude, secundLatitude, secondLongitude, TOLERANCE , PENALTY_POINTS);
		
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testNotSameNotExactMatch() throws JsonParseException, JsonMappingException, IOException {
		
		Double firstLatitude = 51.107887;
		Double firstLongitude = 17.038525;
		Double secundLatitude = 51.107887;
		Double secondLongitude =17.038527;
		Long TOLERANCE = 0L;
		
		ComparisonResult comparationResult = ColocationMatcher.getComparationResult(firstLatitude,firstLongitude, secundLatitude, secondLongitude, TOLERANCE , PENALTY_POINTS);
		
		assertEquals((long)comparationResult.getPenaltyPoints(), PENALTY_POINTS);
		assertFalse(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	private boolean sameWith0Dot1PecentTolerace(BigDecimal distance, BigDecimal knownDistance) {
		System.out.println(distance);
		System.out.println(knownDistance);
		BigDecimal difference = distance.subtract(knownDistance).abs();
		BigDecimal onePercent = distance.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_DOWN);
		System.out.println(onePercent);
		return difference.subtract(onePercent).compareTo(BigDecimal.ZERO)<=0;
	}
	
	@Test
	public void testCalculateDistance1() throws JsonParseException, JsonMappingException, IOException {
		
		Double firstLatitude =    51.107887;
		Double firstLongitude =   17.038525;
		Double secundLatitude =   52.229745;
		Double secondLongitude =  21.012118;
		
		BigDecimal distance = ColocationMatcher.calculateDistance(firstLatitude,firstLongitude, secundLatitude, secondLongitude);
		//Based on http://www.nhc.noaa.gov/gccalc.shtml
		assertTrue(sameWith0Dot1PecentTolerace(distance,BigDecimal.valueOf(187)));
	}
	
	@Test
	public void testCalculateDistance2() throws JsonParseException, JsonMappingException, IOException {
		
		Double firstLatitude =    51.107887;
		Double firstLongitude =   17.038525;
		Double secundLatitude =  -52.229745;
		Double secondLongitude =  21.012118;
		
		BigDecimal distance = ColocationMatcher.calculateDistance(firstLatitude,firstLongitude, secundLatitude, secondLongitude);
		//Based on http://www.nhc.noaa.gov/gccalc.shtml
		assertTrue(sameWith0Dot1PecentTolerace(distance,BigDecimal.valueOf(7139)));
	}
	
	@Test
	public void testCalculateDistance3() throws JsonParseException, JsonMappingException, IOException {
		
		Double firstLatitude =    51.107887;
		Double firstLongitude =   17.038525;
		Double secundLatitude =   52.229745;
		Double secondLongitude = -21.012118;
		
		BigDecimal distance = ColocationMatcher.calculateDistance(firstLatitude,firstLongitude, secundLatitude, secondLongitude);
		//Based on http://www.nhc.noaa.gov/gccalc.shtml
		assertTrue(sameWith0Dot1PecentTolerace(distance,BigDecimal.valueOf(1612)));
	}
	
	@Test
	public void testCalculateDistance4() throws JsonParseException, JsonMappingException, IOException {
		
		Double firstLatitude =    51.107887;
		Double firstLongitude =   17.038525;
		Double secundLatitude =  -52.229745;
		Double secondLongitude = -21.012118;
		
		BigDecimal distance = ColocationMatcher.calculateDistance(firstLatitude,firstLongitude, secundLatitude, secondLongitude);
		//Based on http://www.nhc.noaa.gov/gccalc.shtml
		assertTrue(sameWith0Dot1PecentTolerace(distance,BigDecimal.valueOf(7471)));
	}
	
	

	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		ColocationMatcherTest test = new ColocationMatcherTest();
		test.testBothNullExactMatch();
		test.testCurrentNullStoredNotNullNotExactMatch();
		test.testStoredNullCurrentNotNullNotExactMatch();
		test.testSameExactMatch();
		test.testNotSameNotExactMatch();
		test.testCalculateDistance1();
		test.testCalculateDistance2();
		test.testCalculateDistance3();
		test.testCalculateDistance4();
	}

}
