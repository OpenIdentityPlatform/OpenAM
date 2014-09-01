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
 * $Id: OpenSSOMonitoringUtil.java,v 1.2 2009/11/10 01:33:22 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2012 ForgeRock AS
 */
package com.sun.identity.monitoring;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.util.NetworkMonitor;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import java.util.Map;

public class MonitoringUtil {

    private static boolean initialized = false;
    private static boolean isMonAvailable = true;
    private static String [] networkMonitors = {
        "dbLookupPrivileges",
        "dbLookupReferrals",
        "privilegeSingleLevelEvaluation",
        "privilegeSubTreeEvaluation",
        "hasEntitlementMonitor",
        "evalSingleLevelMonitor",
        "evalSubTreeMonitor",
        "PrivilegeEvaluatorMonitorInit",
        "PrivilegeEvaluatorMonitorResourceIndex",
        "PrivilegeEvaluatorMonitorSubjectIndex",
        "PrivilegeEvaluatorMonitorSearch",
        "PrivilegeEvaluatorMonitorSearchNext",
        "PrivilegeEvaluatorMonitorSubmit",
        "PrivilegeEvaluatorMonitorCombineResults"
        };

    public static String HTTP_AUTH_FILE = "openam_mon_auth";

    private static Debug debug = Debug.getInstance("amMonitoring");

    // constructor
    private MonitoringUtil () {
    }

    public static String[] getNetworkMonitorNames() {
        return networkMonitors;
    }

    /**
     * return whether the specified network monitor
     * has been instantiated in the entitlements service yet
     */
    protected static boolean networkMonitorExist(String nwMonName) {
        String classMethod = "OpenSSOMonitoringUtil.networkMonitorExist: ";

        if (debug.messageEnabled()) {
            debug.message(classMethod + "checking " + nwMonName);
        }
        if ((nwMonName == null) || (nwMonName.length() == 0)) {
            if (debug.warningEnabled()) {
                debug.warning(classMethod + "isNull");
            }
            return false;
        }
        Set<String> ntwStats = NetworkMonitor.getInstanceNames();
        // names are all lowercase
        String ss = nwMonName.toLowerCase();
        if (ntwStats.contains(ss)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     *  Escapes colons (":") in the supplied String to "&#58;".
     *  Useful mostly for the MBean names, as jdmk really doesn't
     *  like them.
     */
    public static String escapeColonInString(String str) {
        if (str != null) {
            if (str.indexOf(":") >= 0) {
                str = str.replaceAll(":", "&#58;");
            }
        }
        return str;
    }

    protected static SSOToken getSSOToken() throws SSOException {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    protected static String getRealmDNForRealm(String realm) {
        String classMethod = "OpenSSOMonitoringUtil.getRealmDNForRealm:";
        String realmDN = "/";
        try {
            SSOToken ssoToken = getSSOToken();
            AMIdentityRepository idRepo =
                new AMIdentityRepository(ssoToken, realm);
            AMIdentity realmAMId = idRepo.getRealmIdentity();
            realmDN = realmAMId.getRealm();
        } catch (SSOException ex) {
            debug.error(classMethod + "SSOError getting token for realm '" +
                realm + "': " + ex.getMessage());
        } catch (IdRepoException ex) {
            debug.error(classMethod +
                "IdRepoError getting identity for realm '" +
                realm + "': " + ex.getMessage());
        }
        return realmDN;
    }

    protected static Map<String, String> getMonAuthList(String authFilePath) {
        String classMethod = "OpenSSOMonitoringUtil.getMonAuthList: ";

        if ((authFilePath == null) || ((authFilePath.trim().length() == 0))) {
            debug.error(classMethod + "No authentication file specified.");
            return null;
        }

        // prep for the "%BASE_DIR%/%SERVER_URI%/" style filepath
        if (authFilePath.contains("%BASE_DIR%") ||
            (authFilePath.contains("%SERVER_URI%")))
        {
            String ossoUri = SystemProperties.get(
                    Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
            ossoUri = ossoUri.replace('\\','/');
            String baseDir = SystemProperties.get(SystemProperties.CONFIG_PATH);
            baseDir = baseDir.replace('\\','/');
            if (ossoUri.startsWith("/")) {
                ossoUri = ossoUri.substring(1);
            }
            if (!ossoUri.endsWith("/")) {
                ossoUri += "/";
            }
            if (!baseDir.endsWith("/")) {
                baseDir += "/";
            }

            authFilePath = authFilePath.replaceAll("%BASE_DIR%", baseDir);
            authFilePath = authFilePath.replaceAll("%SERVER_URI%", ossoUri);
        }

        Map<String, String> hm = new HashMap<String, String>();
        try {
            BufferedReader frdr = new BufferedReader(new FileReader(authFilePath));
            String fbuff = null;
            while ((fbuff = frdr.readLine()) != null) {
                if (fbuff.trim().length() > 0)  {
                    StringTokenizer st = new StringTokenizer(fbuff);
                    // assume first is userid, second is password, ignore rest
                    if (st.countTokens() > 1) {
                        String userid = st.nextToken();
                        String passwd = st.nextToken();
                        String decpswd = AccessController.doPrivileged(
                                new DecodeAction(passwd));
                        hm.put(userid, decpswd);
                    }
                }
            }
            if (!hm.isEmpty()) {
                return hm;
            } else {
                return null;
            }
        } catch (IOException e) {
            debug.error(classMethod + "IOex on file " + authFilePath + ": " +
                e.getMessage());
        } catch (RuntimeException e) {
            debug.error(classMethod +
                "RuntimeEx on file " + authFilePath + ": ", e);
        } catch (Exception e) {
            debug.error(classMethod +
                "Exception on file " + authFilePath + ": ", e);
        }
        return null;
    }

    /**
     *  date/time format is "YYYY-MM-DD HH:MM:SS"
     *  it should be an 8-Byte array, where
     *    bytes 0-1: year
     *    byte    2: month
     *    byte    3: day
     *    byte    4: hours
     *    byte    5: minutes
     *    byte    6: seconds
     *    byte    7: deci-seconds (will be 0)
     * 
     * @param date
     * @return the converted date
     */
    public static Byte[] convertDate(String date) {
        StringTokenizer st = new StringTokenizer(date);
        String sdate = st.nextToken();
        String stime = st.nextToken();

        st = new StringTokenizer(sdate, "-");
        String year = st.nextToken();
        int iyear = 0;
        try {
            iyear = Integer.parseInt(year);
        } catch (NumberFormatException ex) {
            debug.error("MonitoringUtil.convertDate year = " + year + " not parsable");
        }
        byte yrlow = (byte) (iyear & 0xff);
        byte yrhigh = (byte) (((iyear & 0xff00) >> 8) & 0xff);
        String month = st.nextToken();
        String day = st.nextToken();

        st = new StringTokenizer(stime, ":");
        String hour = st.nextToken();
        String min = st.nextToken();
        String sec = st.nextToken();

        Byte bz = Byte.valueOf((byte) 0);
        Byte byrhi = bz;
        Byte byrlo = bz;
        Byte bmo = bz;
        Byte bdy = bz;
        Byte bhr = bz;
        Byte bmn = bz;
        Byte bsc = bz;
        try {
            byrhi = new Byte(yrhigh);
            byrlo = new Byte(yrlow);
            bmo = new Byte(month);
            bdy = new Byte(day);
            bhr = new Byte(hour);
            bmn = new Byte(min);
            bsc = new Byte(sec);
        } catch (NumberFormatException ex) {
            debug.error("MonitoringUtil error converting start date/time"
                    + ", date = " + sdate + ", time = " + stime);
        }
        Byte[] ret = new Byte[8];
        ret[0] = byrhi;
        ret[1] = byrlo;
        ret[2] = bmo;
        ret[3] = bdy;
        ret[4] = bhr;
        ret[5] = bmn;
        ret[6] = bsc;
        ret[7] = bz;

        return ret;
    }

    public static boolean isRunning() {
        if (!initialized) {
            try {
                Class.forName("com.sun.identity.monitoring.Agent");
            } catch (ClassNotFoundException cnfe) {
                isMonAvailable = false;
            } catch (NoClassDefFoundError ncdfe) {
                // if the Agent class is avaliable, but the SNMP library isn't (ssoadm)
                // TODO: fix this when the project is modularized
                isMonAvailable = false;
            }
            initialized = true;
        }
        
        return isMonAvailable ? Agent.isRunning() : false;
    }
}
