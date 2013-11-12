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
 * $Id: ResourceNameSplitTest.java,v 1.1 2009/08/19 05:41:02 veiming Exp $
 *
 * Portions copyright 2013 ForgeRock, Inc.
 */
package com.sun.identity.entitlement.util;

import com.sun.identity.entitlement.ResourceSearchIndexes;
import com.sun.identity.unittest.UnittestLog;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import org.testng.annotations.Test;

/**
 * @author dennis
 */
public class ResourceNameSplitTest {
    @Test
    public boolean testHost() 
        throws Exception {
        ResourceNameSplitter splitter = new ResourceNameSplitter();
        Map<String, Set<String>> map = parseResource("resourceNameSplitHost");
        for (String k : map.keySet()) {
            Set<String> set = map.get(k);
            ResourceSearchIndexes comp = splitter.getIndexes(k, null);
            Set<String> results = comp.getHostIndexes();
            if (!results.equals(set)) {
                String msg = "ResourceNameSplitTest.testHost: " + k + 
                    " failed.";
                UnittestLog.logError(msg);
                throw new Exception(msg);
            }
        }
        
        return true;
    }

    @Test
    public boolean testPath() 
        throws Exception {
        ResourceNameSplitter splitter = new ResourceNameSplitter();
        Map<String, Set<String>> map = parseResource("resourceNameSplitURI");
        for (String k : map.keySet()) {
            Set<String> set = map.get(k);
            
            ResourceSearchIndexes comp = splitter.getIndexes(k, null);
            Set<String> results = comp.getPathIndexes();
            if (!results.equals(set)) {
                String msg = "ResourceNameSplitTest.testPath: " + k + 
                    " failed.";
                UnittestLog.logError(msg);
                throw new Exception(msg);
            }
        }
        
        return true;
    }
    
    private Map<String, Set<String>> parseResource(String rbName) {
        Map<String, Set<String>> results = new HashMap<String, Set<String>>();
        ResourceBundle rb = ResourceBundle.getBundle(rbName);
        for (Enumeration e = rb.getKeys(); e.hasMoreElements(); ) {
            String k = (String)e.nextElement();
            String val = rb.getString(k);
            Set<String> set = new HashSet<String>();
            
            for (StringTokenizer st = new StringTokenizer(val, ",");
                st.hasMoreElements(); ) {
                set.add(st.nextToken().trim());
            }
            
            results.put(k, set);
        }
        return results;
    }
}
