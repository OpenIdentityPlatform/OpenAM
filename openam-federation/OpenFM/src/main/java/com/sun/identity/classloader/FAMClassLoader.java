/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FAMClassLoader.java,v 1.20 2009/06/04 01:16:48 mallas Exp $
 *
 */

/**
 * Portions Copyrighted 2013 ForgeRock AS
 */
package com.sun.identity.classloader;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.ArrayList;
import javax.servlet.ServletContext;
import java.util.Arrays;
import com.sun.identity.common.SystemConfigurationUtil;

/**
 * OpenSSO class loader to overcome the class loading
 * issues of jars that are not compatible for OpenSSO.
 */
public class FAMClassLoader {
    
    public static ClassLoader cl;
    
    // Jar files path at OpenSSO client, for OpenSSO
    // classloader to load those jar files. 
    public static final String FAM_CLASSLOADER_DIR_PATH = 
        "com.sun.identity.classloader.client.jarsPath";
    
    /** Creates a new instance of FAMClassLoader */
    public FAMClassLoader() {
    }
    
    public static ClassLoader getFAMClassLoader(ServletContext context, 
        String[] reqJars) {
        ClassLoader oldcc = Thread.currentThread().getContextClassLoader();
        
        URL res = oldcc.getResource("com/sun/xml/ws/security/trust/impl/ic/ICContractImpl.class");
        if (res != null) {
            if (!(res.toString().contains("/WEB-INF/lib/webservices-rt.jar"))) {
                System.out.println("FAMClassLoader : found new Metro class in global classpath");
                cl = oldcc;
                return cl;
            }
        }
        
        setSystemProperties();
        if (cl == null) {
            try {
                URL[] urls = null;
                if (context != null) {
                    urls = jarFinder(context, reqJars);
                } else {
                    urls = getJarsFromConfigFile(reqJars);
                }

                ClassLoader localcc = FAMClassLoader.class.getClassLoader();
                String[] mPackages = maskedPackages;
                String version = System.getProperty("java.version");
                if(version != null && version.startsWith("1.6")) {                   
                   mPackages = maskedPackages16;
                }

                List<String> mask = 
                    new ArrayList<String>(Arrays.asList(mPackages));
                
                List<String> maskRes =
                    new ArrayList<String>(Arrays.asList(maskedResouces));

                // first create a protected area so that we load WS 2.1 API
                // and everything that depends on them, inside OpenSSO
                // Enterprise classloader.
                localcc = new MaskingClassLoader(localcc,mask,maskRes,urls);

                // then this classloader loads the API and tools.jar
                cl = new URLClassLoader(urls, localcc);

                //Thread.currentThread().setContextClassLoader(cl);
            } catch (Exception ex) {                
                ex.printStackTrace();
            }
        }
        if (cl != null) {
            Thread.currentThread().setContextClassLoader(cl);
        }
        return (cl);        
    }
    
    private static URL[] jarFinder(ServletContext context, String[] reqJars) {
        if (reqJars != null) {
            jars = reqJars;
        }
        URL[] urls = new URL[jars.length];
        
        try {
            for (int i=0; i < jars.length; i++) {
                urls[i] = context.getResource("/WEB-INF/lib/" + jars[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return urls;
    }
    
    private static URL[] getJarsFromConfigFile(String[] reqJars) {
        if (reqJars != null) {
            jars = reqJars;
        }
        URL[] urls = new URL[jars.length + 1];
        String FILE_BEGIN = "file:";
        String osName = System.getProperty("os.name");
        if ((osName != null) && (osName.toLowerCase().startsWith("windows"))) {
            FILE_BEGIN = "file:/";
        }
        String FILE_SEPARATOR = "/";
        String installRoot = System.getProperty("com.sun.aas.installRoot");
        String defaultJarsPath = installRoot + FILE_SEPARATOR + "addons" 
            + FILE_SEPARATOR + "opensso";
        String jarsPath = FILE_BEGIN + SystemConfigurationUtil.getProperty(
            FAM_CLASSLOADER_DIR_PATH, defaultJarsPath) + FILE_SEPARATOR;
        try {
            for (int i=0; i < jars.length; i++) {
                urls[i] = new URL(jarsPath + jars[i]);
            }
            urls[jars.length] = new URL(jarsPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return urls;
    }
    
    private static void setSystemProperties() {
        // Fix for Geronimo Application server and WebLogic 10       
        System.setProperty("javax.xml.soap.MetaFactory", 
            "com.sun.xml.messaging.saaj.soap.SAAJMetaFactoryImpl");
        System.setProperty("javax.xml.soap.MessageFactory", 
            "com.sun.xml.messaging.saaj.soap.ver1_1.SOAPMessageFactory1_1Impl");
        System.setProperty("javax.xml.soap.SOAPConnectionFactory", 
            "com.sun.xml.messaging.saaj.client.p2p.HttpSOAPConnectionFactory");
        System.setProperty("javax.xml.soap.SOAPFactory", 
            "com.sun.xml.messaging.saaj.soap.ver1_1.SOAPFactory1_1Impl");
    }

    /**
     * The list of jar files to be loaded by FAMClassLoader.
     *
     * TODO -- Revisit to fix.
     */
    public static String[] jars = new String[]{
        "webservices-api-2009-14-01.jar",               // "webservices-api.jar",
        "webservices-rt-2009-29-07.jar",                // "webservices-rt.jar",
        "webservices-tools-2.1-b16.jar",                // "webservices-tools.jar",
        "webservices-extra-api-2003-09-04.jar",         // "webservices-extra-api.jar",
        "webservices-extra-2008-03-12.jar",             // "webservices-extra.jar",
        "xercesImpl-2.11.0.jar",                        // "xercesImpl.jar",
        "xml-apis-2.11.0.jar",
        "xml-resolver-2.11.0.jar",
        "xml-serializer-2.11.0.jar",
        "openam-shared-11.0.0-SNAPSHOT.jar",            // "opensso.jar",
        "openam-dtd-schema-11.0.0-SNAPSHOT.jar",
        "openam-entitlements-11.0.0-SNAPSHOT.jar",
        "openam-idsvcs-schema-11.0.0-SNAPSHOT.jar",
        "openam-jaxrpc-schema-11.0.0-SNAPSHOT.jar",
        "openam-liberty-schema-11.0.0-SNAPSHOT.jar",
        "openam-rest-11.0.0-SNAPSHOT.jar",
        "openam-saml2-schema-11.0.0-SNAPSHOT.jar",
        "openam-wsfederation-schema-11.0.0-SNAPSHOT.jar",
        "openam-xacml3-schema-11.0.0-SNAPSHOT.jar",
        "xalan-2.7.1.jar",                               // "xalan.jar",
        "openam-federation-library-11.0.0-SNAPSHOT.jar", // "openfedlib.jar"
        "OpenFM-11.0.0-SNAPSHOT.jar"
    };

    /**
     * The list of package prefixes we want the
     * {@link MaskingClassLoader} to prevent the parent
     * classLoader from loading.
     */
    public static String[] maskedPackages = new String[]{
        "com.sun.istack.tools.",
        "com.sun.tools.jxc.",
        "com.sun.tools.xjc.",
        "com.sun.tools.ws.",
        "com.sun.codemodel.",
        "com.sun.relaxng.",
        "com.sun.xml.xsom.",
        "com.sun.xml.bind.",
        "com.sun.xml.bind.v2.",
        "com.sun.xml.messaging.",
        "com.sun.xml.ws.",
        "com.sun.xml.ws.addressing.",
        "com.sun.xml.ws.api.",
        "com.sun.xml.ws.api.addressing.",
        "com.sun.xml.ws.server.",
        "com.sun.xml.ws.transport.",
        "com.sun.xml.wss.",
        "com.sun.xml.security.",
        "com.sun.xml.xwss.",
        "javax.xml.bind.",
        "javax.xml.ws.",
        "javax.jws.",
        "javax.jws.soap.",
        "javax.xml.soap.",
        "com.sun.istack.",
        "com.sun.identity.wss.",
        "com.sun.identity.wssagents.",
        "com.sun.org.apache.xml.internal.",
        "com/sun/org/apache/xml/internal/",
        "com.sun.org.apache.xpath.internal.",
        "com.sun.org.apache.xalan.internal.",
        "com.sun.org.apache.xerces.internal.",
        "com.sun.identity.saml.xmlsig.",
        "com.sun.identity.saml.",
        "com.sun.identity.liberty.ws.",
        "com.sun.identity.xmlenc.",
        "com.sun.xml.stream."
    };
    
    public static String[] maskedPackages16 = new String[]{
        "com.sun.istack.tools.",
        "com.sun.tools.jxc.",
        "com.sun.tools.xjc.",
        "com.sun.tools.ws.",
        "com.sun.codemodel.",
        "com.sun.relaxng.",
        "com.sun.xml.xsom.",
        "com.sun.xml.bind.",
        "com.sun.xml.bind.v2.",
        "com.sun.xml.messaging.",
        "com.sun.xml.ws.",
        "com.sun.xml.ws.addressing.",
        "com.sun.xml.ws.api.",
        "com.sun.xml.ws.api.addressing.",
        "com.sun.xml.ws.server.",
        "com.sun.xml.ws.transport.",
        "com.sun.xml.wss.",
        "com.sun.xml.security.",
        "com.sun.xml.xwss.",
        "javax.xml.bind.",
        "javax.xml.ws.",
        "javax.jws.",
        "javax.jws.soap.",
        "javax.xml.soap.",
        "com.sun.istack.",
        "com.sun.identity.wss.",
        "com.sun.identity.wssagents.",        
        "com.sun.org.apache.xpath.internal.",
        "com.sun.org.apache.xalan.internal.",
        "com.sun.org.apache.xerces.internal.",
        "com.sun.identity.saml.xmlsig.",
        "com.sun.identity.xmlenc.",
        "com.sun.identity.saml.",
        "com.sun.identity.liberty.ws.",
        "com.sun.xml.stream.",
        "javax.xml.crypto.",
        "org.jcp.xml.dsig."
    };
    
    /**
     * The list of resources we want the
     * {@link MaskingClassLoader} to prevent the parent
     * classLoader from loading.
     */
    public static String[] maskedResouces = new String[]{
        "META-INF/services/javax.xml.bind.JAXBContext",
        "META-INF/services",
        "/META-INF/services",
        "javax/xml/bind/",
        "com/sun/xml/ws/",
        "com/sun/xml/wss/",
        "com/sun/xml/bind/",
        "com/sun/xml/messaging/",
        "com/sun/org/apache/xml/internal/"
    };
    
    
}
