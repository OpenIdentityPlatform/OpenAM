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
 * $Id: GroupResolver.java,v 1.4 2009/01/28 05:34:50 ww203982 Exp $
 *
 */

package com.iplanet.ums;

import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.LDAPUrl;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;

public class GroupResolver extends DefaultClassResolver {

    private static Debug debug;
    static {
        debug = Debug.getInstance(IUMSConstants.UMS_DEBUG);
    }

    public Class resolve(String id, AttrSet set) {
        Class c = super.resolve(id, set);
        if ((c != null) && c.equals(com.iplanet.ums.DynamicGroup.class)) {
            Attr attr = set.getAttribute("memberurl");
            if (attr != null) {
                String[] vals = attr.getStringValues();
                if ((vals != null) && (vals.length > 0)) {
                    if (isAssignable(id, vals[0])) {
                        c = com.iplanet.ums.AssignableDynamicGroup.class;
                    }
                }
            }
        }
        return c;
    }

    private boolean isAssignable(String id, String val) {
        try {
            LDAPUrl url = new LDAPUrl(val);
            String filter = url.getFilter().trim();
            if (debug.messageEnabled()) {
                debug.message("AssignableDynamicGroup.GroupResolver."
                        + "isAssignable: filter = <" + filter + ">");
            }
            if ((filter.startsWith("(")) && (filter.endsWith(")"))) {
                filter = filter.substring(1, filter.length() - 1);
                if (debug.messageEnabled()) {
                    debug.message("AssignableDynamicGroup.GroupResolver."
                            + "isAssignable: adjusted to <" + filter + ">");
                }
            }
            int ind = filter.indexOf('=');
            if (ind > 0) {
                String attrName = filter.substring(0, ind);
                if (debug.messageEnabled()) {
                    debug.message("AssignableDynamicGroup.GroupResolver."
                            + "isAssignable: attrName = <" + attrName + ">");
                }
                if (attrName.equalsIgnoreCase("memberof")) {
                    String attrVal = filter.substring(ind + 1).trim();
                    String dn = LDAPDN.normalize(guidToDN(attrVal));
                    if (debug.messageEnabled()) {
                        debug.message("AssignableDynamicGroup.GroupResolver."
                                + "isAssignable: comparing <" + dn + "> to <"
                                + id + ">");
                    }
                    return dn.equalsIgnoreCase(LDAPDN.normalize(guidToDN(id)));
                }
            }
        } catch (java.net.MalformedURLException ex) {
            // TODO - Log Exception
            if (debug.messageEnabled()) {
                debug.message("AssignableDynamicGroup.isAssignable : "
                        + "Exception : " + ex.getMessage());
            }
        }
        return false;
    }

    private String guidToDN(String id) {
        return id;
    }
}
