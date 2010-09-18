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
 * $Id: ResourceNameIndexTest.java,v 1.1 2009/08/19 05:41:02 veiming Exp $
 */

package com.sun.identity.entitlement.util;

import com.sun.identity.entitlement.ResourceSaveIndexes;
import com.sun.identity.unittest.UnittestLog;
import java.util.Collections;
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
public class ResourceNameIndexTest {
    @Test
    public void testHost() 
        throws Exception {
        ResourceNameIndexGenerator gen = new ResourceNameIndexGenerator();
        Map<String, String> map = parseResource("resourceNameIndexHost");
        for (String k : map.keySet()) {
            String expectedResult = map.get(k);
            ResourceSaveIndexes indexes = gen.getIndexes(k);
            String host = indexes.getHostIndexes().iterator().next();
            if (!host.equals(expectedResult)) {
                String msg = "ResourceNameIndexTest.testHost: " + k + 
                    " failed.";
                UnittestLog.logError(msg);
                throw new Exception(msg);
            }
        }
    }

    @Test
    public void testPath() 
        throws Exception {
        ResourceNameIndexGenerator gen = new ResourceNameIndexGenerator();
        Map<String, String> map = parseResource("resourceNameIndexURI");
        for (String k : map.keySet()) {
            String expectedResult = map.get(k);
            ResourceSaveIndexes indexes = gen.getIndexes(k);
            String path = indexes.getPathIndexes().iterator().next();
            if (!path.equals(expectedResult)) {
                String msg = "ResourceNameIndexTest.testPath: " + k + 
                    " failed.";
                UnittestLog.logError(msg);
                throw new Exception(msg);
            }
        }
    }
    
    @Test
    public void testPathParent() 
        throws Exception {
        ResourceNameIndexGenerator gen = new ResourceNameIndexGenerator();
        Map<String, Set<String>> map = parseResources(
            "resourceNameIndexPathParent");
        for (String k : map.keySet()) {
            Set<String> expectedResult = map.get(k);
            ResourceSaveIndexes indexes = gen.getIndexes(k);

            if (!indexes.getParentPathIndexes().equals(expectedResult)) {
                String msg = "ResourceNameIndexTest.testPathParent: " + k + 
                    " failed.";
                UnittestLog.logError(msg);
                throw new Exception(msg);
            }
        }
    }

    private Map<String, String> parseResource(String rbName) {
        Map<String, String> results = new HashMap<String, String>();
        ResourceBundle rb = ResourceBundle.getBundle(rbName);
        for (Enumeration e = rb.getKeys(); e.hasMoreElements(); ) {
            String k = (String)e.nextElement();
            String val = rb.getString(k).trim();
            results.put(k, val);
        }
        return results;
    }

    private Map<String, Set<String>> parseResources(String rbName) {
        Map<String, Set<String>> results = new HashMap<String, Set<String>>();
        ResourceBundle rb = ResourceBundle.getBundle(rbName);
        for (Enumeration e = rb.getKeys(); e.hasMoreElements(); ) {
            String k = (String)e.nextElement();
            String val = rb.getString(k).trim();
            if (val.length() > 0) {
                Set<String> set = new HashSet<String>();
                StringTokenizer st = new StringTokenizer(val, ",");
                while (st.hasMoreElements()) {
                    set.add(st.nextToken());
                }
            } else {
                results.put(k, Collections.EMPTY_SET);
            }
        }
        return results;
    }
}
