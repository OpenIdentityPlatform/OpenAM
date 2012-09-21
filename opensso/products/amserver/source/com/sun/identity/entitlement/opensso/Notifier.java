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
 * $Id: Notifier.java,v 1.3 2010/01/07 00:19:11 veiming Exp $
 */

/*
 * Portions Copyrighted [2010-2011] [ForgeRock AS]
 */

package com.sun.identity.entitlement.opensso;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.entitlement.EntitlementThreadPool;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.interfaces.IThreadPool;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSException;
import com.sun.identity.shared.Constants;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.AccessController;
import java.util.Map;
import java.util.Set;

public class Notifier implements Runnable {
    private static final int CONN_TIMEOUT = 
        EntitlementService.getConfiguration("entitlement-notifier-conn-timeout",
        1000);
    private static final int NUM_RETRY =
        EntitlementService.getConfiguration("entitlement-notifier-retries", 3);
    private static final int WAIT_BETWEEN_RETRY = 
        EntitlementService.getConfiguration(
        "entitlement-notifier-duration-between-retries", 100);

    private static final String currentServerInstance =
        SystemProperties.getServerInstanceName();
    private String action;
    private Map<String, String> params;
    private static IThreadPool threadPool = new EntitlementThreadPool(4);
    private static boolean sitemonitorDisabled = Boolean.valueOf(
        SystemProperties.get(Constants.SITEMONITOR_DISABLED, "false")).booleanValue();

    public static void submit(String action, Map<String, String> params) {
        threadPool.submit(new Notifier(action, params));
    }

    private Notifier(String action, Map<String, String> params) {
        this.action = action;
        this.params = params;
    }

    public void run() {
        try {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            Set<String> serverURLs = 
                ServerConfiguration.getServerInfo(adminToken);

            for (String url : serverURLs) {
                int idx = url.indexOf("|");
                if (idx != -1) {
                    url = url.substring(0, idx);
                }

                if (sitemonitorDisabled || !url.equals(currentServerInstance)) {
                    String strURL = url + NotificationServlet.CONTEXT_PATH +
                        "/" + action;

                    StringBuilder buff = new StringBuilder();
                    boolean bFirst = true;
                    for (String k : params.keySet()) {
                        if (bFirst) {
                            bFirst = false;
                        } else {
                            buff.append("&");
                        }
                        buff.append(URLEncoder.encode(k, "UTF-8")).append("=")
                            .append(URLEncoder.encode(params.get(k),
                            "UTF-8"));
                    }

                    for (int i = 0; i < NUM_RETRY; i++) {
                        if (postRequest(strURL, buff.toString())) {
                            break;
                        } else {
                            try {
                                Thread.sleep(WAIT_BETWEEN_RETRY);
                            } catch (InterruptedException ex) {
                                //DO NOTHING
                            }
                        }
                    }
                }
            }
        } catch (UnsupportedEncodingException ex) {
            PrivilegeManager.debug.error("Notifier.notifyChanges", ex);
        } catch (IOException ex) {
            PrivilegeManager.debug.error("Notifier.notifyChanges", ex);
        } catch (SMSException ex) {
            PrivilegeManager.debug.error("Notifier.notifyChanges", ex);
        } catch (SSOException ex) {
            PrivilegeManager.debug.error("DataStore.notifyChanges", ex);
        }
    }

    private boolean postRequest(String strURL, String data)
        throws IOException {
        OutputStreamWriter wr = null;

        try {
            URL url = new URL(strURL);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setConnectTimeout(CONN_TIMEOUT);
            conn.setReadTimeout(CONN_TIMEOUT);
            conn.setDoOutput(true);
            wr = new OutputStreamWriter(
                conn.getOutputStream());
            wr.write(data);
            wr.flush();
            int status = conn.getResponseCode();
            return (status == HttpURLConnection.HTTP_OK);
        } catch (SocketTimeoutException e) {
            PrivilegeManager.debug.error("Notifier.post", e);
            return false;
        } finally {
            if (wr != null) {
                wr.close();
            }
        }
    }
}
