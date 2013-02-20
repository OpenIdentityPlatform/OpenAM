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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.ProfileMatchingRuleResult;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.UserProfile;

/**
 * Result of matching device print against profiles
 * @author ljaromin
 *
 */
public class ProfileMatchingRunnerResult {
	/**
	 * Result of successful rule matching
	 */
	private ProfileMatchingRuleResult matchingRuleResult = new ProfileMatchingRuleResult();
	
	/**
	 * User profile selected by successful rule 
	 */
	private UserProfile selectedUserProfile;

	/**
	 * Getter
	 * @return
	 */
	public ProfileMatchingRuleResult getMatchingRuleResult() {
		return matchingRuleResult;
	}

	/**
	 * Setter
	 * @param matchingRuleResult
	 */
	public void setMatchingRuleResult(ProfileMatchingRuleResult matchingRuleResult) {
		this.matchingRuleResult = matchingRuleResult;
	}

	/**
	 * Getter
	 * 
	 * @return
	 */
	public UserProfile getSelectedUserProfile() {
		return selectedUserProfile;
	}

	/**
	 * Setter
	 * @param selectedUserProfile
	 */
	public void setSelectedUserProfile(UserProfile selectedUserProfile) {
		this.selectedUserProfile = selectedUserProfile;
	}
}
