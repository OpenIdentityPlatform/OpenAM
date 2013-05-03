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

/**
 * Stores the result of the comparison of two Device Print objects and the resulting penalty points assigned to the
 * comparison.
 */
public class ComparisonResult implements Comparable<ComparisonResult> {

	public static final ComparisonResult ZERO_PENALTY_POINTS = new ComparisonResult(0L);

	private Long penaltyPoints = 0L;
	private Boolean additionalInfoInCurrentValue = false;

    /**
     * Constructs an instance of a ComparisonResult with zero penalty points.
     */
    public ComparisonResult() {
    }

    /**
     * Constructs an instance of a ComparisonResult with the given penalty points.
     *
     * @param penaltyPoints The penalty points for the comparison.
     * @param additionalInfoInCurrentValue Whether the current value contains more information than the stored value.
     */
	public ComparisonResult(Long penaltyPoints, Boolean additionalInfoInCurrentValue) {
		this.penaltyPoints = penaltyPoints;
		this.additionalInfoInCurrentValue = additionalInfoInCurrentValue;
	}

    /**
     *
     * Constructs an instance of a ComparisonResult with the given penalty points.
     *
     * @param differencePenaltyPoints The penalty points for the comparison.
     */
	public ComparisonResult(Long differencePenaltyPoints) {
		this.penaltyPoints = differencePenaltyPoints;
	}

    /**
     * Constructs an instance of a ComparisonResult with zero penalty points.
     *
     * @param additionalInfoInCurrentValue Whether the current value contains more information than the stored value.
     */
	public ComparisonResult(Boolean additionalInfoInCurrentValue) {
		this.additionalInfoInCurrentValue = additionalInfoInCurrentValue;
	}

    /**
     * Amalgamates the given ComparisonResult into this ComparisonResult.
     *
     * @param comparisonResult The ComparisonResult to include.
     */
	public void addComparisonResult(ComparisonResult comparisonResult) {
		this.penaltyPoints += comparisonResult.penaltyPoints;
		if (comparisonResult.getAdditionalInfoInCurrentValue()) {
			this.additionalInfoInCurrentValue = comparisonResult.getAdditionalInfoInCurrentValue();
		}
	}

    /**
     * Gets the number of penalty points assigned to the comparison.
     *
     * @return The penalty points.
     */
	public Long getPenaltyPoints() {
		return penaltyPoints;
	}

    /**
     * Returns whether the current value of the comparison contains more than the stored value.
     *
     * @return If the current value contains more information.
     */
	public Boolean getAdditionalInfoInCurrentValue() {
		return additionalInfoInCurrentValue;
	}

    /**
     * Returns true if no penalty points have been assigned for the comparison.
     *
     * @return If the comparison was successful.
     */
	public boolean isSuccessful() {
		return penaltyPoints == null || penaltyPoints == 0L;
	}

    /**
     * {@inheritDoc}
     */
	public int compareTo(ComparisonResult o) {
		if (o == null) {
			return 1;
		} else {
			if (!o.getPenaltyPoints().equals(getPenaltyPoints())) {
				return getPenaltyPoints().compareTo(o.getPenaltyPoints());
			} else {
				return getAdditionalInfoInCurrentValue().compareTo(o.getAdditionalInfoInCurrentValue());
			}
		}
	}

    /**
     * {@inheritDoc}
     * @return
     */
	public String toString() {
		return "ComparisonResult [penaltyPoints=" + penaltyPoints
				+ ", additionalInfoInCurrentValue="
				+ additionalInfoInCurrentValue + "]";
	}
}
