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
 * $Id: ISSecurityPermission.java,v 1.4 2008/08/19 19:14:56 veiming Exp $
 *
 */

package com.sun.identity.security;

import java.security.Permission;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * This class <code>ISSecurityPermission</code> is used to protect the Access
 * Manager resources which should be accessed only by trusted application. The
 * resources this Permission is used to protect are: OpenSSO
 * administrator DN and password, and access to the encryption and decryption
 * methods used to encrypt all passwords in OpenSSO services. The
 * supported permissions is <code>"access"</code> and supported actions are
 * <code>"adminpassword"</code> and <code>"crypt"</code>. So in the Java
 * security policy file which will define the security options to grant this
 * permission to code bases, it should be done as below:
 * 
 * <pre>
 * grant codeBase "file:{directory where jars are located}/-" {
 * com.sun.identity.security.ISSecurityPermission "access",
 * "adminpassword,crypt"; };
 *</pre>
 * 
 * Note: The property <code>com.sun.identity.security.checkcaller</code>
 * should be set to true in <code>AMConfig.properties</code> file to enable the
 * Java security permissions check.
 *
 * @supported.all.api
 */
public class ISSecurityPermission extends Permission {
    private static Random rnd = new Random();

    private String perm;

    private Set actions = new HashSet();

    private int hashCode;

    /**
     * Constructs <code>ISSecurityPermission</code> object.
     * 
     * @param access
     *            Has to be string "access"
     * @param action
     *            Can be <code>adminpassword</code> or <code>crypt</code>.
     */
    public ISSecurityPermission(String access, String action) {
        super(access);
        perm = access;
        this.actions = convertActionStringToSet(action);
        hashCode = rnd.nextInt();
    }

    /**
     * Constructs <code>ISSecurityPermission</code> object. This constructor
     * sets the action to <code>"adminpassword"</code> by default.
     * 
     * @param access
     *            Has to be string "access"
     */
    public ISSecurityPermission(String access) {
        super(access);
        perm = access;
        actions = convertActionStringToSet("adminpassword");
        hashCode = rnd.nextInt();
    }

    /**
     * This method checks to see if this instance of
     * <code>ISSecurityPermission</code> implies the Permission being passed
     * as the argument. For more information on this, see the Javadocs of
     * <code>java.security.Permission</code>
     * 
     * @param p
     *            Instance of
     *            <code>com.sun.identity.security.ISSecurityPermission</code>
     * @return true if this instance of <code>ISSecurityPermission</code>
     *         implies the actions of the argument p. False otherwise
     *         <code>java.security.Permission</code>
     */
    public boolean implies(Permission p) {
        if (!(p instanceof ISSecurityPermission)) {
            return false;
        }
        Set pActions = convertActionStringToSet(p.getActions());
        // Action "crypt" is implied by the action "adminpassword"
        if (actions.contains("adminpassword")
                && (pActions.contains("adminpassword") || pActions
                        .contains("crypt"))) {
            return true;
        } else {
            if (pActions.contains("crypt") && actions.contains("crypt")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns hash code for this object.
     * 
     * @see java.security.Permission#hashCode()
     * @return hash code representing this object
     */
    public int hashCode() {
        return hashCode;
    }

    /**
     * Returns true if this object is equals to <code>o</code>.
     * 
     * @param o
     *            object fro comparison.
     * @return true if both object are similar.
     */
    public boolean equals(Object o) {
        if (o instanceof ISSecurityPermission) {
            ISSecurityPermission p = (ISSecurityPermission) o;
            if (p.hashCode() == hashCode) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see java.security.Permission#getActions()
     * @return String representation of actions supported by
     *         <code>ISSecurityPermission</code>
     */
    public String getActions() {
        return convertSetToActionString(actions);
    }

    private Set convertActionStringToSet(String ac) {
        StringTokenizer tzer = new StringTokenizer(ac, ",");
        Set res = new HashSet();
        while (tzer.hasMoreTokens()) {
            String tmp = tzer.nextToken();
            res.add(tmp);
        }
        return res;
    }

    private String convertSetToActionString(Set a) {
        StringBuffer sb = new StringBuffer();
        Iterator it = a.iterator();
        while (it.hasNext()) {
            String t = (String) it.next();
            sb.append(t).append(",");
        }
        String s = sb.toString();
        int lastComma = s.lastIndexOf(",");
        return s.substring(0, lastComma);
    }
}
