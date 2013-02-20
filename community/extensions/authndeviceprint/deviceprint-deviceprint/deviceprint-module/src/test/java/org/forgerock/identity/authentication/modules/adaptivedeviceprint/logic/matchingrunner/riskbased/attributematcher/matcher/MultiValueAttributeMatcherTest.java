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

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.ComparisonResult;
import org.testng.annotations.Test;

public class MultiValueAttributeMatcherTest {
	
	private static final long PENALTY_POINTS = 100L;
	private static final long NO_PENALTY_POINTS = 0L;
	private static final char COMMA_DELIMITER = ',';
	
	@Test
	public void testBothNullExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				null,
				null,
				COMMA_DELIMITER, 
				0,
				0,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testNullAndEmptyNotExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				null,
				"",
				COMMA_DELIMITER, 
				0,
				0,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testEmptyAndNullExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				"",
				null,
				COMMA_DELIMITER, 
				0,
				0,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertTrue(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testSameExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				"some simple text",
				"some simple text",
				COMMA_DELIMITER, 
				0,
				0,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testMultiValueSameExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				"some,simple,text",
				"some,simple,text",
				COMMA_DELIMITER, 
				0,
				0,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testMultiValueDifferentOrderSameExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				"some,simple,text",
				"simple,text,some",
				COMMA_DELIMITER, 
				0,
				0,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testMultiValueDifferentOrderWithWhiteSpacesSameExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				"     some  ,   simple  ,     text   ",
				"  simple,  text  ,  some",
				COMMA_DELIMITER, 
				0,
				0,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testMultiValueAdditionalEmptyFieldsExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				"some,simple,text,,,,   ,, , , ,   ",
				",,, ,, ,      , ,  simple,text,some",
				COMMA_DELIMITER, 
				0,
				0,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testMultiValueFirstMissingOneNotExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				"some,simple,text",
				"text,some",
				COMMA_DELIMITER, 
				0,
				0,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), PENALTY_POINTS);
		assertFalse(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testMultiValueSecondMissingOneNotExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				"some,text",
				"simple,text,some",
				COMMA_DELIMITER, 
				0,
				0,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), PENALTY_POINTS);
		assertFalse(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testMultiValueOneDifferentNotExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				"some,text,notsosimple",
				"simple,text,some",
				COMMA_DELIMITER, 
				0,
				0,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), PENALTY_POINTS);
		assertFalse(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testMultiValueTwoOfTenDifferentNotExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				"some,text,simple,and,also,other,new,more,complicated,example",
				"some,text,xxxxxx,and,also,other,new,more,yyyyyyyyyyy,example",
				COMMA_DELIMITER, 
				0,
				0,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), PENALTY_POINTS);
		assertFalse(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testMultiValueTwoOfTenDifferentWithTolerateOneNotExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				"some,text,simple,and,also,other,new,more,complicated,example",
				"some,text,xxxxxx,and,also,other,new,more,yyyyyyyyyyy,example",
				COMMA_DELIMITER, 
				0,
				1,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), PENALTY_POINTS);
		assertFalse(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testMultiValueTwoOfTenDifferentWithTolerateTwoNotExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				"some,text,simple,and,also,other,new,more,complicated,example",
				"some,text,xxxxxx,and,also,other,new,more,yyyyyyyyyyy,example",
				COMMA_DELIMITER, 
				0,
				2,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertTrue(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testMultiValueTwoOfTenDifferentWithTolerateThreeNotExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				"some,text,simple,and,also,other,new,more,complicated,example",
				"some,text,xxxxxx,and,also,other,new,more,yyyyyyyyyyy,example",
				COMMA_DELIMITER, 
				0,
				3,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertTrue(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testMultiValueTwoOfTenDifferentWithTolerateTenNotExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				"some,text,simple,and,also,other,new,more,complicated,example",
				"some,text,xxxxxx,and,also,other,new,more,yyyyyyyyyyy,example",
				COMMA_DELIMITER, 
				0,
				10,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertTrue(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testMultiValueTwoOfTenDifferentWithMax10PercentToleranceNotExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				"some,text,simple,and,also,other,new,more,complicated,example",
				"some,text,xxxxxx,and,also,other,new,more,yyyyyyyyyyy,example",
				COMMA_DELIMITER, 
				10,
				0,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), PENALTY_POINTS);
		assertFalse(comparationResult.isSuccessful());
		assertFalse(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testMultiValueTwoOfTenDifferentWithMax20PercentToleranceExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				"some,text,simple,and,also,other,new,more,complicated,example",
				"some,text,xxxxxx,and,also,other,new,more,yyyyyyyyyyy,example",
				COMMA_DELIMITER, 
				20,
				0,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertTrue(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testMultiValueTwoOfTenDifferentWithMax30PercentToleranceExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				"some,text,simple,and,also,other,new,more,complicated,example",
				"some,text,xxxxxx,and,also,other,new,more,yyyyyyyyyyy,example",
				COMMA_DELIMITER, 
				30,
				0,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertTrue(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testMultiValueTwoOfTenDifferentWithMax100PercentToleranceExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				"some,text,simple,and,also,other,new,more,complicated,example",
				"some,text,xxxxxx,and,also,other,new,more,yyyyyyyyyyy,example",
				COMMA_DELIMITER, 
				100,
				0,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertTrue(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testMultiValueAllDifferentWithMax100PercentToleranceExactMatch() throws JsonParseException, JsonMappingException, IOException {
		ComparisonResult comparationResult = MultiValueAttributeMatcher.getComparationResult(
				"some,text,simple,and,also,other,new,more,complicated,example",
				"",
				COMMA_DELIMITER, 
				100,
				0,
				PENALTY_POINTS);
		assertEquals((long)comparationResult.getPenaltyPoints(), NO_PENALTY_POINTS);
		assertTrue(comparationResult.isSuccessful());
		assertTrue(comparationResult.isAdditionalInfoInCurrentValue());
	}
	
	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		MultiValueAttributeMatcherTest test = new MultiValueAttributeMatcherTest();
		test.testBothNullExactMatch();
		test.testEmptyAndNullExactMatch();
		test.testNullAndEmptyNotExactMatch();
		test.testSameExactMatch();
		test.testMultiValueSameExactMatch();
		test.testMultiValueDifferentOrderSameExactMatch();
		test.testMultiValueAdditionalEmptyFieldsExactMatch();
		test.testMultiValueFirstMissingOneNotExactMatch();
		test.testMultiValueSecondMissingOneNotExactMatch();
		test.testMultiValueOneDifferentNotExactMatch();
		test.testMultiValueTwoOfTenDifferentNotExactMatch();
		test.testMultiValueTwoOfTenDifferentWithTolerateOneNotExactMatch();
		test.testMultiValueTwoOfTenDifferentWithTolerateTwoNotExactMatch();
		test.testMultiValueTwoOfTenDifferentWithTolerateThreeNotExactMatch();
		test.testMultiValueTwoOfTenDifferentWithTolerateTenNotExactMatch();
		test.testMultiValueTwoOfTenDifferentWithMax10PercentToleranceNotExactMatch();
		test.testMultiValueTwoOfTenDifferentWithMax20PercentToleranceExactMatch();
		test.testMultiValueTwoOfTenDifferentWithMax30PercentToleranceExactMatch();
		test.testMultiValueTwoOfTenDifferentWithMax100PercentToleranceExactMatch();
		test.testMultiValueAllDifferentWithMax100PercentToleranceExactMatch();
	}

}
