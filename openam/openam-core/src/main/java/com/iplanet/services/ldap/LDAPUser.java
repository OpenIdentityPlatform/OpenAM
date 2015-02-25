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
 * $Id: LDAPUser.java,v 1.3 2008/06/25 05:41:36 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.ldap;

import com.iplanet.services.util.Crypt;
import com.iplanet.services.util.GenericNode;
import com.iplanet.services.util.ParseOutput;
import com.iplanet.services.util.XMLParser;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class represents user who is going to be authorized to log onto the
 * server. This class also contains the user credentials. Currently the
 * credential information is password only, but it extensible to certificates.
 */
public class LDAPUser implements ParseOutput {

    public LDAPUser() {
        userType = Type.AUTH_ANONYMOUS;
    }

    public void process(
        XMLParser parser,
        String name,
        Vector elems,
        Hashtable atts, 
        String Pcdata
    ) { 
        if (DSConfigMgr.debugger.messageEnabled()) {
            DSConfigMgr.debugger.message("in LDAPUser.process()");
        }

        userID = (String) atts.get(DSConfigMgr.NAME);

        for (int i = 0; i < elems.size(); i++) {
            GenericNode genNode = (GenericNode) elems.elementAt(i);
            if (genNode._name.equals(DSConfigMgr.AUTH_ID)) {
                // Get the bind dn
                userName = genNode._pcdata;
            }

            // The auth type.
            String str = (String) atts.get(DSConfigMgr.AUTH_TYPE);
            if (str == null || str.equalsIgnoreCase(DSConfigMgr.VAL_AUTH_BASIC))
            {
                userType = Type.AUTH_BASIC;
            } else if (str.equalsIgnoreCase(DSConfigMgr.VAL_AUTH_PROXY)) {
                userType = Type.AUTH_PROXY;
            } else if (str.equalsIgnoreCase(DSConfigMgr.VAL_AUTH_REBIND)) {
                userType = Type.AUTH_REBIND;
            } else if (str.equalsIgnoreCase(DSConfigMgr.VAL_AUTH_ADMIN)) {
                userType = Type.AUTH_ADMIN;
            } else {
                userType = Type.AUTH_ANONYMOUS;
            }

            // If this element is the password.
            if (genNode._name.equals(DSConfigMgr.AUTH_PASSWD)) {
                userPasswd = genNode._pcdata;
            }
        }
    }

    /**
     * Get the authentication ID of this user.
     * 
     * @return String The LDAP Bind DN
     */
    protected String getAuthID() {
        if (userName == null)
            return ANONYMOUS_USER;
        return userName;
    }

    /**
     * Get the authentication password.
     * 
     * @return String the bind password.
     */
    protected String getPasswd() {
        if (userPasswd == null)
            return ANONYMOUS_PASSWD;
        /*
         * return (String) AccessController.doPrivileged( new
         * DecodeAction(userPasswd));
         */
        return Crypt.decode(userPasswd);
    }

    /**
     * Get the type of authentication of this user.
     * 
     * @return Type The authentication type.
     */
    public Type getAuthType() {
        return userType;
    }

    public String getUserID() {
        return userID;
    }

    public static final String ANONYMOUS_USER = "";

    public static final String ANONYMOUS_PASSWD = "";

    private String userID;

    private String userName;

    private Type userType;

    private String userPasswd;

    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append("UserID=");
        buf.append(userID);
        buf.append(" UserName=");
        buf.append(userName);
        buf.append(" authType=");
        buf.append(userType.toString());
        buf.append(" authPassword=");
        buf.append(userPasswd);
        return buf.toString();
    }

    public static class Type {
        int authType = -1;

        /**
         * The user has anonyomous rights.
         */
        public static final Type AUTH_ANONYMOUS = new Type(0);

        /**
         * The user is authenticated with a rootdn and password.
         */
        public static final Type AUTH_BASIC = new Type(1);

        /**
         * The user has proxy rights.
         */
        public static final Type AUTH_PROXY = new Type(2);

        /**
         * This user must be used only for rebind
         */
        public static final Type AUTH_REBIND = new Type(3);

        /**
         * This user has root privilages.
         */
        public static final Type AUTH_ADMIN = new Type(4);

        private Type(int type) {
            authType = type;
        }

        public boolean equals(Type type) {
            return (authType == type.authType);
        }

        public String toString() {
            if (equals(AUTH_ANONYMOUS)) {
                return "ANONYMOUS";
            }
            if (equals(AUTH_BASIC)) {
                return "BASIC";
            }
            if (equals(AUTH_PROXY)) {
                return "PROXY";
            }
            if (equals(AUTH_REBIND)) {
                return "REBIND";
            }
            if (equals(AUTH_ADMIN)) {
                return "ADMIN";
            }
            return "ANONYMOUS";
        }
    }

}
