/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2015 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package com.sun.identity.shared.encode;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import org.testng.annotations.Test;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

public class CookieUtilsTest {

    @Test
    public void getMatchingCookieDomains() {
    	SystemPropertiesManager.initializeProperties(Constants.SET_COOKIE_TO_ALL_DOMAINS, "false");
    	HttpServletRequest request=mock(HttpServletRequest.class);
    	when(request.getServerName()).thenReturn("openam.openshift.dev.domain.ru");
    	assertEquals(
	    	CookieUtils.getMatchingCookieDomains(request, Arrays.asList(new String[] {
	    			"localhost",
	    			".openshift.dev.domain.ru",
	    			".dev.domain.ru",
	    			".inside.domain.ru",
	    			".domain.ru"
	    	})), 
	    	(Set<String>)new HashSet<String>(Arrays.asList(new String[] {
	    		"domain.ru",
	    		"dev.domain.ru",
	    		"openshift.dev.domain.ru"
	    	}))
	    );
    }
  
}
