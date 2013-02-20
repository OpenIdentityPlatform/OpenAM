/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openam.extensions.crowd.util;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

/**
 * Utility class for OpenAM specific code, such as getting the username if a valid OpenAM token is present.
 * 
 * @author Dave van Eijck
 *
 */
public final class OpenAMUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAMUtil.class);

    /** OpenAM property name for User Id property. */
    private static final String USER_ID_PROPERTY_NAME = "UserId";

    /** Force utility class. */
    private OpenAMUtil() {
        super();
    }

    /**
     * Gets the username if a valid OpenAM token is present in the {@link HttpServletRequest}.
     * @param request the {@link HttpServletRequest} that might contain a valid OpenAM token.
     * @return the username if a valid OpenAM token is present, <code>null</code> otherwise.
     */
    public static String obtainUsername(HttpServletRequest request) {
        String result = null;
        SSOToken ssoToken = getToken(request);

        if (ssoToken != null && isTokenValid(ssoToken)) {
            try {
                result = ssoToken.getProperty(USER_ID_PROPERTY_NAME);
            } catch (SSOException e) {
                LOGGER.error("Error getting UserId from SSOToken.", e);
            }
        }

        return result;
    }

    private static SSOToken getToken(HttpServletRequest request) {
        SSOToken token = null;

        try {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            token = manager.createSSOToken(request);
        } catch (Exception e) {
            LOGGER.error("Error creating SSOToken.", e);
        }

        return token;
    }

    private static boolean isTokenValid(SSOToken token) {
        boolean result = false;

        try {
            SSOTokenManager ssoTokenManager = SSOTokenManager.getInstance();
            result = ssoTokenManager.isValidToken(token);
        } catch (Exception e) {
            LOGGER.error("Error validating SSOToken.", e);
        }

        return result;
    }

}
