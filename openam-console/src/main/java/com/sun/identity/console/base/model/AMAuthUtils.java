/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMAuthUtils.java,v 1.2 2008/06/25 05:42:49 qcheng Exp $
 *
 */

package com.sun.identity.console.base.model;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import javax.servlet.http.HttpServletRequest;

/**
 * This class provides authentication related helper methods.
 */
public class AMAuthUtils {
    public static Debug debug = Debug.getInstance(
        AMAdminConstants.CONSOLE_DEBUG_FILENAME);

    /**
     * Gets the organization where user authenticated to. This value is found
     * in single-sign on token.
     *
     * @param ssoToken - Single-Sign-On Token
     * return organization where user authenticated.
     */
    public static String getAuthenticatedOrgDN(SSOToken ssoToken) {
        String orgDN = "";

        try {
            orgDN = ssoToken.getProperty(Constants.ORGANIZATION);
        } catch (SSOException ssoe) {
            if (debug.warningEnabled()) {
                debug.warning("AMAuthUtils.getAuthenticatedOrgDN", ssoe);
            }
        }

        return orgDN;
    }

    /**
     * Returns user's single sign on token.
     *
     * @param req HTTP Servlet request.
     * @return single-sign-on token.
     * @throws SSOException if single-sign-on token cannot be created
     */
    public static SSOToken getSSOToken(HttpServletRequest req)
        throws SSOException
    {
        SSOTokenManager manager = SSOTokenManager.getInstance();
        SSOToken ssoToken = manager.createSSOToken(req);
        manager.validateToken(ssoToken);
        return ssoToken;
    }
}
