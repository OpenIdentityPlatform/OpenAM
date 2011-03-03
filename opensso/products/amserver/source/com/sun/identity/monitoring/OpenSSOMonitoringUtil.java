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
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.monitoring;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Iterator;
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

public class OpenSSOMonitoringUtil {

    private String [] networkMonitors = {
        "dbLookupPrivileges",
        "dbLookupReferrals",
        "privilegeSingleLevelEvaluation",
        "privilegeSubTreeEvaluation",
        "hasEntitltmentMonitor",
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

    public String HTTP_AUTH_FILE = "opensso_mon_auth";

    Debug debug = Debug.getInstance("amMonitoring");

    // constructor
    public OpenSSOMonitoringUtil () {
    }

    public String[] getNetworkMonitorNames() {
        return networkMonitors;
    }

    /**
     * return whether the specified network monitor
     * has been instantiated in the entitlements service yet
     */
    protected boolean networkMonitorExist(String nwMonName) {
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
    public String escapeColonInString(String str) {
        if (str != null) {
            if (str.indexOf(":") >= 0) {
                str = str.replaceAll(":", "&#58;");
            }
        }
        return str;
    }

    protected SSOToken getSSOToken() throws SSOException {
        return (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    }

    protected String getRealmDNForRealm(String realm) {
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

    protected String[][] getMonAuthList() {
        String classMethod = "OpenSSOMonitoringUtil.getMonAuthList";
        String ossoUri =
            SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        ossoUri = ossoUri.replace('\\','/');
        String baseDir = SystemProperties.get(SystemProperties.CONFIG_PATH);
        baseDir = baseDir.replace('\\','/');
        if (ossoUri.startsWith("/")) {
            byte[] btmp = ossoUri.getBytes();
            ossoUri = new String(btmp, 1, (btmp.length - 1));
        }
        if (!ossoUri.endsWith("/")) {
            ossoUri += "/";
        }
        if (!baseDir.endsWith("/")) {
            baseDir += "/";
        }
        String filePath = baseDir + ossoUri + HTTP_AUTH_FILE;

        HashMap hm = new HashMap();
        try {
            BufferedReader frdr = new BufferedReader(new FileReader(filePath));
            String fbuff = null;
            while ((fbuff = frdr.readLine()) != null) {
                if (fbuff.trim().length() > 0)  {
                    StringTokenizer st = new StringTokenizer(fbuff);
                    // assume first is userid, second is password, ignore rest
                    if (st.countTokens() > 1) {
                        String userid = st.nextToken();
                        String passwd = st.nextToken();
                        String decpswd =
                            (String)AccessController.doPrivileged(
                                new DecodeAction(passwd));
                        hm.put(userid, decpswd);
                    }
                }
            }
            if (!hm.isEmpty()) {
                int len = hm.size();
                String ents[][] = new String[len][2];
                Set hs = hm.keySet();
                int i = 0;
                for (Iterator it = hs.iterator(); it.hasNext(); ) {
                    String userid = (String)it.next();
                    ents[i][0] = userid;
                    ents[i++][1] = (String)hm.get(userid);
                }
                return(ents);
            } else {
                return null;
            }
        } catch (IOException e) {
            debug.error(classMethod + "IOex on file " + filePath + ": " +
                e.getMessage());
        } catch (RuntimeException e) {
            debug.error(classMethod +
                "RuntimeEx on file " + filePath + ": ", e);
        } catch (Exception e) {
            debug.error(classMethod +
                "Exception on file " + filePath + ": ", e);
        }
        return null;
    }

    protected String[][] getMonAuthList(String authFilePath) {
        String classMethod = "OpenSSOMonitoringUtil.getMonAuthList: ";

        if ((authFilePath == null) || (!(authFilePath.trim().length() > 0))) {
            debug.error(classMethod + "No authentication file specified.");
            return null;
        }

        String filePath = authFilePath;

        // prep for the "%BASE_DIR%/%SERVER_URI%/" style filepath
        if (authFilePath.contains("%BASE_DIR%") ||
            (authFilePath.contains("%SERVER_URI%")))
        {
            String ossoUri =
                SystemProperties.get(
                    Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
            ossoUri = ossoUri.replace('\\','/');
            String baseDir = SystemProperties.get(SystemProperties.CONFIG_PATH);
            baseDir = baseDir.replace('\\','/');
            if (ossoUri.startsWith("/")) {
                byte[] btmp = ossoUri.getBytes();
                ossoUri = new String(btmp, 1, (btmp.length - 1));
            }
            if (!ossoUri.endsWith("/")) {
                ossoUri += "/";
            }
            if (!baseDir.endsWith("/")) {
                baseDir += "/";
            }

            filePath = filePath.replaceAll("%BASE_DIR%", baseDir);
            filePath = filePath.replaceAll("%SERVER_URI%", ossoUri);
        }

        HashMap hm = new HashMap();
        try {
            BufferedReader frdr = new BufferedReader(new FileReader(filePath));
            String fbuff = null;
            while ((fbuff = frdr.readLine()) != null) {
                if (fbuff.trim().length() > 0)  {
                    StringTokenizer st = new StringTokenizer(fbuff);
                    // assume first is userid, second is password, ignore rest
                    if (st.countTokens() > 1) {
                        String userid = st.nextToken();
                        String passwd = st.nextToken();
                        String decpswd =
                            (String)AccessController.doPrivileged(
                                new DecodeAction(passwd));
                        hm.put(userid, decpswd);
                    }
                }
            }
            if (!hm.isEmpty()) {
                int len = hm.size();
                String ents[][] = new String[len][2];
                Set hs = hm.keySet();
                int i = 0;
                for (Iterator it = hs.iterator(); it.hasNext(); ) {
                    String userid = (String)it.next();
                    ents[i][0] = userid;
                    ents[i++][1] = (String)hm.get(userid);
                }
                return(ents);
            } else {
                return null;
            }
        } catch (IOException e) {
            debug.error(classMethod + "IOex on file " + filePath + ": " +
                e.getMessage());
        } catch (RuntimeException e) {
            debug.error(classMethod +
                "RuntimeEx on file " + filePath + ": ", e);
        } catch (Exception e) {
            debug.error(classMethod +
                "Exception on file " + filePath + ": ", e);
        }
        return null;
    }
}
