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

import java.util.List;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.UserProfile;

/**
 * Interface of profile matching runner. It runs profile matching.
 * @author ljaromin
 *
 */
public interface ProfileMatchingRunner {

	/**
	 * This method matches current device to one of stored user profiles. Classes implementing this
	 * interface define algorithms used utilized in profile matching.
	 * @param storedUserProfiles list of the user profiles  
	 * @param devicePrint device print against which profiles are matched
	 * @param props runner specific properties
	 * @return matched profile result or null if no profile has been matched
	 */
	ProfileMatchingRunnerResult runProfileMatching(
			List<UserProfile> userProfiles, DevicePrint devicePrint);

}
