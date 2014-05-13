/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: TestHarness.java,v 1.1 2009/08/19 05:41:03 veiming Exp $
 */

/**
 * Portions Copyrighted 2014 ForgeRock AS
 */
package com.sun.identity.unittest;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.common.HttpURLConnectionManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import org.testng.TestNG;

/**
 * Test Harness instantiates the test classes and executes them.
 */
public final class TestHarness {
   
    public TestHarness() {
    }
   
    public void execute(HttpServletResponse res, String tests) {
        List<String> classes = getTestClasses(tests);
        List<Class> javaClasses = new ArrayList<Class>();
        try {
            for (String strClass : classes) {
                if (strClass.endsWith(".jsp")) {
                    executeJSP(strClass);
                } else {
                    javaClasses.add(Class.forName(strClass));
                }
            }
        } catch (ClassNotFoundException e) {
            UnittestLog.logError("TestHarness.execute", e);
        }

        if (!javaClasses.isEmpty()) {
            Class[] testngClasses = new Class[javaClasses.size()];
            int i = 0;
            for (Class c : javaClasses) {
                testngClasses[i++] = c;
            }

            try {
                TestNG testng = new TestNG();
                testng.addListener(new TestListener());
                testng.setTestClasses(testngClasses);
                testng.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        UnittestLog.logMessage("TestHarness:DONE");
    }
    
    private void executeJSP(String strJSP) {
        String jsp = strJSP.substring(0, strJSP.lastIndexOf(".jsp"));
        jsp = SystemProperties.getServerInstanceName() + "/unittest/" +
            jsp.replace('.',  '/') + ".jsp";
        UnittestLog.logMessage("Executing JSP, " + jsp);
        
        try {
            URL url = new URL(jsp);
            URLConnection conn = HttpURLConnectionManager.getConnection(url);
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(
                conn.getOutputStream());
            wr.write("hello=1");
            wr.flush();
            BufferedReader rd = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                //
            }
            wr.close();
            rd.close();
        } catch (MalformedURLException e) {
            UnittestLog.logError(
                "TestHarness.executeJSP: execute JSP failed", e);
        } catch (IOException e) {
            UnittestLog.logError(
                "TestHarness.executeJSP: execute JSP failed", e);
        }
        UnittestLog.logMessage("Executed JSP, " + jsp);
    }
    
    private static List<String> getTestClasses(String tests) {
        List classes = new ArrayList();
        StringTokenizer st = new StringTokenizer(tests, ",");
        while (st.hasMoreTokens()) {
            classes.add(st.nextToken());
        }
        return classes;
    }
    
    public static Map getTests(ServletContext servletContext) {
        Map map = new HashMap();
        getTestClasses(servletContext, map);
        getTestJSPs(servletContext, map, "/unittest", true);
        return map;
    }
    
    public static void getTestClasses(ServletContext servletContext, Map map) {
        JarInputStream in = null;
        try {
            in = new JarInputStream(servletContext.getResourceAsStream(
                "WEB-INF/lib/unittest.jar"));
            JarEntry jarEntry = in.getNextJarEntry();

            while (jarEntry != null) {
                String name = jarEntry.getName();
                if (name.endsWith(".class") && (name.indexOf("$") == -1)) {
                    name = name.replaceAll("/", ".");
                    int idx = name.lastIndexOf('.');
                    name = name.substring(0, idx);
                    idx = name.lastIndexOf('.');
                    String pkgName = name.substring(0, idx);

                    if (!pkgName.equals("com.sun.identity.unittest")) {
                        Set set = (Set)map.get(pkgName);
                        if (set == null) {
                            set = new TreeSet();
                            map.put(pkgName, set);
                        }
                        set.add(name);
                    }
                }
                jarEntry = in.getNextJarEntry();
            }
        } catch (IOException e) {
            UnittestLog.logError("TestHarness.getTestClasses: failed", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

    public static void getTestJSPs(
        ServletContext servletContext, 
        Map map,
        String root, 
        boolean top
    ) {
        Set paths = servletContext.getResourcePaths(root);
        for (Iterator i = paths.iterator(); i.hasNext(); ) {
            String path = (String)i.next();
            if (path.endsWith("/")) { //directory
                getTestJSPs(servletContext, map, path, false);
            } else if (!top) {
                if (path.endsWith(".jsp")) {
                    int idx = path.lastIndexOf('/');
                    String name = path.substring(idx + 1);
                    String pkgName = path.substring(0, idx);
                    pkgName = pkgName.replace('/', '.');
                    pkgName = pkgName.substring(10); // strip .unittest.

                    Set set = (Set) map.get(pkgName);
                    if (set == null) {
                        set = new TreeSet();
                        map.put(pkgName, set);
                    }

                    path = path.substring(10); // strip /unittest/
                    set.add(path);
                }
                
                
            }
        }
    }
}

