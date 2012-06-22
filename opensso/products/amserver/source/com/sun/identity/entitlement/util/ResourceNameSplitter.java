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
 * $Id: ResourceNameSplitter.java,v 1.2 2009/08/28 06:16:31 veiming Exp $
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.entitlement.util;

import com.sun.identity.entitlement.ResourceSearchIndexes;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/*
 * This class splits resource name (URL) to different parts so that they
 * can be used for resource name comparison.
 */
public class ResourceNameSplitter implements ISearchIndex {
    public ResourceNameSplitter() {
    }

    /**
     * Returns the different components on a resource that can be
     * used to search for policies.
     * 
     * @param resName Resource name.
     * @return the different components on a resource.
     */
    public ResourceSearchIndexes getIndexes(String resName) {
        try {
            RelaxedURL url = new RelaxedURL(resName);
            Set<String> hostIndexes = splitHost(url);
            Set<String> pathIndexes = splitPath(url);
            String path = url.getPath();
            if (path.length() == 0) {
                path = "/";
            }
            Set<String> parentPath = new HashSet<String>();
            parentPath.add(path);
            return new ResourceSearchIndexes(hostIndexes, pathIndexes,
                parentPath);
        } catch (MalformedURLException e) {
            Set<String> setHost = new HashSet<String>();
            setHost.add(".");
            
            if (!resName.startsWith("/")) {
                resName = "/" + resName;
            }

            Set<String> setPath = splitPath(resName);
            Set<String> parentPath = new HashSet<String>();
            parentPath.add(resName);
            return new ResourceSearchIndexes(setHost, setPath, parentPath);
        }
    }
    
    /**
     * Returns a list of sub parts of host of a resource name.
     *
     * @param url Resource name.
     * @return a list of sub parts of host of a resource name.
     */
    public static Set<String> splitHost(RelaxedURL url) {
        Set<String> results = new HashSet<String>();
        String host = url.getHostname().toLowerCase();
        
        results.add("://");
        results.add("://" + host);
        int idx = host.indexOf('.');
        
        while (idx != -1) {
            results.add("://" + host.substring(idx));
            idx = host.indexOf('.', idx + 1);
        }

        return results;
    }
    
    /**
     * Returns a list of sub parts of path of a resource name.
     *
     * @param resName Resource name.
     * @param a list of sub parts of path of a resource name.
     */
    private static Set<String> splitPath(RelaxedURL url) {
        Set<String> results = new HashSet<String>();
        Set<String> queries = normalizeQuery(url.getQuery());
        results.add("/");
        for (String q : queries) {
            results.add("/?" + q);
        }
        Set<String> paths = splitPath(url.getPath());
        results.addAll(paths);
        for (String p : paths) {
            for (String q : queries) {
                results.add(p + "?" + q);
            }
        }

        return results;
    }

    private static Set<String> splitPath(String path) {
        Set<String> results = new HashSet<String>();
        results.add("/");
        path = path.toLowerCase();

        if ((path.length() > 0) && !path.equals("/")) {
            String prefix = "";
            StringTokenizer st = new StringTokenizer(path, "/");
            while (st.hasMoreTokens()) {
                String s = st.nextToken();
                prefix += "/" + s;
                results.add(prefix);
            }
        }

        return results;
    }
    
    private static Set<String> normalizeQuery(String path) {
        Set<String> results = new HashSet<String>();
        if ((path == null) || (path.length() == 0)) {
            return results;
        }

        path = path.toLowerCase();
        List<List<String>> possibleCombinations = new ArrayList<List<String>>();        
        List<String> list = new ArrayList<String>();
        
        StringTokenizer st = new StringTokenizer(path, "&");
        while (st.hasMoreTokens()) {
            list.add(st.nextToken());
        }
        possibleCombinations.add(list);
        
        Map<String, String> map = new HashMap<String, String>();
        for (String s : list) {
            int idx = s.indexOf('=');
            String key = (idx == -1) ? s : s.substring(0, idx);
            String val = ((idx == -1) || (idx == (s.length() -1))) 
                ? "" : s.substring(idx+1);
            map.put(key, val);
        }
        
        Set<String> keys = new HashSet<String>();
        for (String s : map.keySet()) {
            keys.add(s);
        }

        List<String> allBlanks = new ArrayList<String>();
        for (String s : keys) {
            allBlanks.add(s + "=");
        }
        possibleCombinations.add(allBlanks);
        
        while (!keys.isEmpty()) {
            String s = keys.iterator().next();
            List<String> l = new ArrayList<String>();
            for (String key : map.keySet()) {
                String val = key.equals(s) ? "" : map.get(key);
                l.add(key + "=" + val);
            }
            possibleCombinations.add(l);
            keys.remove(s);
        }
        
        for (List<String> l : possibleCombinations) {
            results.add(queryToString(l));
        }
        
        return results;
    }
    
    static String queryToString(List<String> query) {
        StringBuilder buff = new StringBuilder();
        Collections.sort(query);
        boolean first = true;
        for (String s : query) {
            if (first) {
                first = false;
            } else {
                buff.append("&");
            }
            buff.append(s);
        }
        return buff.toString();
    }

}
