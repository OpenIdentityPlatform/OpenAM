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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.marchingrunner.riskbased;

import static org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.matcher.AttributeComparatorHelper.*;
import static org.testng.Assert.*;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.ComparisonResult;
import org.testng.annotations.Test;


public class AttributeComparatorTest {
	
	private static final Long ZERO_LONG = new Long(0L);
	private static final String STORED = "stored";
	private static final String CURRENT = "current";
	private static final long PENALTY_POINTS = 10L;

	@Test
	public void testAttributesEqals() {
		ComparisonResult compareAttribute = compareAttribute(CURRENT,CURRENT,PENALTY_POINTS);
		assertEquals(compareAttribute.getPenaltyPoints(),ZERO_LONG);
		assertTrue(compareAttribute.isSuccessful());
		assertFalse(compareAttribute.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testAttributesDiffers() {
		ComparisonResult compareAttribute = compareAttribute(CURRENT,STORED,PENALTY_POINTS);
		assertEquals((long)compareAttribute.getPenaltyPoints(), PENALTY_POINTS);
		assertFalse(compareAttribute.isSuccessful());
		assertFalse(compareAttribute.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testAdditionalInfoInCurrentValue() {
		ComparisonResult compareAttribute = compareAttribute(CURRENT,null,PENALTY_POINTS);
		assertEquals(compareAttribute.getPenaltyPoints(),ZERO_LONG);
		assertTrue(compareAttribute.isSuccessful());
		assertTrue(compareAttribute.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testBothValuesNull() {
		ComparisonResult compareAttribute = compareAttribute(null,null,PENALTY_POINTS);
		assertEquals(compareAttribute.getPenaltyPoints(),ZERO_LONG);
		assertTrue(compareAttribute.isSuccessful());
		assertFalse(compareAttribute.isAdditionalInfoInCurrentValue());
	}
}
