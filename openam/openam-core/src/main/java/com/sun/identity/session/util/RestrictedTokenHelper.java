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
 * $Id: RestrictedTokenHelper.java,v 1.2 2008/06/25 05:43:59 qcheng Exp $
 *
 */

package com.sun.identity.session.util;

import com.iplanet.dpro.session.Session;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

/**
 * This is a Utility class to create the restricted token using the context
 */

public class RestrictedTokenHelper {

    /**
     * Create a restricted token for the given context
     * 
     * @param sid
     *            restricted tokenID
     * @param context
     *            check the restriction for the given context
     * @return SSOToken a restricted token if present for the given sid
     * @throws Exception
     *             if there was an error
     */
    public static SSOToken resolveRestrictedToken(final String sid,
            Object context) throws Exception {
        return (SSOToken) RestrictedTokenContext.doUsing(context,
                new RestrictedTokenAction() {
                    public Object run() throws Exception {
                        return SSOTokenManager.getInstance()
                                .createSSOToken(sid);
                    }
                });
    }

    /**
     * Returns true if the SSOToken is restricted, false otherwise
     * 
     * @param token
     *            SSOToken to be checked for the presence of restriction
     *            property
     * @return true if the token is a restricted token, false otherwise
     * @throws SSOException
     *             is thrown if the there was an error
     */
    public static boolean isRestricted(SSOToken token) throws SSOException {
        return token.getProperty(Session.TOKEN_RESTRICTION_PROP) != null;
    }
}
