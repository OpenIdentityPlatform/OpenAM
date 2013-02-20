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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.ComparisonResult;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.RiskBasedDevicePrintComparator;

import com.sun.identity.shared.debug.Debug;

/**
 * @author jdabrowski
 */
public final class MultiValueAttributeMatcher {

	private MultiValueAttributeMatcher() {
		super();
	}

	private static final Debug debug = Debug.getInstance(RiskBasedDevicePrintComparator.class.getName());

	private static final BigDecimal hundred = BigDecimal.valueOf(100);

	/**
	 * * Splits both attributes using delimiter, trims every value and compares
	 * collections of values. Returns zero-result for same multivalue
	 * attributes. If collections are not same checks if number of differences is
	 * less or equal maxToleratedNumberOfDifferences or percentage of difference
	 * is less or equal maxToleratedPercentageToMarkAsDifferent. If yes then
	 * returns zero-result with additional info, else returns
	 * penaltyPoints-result.
	 * 
	 * @param currentAttribute
	 * @param storedAttribute
	 * @param delimeter
	 * @param maxToleratedPercentageToMarkAsDifferent
	 * @param maxToleratedNumberOfDifferences
	 * @param penaltyPoints
	 * @return
	 */
	public static ComparisonResult getComparationResult(
			String currentAttribute, String storedAttribute, char delimeter,
			int maxToleratedPercentageToMarkAsDifferent,
			int maxToleratedNumberOfDifferences, Long penaltyPoints) {
		
		List<String> currentAttributes = getAtributesFromMultiAttribute(currentAttribute, delimeter);
		List<String> storedAttributes = getAtributesFromMultiAttribute(storedAttribute, delimeter);
		
		if(storedAttribute==null && currentAttribute != null && currentAttributes.size() == 0){
			return new ComparisonResult(true);
		}

		BigDecimal maxToleratedNumberOfDifferencesBI = BigDecimal.valueOf(maxToleratedNumberOfDifferences);
		BigDecimal maxToleratedPercentage = BigDecimal.valueOf(maxToleratedPercentageToMarkAsDifferent);
		
		BigDecimal maxNumberOfElements = BigDecimal.valueOf(Math.max(currentAttributes.size(), storedAttributes.size()));
		BigDecimal numberOfTheSameElements = getNumberOfSameElements(currentAttributes,storedAttributes);
		BigDecimal numberOfDifferences = getNumberOfDifferences(numberOfTheSameElements,maxNumberOfElements);
		BigDecimal percentageOfDifferences = getPecentageOfDifferences(numberOfDifferences, maxNumberOfElements);
		

		if (debug.messageEnabled()) {
			debug.message(numberOfTheSameElements + " of "	+ maxNumberOfElements + " are same");
		}

		if (maxNumberOfElements.equals(BigDecimal.ZERO)) {
			if (debug.messageEnabled()) {
				debug.message("Ignored because no attributes found in both profiles");
			}
			return ComparisonResult.ZERO_PENALTY_POINTS;
		}
		
		if (numberOfTheSameElements.equals(maxNumberOfElements)) {
			if (debug.messageEnabled()) {
				debug.message("Ignored because all attributes are same");
			}
			return ComparisonResult.ZERO_PENALTY_POINTS;
		}
		
		if (isMoreDifferencesThenTolerated(numberOfDifferences, maxToleratedNumberOfDifferencesBI)) {
			if (debug.messageEnabled()) {
				debug.message("Would be ignored if not more then " + maxToleratedNumberOfDifferencesBI + " differences");
			}
		} else {
			if (debug.messageEnabled()) {
				debug.message("Ignored because number of differences("+numberOfDifferences+ ") not more then "	+ maxToleratedNumberOfDifferencesBI);
			}
			return new ComparisonResult(true);
		}

		if (percentageOfDifferenceIsMoreThenTolerated(percentageOfDifferences, maxToleratedPercentage)) {
			if (debug.messageEnabled()) {
				debug.message(percentageOfDifferences + " percents are different");
				debug.message("Would be ignored if not more then " + maxToleratedPercentage + " percent");
			}
			return new ComparisonResult(penaltyPoints);
		} else {
			if (debug.messageEnabled()) {
				debug.message(percentageOfDifferences + " percents are different");
				debug.message("Ignored because not more then " + maxToleratedPercentage + " percent");
			}
			return new ComparisonResult(true);
		}
	}

	private static boolean percentageOfDifferenceIsMoreThenTolerated(BigDecimal percentageOfDifferences, BigDecimal maxNumberOfElements) {
		return percentageOfDifferences.compareTo(maxNumberOfElements)>0;
	}

	private static boolean isMoreDifferencesThenTolerated(BigDecimal numberOfDifferences, BigDecimal maxToleratedNumberOfDifferencesBI) {
		return numberOfDifferences.compareTo(maxToleratedNumberOfDifferencesBI)>0;
	}

	private static BigDecimal getPecentageOfDifferences(BigDecimal numberOfDifferences, BigDecimal maxNumberOfElements) {
		if(maxNumberOfElements.equals(BigDecimal.ZERO)){
			return BigDecimal.ZERO;
		}
		return numberOfDifferences.divide(maxNumberOfElements, 2, RoundingMode.HALF_UP).multiply(hundred);
	}

	private static BigDecimal getNumberOfDifferences(BigDecimal numberOfSameElements, BigDecimal maxNumberOfElements) {
		return maxNumberOfElements.subtract(numberOfSameElements);
	}

	private static BigDecimal getNumberOfSameElements(List<String> currentAttributes, List<String> storedAttributes) {
		List<String> tmpCurrentAttributes = new ArrayList<String>(currentAttributes);
		List<String> tmpStoredAttributes = new ArrayList<String>(storedAttributes);
		
		tmpCurrentAttributes.retainAll(tmpStoredAttributes);
		return BigDecimal.valueOf(tmpCurrentAttributes.size());
	}

	private static List<String> getAtributesFromMultiAttribute(String multiAttribute, char delimeter) {
		ArrayList<String> result = new ArrayList<String>();
		if (multiAttribute == null) {
			return result;
		}

		String[] attributes = multiAttribute.split(Character.toString(delimeter));
		for (String attribute : attributes) {
			if (!attribute.trim().isEmpty()) {
				result.add(attribute.trim());
			}
		}
		return result;
	}

}
