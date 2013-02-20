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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher;

public class ComparisonResult implements Comparable<ComparisonResult>{
	
	public static final ComparisonResult ZERO_PENALTY_POINTS = new ComparisonResult();
	
	private Long penaltyPoints = 0L;
	private Boolean additionalInfoInCurrentValue = false;
	
	public ComparisonResult(Long penaltyPoints,
			Boolean additionalInfoInCurrentValue) {
		super();
		this.penaltyPoints = penaltyPoints;
		this.additionalInfoInCurrentValue = additionalInfoInCurrentValue;
	}

	public ComparisonResult(Long differencePenaltyPoints) {
		this.penaltyPoints = differencePenaltyPoints;
	}
	
	public ComparisonResult(Boolean additionalInfoInCurrentValue) {
		this.additionalInfoInCurrentValue = additionalInfoInCurrentValue;
	}
	
	public ComparisonResult() {
	}
	
	public void addComparisonReslt(ComparisonResult comparisonResult) {
		this.penaltyPoints += comparisonResult.penaltyPoints;
		if(comparisonResult.getAdditionalInfoInCurrentValue()) {
			this.additionalInfoInCurrentValue = comparisonResult.getAdditionalInfoInCurrentValue();
		}
	}

	public Long getPenaltyPoints() {
		return penaltyPoints;
	}
	public void setPenaltyPoints(Long penaltyPoints) {
		this.penaltyPoints = penaltyPoints;
	}

	public Boolean getAdditionalInfoInCurrentValue() {
		return additionalInfoInCurrentValue;
	}

	public void setAdditionalInfoInCurrentValue(Boolean additionalInfoInCurrentValue) {
		this.additionalInfoInCurrentValue = additionalInfoInCurrentValue;
	}
	
	public boolean isSuccessful() {
		return penaltyPoints == null || penaltyPoints == 0L;
	}
	
	public boolean isAdditionalInfoInCurrentValue() {
		return (additionalInfoInCurrentValue != null && additionalInfoInCurrentValue.booleanValue());
	}

	@Override
	public int compareTo(ComparisonResult o) {
		if(o == null) {
			return 1;
		} else {
			if(!o.getPenaltyPoints().equals(getPenaltyPoints())) {
				return getPenaltyPoints().compareTo(o.getPenaltyPoints());
			} else {
				return getAdditionalInfoInCurrentValue().compareTo(o.getAdditionalInfoInCurrentValue());
			}
		}
	}

	@Override
	public String toString() {
		return "ComparisonResult [penaltyPoints=" + penaltyPoints
				+ ", additionalInfoInCurrentValue="
				+ additionalInfoInCurrentValue + "]";
	}
	
}
