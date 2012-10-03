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
 * $Id: DNMapper.java,v 1.13 2009/11/20 23:52:56 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;

import com.sun.identity.shared.debug.Debug;

/**
 * This class is used to convert a DN to iplanet UID and vice versa.
 */
public class DNMapper {

    private static Debug debug = SMSEntry.debug;

    private static HashMap cache = new HashMap(2);

    // Look for realmEnabled and cache the value.
    protected static boolean realmEnabled = ServiceManager.isRealmEnabled();

    protected static String serviceDN = (new DN(SMSEntry.SERVICES_RDN
            + SMSEntry.COMMA + SMSEntry.baseDN)).toRFCString().toLowerCase();

    // This set is used in reversing the realm names to sdk format.
    static boolean migration = false;

    /**
     * Converts orgname which is "/" seperated to DN, else if DN normalize the
     * DN and return
     */
    public static String orgNameToDN(String orgName) {
        // Check if it is null or empty
        if (orgName == null || orgName.trim().length() == 0
                || orgName.equals(SMSEntry.SLASH_STR)) {
            return (SMSEntry.baseDN);
        }

        // Check in cache
        String orgdn = (String) cache.get(orgName);
        if (orgdn != null) {
            return (orgdn);
        }

        /*
         * Check if orgName is a valid DN. If so, check if realmEnabled. if
         * realmEnabled, 1) Check if rest of the DN (before the baseDN) has "o"
         * as the naming attribute. If not, replace it with 'o' and concat the
         * value with the previous naming attribute. eg.,if orgName is
         * dc=abc,l=xyz,o=coke,ou=services,dc=iplanet,dc=com then, the final
         * string should be
         * o=dc_abc,o=l_xyz,o=coke,ou=services,dc=iplanet,dc=com 2) Check if
         * "ou=services" is present in the orgName. If not add it to the
         * orgName.
         */
        DN orgdnObject = new DN(orgName);
        if (orgdnObject.isDN()) {
            // If orgName is either the baseDN or root service's DN
            // return the baseDN
            orgdn = orgdnObject.toRFCString();
            String orgdnlc = orgdn.toLowerCase();

            // Check if orgdn is a hidden internal realm, if so return
            if (orgdnlc.startsWith(SMSEntry.SUN_INTERNAL_REALM_PREFIX)) {
                orgdn = LDAPDN.explodeDN(orgdn, false)[0] + "," + serviceDN;
                // Add to cache and return
                updateCache(orgName, orgdn);
                return (orgdn);
            }
            // Check for root suffix and SMS base DN
            if (orgdnlc.equals(SMSEntry.baseDN) || 
                orgdnlc.equals(serviceDN) ||
                orgdnlc.equals(SMSEntry.amsdkbaseDN)) {
                // Add to cache and return
                updateCache(orgName, SMSEntry.baseDN);
                return (SMSEntry.baseDN);
            }
            // If realm is enabled, normalize the DN and return
            if (realmEnabled) {
                int ndx = orgdn.indexOf(serviceDN);
                if (ndx == -1) {
                    // Check for baseDN
                    ndx = orgdn.lastIndexOf(SMSEntry.baseDN);
                }
                if (ndx > 0) {
                    orgdn = orgdn.substring(0, ndx - 1);
                }
                int indx = orgdn.lastIndexOf(SMSEntry.COMMA);
                if (indx >= 0) {
                    if (orgdn.substring(indx).equals(SMSEntry.COMMA)) {
                        orgdn = orgdn.substring(0, indx);
                    }
                }
                if (debug.messageEnabled()) {
                    debug.message("DNMapper.orgNameToDN():orgdn " + orgdn);
                }
                String answer = normalizeDN(orgdn) + serviceDN;
                if (debug.messageEnabled()) {
                    debug.message("DNMapper.orgNameToDN(" + orgName + ")="
                            + answer);
                }
                // Add to cache and return
                updateCache(orgName, answer);
                return (answer);
            } else if (!migration) {
                // Check if "ou=services" is present, if present remove it
                orgdn = replaceString(orgdn, ",ou=services,", ",");
                // Add to cache and return
                updateCache(orgName, orgdn);
                return (orgdn);
            } else {
                // When SMS Migration to 7.0 happens, the coexist mode is
                // 'true' and realm is 'false'. In coexist mode, the
                // 'ou=services' gets removed. But we need the new realm node
                // for data migration from old DIT to new realm tree.
                // So after creation of the realm during SMSMigration70,
                // we set the DNMapper.migration flag to true to avoid
                // removal of 'ou=services' from the newly formed realm DN
                // and return the orgdn as such to the serviceconfig* class.

                return (orgdn);
            }
        }

        // The org name is "/" separated, construct the DN
        StringBuffer buf = convertToDN(orgName);

        if (realmEnabled
                || buf.toString().toLowerCase().indexOf(
                        SMSEntry.SUN_INTERNAL_REALM_NAME) != -1) {
            buf.append(",").append(serviceDN);
        } else if (SMSEntry.baseDN.length() > 0) {
            buf.append(",").append(SMSEntry.baseDN);
        }
        if (debug.messageEnabled()) {
            debug.message("DNMapper.orgNameToDN(" + orgName + ")="
                    + buf.toString());
        }
        String answer = buf.toString();
        // Add to cache
        updateCache(orgName, answer);
        return (answer);
    }

    private static void updateCache(String orgName, String realmName) {
        HashMap ncache = new HashMap(cache);
        // %%% TODO Need to check the size and remove least recently used
        ncache.put(orgName, realmName);
        cache = ncache;
    }

    /**
     * Converts realm name to AMSDK compliant organization name
     */
    public static String realmNameToAMSDKName(String realmName) {
        String dn = orgNameToDN(realmName);
        String dnlc = dn.toLowerCase();
        if (debug.messageEnabled()) {
            debug.message("DNMapper.realmNameToAMSDKName realmName ="
                    + realmName);
            debug.message("DNMapper.realmNameToAMSDKName orgDN =" + dn);
        }

        // Check for baseDN and internal hidden realm names
        if ((dnlc.equals(SMSEntry.baseDN)) &&
            (!dnlc.equals(SMSEntry.amsdkbaseDN))) {
            return (SMSEntry.amsdkbaseDN);
        }

        if (dnlc.equals(SMSEntry.baseDN)
                || dnlc.startsWith(SMSEntry.SUN_INTERNAL_REALM_PREFIX)) {
            return (SMSEntry.baseDN);
        }

        // If realm is not enabled, remove "ou=services" node
        StringBuilder buf = new StringBuilder(dn.length());
        String orgAttr = OrgConfigViaAMSDK.getNamingAttrForOrg();

        // If orgAttr is null or is "o", return after removing "ou=services"
        if (orgAttr == null
                || orgAttr.equalsIgnoreCase(SMSEntry.ORGANIZATION_RDN)) {
            String answer = replaceString(dn, ",ou=services,", ",");
            if (debug.messageEnabled()) {
                debug.message("DNMapper.realmNameToAMSDKName sdkName ="
                        + answer);
            }
            return (answer);
        }

        // Remove the baseDN and parse the DN
        int index = dnlc.indexOf(serviceDN);
        if (index == -1) {
            // Try the baseDN
            index = dnlc.indexOf(SMSEntry.baseDN);
        }

        String answer = (index == -1) ? dn : dn.substring(0, index - 1);
        String[] rdns = LDAPDN.explodeDN(answer, true);
        int size = rdns.length;
        for (int i = 0; i < size; i++) {
            buf.append(orgAttr).append(SMSEntry.EQUALS).append(rdns[i]);
            buf.append(',');
        }
        // Append baseDN and return
        buf.append(SMSEntry.baseDN);
        if (debug.messageEnabled()) {
            debug.message("DNMapper.realmNameToAMSDKName sdkName ="
                    + buf.toString());
        }
        return (buf.toString());
    }

    /**
     * Returns realm name in "/" separated format for the provided
     * realm/organization name in DN format.
     * 
     * @param orgName Name of organization.
     * @return DN format "/" separated realm name of organization name.
     */
    public static String orgNameToRealmName(String orgName) {
        if ((orgName == null) || (orgName.length() == 0)) {
            return "/";
        }
        if (orgName.equalsIgnoreCase(SMSEntry.baseDN) ||
            orgName.equalsIgnoreCase(serviceDN)
        ) {
            return "/";
        }
        DN orgdnObject = new DN(orgName);
        if (!orgdnObject.isDN()) {
            return orgName;
        }

        StringBuilder answer = new StringBuilder(100);
        answer.append("/");

        Set resultSet = new HashSet(2);
        resultSet.add(orgName);

        // Check if orgName ends with baseDN or serviceDN
        String orgdn = orgdnObject.toRFCString();
        String orgdnlc = orgdn.toLowerCase();
        Set returnSet = null;

        if (orgdnlc.endsWith(serviceDN)) {
            returnSet = SMSEntry.parseResult(resultSet, serviceDN, true);
        } else if (orgdnlc.endsWith(SMSEntry.baseDN)) {
            returnSet = SMSEntry.parseResult(resultSet, serviceDN, true);
        }
        if (returnSet != null && !returnSet.isEmpty()) {
            answer.append(returnSet.iterator().next().toString());
        }
        return (answer.toString());
    }

    /*
     * Splits a string and returns the tokens.
     * 
     * @param str original String. 
     * @return a String Array object of tokens after split.
     */
    static String[] splitString(String str) {
        String[] strArray = new String[2];
        int idx = str.indexOf('=');

        if (idx != -1) {
            strArray[0] = str.substring(0, idx).trim();
            strArray[1] = str.substring(idx + 1).trim();
        }
        return strArray;
    }

    /*
     * Replaces a string with another string in a String object.
     * 
     * @param originalString original String. 
     * @param token string to be replaced. 
     * @param newString new string to replace token. 
     * @return a String object after replacement.
     * 
     */
    static String replaceString(String originalString, String token,
            String newString) {
        int lenToken = token.length();
        int idx = originalString.indexOf(token);

        if (!originalString.startsWith(SMSEntry.SLASH_STR)) {
            if (idx >= 0) {
                int slashndx =
                    originalString.substring(idx).indexOf(SMSEntry.SLASH_STR);
                // This is to escape "/" embedded in realm names.
                while (slashndx != -1) {
                    originalString = originalString.substring(0, slashndx) +
                        "&#47;" + originalString.substring(slashndx+1);
                    slashndx =
                        originalString.indexOf(SMSEntry.SLASH_STR, slashndx+5);
                }
            }
        }
        while (idx != -1) {
            originalString = originalString.substring(0, idx) + newString
                    + originalString.substring(idx + lenToken);
            idx = originalString.indexOf(token, idx + lenToken);
        }

        if (debug.messageEnabled()) {
            debug.message("DNMapper.replaceString() " + originalString);
        }
        return originalString;
    }

    /**
     * Normalized the DN as per the Realm requirements for organization name
     */
    static String normalizeDN(String orgName) {
        String orgAttr = "";
        String placeHold = "";
        StringBuilder buf = new StringBuilder(orgName.length());
        String[] rdns = LDAPDN.explodeDN(orgName, false);
        int size = rdns.length;

        if (debug.messageEnabled()) {
            debug.message("DNMapper.normalizeDN():orgName "+ orgName);
        }
        if (!realmEnabled) {
            orgAttr = OrgConfigViaAMSDK.getNamingAttrForOrg();
        }
        placeHold = (realmEnabled) ? SMSEntry.ORGANIZATION_RDN : orgAttr;
        for (int i = 0; i < size; i++) {
            String[] strArr = splitString(rdns[i]);

            // Check if orgName is a hidden internal realm,if so prepend with o
            if (orgName.toLowerCase().
                startsWith(SMSEntry.SUN_INTERNAL_REALM_PREFIX)) {
                buf.append(SMSEntry.ORGANIZATION_RDN);
            } else {
                buf.append(placeHold);
            }
            buf.append(SMSEntry.EQUALS)
               .append(strArr[1]).append(SMSEntry.COMMA);
        }
        if (debug.messageEnabled()) {
            debug.message("DNMapper.normalizeDN():finalorgdn "+
                buf.toString());
        }
        return (buf.toString());
    }

    /**
     * Converts "/" separted organization names to DN
     */
    static StringBuffer convertToDN(String orgName) {
        StringBuffer buf = new StringBuffer();
        String placeHold = (realmEnabled) ? SMSEntry.ORGANIZATION_RDN
                : OrgConfigViaAMSDK.getNamingAttrForOrg();
        ArrayList arr = new ArrayList();
        StringTokenizer strtok = new StringTokenizer(orgName, "/");
        while (strtok.hasMoreElements()) {
            String token = strtok.nextToken().trim();
            if (token != null && token.length() != 0) {
                arr.add(token);
            }
        }
        int size = arr.size();
        for (int i = 0; i < size; i++) {
            String theOrg = (String) arr.get(size - i - 1);
            // Check if orgdn is a hidden internal realm, if so prepend with o
            if (theOrg.toLowerCase().startsWith(
                    SMSEntry.SUN_INTERNAL_REALM_NAME)) {
                placeHold = SMSEntry.ORGANIZATION_RDN;
            }
            buf.append(placeHold);
            buf.append('=').append(theOrg);
            if (i != size - 1) {
                buf.append(',');
            }
        }
        if (debug.messageEnabled()) {
            debug.message("DNMapper.convertToDN():finalorgdn "+
                buf.toString());
        }
        if ((buf.toString()).indexOf("&#47;") >= 0) {
            String realmName = SMSSchema.unescapeName(buf.toString());
            if (debug.messageEnabled()) {
                debug.message("DNMapper.convertToDN():realmName "+realmName);
            }
            StringBuffer newBuf =  new StringBuffer();
            newBuf.append(realmName);
            buf = newBuf;
            if (debug.messageEnabled()) {
                debug.message("DNMapper.convertToDN():newRealmName "+
                    buf.toString());
            }
        }
        return (buf);
    }

    static void clearCache() {
        cache = new HashMap();
        realmEnabled = ServiceManager.isRealmEnabled();
    }
}
