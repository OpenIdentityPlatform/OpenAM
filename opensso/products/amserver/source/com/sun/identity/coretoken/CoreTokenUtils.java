/**
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
 * $Id: CoreTokenUtils.java,v 1.1 2009/11/19 00:07:40 qcheng Exp $
 *
 */

package com.sun.identity.coretoken;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.ldap.util.DN;
import java.security.AccessController;
import java.text.ParseException;
import java.util.Date;
import javax.security.auth.Subject;

/**
 * This class provides utility methods for Core Token Service.
 * 
 */
public class CoreTokenUtils {
    public static Debug debug = Debug.getInstance("CoreToken");

    public static boolean areDNIdentical(String dn1, String dn2) {
        DN dnObj1 = new DN(dn1);
        DN dnObj2 = new DN(dn2);
        return dnObj1.equals(dnObj2);
    }

    public static Subject getAdminSubject() {
        SSOToken dsameUserToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
        return SubjectUtils.createSubject(dsameUserToken);
    }

    /**
     * Checks if the token expired
     * @param tokenExpiry Token expiry as UTC time string.
     * @return true if the token expired, false otherwise
     * @throws CoreTokenException if failed to parse the token expiry string.
     */
    public static boolean isTokenExpired(String tokenExpiry)
        throws CoreTokenException {
        try {
            Date expiryDate = DateUtils.stringToDate(tokenExpiry);
            long now = System.currentTimeMillis();
            if (expiryDate.getTime() <= now) {
                return true;
            } else {
                return false;
            }
        } catch (ParseException ex) {
            String[] data = new String[]{tokenExpiry};
            throw new CoreTokenException(10, data, 400);
        }
    }
}
