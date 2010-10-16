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
 * $Id: EntitlementListener.java,v 1.3 2009/12/15 00:44:18 veiming Exp $
 */

package com.sun.identity.entitlement;

import com.sun.identity.shared.JSONUtils;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Listener for receiving entitlements related changes.
 */
public final class EntitlementListener {
    private URL url;
    private Map<String, Set<String>> mapAppToRes;

    public EntitlementListener(JSONObject jo)
        throws JSONException, EntitlementException {
        String strURL = jo.optString("url");

        try {
            url = new URL(strURL);
        } catch (MalformedURLException e) {
            throw new EntitlementException(426);
        }

        mapAppToRes = JSONUtils.getMapStringSetString(jo, "mapAppToRes");
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("url", url.toString());
        jo.put("mapAppToRes", mapAppToRes);
        return jo;
    }

    /**
     * Constructor.
     *
     * @param url URL of which notification will be sent to.
     * @param application Application name.
     * @param resourceNames Notification will be send to <code>url</code>
     *        if privilege for resources matches with the elements in
     *        <code>resourceNames</code>. Wildcard and sub resource matching
     *        apply too.
     * @throws EntitlementException if <code>url</code> or
     *         <code>resourceNames</code> is null.
     */
    public EntitlementListener(
        String url,
        String application,
        Collection<String> resourceNames
    ) throws EntitlementException {
        if (url == null) {
            throw new EntitlementException(426);
        }

        try {
            init(new URL(url), application, resourceNames);
        } catch (MalformedURLException e) {
            throw new EntitlementException(435);
        }
    }

    /**
     * Constructor.
     *
     * @param url URL of which notification will be sent to.
     * @param application Application name.
     * @param resourceNames Notification will be send to <code>url</code>
     *        if privilege for resources matches with the elements in
     *        <code>resourceNames</code>. Wildcard and sub resource matching
     *        apply too.
     * @throws EntitlementException if <code>url</code> or
     *         <code>resourceNames</code> is null.
     */
    public EntitlementListener(
        URL url,
        String application,
        Collection<String> resourceNames)
        throws EntitlementException {
        init(url, application, resourceNames);
    }

    private void init(
        URL url,
        String application,
        Collection<String> resourceNames)
        throws EntitlementException {
        if (url == null) {
            throw new EntitlementException(426);
        }
        if ((application == null) || (application.length() == 0)){
            throw new EntitlementException(428);
        }

        this.url = url;
        mapAppToRes = new HashMap<String, Set<String>>();
        Set<String> resNames = new HashSet<String>();
        if ((resourceNames != null) && !resourceNames.isEmpty()) {
            for (String r : resourceNames) {
                r = r.trim();
                if (r.length() > 0) {
                    resNames.add(r);
                }
            }
        }
        
        mapAppToRes.put(application, resNames);
    }

    /**
     * Returns map of application name to resource names.
     *
     * @return map of application name to resource names.
     */
    public Map<String, Set<String>> getMapAppToRes() {
        return mapAppToRes;
    }

    /**
     * Returns notification URL.
     *
     * @return notification URL.
     */
    public URL getUrl() {
        return url;
    }

    /**
     * Combines other listener. Returns <code>true</code> if combination is
     * possible.
     * @param l listener.
     * @return <code>true</code> if combination is done.
     */
    public boolean combine(EntitlementListener l) {
        if (!l.url.equals(url)) {
            return false;
        }

        for (String appName : l.mapAppToRes.keySet()) {
            Set<String> res = mapAppToRes.get(appName);
            if (res == null) {
                res = new HashSet<String>();
                mapAppToRes.put(appName, res);
            }
            res.addAll(l.mapAppToRes.get(appName));
        }
        return true;
    }

    @Override
    public int hashCode() {
        return url.hashCode() + mapAppToRes.hashCode();
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EntitlementListener)) {
            return false;
        }

        EntitlementListener otherListener = (EntitlementListener)other;
        if (!url.equals(otherListener.url)) {
            return false;
        }

        if (!mapAppToRes.equals(otherListener.mapAppToRes)) {
            return false;
        }

        return true;
    }
}
