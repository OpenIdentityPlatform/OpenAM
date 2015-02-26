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
 * $Id: GuidUtils.java,v 1.3 2009/01/28 05:34:51 ww203982 Exp $
 *
 */

package com.iplanet.ums.util;

import com.sun.identity.shared.ldap.LDAPDN;

import com.iplanet.ums.Guid;
import com.sun.identity.sm.ServiceManager;

/**
 * Utilitiy Class for Guid.
 */
public class GuidUtils {

    /**
     * Gets the organization Guid.
     * <p>
     * For example, the following section of code gets the organization Guid of
     * "uid=joe,ou=People,o=iplanet.com"
     * 
     * <pre>
     * Guid guid = new Guid(&quot;uid=joe,ou=People,o=iplanet.com&quot;);
     * 
     * Guid orgGuid = GuidUtils.getOrgGuid(guid);
     * </pre>
     * 
     * The organization Guid in this example is "o=iplanet.com"
     * </p>
     * 
     * @param guid
     *            The Guid to look up
     * @return the organization Guid
     */
    static String baseDN = ServiceManager.getBaseDN();

    public static Guid getOrgGuid(Guid guid) {
        String dn = LDAPDN.normalize(guid.getDn());
        int index = dn.indexOf("o=");
        if (index > -1) {
            return (new Guid(dn.substring(index, dn.length())));
        } else {
            // If no "o=", then does it match baseDN
            int index2 = dn.toLowerCase().indexOf(baseDN.toLowerCase());
            if (index2 > -1) {
                return (new Guid(baseDN));
            } else {
                return null;
            }
        }
    }

}
