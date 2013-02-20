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
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.ComparisonResult;
import org.testng.annotations.Test;

public class AdvancedMatcherTest {
	
	private static final Set<AttributeRebuilder> NO_REBUILDERS = new HashSet<AttributeRebuilder>();
	private static final Set<AttributeRebuilder> IGNORE_VERSION_CHARS_REBUILDERS = new HashSet<AttributeRebuilder>();
	private static final long PENALTY_POINTS = 100L;
	private static final long NO_PENALTY_POINTS = 0L;
	
	static{
		IGNORE_VERSION_CHARS_REBUILDERS.add(AttributeRebuilders.IGNORE_VERSION_CHARS);
	}

	@Test
	public void testBothNullExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = AdvancedMatcher.getComparationResult(null, null, NO_REBUILDERS, PENALTY_POINTS);
		
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testNullAndEmptyNotExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = AdvancedMatcher.getComparationResult(null, "", NO_REBUILDERS, PENALTY_POINTS);
		
		assertEquals((long)comparationResult.getPenaltyPoints(), PENALTY_POINTS);
		assertFalse(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testEmptyAndNullExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = AdvancedMatcher.getComparationResult("", null, NO_REBUILDERS, PENALTY_POINTS);
		
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertTrue(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testSameExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = AdvancedMatcher.getComparationResult("some #$%34+)!@ fs text", "some #$%34+)!@ fs text", NO_REBUILDERS, PENALTY_POINTS);
		
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testOneCharDifferentNotExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = AdvancedMatcher.getComparationResult("some $%34+)!@ fs text", "some #$%34+)!@ fs text", NO_REBUILDERS, PENALTY_POINTS);
		
		assertEquals((long)comparationResult.getPenaltyPoints(), PENALTY_POINTS);
		assertFalse(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testDifferentNotExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = AdvancedMatcher.getComparationResult("553i5n5n 5y4n", "some #$%34+)!@ fs text", NO_REBUILDERS, PENALTY_POINTS);
		
		assertEquals((long)comparationResult.getPenaltyPoints(), PENALTY_POINTS);
		assertFalse(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testDifferentNumbersNotExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = AdvancedMatcher.getComparationResult("AppleWebKit/535.1", "AppleWebKit/534.1+", NO_REBUILDERS, PENALTY_POINTS);
		
		assertEquals((long)comparationResult.getPenaltyPoints(), PENALTY_POINTS);
		assertFalse(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testDifferentNumbersWithIgnoreVersionCharsExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = AdvancedMatcher.getComparationResult("AppleWebKit/535.1.242", "AppleWebKit/534.1+", IGNORE_VERSION_CHARS_REBUILDERS, PENALTY_POINTS);
		
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testDifferentCharsWithIgnoreVersionCharsNotExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = AdvancedMatcher.getComparationResult("NewAppleWebKit/535.1.242", "AppleWebKit/534.1+", IGNORE_VERSION_CHARS_REBUILDERS, PENALTY_POINTS);
		
		assertEquals((long)comparationResult.getPenaltyPoints(), PENALTY_POINTS);
		assertFalse(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		AdvancedMatcherTest test = new AdvancedMatcherTest();
		test.testBothNullExactMatch();
		test.testEmptyAndNullExactMatch();
		test.testNullAndEmptyNotExactMatch();
		test.testSameExactMatch();
		test.testOneCharDifferentNotExactMatch();
		test.testDifferentNotExactMatch();
		test.testDifferentNumbersNotExactMatch();
		test.testDifferentNumbersWithIgnoreVersionCharsExactMatch();
		test.testDifferentCharsWithIgnoreVersionCharsNotExactMatch();
	}

}
