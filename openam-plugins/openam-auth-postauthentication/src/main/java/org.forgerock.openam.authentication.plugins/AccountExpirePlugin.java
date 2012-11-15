/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
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
 *
 */
package org.forgerock.openam.authentication.plugins;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.AccessController;
import java.util.*;


public class AccountExpirePlugin implements AMPostAuthProcessInterface {
    static final String EXPIRPROPERTY = "org.forgerock.openam.authentication.accountExpire.days";

    @Override
    public void onLoginSuccess(Map requestParamsMap, HttpServletRequest request,
                               HttpServletResponse response, SSOToken token)
            throws AuthenticationException {
        Map<String, Set> attrMap = new HashMap<String, Set>();
        Debug debug = Debug.getInstance("AccountExpirePlugin");

        try {
            // Fetch the expiry time from service config
            // We update the user account expiry, since we have a valid login
            // Since there is no convenient way to pass init params to a postAuthN,
            // We simply get the value from a System defined property.
            //  If not defined,  use a default of 30 days
            String daysToExpireDefault = SystemProperties.get(EXPIRPROPERTY);
            int daysToExpire = Integer.getInteger(daysToExpireDefault, 30);

            Calendar cal = java.util.Calendar.getInstance();
            cal.add(cal.DATE, daysToExpire);
            Set attrValue = new HashSet();
            attrValue.add(Locale.getNormalizedDateString(cal.getTime()));
            attrMap.put(ISAuthConstants.ACCOUNT_LIFE, attrValue);
            AMIdentity id = IdUtils.getIdentity(
                    AccessController.doPrivileged(AdminTokenAction.getInstance()),
                    token.getProperty(Constants.UNIVERSAL_IDENTIFIER));
            id.setAttributes(attrMap);
            id.store();
        } catch (Exception e) {
            debug.error("AccountExpirePlugin.onLoginSuccess : Unable to save ExpireTime : ", e);
        }
    }

    @Override
    public void onLoginFailure(Map requestParamsMap, HttpServletRequest request,
                               HttpServletResponse response) throws AuthenticationException {
    }

    @Override
    public void onLogout(HttpServletRequest request,
                         HttpServletResponse response, SSOToken token)
            throws AuthenticationException {
    }

}
