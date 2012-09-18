/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: DirectoryServerVendor.java,v 1.3 2009/01/28 05:35:03 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */
package com.sun.identity.sm;

import java.util.Enumeration;
import com.sun.identity.shared.ldap.LDAPAttribute;
import com.sun.identity.shared.ldap.LDAPAttributeSet;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPEntry;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPSearchResults;
import com.sun.identity.shared.ldap.LDAPv2;

/**
 * This singleton attempts to query vendor information of a Directory Server. 
 */
public class DirectoryServerVendor {
    public static String OPENDS = "opends";
    public static String OPENDJ = "opendj";
    public static String ODSEE = "odsee";

    private static String VENDOR_OPENDJ = "OpenDJ ";
    private static String VENDOR_OPENDS = "OpenDS Directory Server ";
    private static String VENDOR_SUNDS_5 = "Sun-ONE-Directory/";
    private static String VENDOR_SUNDS_6 = "Sun-Java(tm)-System-Directory/";
    private static String VENDOR_SUNDS_7 = "Sun-Directory-Server/";
    private static String VENDOR_ODSEE_11 = "Oracle Directory Server Enterprise Edition";
    private static DirectoryServerVendor instance = new DirectoryServerVendor();
    
    private String[] attrs = {
        "vendorversion", "rootDomainNamingContext", "forestFunctionality"};
    private Vendor unknownVendor = new Vendor("unknown", "unknown");
    
    private DirectoryServerVendor () {
    }
    
    /**
     * Returns an instance of this class.
     * 
     * @return an instance of this class.
     */
    public static DirectoryServerVendor getInstance() {
        return instance;
    }
    
    /**
     * Returns the vendor of Directory Server.
     * @param conn LDAP connection to the server.
     * @return the vendor of Directory Server.
     * @throws LDAPException if unable to get the vendor information.
     */
    public Vendor query(LDAPConnection conn) throws LDAPException {
        String result = null;
        LDAPSearchResults res = conn.search("", LDAPv2.SCOPE_BASE,
            "(objectclass=*)", attrs, false);

        while (res.hasMoreElements()) {
            LDAPEntry findEntry = (LDAPEntry) res.nextElement();

            /* Get the attributes of the root DSE. */
            LDAPAttributeSet findAttrs = findEntry.getAttributeSet();
            Enumeration enumAttrs = findAttrs.getAttributes();

            while (enumAttrs.hasMoreElements()) {
                LDAPAttribute anAttr = (LDAPAttribute) enumAttrs.nextElement();
                String attrName = anAttr.getName();

                if (attrName.equalsIgnoreCase("vendorversion")) {
                    Enumeration enumVals = anAttr.getStringValues();

                    while (enumVals.hasMoreElements()) {
                        result = (String)enumVals.nextElement();
                        break;
                    }
                }
            }
        }
        
        Vendor vendor = unknownVendor;
        
        if (result != null) {
            if (result.startsWith(VENDOR_OPENDJ)) {
                String version = result.substring(VENDOR_OPENDJ.length());
                vendor = new Vendor(OPENDJ, version);
            } else if (result.startsWith(VENDOR_OPENDS)) {
                String version = result.substring(VENDOR_OPENDS.length());
                vendor = new Vendor(OPENDS, version);
            } else if (result.startsWith(VENDOR_SUNDS_5)) {
                String version = result.substring(VENDOR_SUNDS_5.length());
                vendor = new Vendor(ODSEE, version);
            } else if (result.startsWith(VENDOR_SUNDS_6)) {
                String version = result.substring(VENDOR_SUNDS_6.length());
                vendor = new Vendor(ODSEE, version);
            } else if (result.startsWith(VENDOR_SUNDS_7)) {
                String version = result.substring(VENDOR_SUNDS_7.length());
                vendor = new Vendor(ODSEE, version);
            }  else if (result.startsWith(VENDOR_ODSEE_11)) {
                String version = result.substring(VENDOR_ODSEE_11.length());
                vendor = new Vendor(ODSEE, version);
            }
        }

        return vendor;
    }
    
    public class Vendor {
        
        public String name;
        public String version;
        
        public Vendor(String name, String version) {
            this.name = name;
            this.version = version;
        }
        
        public String toString() {
            return name + " " + version;
        }
    }
}
