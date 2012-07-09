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
 * $Id: PrivilegeChangeNotifier.java,v 1.5 2010/01/07 00:19:11 veiming Exp $
 */

package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ResourceName;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.json.JSONException;
import org.json.JSONObject;

public class PrivilegeChangeNotifier {
    private static PrivilegeChangeNotifier instance = new
        PrivilegeChangeNotifier();
    private static int POOL_SIZE = 5;
    private static int HTTP_TIMEOUT = 1000;
    private static int NUM_RETRY = 3;
    private static int RETRY_INTERVAL = 3000;

    static {
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            PrivilegeManager.superAdminSubject, "/");
        POOL_SIZE = getConfiguration(ec,
            "privilege-notifier-threadpool-size", 5);
        HTTP_TIMEOUT = getConfiguration(ec,
            "privilege-notifier-conn-timeout", 1000);
        NUM_RETRY = getConfiguration(ec,
            "privilege-notifier-retries", 3);
        RETRY_INTERVAL = getConfiguration(ec,
            "privilege-notifier-duration-between-retries", 3000);
    }

    private static int getConfiguration(
        EntitlementConfiguration ec,
        String name,
        int defaultVal
    ) {
        Set<String> values = ec.getConfiguration(name);
        if ((values == null) || values.isEmpty()) {
            return defaultVal;
        }

        try {
            return Integer.parseInt(values.iterator().next());
        } catch (NumberFormatException e) {
            PrivilegeManager.debug.error(
                "PrivilegeChangeNotifier.getConfiguration: attribute name=" +
                name, e);
            return defaultVal;
        }
    }


    private static EntitlementThreadPool thrdPool =
        new EntitlementThreadPool(POOL_SIZE);



    private PrivilegeChangeNotifier() {
    }

    public static PrivilegeChangeNotifier getInstance() {
        return instance;
    }

    public void notify(
        Subject adminSubject,
        String realm,
        String applicationName,
        String privilegeName,
        Set<String> resources) {
        try {
            Set<EntitlementListener> listeners =
                ListenerManager.getInstance().getListeners(adminSubject);
            Set<URL> urls = new HashSet<URL>();

            for (EntitlementListener l : listeners) {
                if (toSendNotification(adminSubject, realm, l,
                    applicationName, resources)) {
                    urls.add(l.getUrl());
                }
            }

            String json = toJSONString(realm, privilegeName, resources);
            for (URL url : urls) {
                thrdPool.submit(new Task(adminSubject, url.toString(), json));
            }
        } catch (JSONException e) {
            PrivilegeManager.debug.error("PrivilegeChangeNotifier.notify", e);
        } catch (EntitlementException e) {
            PrivilegeManager.debug.error("PrivilegeChangeNotifier.notify", e);
        }
    }

    private boolean toSendNotification(
        Subject adminSubject,
        String realm,
        EntitlementListener l,
        String applicationName,
        Set<String> resources) throws EntitlementException {
        Map<String, Set<String>> map = l.getMapAppToRes();

        for (String appName : map.keySet()) {

            if (appName.equals(applicationName)) {
                Set<String> res = map.get(appName);

                if ((res == null) || res.isEmpty()) {
                    return true;
                }

                Application app = ApplicationManager.getApplication(
                    PrivilegeManager.superAdminSubject, realm, appName);
                ResourceName resourceComp = app.getResourceComparator();

                for (String r : res) {
                    if (doesResourceMatch(resourceComp, r, resources)) {
                        return true;
                    }
                }

            }
        }
        return false;
    }

    private boolean doesResourceMatch(
        ResourceName resourceComp,
        String resource,
        Set<String> resources) {
        for (String r : resources) {
            ResourceMatch match = resourceComp.compare(r, resource, true);
            if (match.equals(ResourceMatch.EXACT_MATCH) ||
                match.equals(ResourceMatch.SUPER_RESOURCE_MATCH) ||
                match.equals(ResourceMatch.WILDCARD_MATCH)) {
                return true;
            }
        }
        return false;
    }

    private static String toJSONString(
        String realm,
        String privilegeName,
        Set<String> resources) throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("realm", realm);
        jo.put("privilegeName", privilegeName);
        jo.put("resources", resources);
        return jo.toString();
    }

    public class Task implements Runnable {
        private Subject adminSubject;
        private String url;
        private String json;

        Task(
            Subject adminSubject,
            String url,
            String json
        ) {
            this.adminSubject = adminSubject;
            this.url = url;
            this.json = json;
        }

        public void run() {
            int cnt = 0;
            boolean done = false;
            while ((cnt++ < NUM_RETRY) && !done) {
                done = postRequest();
                if (!done) {
                    try {
                        Thread.sleep(RETRY_INTERVAL);
                    } catch (InterruptedException e) {
                        //ignore
                    }
                }
            }

            if (!done) {
                try {
                    ListenerManager.getInstance().
                        removeListener(adminSubject, url);
                } catch (EntitlementException ex) {
                    PrivilegeManager.debug.error(
                        "PrivilegeChangeNotifier.Task.run", ex);
                }
            }
        }

        private boolean postRequest() {
            OutputStreamWriter wr = null;
            BufferedReader rd = null;

            try {
                try {
                    URL urlObj = new URL(url);
                    HttpURLConnection conn = (HttpURLConnection)
                        urlObj.openConnection();
                    conn.setConnectTimeout(HTTP_TIMEOUT);
                    conn.setDoOutput(true);
                    wr = new OutputStreamWriter(
                        conn.getOutputStream());
                    wr.write(json);
                    wr.flush();

                    int status = conn.getResponseCode();
                    return (status == HttpURLConnection.HTTP_OK);
                } catch (SocketTimeoutException e) {
                    PrivilegeManager.debug.error(
                        "PrivilegeChangeNotifier.Task.postRequest", e);
                    return false;
                } catch (IOException e) {
                    PrivilegeManager.debug.error(
                        "PrivilegeChangeNotifier.Task.postRequest", e);
                    return false;
                }
            } finally {
                try {
                    if (wr != null) {
                        wr.close();
                    }
                    if (rd != null) {
                        rd.close();
                    }
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }
}
