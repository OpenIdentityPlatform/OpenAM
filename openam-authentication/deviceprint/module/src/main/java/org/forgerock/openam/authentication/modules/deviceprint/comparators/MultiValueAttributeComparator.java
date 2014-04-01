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
 * Portions Copyrighted 2013-2014 ForgeRock Inc.
 * Portions Copyrighted 2014 Nomura Research Institute, Ltd.
 */

package org.forgerock.openam.authentication.modules.deviceprint.comparators;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.sun.identity.shared.debug.Debug;

/**
 * Compares two Strings of comma separated values.
 *
 * @author jdabrowski
 */
public class MultiValueAttributeComparator {

	private static final Debug DEBUG = Debug.getInstance("amAuthDevicePrint");

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private static final String DELIMITER = ";";

	/**
	 * Splits both attributes using delimiter, trims every value and compares collections of values.
     * Returns zero-result for same multi-value attributes.
     *
     * If collections are not same checks if number of differences is less or equal maxToleratedNumberOfDifferences or
     * percentage of difference is less or equal maxToleratedPercentageToMarkAsDifferent.
     *
     * If yes then returns zero-result with additional info, else returns penaltyPoints-result.
	 *
	 * @param currentAttribute The current value.
	 * @param storedAttribute The stored value.
     * @param maxToleratedNumberOfDifferences The max number of differences in the values, before the penalty points
     *                                        are assigned.
     * @param maxToleratedPercentageToMarkAsDifferent The max difference percentage in the values, before the penalty
     *                                                is assigned.
     * @param penaltyPoints The number of penalty points.
     * @return A ComparisonResult.
	 */
	public ComparisonResult compare(String currentAttribute, String storedAttribute,
            Integer maxToleratedPercentageToMarkAsDifferent, Integer maxToleratedNumberOfDifferences,
            long penaltyPoints) {

		List<String> currentAttributes = convertAttributesToList(currentAttribute);
		List<String> storedAttributes = convertAttributesToList(storedAttribute);

		if (storedAttribute == null && currentAttribute != null && currentAttributes.size() == 0) {
			return new ComparisonResult(true);
		}

		BigDecimal maxToleratedDifferences = BigDecimal.valueOf(maxToleratedNumberOfDifferences);
		BigDecimal maxToleratedPercentage = BigDecimal.valueOf(maxToleratedPercentageToMarkAsDifferent);

		BigDecimal maxNumberOfElements = BigDecimal.valueOf(Math.max(currentAttributes.size(),
                storedAttributes.size()));
		BigDecimal numberOfTheSameElements = getNumberOfSameElements(currentAttributes,storedAttributes);
		BigDecimal numberOfDifferences = getNumberOfDifferences(numberOfTheSameElements,maxNumberOfElements);
		BigDecimal percentageOfDifferences = getPercentageOfDifferences(numberOfDifferences, maxNumberOfElements);


		if (DEBUG.messageEnabled()) {
			DEBUG.message(numberOfTheSameElements + " of " + maxNumberOfElements + " are same");
		}

		if (maxNumberOfElements.equals(BigDecimal.ZERO)) {
			if (DEBUG.messageEnabled()) {
				DEBUG.message("Ignored because no attributes found in both profiles");
			}
			return ComparisonResult.ZERO_PENALTY_POINTS;
		}

		if (numberOfTheSameElements.equals(maxNumberOfElements)) {
			if (DEBUG.messageEnabled()) {
				DEBUG.message("Ignored because all attributes are same");
			}
			return ComparisonResult.ZERO_PENALTY_POINTS;
		}

		if (numberOfDifferences.compareTo(maxToleratedDifferences) > 0) {
			if (DEBUG.messageEnabled()) {
				DEBUG.message("Would be ignored if not more than " + maxToleratedDifferences + " differences");
			}
            return new ComparisonResult(penaltyPoints);
		}

		if (percentageOfDifferences.compareTo(maxToleratedPercentage) > 0) {
			if (DEBUG.messageEnabled()) {
				DEBUG.message(percentageOfDifferences + " percents are different");
				DEBUG.message("Would be ignored if not more than " + maxToleratedPercentage + " percent");
			}
			return new ComparisonResult(penaltyPoints);
		}

        if (DEBUG.messageEnabled()) {
            DEBUG.message("Ignored because number of differences(" + numberOfDifferences + ") not more than "
                    + maxToleratedDifferences);
            DEBUG.message(percentageOfDifferences + " percents are different");
            DEBUG.message("Ignored because not more than " + maxToleratedPercentage + " percent");
        }
        return new ComparisonResult(true);
	}

    /**
     * Gets the percentage of the differences between the two values.
     *
     * @param numberOfDifferences The actual number of differences.
     * @param maxNumberOfElements The number of values in the largest multi-value.
     * @return The percentage of differences.
     */
	private BigDecimal getPercentageOfDifferences(BigDecimal numberOfDifferences, BigDecimal maxNumberOfElements) {
		if (maxNumberOfElements.equals(BigDecimal.ZERO)) {
			return BigDecimal.ZERO;
		}
		return numberOfDifferences.divide(maxNumberOfElements, 2, RoundingMode.HALF_UP).multiply(HUNDRED);
	}

    /**
     * Gets the number of differences between the two values.
     *
     * @param numberOfSameElements The number of elements that are equal.
     * @param maxNumberOfElements The number of values in the largest multi-value.
     * @return The number of differences.
     */
	private BigDecimal getNumberOfDifferences(BigDecimal numberOfSameElements, BigDecimal maxNumberOfElements) {
		return maxNumberOfElements.subtract(numberOfSameElements);
	}

    /**
     * Gets the number of elements that are equal between the two lists of values.
     *
     * @param currentAttributes The current values.
     * @param storedAttributes The stored values.
     * @return The number of elements that are equal.
     */
	private BigDecimal getNumberOfSameElements(List<String> currentAttributes, List<String> storedAttributes) {
		List<String> tmpCurrentAttributes = new ArrayList<String>(currentAttributes);
		List<String> tmpStoredAttributes = new ArrayList<String>(storedAttributes);

		tmpCurrentAttributes.retainAll(tmpStoredAttributes);
		return BigDecimal.valueOf(tmpCurrentAttributes.size());
	}

    /**
     * Converts a comma separated String into a List.
     *
     * @param multiAttribute The comma separated String.
     * @return A list of the comma separated values.
     */
	private List<String> convertAttributesToList(String multiAttribute) {

        List<String> result = new ArrayList<String>();
		if (multiAttribute == null) {
			return result;
		}

		String[] attributes = multiAttribute.split(DELIMITER);
		for (String attribute : attributes) {
			if (!attribute.trim().isEmpty()) {
				result.add(attribute.trim());
			}
		}
		return result;
	}
}
