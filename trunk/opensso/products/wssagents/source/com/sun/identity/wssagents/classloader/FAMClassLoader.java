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
 * $Id: FAMClassLoader.java,v 1.7 2008/08/13 18:56:42 mrudul_uchil Exp $
 *
 */

package com.sun.identity.wssagents.classloader;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import com.sun.identity.classloader.MaskingClassLoader;

/**
 * Federated Access Mananger class loader to overcome the class loading
 * issues of jars that are not compatible for Federation Access Manager's 
 * Web Services Security Providers.
 */
public class FAMClassLoader {
    
    public static ClassLoader cl;
    
    /** Creates a new instance of FAMClassLoader */
    public FAMClassLoader() {
    }
    
    public static ClassLoader getFAMClassLoader(String[] reqJars) {
        boolean recreateClassLoader = false;
        ClassLoader oldcc = Thread.currentThread().getContextClassLoader();
        
        URL resClient = 
            oldcc.getResource("com/sun/identity/wss/sts/ClientUserToken.class");
        if (resClient != null) {
            if ( (!(resClient.toString().contains("/WEB-INF/lib/openssoclientsdk.jar"))) &&
               (!(resClient.toString().contains("/WEB-INF/lib/opensso.jar"))) ) {
                System.out.println("FAMClassLoader WSS : clientSDK found, return global classloader");
                cl = oldcc;
                return cl;
            } else {
                recreateClassLoader = true;
            }
        } 
            
        if ( (cl == null) || (recreateClassLoader) ) {
            try {
                URL[] urls = getJarsFromConfigFile(reqJars);

                ClassLoader localcc = FAMClassLoader.class.getClassLoader();
               
                List<String> maskProviders = 
                    new ArrayList<String>(Arrays.asList(maskedPackagesProviders));
                localcc = new MaskingClassLoader(localcc,maskProviders,null,urls);
                
                cl = new URLClassLoader(urls, localcc);

            } catch (Exception ex) {                
                ex.printStackTrace();
            }
        }
        if (cl != null) {
            Thread.currentThread().setContextClassLoader(cl);
        }
        return (cl);        
    }
    
    private static URL[] getJarsFromConfigFile(String[] reqJars) {
        if (reqJars == null) {
            reqJars = jarsForProviders;
        }
        URL[] urls = new URL[reqJars.length + 1];
        String FILE_BEGIN = "file:";
        String osName = System.getProperty("os.name");
        if ((osName != null) && (osName.toLowerCase().startsWith("windows"))) {
            FILE_BEGIN = "file:/";
        }
        String FILE_SEPARATOR = "/";
        String installRoot = System.getProperty("com.sun.aas.installRoot");
        String defaultJarsPath = installRoot + FILE_SEPARATOR + "addons" 
            + FILE_SEPARATOR + "opensso";
        String jarsPath = FILE_BEGIN + defaultJarsPath + FILE_SEPARATOR;
        try {
            for (int i=0; i < reqJars.length; i++) {
                urls[i] = new URL(jarsPath + reqJars[i]);
            }
            urls[reqJars.length] = new URL(jarsPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return urls;
    }
    
    /**
     * The list of package prefixes we want the
     * {@link MaskingClassLoader} to prevent the parent
     * classLoader from loading.
     */
    public static String[] maskedPackagesProviders = new String[]{
        "com.sun.identity.wss.",
        "com.sun.org.apache.xml.internal.",
        "com.sun.org.apache.xml.internal.utils.",
        "com.sun.org.apache.xpath.internal.",
        "com.sun.org.apache.xalan.internal.",
        "com.sun.identity.saml.xmlsig."
    };
    
    /**
     * The list of jar files to be loaded by FAMClassLoader.
     */
    public static String[] jarsForProviders = new String[]{
        "webservices-rt.jar",
        "openssoclientsdk.jar",
        "xalan.jar",
        "xercesImpl.jar"
    };
    
    
}
