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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic;

import java.util.Date;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.UserProfile;
import org.forgerock.identity.authentication.modules.utils.RandomHashGenerator;

public class PersistentCookieLogic implements PersistentCookieLogicIface {
	
	private int cookieAge;
	
	private static final int MULTIPLER = 24 * 60 * 60 * 1000;
	
	/** @{inheritedDoc} */
	@Override
	public boolean proceed(UserProfile selectedProfile, DevicePrint currentDevicePrint) {
		if( selectedProfile == null ) {
			currentDevicePrint.setPersistentCookie(generateNewCookie());
			return true;
		} else {
			if(selectedProfile.getLastPersistentCookieUpdateDate() == null) {
				selectedProfile.getDevicePrint().setPersistentCookie(generateNewCookie());
				return true;
			}
			
			Date expireDate = new Date(new Date().getTime() - cookieAge * MULTIPLER);
			
			if(selectedProfile.getLastPersistentCookieUpdateDate().before(expireDate)) {
				selectedProfile.getDevicePrint().setPersistentCookie(generateNewCookie());
				return true;
			}
		}
		
		return false;
	}
	
	private String generateNewCookie() {
		String timestamp = Long.toString(new Date().getTime());
		String hash = new RandomHashGenerator().getRandomHash();
		return timestamp + "-" + hash;
	}

	@Override
	public void setPersistentCookieAge(int days) {
		cookieAge = days;		
	}

	public int getCookieAge() {
		return cookieAge;
	}

	public void setCookieAge(int cookieAge) {
		this.cookieAge = cookieAge;
	}	
}
