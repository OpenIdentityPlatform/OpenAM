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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.model;

public class ProfileMatchingRuleResult {
	
	/**
	 * HOTP confirmation is required
	 */
	private boolean requireHOTPConfirmation = false;
	
	/**
	 * Adding new profile with prior confirmation
	 */
	private boolean createCurrentDevicePrintProfileWithoutPriorConfirmation = false;
	
	/**
	 * Adding new profile without prior confirmation
	 */
	private boolean createCurrentDevicePrintProfileWithPriorConfirmation = false;
	
	/**
	 * Update selected profile with prior confirmation
	 */
	private boolean updateSelectedProfileWithoutPriorConfirmation = false;
	
	/**
	 * Update selected profile without confirmation
	 */
	private boolean updateSelectedProfileWithPriorConfirmation = false;

	/**
	 * Getter
	 * @return
	 */
	public boolean isRequireHOTPConfirmation() {
		return requireHOTPConfirmation;
	}

	/**
	 * Setter
	 * @return
	 */
	public void setRequireHOTPConfirmation(boolean requireHOTPConfirmation) {
		this.requireHOTPConfirmation = requireHOTPConfirmation;
	}

	/**
	 * Getter
	 * @return
	 */
	public boolean isCreateCurrentDevicePrintProfileWithoutPriorConfirmation() {
		return createCurrentDevicePrintProfileWithoutPriorConfirmation;
	}

	/**
	 * Setter
	 * @return
	 */
	public void setCreateCurrentDevicePrintProfileWithoutPriorConfirmation(
			boolean createCurrentDevicePrintProfileWithoutPriorConfirmation) {
		this.createCurrentDevicePrintProfileWithoutPriorConfirmation = createCurrentDevicePrintProfileWithoutPriorConfirmation;
	}

	/**
	 * Getter
	 * @return
	 */
	public boolean isCreateCurrentDevicePrintProfileWithPriorConfirmation() {
		return createCurrentDevicePrintProfileWithPriorConfirmation;
	}

	/**
	 * Setter
	 * @return
	 */
	public void setCreateCurrentDevicePrintProfileWithPriorConfirmation(
			boolean createCurrentDevicePrintProfileWithPriorConfirmation) {
		this.createCurrentDevicePrintProfileWithPriorConfirmation = createCurrentDevicePrintProfileWithPriorConfirmation;
	}

	/**
	 * Getter
	 * @return
	 */
	public boolean isUpdateSelectedProfileWithoutPriorConfirmation() {
		return updateSelectedProfileWithoutPriorConfirmation;
	}

	/**
	 * Setter
	 * @return
	 */
	public void setUpdateSelectedProfileWithoutPriorConfirmation(
			boolean updateSelectedProfileWithoutPriorConfirmation) {
		this.updateSelectedProfileWithoutPriorConfirmation = updateSelectedProfileWithoutPriorConfirmation;
	}

	/**
	 * Getter
	 * @return
	 */
	public boolean isUpdateSelectedProfileWithPriorConfirmation() {
		return updateSelectedProfileWithPriorConfirmation;
	}

	/**
	 * Setter
	 * @return
	 */
	public void setUpdateSelectedProfileWithPriorConfirmation(
			boolean updateSelectedProfileWithPriorConfirmation) {
		this.updateSelectedProfileWithPriorConfirmation = updateSelectedProfileWithPriorConfirmation;
	}

	@SuppressWarnings("all")
	@Override
	public String toString() {
		return "ProfileMatchingRuleResult [requireHOTPConfirmation="
				+ requireHOTPConfirmation
				+ ", createCurrentDevicePrintProfileWithoutPriorConfirmation="
				+ createCurrentDevicePrintProfileWithoutPriorConfirmation
				+ ", createCurrentDevicePrintProfileWithPriorConfirmation="
				+ createCurrentDevicePrintProfileWithPriorConfirmation
				+ ", updateSelectedProfileWithoutPriorConfirmation="
				+ updateSelectedProfileWithoutPriorConfirmation
				+ ", updateSelectedProfileWithPriorConfirmation="
				+ updateSelectedProfileWithPriorConfirmation + "]";
	}
	
}
