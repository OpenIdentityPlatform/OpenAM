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
 * $Id: SetupDistAuthWAR.java,v 1.4 2008/06/25 05:40:27 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.distauth.setup;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.encode.Base64;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * This setup the Distauth WAR.
 */
public class SetupDistAuthWAR {

    private static final String TAG_SERVER_PROTOCOL = "SERVER_PROTOCOL";
    private static final String TRUST_ALL_CERTS =
        "com.iplanet.am.jssproxy.trustAllServerCerts=true\n";
    private static final String CONFIG_VAR_DEFAULT_SHARED_KEY =
        "KmhUnWR1MYWDYW4xuqdF5nbm+CXIyOVt";
    private static String configFile = null;
    private static String configFileDir = null; 
    ServletContext servletContext;

    /**
     * Constructor
     */
    public SetupDistAuthWAR(ServletContext context)
        throws ServletException {
        servletContext = context;
        configFile = System.getProperty("user.home") + File.separator
                     + Constants.CONFIG_VAR_DISTAUTH_BOOTSTRAP_BASE_DIR 
                     + File.separator
                     + getNormalizedRealPath(servletContext) + 
                     "AMDistAuthConfig.properties";
        configFileDir = System.getProperty("user.home") + File.separator
                     + Constants.CONFIG_VAR_DISTAUTH_BOOTSTRAP_BASE_DIR;         
    }

    /**
     * Creates AMDistAuthConfig.properties file
     * @param configFile Absolute path to the AMDistAuthConfig.properties to be created.
     * @param templateFile Template file for AMDistAuthConfig.properties
     * @param properties Properties to be swapped in the template
     */
    public void createAMDistAuthConfigProperties( 
        String templateFile, Properties properties) throws IOException {
        String content = getFileContent(templateFile);
        for (Iterator i = properties.keySet().iterator(); i.hasNext(); ) {
            String tag = (String)i.next();
            content = content.replaceAll("@" + tag + "@",
                (String)properties.get(tag));
        }        

        StringBuilder sbDistAuthConfig = new StringBuilder();
        
        sbDistAuthConfig.append("/*************************************");
        sbDistAuthConfig.append("*************************\n");
        sbDistAuthConfig.append(" * Distributed Authentication Service ");
        sbDistAuthConfig.append("Configuration parameters\n");
        sbDistAuthConfig.append(" *************************************");
        sbDistAuthConfig.append("************************/\n");
        sbDistAuthConfig.append("com.iplanet.distAuth.server.protocol=");
        sbDistAuthConfig.append
            ((String)properties.get("DISTAUTH_SERVER_PROTOCOL"));
        sbDistAuthConfig.append("\n");
        sbDistAuthConfig.append("com.iplanet.distAuth.server.host=");
        sbDistAuthConfig.append
            ((String)properties.get("DISTAUTH_SERVER_HOST"));
        sbDistAuthConfig.append("\n");
        sbDistAuthConfig.append("com.iplanet.distAuth.server.port=");
        sbDistAuthConfig.append
            ((String)properties.get("DISTAUTH_SERVER_PORT"));
        sbDistAuthConfig.append("\n");
        sbDistAuthConfig.append
            ("\ncom.iplanet.am.distauth.deploymentDescriptor=");
        sbDistAuthConfig.append((String)properties.get("DISTAUTH_DEPLOY_URI"));
        sbDistAuthConfig.append("\n");
        sbDistAuthConfig.append("\ncom.iplanet.am.cookie.secure=");
        sbDistAuthConfig.append((String)properties.get("AM_COOKIE_SECURE"));
        sbDistAuthConfig.append("\n");
        sbDistAuthConfig.append("com.iplanet.am.cookie.encode=");
        sbDistAuthConfig.append((String)properties.get("AM_COOKIE_ENCODE"));
        sbDistAuthConfig.append("\n");
        sbDistAuthConfig.append("\n/*\n");
        sbDistAuthConfig.append(" * Load Balancer cookie name and value ");
        sbDistAuthConfig.append("to be used when there are multiple\n");
        sbDistAuthConfig.append(" * distributed authentication web ");
        sbDistAuthConfig.append("application servers behind Load Balancer\n");      
        sbDistAuthConfig.append(" */\n");
        sbDistAuthConfig.append("openam.auth.distauth.lb_cookie_name=");
        sbDistAuthConfig.append(properties.getProperty("DISTAUTH_LB_COOKIE_NAME"));
        sbDistAuthConfig.append("\n");
        sbDistAuthConfig.append("openam.auth.distauth.lb_cookie_value=");
        sbDistAuthConfig.append(properties.getProperty("DISTAUTH_LB_COOKIE_VALUE"));
        sbDistAuthConfig.append("\n/*\n");
        sbDistAuthConfig.append(" * Load Balancer cookie name and value ");
        sbDistAuthConfig.append("to be used when there are multiple\n");
        sbDistAuthConfig.append(" * OpenAM server instances behind Load Balancer\n");
        sbDistAuthConfig.append(" */\n");
        sbDistAuthConfig.append("com.iplanet.am.lbcookie.name=");
        sbDistAuthConfig.append(properties.getProperty("LB_COOKIE_NAME"));
        sbDistAuthConfig.append("\n");
        sbDistAuthConfig.append("\n/*\n");
        sbDistAuthConfig.append(" * DistAuth cookie name\n ");
        sbDistAuthConfig.append(" */\n");
        sbDistAuthConfig.append("com.sun.identity.auth.cookieName=");
        sbDistAuthConfig.append((String)properties.get("DISTAUTH_COOKIE_NAME"));
        sbDistAuthConfig.append("\n");
        
        content += sbDistAuthConfig.toString();        

        String protocol = (String)properties.get(TAG_SERVER_PROTOCOL);
        if (protocol.equalsIgnoreCase("https")) {
            content += TRUST_ALL_CERTS;
        }

        File configFileDirectory = new File(configFileDir);
        if (!configFileDirectory.exists()) {
            configFileDirectory.mkdirs();
        }
        BufferedWriter out = new BufferedWriter(new FileWriter(configFile));
        out.write(content);
        out.close();
    }

    private String getFileContent(String fileName)
        throws IOException
    {
        InputStream in = servletContext.getResourceAsStream(fileName);
        if (in == null) {
            throw new IOException("Unable to open " + fileName);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder buff = new StringBuilder();
        String line = reader.readLine();

        while (line != null) {
            buff.append(line).append("\n");
            line = reader.readLine();
        }
        reader.close();
        return buff.toString();      
    }

    /**
     * Sets properties from AMDistAuthConfig.properties
     * @param configFile path to the AMDistAuthConfig.properties file
     * @throws ServletException when error occurs
     */
    public void setAMDistAuthConfigProperties() 
        throws ServletException {
        FileInputStream fileStr = null;
        try {
            fileStr = new FileInputStream(configFile);
            if (fileStr != null) {
                Properties props = new Properties();
                props.load(fileStr);
                SystemProperties.initializeProperties(props);
                DistAuthConfiguratorFilter.isConfigured = true;
            } else {
                throw new ServletException("Unable to open: " + configFile);
            }
        } catch (FileNotFoundException fexp) {
            fexp.printStackTrace();
            throw new ServletException(fexp.getMessage());
        } catch (IOException ioexp) {
            ioexp.printStackTrace();
            throw new ServletException(ioexp.getMessage());
        } finally {
            if (fileStr != null) {
                try {
                    fileStr.close();
                } catch (IOException ioe) {
                }
            } 
        }
    }
    

    public static String generateKey() {
        String randomStr = null;
        try {
            byte [] bytes = new byte[24];
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.nextBytes(bytes);
            randomStr = Base64.encode(bytes).trim();
        } catch (Exception e) {
            randomStr = null;
            e.printStackTrace();
        }
        return (randomStr != null) ? randomStr : CONFIG_VAR_DEFAULT_SHARED_KEY;
    }
    
    public static String getNormalizedRealPath(ServletContext servletCtx)
        throws ServletException {    
        String path = null;
        if (servletCtx != null) {
            path = getAppResource(servletCtx);
            
            if (path != null) {
                String realPath = servletCtx.getRealPath("/");
                if ((realPath != null) && (realPath.length() > 0)) {
                    realPath = realPath.replace('\\', '/');
                    path = realPath.replaceAll("/", "_");
                } else {
                    path = path.replaceAll("/", "_");
                }
                int idx = path.indexOf(":");
                if (idx != -1) {
                    path = path.substring(idx + 1);
                }
            }
        }
        return path;
    }

    /**
     * Returns URL of the default resource.
     *
     * @return URL of the default resource. Returns null if servlet context is
     *         null.
     */
    private static String getAppResource(ServletContext servletCtx)
        throws ServletException {
        if (servletCtx != null) {
            try {
                java.net.URL turl = servletCtx.getResource("/");
                return turl.getPath();
            } catch (MalformedURLException mue) {
                throw new ServletException(mue.getMessage());
            }
        }
        return null;
    }
    
}
