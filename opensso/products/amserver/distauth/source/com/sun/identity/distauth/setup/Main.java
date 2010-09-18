/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Main.java,v 1.4 2008/06/25 05:40:28 qcheng Exp $
 *
 */

package com.sun.identity.distauth.setup;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.util.Crypt;
import com.sun.identity.shared.encode.Base64;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


/**
 * This setup the DistAuth
 */
public class Main {
    private Map properties = new HashMap();
    private Map labels = new HashMap();

    private static final String TEMPLATE_AMCONFIG_PROPERTIES = 
        "resources/AMConfigTemplate.properties";
    private static final String FILE_AMCONFIG_PROPERTIES =
        "war/WEB-INF/classes/AMConfig.properties";
        
    private static final String TAG_DEBUG_LEVEL = "DEBUG_LEVEL";
    private static final String TAG_PASSWD_ENC_DEC_KEY = "ENCRYPTION_KEY";
    private static final String TAG_CLIENT_ENC_KEY = "ENCRYPTION_KEY_LOCAL";
    private static final String TAG_APPLICATION_PASSWD = 
        "ENCODED_APPLICATION_PASSWORD";               
    private static final String TAG_NAMING_URL = "NAMING_URL";
    private static final String TAG_SERVER_PROTOCOL = "SERVER_PROTOCOL";
    private static final String TAG_SERVER_HOST = "SERVER_HOST";
    private static final String TAG_SERVER_PORT = "SERVER_PORT";
    private static final String TAG_DEPLOY_URI = "DEPLOY_URI";
    private static final String TAG_NOTIFICATION_URL = "NOTIFICATION_URL";    

    private static final String TRUST_ALL_CERTS =
        "com.iplanet.am.jssproxy.trustAllServerCerts=true\n";
        
    private static final String CONFIG_VAR_DEFAULT_SHARED_KEY =
        "KmhUnWR1MYWDYW4xuqdF5nbm+CXIyOVt";
                       
    private static final String ENC_PWD_PROPERTY = "am.encryption.pwd";                        

    private static List questions = new ArrayList();
    
    
    static {
        questions.add("DEBUG_DIR");
        questions.add("DEBUG_LEVEL");
        questions.add("ENCRYPTION_KEY");                
        questions.add("APPLICATION_USER");
        questions.add("ENCODED_APPLICATION_PASSWORD");
        questions.add("SERVER_PROTOCOL");
        questions.add("SERVER_HOST");
        questions.add("SERVER_PORT");
        questions.add("DEPLOY_URI");
        questions.add("NAMING_URL");
        questions.add("DISTAUTH_SERVER_PROTOCOL");
        questions.add("DISTAUTH_SERVER_HOST");
        questions.add("DISTAUTH_SERVER_PORT");
        questions.add("DISTAUTH_DEPLOY_URI");
        questions.add("NOTIFICATION_URL");
    }
    
    public Main()
        throws IOException, MissingResourceException
    {
        getDefaultValues();
        promptForAnswers();
        createPropertiesFile();
    }

    private void getDefaultValues() 
        throws MissingResourceException
    {
        ResourceBundle rb = ResourceBundle.getBundle("configDefault");
        for (Enumeration e = rb.getKeys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            String value = (String)rb.getString(key);

            if (value.startsWith("[")) {
                labels.put(key, value.substring(1, value.length() -1));
            } else {
                properties.put(key, value);
            }
        }
    }

    private void promptForAnswers()
        throws IOException
    {
        for (Iterator i = questions.iterator(); i.hasNext(); ) {
            String q = (String)i.next();
            String value = "";
            while (value.length() == 0) {
                String defaultValue = null;
                if (q.equals(TAG_NAMING_URL)) {
                    defaultValue = properties.get(TAG_SERVER_PROTOCOL) + "://" +
                        properties.get(TAG_SERVER_HOST) + ":" + 
                        properties.get(TAG_SERVER_PORT) + "/" +
                        properties.get(TAG_DEPLOY_URI) + "/namingservice";
                }else if(q.equals(TAG_DEBUG_LEVEL)){
                    defaultValue = "error";
                }else if(q.equals(TAG_PASSWD_ENC_DEC_KEY)){
                    defaultValue = generateKey();
                }

                String label = (String)labels.get(q);

                if (defaultValue != null) {
                    label += " (hit enter to accept default value, " + 
                        defaultValue + ")";
                }

                System.out.print(label + ": ");
                value = (new BufferedReader(
                    new InputStreamReader(System.in))).readLine();
                value = value.trim();

                if ((value.length() == 0) && (defaultValue != null)) {
                    value = defaultValue;
                }else if ((value.length() == 0) && 
                    (q.equals(TAG_NOTIFICATION_URL))) {
                    break;
                }else if ((value.length() > 0) &&
                    (q.equals(TAG_APPLICATION_PASSWD))) {
                    SystemProperties.initializeProperties(ENC_PWD_PROPERTY,
                    (String)properties.get("ENCRYPTION_KEY"));
                    value = Crypt.encrypt(value);
                }               
            }

            properties.put(q, value);
            if (q.equals(TAG_PASSWD_ENC_DEC_KEY)) {
                properties.put(TAG_CLIENT_ENC_KEY, value);
            }           
        }
    }

    private void createPropertiesFile()
        throws IOException
    {
        String content = getFileContent(TEMPLATE_AMCONFIG_PROPERTIES);
        for (Iterator i = properties.keySet().iterator(); i.hasNext(); ) {
            String tag = (String)i.next();
            content = content.replaceAll("@" + tag + "@",
                (String)properties.get(tag));
        }
        
        StringBuffer sbDistAuthConfig = new StringBuffer();
        
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
            ("\ncom.iplanet.am.services.deploymentDescriptor=/");
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
        sbDistAuthConfig.append("#com.iplanet.am.lbcookie.name=");
        sbDistAuthConfig.append("DistAuthLBCookieName");
        sbDistAuthConfig.append("\n");        
        sbDistAuthConfig.append("#com.iplanet.am.lbcookie.value=");
        sbDistAuthConfig.append("DistAuthLBCookieValue");
        sbDistAuthConfig.append("\n");        
        
        content += sbDistAuthConfig.toString();
        
        String protocol = (String)properties.get(TAG_SERVER_PROTOCOL);
        if (protocol.equalsIgnoreCase("https")) {
            content += TRUST_ALL_CERTS;
        }

        BufferedWriter out = new BufferedWriter(new FileWriter(
            FILE_AMCONFIG_PROPERTIES));
        out.write(content);
        out.close();
    }

    private String getFileContent(String fileName)
        throws IOException
    {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        StringBuffer buff = new StringBuffer();
        String line = reader.readLine();

        while (line != null) {
            buff.append(line).append("\n");
            line = reader.readLine();
        }
        reader.close();
        return buff.toString();      
    }
    
    private static String generateKey() {
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
    
    public static void main(String args[]) {
        try {
            Main main = new Main();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MissingResourceException e) {
            e.printStackTrace();
        }
    }
}
