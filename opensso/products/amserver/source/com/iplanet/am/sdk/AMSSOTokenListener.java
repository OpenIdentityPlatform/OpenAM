/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMSSOTokenListener.java,v 1.3 2008/06/25 05:41:22 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenEvent;
import com.iplanet.sso.SSOTokenListener;
import com.iplanet.sso.SSOTokenManager;

/**
 * This class implements the <code>com.iplanet.sso.SSOTokenListene</code>
 * interface. This listener updates the profile name table, the objImplListeners
 * table and AMCommonUtils's dpCache by invalidating (removing) all entires
 * affected because of the SSOToken becoming invalid.
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */
class AMSSOTokenListener implements SSOTokenListener {

    private String principalName = "";

    /**
     * Constructor to create a new instance of AMSSOTokenListener
     * 
     * @param principalName
     *            the principal name of the SSOToken for which this listener has
     *            been added.
     */
    public AMSSOTokenListener(String principalName) {
        this.principalName = principalName;
    }

    /**
     * This method is the implementation for the SSOTokenListener interface.
     */
    public void ssoTokenChanged(SSOTokenEvent stEvent) {
        boolean isValid;
        SSOToken ssoToken = stEvent.getToken();
        try {
            isValid = SSOTokenManager.getInstance().isValidToken(ssoToken);
        } catch (SSOException se) {
            isValid = false;
        }

        if (AMCommonUtils.debug.messageEnabled()) {
            AMCommonUtils.debug.message("In AMSSOTokenListener."
                    + "ssoTokenChanged(): Principal: " + principalName
                    + " ssoToken: " + isValid);
        }

        if (!isValid) {
            // Remove the entires for the SSOToken to which this listener
            // corresponds to from the ProfileNameTable of AMObjectImpl class
            Set dnSet = AMObjectImpl.removeFromProfileNameTable(ssoToken);
            if (dnSet != null) {
                if (AMCommonUtils.debug.messageEnabled()) {
                    AMCommonUtils.debug.message("In AMSSOTokenListener."
                            + "ssoTokenChanged(): dnSet NOT null!");
                }
                // Also update the AMObjectImpl's objImplListener table
                AMObjectImpl.removeObjImplListeners(dnSet, ssoToken
                        .getTokenID());
            }
        }
    }
}
