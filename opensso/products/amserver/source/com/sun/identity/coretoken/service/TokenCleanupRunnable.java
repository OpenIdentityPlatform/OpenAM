/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms of the Common
 * Development and Distribution License (the License). You may not use
 * this file except in compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each
 * file and include the License file at opensso/legal/CDDLv1.0.txt. If
 * applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: TokenCleanupRunnable.java,v 1.1 2009/11/19 00:07:40 qcheng Exp $
 */

package com.sun.identity.coretoken.service;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.coretoken.spi.OpenSSOCoreTokenStore;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.coretoken.CoreTokenException;
import com.sun.identity.coretoken.CoreTokenConstants;
import com.sun.identity.coretoken.CoreTokenUtils;
import com.sun.identity.coretoken.TokenLogUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import java.security.AccessController;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The class is used to cleanup expired token in the core token store.
 */
public class TokenCleanupRunnable extends GeneralTaskRunnable {

    // TODO : evaluate other options for cleanup based on performance
    // 1. use directory server plugin

    // run period of the cleanup thread
    private long runPeriod;

    /**
     * Constructor.
     * @param runPeriod The period for the clean up to run.
     */
    public TokenCleanupRunnable(long runPeriod) {
        this.runPeriod = runPeriod;
    }

    public boolean addElement(Object obj) {
        // no-op
        return true;
    }

    public boolean removeElement(Object obj) {
        // no-op
        return true;
    }

    public boolean isEmpty() {
        return false;
    }

    public long getRunPeriod() {
        return runPeriod;
    }

    public void run() {
        if (!runCleanup()) {
            // no need to run cleanup on this instance
            return;
        }

        CoreTokenUtils.debug.message("TokenCleanupRunnable.run : START");
        Set<String> tokenSet = getAllTokens();
        Iterator<String> tokens = tokenSet.iterator();
        if (CoreTokenUtils.debug.messageEnabled()) {
            CoreTokenUtils.debug.message("TokenCleanupRunnable.run : found "
                + tokenSet.size() + " tokens");
        }
        while (tokens.hasNext()) {
            String token = tokens.next();
            String dn = OpenSSOCoreTokenStore.getCoreTokenDN(token);
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            if (SMSEntry.checkIfEntryExists(dn, adminToken)) {
                try {
                    SMSEntry s = new SMSEntry(adminToken, dn);
                    String tokenExpiry = getTokenExpiry(s);
                    if (CoreTokenUtils.isTokenExpired(tokenExpiry)) {
                        s.delete();
                        // add logging
                        TokenLogUtils.access(Level.INFO,
                            TokenLogUtils.EXPIRED_TOKEN_DELETE_SUCCESS,
                            null, null, token);
                        if (CoreTokenUtils.debug.messageEnabled()) {
                            CoreTokenUtils.debug.message("TokenCleanupRunnable"
                               + ".run: removed expired token " + token);
                        }
                    }
                } catch (SMSException ex) {
                    CoreTokenUtils.debug.error("TokenCleanupRunnable.run", ex);
                } catch (SSOException ex) {
                    CoreTokenUtils.debug.error("TokenCleanupRunnable.run", ex);
                } catch (CoreTokenException ce) {
                    CoreTokenUtils.debug.error("TokenCleanupRunnable.run", ce);
                }
            }
        }
        CoreTokenUtils.debug.message("TokenCleanupRunnable.run : END");
    }

    private boolean runCleanup() {
        // TODO : need to define algorithm that only the cleanup thread is
        // run on one instance only in case of multi-server deployment.
        // to be done in build9
        return true;
    }

    private Set<String> getAllTokens() {

        SSOToken token = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        Set<String> results = new HashSet<String>();
        if (SMSEntry.checkIfEntryExists(
            OpenSSOCoreTokenStore.SERVICE_DN, token)) {
            Set<String> dns = null;
            try {
                dns = SMSEntry.search(token, OpenSSOCoreTokenStore.SERVICE_DN,
                    "ou=*", 0, 0, false, false);
            } catch (SMSException ex) {
                CoreTokenUtils.debug.error("TokenCleanupThread.getAllTokens",
                    ex);
            }
            for (String dn : dns) {
                if (!CoreTokenUtils.areDNIdentical(
                    OpenSSOCoreTokenStore.SERVICE_DN, dn)) {
                    String rdns[] = LDAPDN.explodeDN(dn, true);
                    if ((rdns != null) && rdns.length > 0) {
                        results.add(rdns[0]);
                    }
                }
            }
        }
        return results;
    }

    private String getTokenExpiry (SMSEntry s) {
        String tokenExpiry = null;
        Map<String, Set<String>> map = s.getAttributes();
        Set<String> attrVals = map.get(SMSEntry.ATTR_KEYVAL);
        if ((attrVals != null) && ! attrVals.isEmpty()) {
            for (Iterator<String> i = attrVals.iterator(); i.hasNext(); ) {
                String value = i.next();
                if (value.startsWith(OpenSSOCoreTokenStore.JSON_ATTR + "=")) {
                    String jsonAttr = value.substring(
                        OpenSSOCoreTokenStore.JSON_ATTR.length() + 1);
                    try {
                        JSONObject jObj = new JSONObject(jsonAttr);
                        JSONArray jArry = jObj.getJSONArray(
                                CoreTokenConstants.TOKEN_EXPIRY);
                        if ((jArry != null) && (jArry.length() != 0)) {
                            tokenExpiry = jArry.getString(0);
                            break;
                        }
                    } catch (JSONException ex) {
                        CoreTokenUtils.debug.error(
                            "TokenCleanupRunnable.getTokenExpity", ex);
                    }
                }
            }
        }
        return tokenExpiry;
    }
}
