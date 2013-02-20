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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.ComparisonResult;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ComparisonResultComparatorTest {
	
	
	
	private static final long LESS_POINTS = 10L;
	private static final long MORE_POINTS = 20L;

	@Test
	public void compareVariousPenaltyPointsValues() {
		List<ComparisonResult> resultList = new ArrayList<ComparisonResult>();
		List<ComparisonResult> expectedList = new ArrayList<ComparisonResult>();
		
		ComparisonResult lessPoints = new ComparisonResult(LESS_POINTS);
		ComparisonResult morePoints = new ComparisonResult(MORE_POINTS);
		ComparisonResult morePointsAdditionalValue = new ComparisonResult(MORE_POINTS, true);
		ComparisonResult lessPointsAdditionalValue = new ComparisonResult(LESS_POINTS, true);
		
		resultList.add(lessPoints);
		resultList.add(morePoints);
		resultList.add(morePointsAdditionalValue);
		resultList.add(lessPointsAdditionalValue);
		Collections.sort(resultList);
		
		expectedList.add(lessPoints);
		expectedList.add(lessPointsAdditionalValue);
		expectedList.add(morePoints);
		expectedList.add(morePointsAdditionalValue);

		assertEquals(resultList, expectedList);
	}
}
