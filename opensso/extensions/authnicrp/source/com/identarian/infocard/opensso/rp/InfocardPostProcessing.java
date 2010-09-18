/* The contents of this file are subject to the terms
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
 * $Id: InfocardPostProcessing.java,v 1.2 2009/09/15 10:45:39 ppetitsm Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2008 Patrick Petit Consulting
 */
package com.identarian.infocard.opensso.rp;

import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.spi.AuthenticationException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Patrick
 */
public class InfocardPostProcessing implements AMPostAuthProcessInterface {

    Debug debug = com.sun.identity.shared.debug.Debug.getInstance(Infocard.amAuthInfocard);

    @Override
    public void onLoginSuccess(Map requestParamsMap, HttpServletRequest request,
            HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException {

        Set<String> keys;
        try {
            String authType = ssoToken.getAuthType();
            if (authType.equals(Infocard.getAuthType())) {
                //System.out.println(">>> InfocardPostProcessing request attributes\n");
                Enumeration attrNames = request.getAttributeNames();
                while (attrNames.hasMoreElements()) {
                    String name = (String) attrNames.nextElement();
                  //  System.out.println(">>> \t" + name + ":" + request.getAttribute(name) + "\n");
                }
            }
        } catch (SSOException e) {
            debug.error("Failed to set session with Information Card claims", e);
        }
    }

    @Override
    public void onLoginFailure(Map requestParamsMap, HttpServletRequest request,
            HttpServletResponse response) throws AuthenticationException {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void onLogout(HttpServletRequest request, HttpServletResponse response,
            SSOToken ssoToken) throws AuthenticationException {
        // throw new UnsupportedOperationException("Not supported yet.");
    }
}
