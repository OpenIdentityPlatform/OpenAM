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
 * $Id: SMSRepositoryMig.java,v 1.4 2009/01/28 05:35:04 ww203982 Exp $
 *
 */

package com.sun.identity.sm.util;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;

import com.sun.identity.shared.ldap.LDAPAttribute;
import com.sun.identity.shared.ldap.LDAPAttributeSet;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPEntry;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPReferralException;
import com.sun.identity.shared.ldap.LDAPSearchResults;
import com.sun.identity.shared.ldap.LDAPv2;

import com.sun.identity.sm.ServiceAlreadyExistsException;
import com.sun.identity.sm.flatfile.SMSFlatFileObject;

/**
 * Migrates a SMS in LDAP to flat file structure usable # by SMSFlatFileObject
 * class.
 */
public class SMSRepositoryMig {

    static private void createSMSEntry(SMSFlatFileObject smsFlatFileObject,
            String dn, LDAPAttributeSet attrs) throws Exception {
        // Convert attrs from LDAPAttributeSet to a Map needed by SMSObject.
        HashMap attrsMap = new HashMap();

        Enumeration attrsEnum = attrs.getAttributes();
        while (attrsEnum.hasMoreElements()) {
            LDAPAttribute attr = (LDAPAttribute) attrsEnum.nextElement();
            String attrName = attr.getName();
            HashSet attrVals = new HashSet();
            Enumeration valsEnum = attr.getStringValues();
            while (valsEnum.hasMoreElements()) {
                String val = (String) valsEnum.nextElement();
                attrVals.add(val);
            }
            attrsMap.put(attrName, attrVals);
        }
        try {
            smsFlatFileObject.create(null, dn, attrsMap);
        } catch (ServiceAlreadyExistsException e) {
            System.out.println("Warning: '" + dn + "' already exists.");
        }
    }

    static private void migrate(String host, int port, String binddn,
            String pw, String basedn, String flatfiledir) throws Exception {
        // check args
        if (port < 0 || binddn == null || binddn.length() == 0 || pw == null
                || pw.length() == 0 || basedn == null || basedn.length() == 0
                || flatfiledir == null || flatfiledir.length() == 0) {
            throw new IllegalArgumentException(
                    "SMSRepositoryMig: One or more invalid " +
                    "arguments in constructor");
        }

        // Create the SMSFlatFileObject
        SMSFlatFileObject smsFlatFileObject = new SMSFlatFileObject();
        LDAPConnection conn = null;
        try {
            conn = new LDAPConnection();
            conn.connect(host, port, binddn, pw);
            String[] attrs = { "*" };

            // Loop through LDAP attributes, create SMS object for each.
            LDAPSearchResults res = conn.search("ou=services," + basedn,
                LDAPv2.SCOPE_SUB, "(objectclass=*)", attrs, false);
            System.out.println("Migrating " + res.getCount() + " results.");
            while (res.hasMoreElements()) {
                LDAPEntry entry = null;
                try {
                    entry = res.next();
                    LDAPAttributeSet attrSet = entry.getAttributeSet();
                    System.out.println(entry.getDN() + ": " + attrSet.size()
                        + " Attributes found.");
                    createSMSEntry(smsFlatFileObject, entry.getDN(), attrSet);
                } catch (LDAPReferralException e) {
                    System.out.println("ERROR: LDAP Referral not supported.");
                    System.out.println("LDAPReferralException received: "
                        + e.toString());
                    e.printStackTrace();
                } catch (LDAPException e) {
                    System.out.println("ERROR: LDAP Exception encountered: "
                        + e.toString());
                    e.printStackTrace();
                }
            }
        } finally {
            if ((conn != null) && (conn.isConnected())) {
                try {
                    conn.disconnect();
                } catch (LDAPException ex) {
                    //ignored
                }
            }
        }
    }

    static private void usage() {
        System.out
                .println("Usage: "
                        + SMSRepositoryMig.class.getName()
                        + " <host> <port> <binddn> <password> " +
                                "<orgdn> <flat-file directory>");
    }

    static public void main(String[] args) throws Exception {
        String host, binddn, pw, basedn, flatfiledir;
        int port;
        if (args.length < 6) {
            usage();
            System.exit(0);
        }

        host = args[0];
        port = Integer.parseInt(args[1]);
        binddn = args[2];
        pw = args[3];
        basedn = args[4];
        flatfiledir = args[5];

        // do the migration
        migrate(host, port, binddn, pw, basedn, flatfiledir);
    }
}
