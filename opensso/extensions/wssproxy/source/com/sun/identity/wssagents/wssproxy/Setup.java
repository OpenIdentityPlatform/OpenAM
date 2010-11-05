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
 * $Id: Setup.java,v 1.1 2008/07/01 06:27:50 veiming Exp $
 */

package com.sun.identity.wssagents.wssproxy;

import com.sun.identity.security.EncodeAction;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Setup {
    private Map properties = new HashMap();
    private String fileClientProperties;
    private String fileAMConfigProperties;

    private static final String TAG_DEBUG_DIR = "DEBUG_DIR";
    private static final String TAG_APPLICATION_PASSWD = "APPLICATION_PASSWORD";
    private static final String TAG_ENCODED_APPLICATION_PASSWD =
        "ENCODED_APPLICATION_PASSWORD";
    private static final String TAG_BASEDIR = "BASEDIR";
    private static final String TAG_KEYPASS = "KEYPASS";
    private static final String TAG_KEYSTORE = "KEYSTORE";
    private static final String TAG_STOREPASS = "STOREPASS";
    private static final String TAG_NAMING_URL = "NAMING_URL";
    private static final String TAG_NOTIFICATION_URL = "NOTIFICATION_URL";
    private static final String TAG_SERVER_PROTOCOL = "SERVER_PROTOCOL";
    private static final String TAG_SERVER_HOST = "SERVER_HOST";
    private static final String TAG_SERVER_PORT = "SERVER_PORT";
    private static final String TAG_DEPLOY_URI = "DEPLOY_URI";
    private static final String TAG_CLIENT_ENC_KEY = "ENCRYPTION_KEY_LOCAL";
    private static final String TAG_ENCRYPTION_KEY = "ENCRYPTION_KEY";
    private static final String TRUST_ALL_CERTS =
        "com.iplanet.am.jssproxy.trustAllServerCerts=true\n";
    private static final String CLIENT_ENC_KEY =
        "com.sun.identity.client.encryptionKey";
    private static final String AM_ENC_KEY = "am.encryption.pwd";
    private static final String NOTIFICATION_SERVLET = "notificationservice";

    private static List configurations = new ArrayList();

    
    public Setup(String clientP, String configP)
        throws IOException, MissingResourceException
    {
        fileClientProperties = clientP;
        fileAMConfigProperties = configP;
        getDefaultValues();
        populateValues();
        createPropertiesFile();
    }

    private void getDefaultValues() 
        throws MissingResourceException
    {
        ResourceBundle rb = ResourceBundle.getBundle("setupValues");
        for (Enumeration e = rb.getKeys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            String value = (String)rb.getString(key);

            if (key.equals(TAG_DEPLOY_URI)) {
                if (!value.startsWith("/")) {
                    value = "/" + value;
                }
            }

            properties.put(key, value);
        }

        System.setProperty(CLIENT_ENC_KEY,
            (String)properties.get(TAG_CLIENT_ENC_KEY));
        System.setProperty(AM_ENC_KEY,
            (String)properties.get(TAG_ENCRYPTION_KEY));
    }

    private void populateValues()
        throws IOException
    {
        String encodePwd = "";

        for (Iterator i = properties.keySet().iterator(); i.hasNext(); ) {
            String q = (String)i.next();
            String value = (String)properties.get(q);
            if (value != null) {
                value = value.trim();
            }
            if ((value == null) || (value.length() == 0)) {
                if (q.equals(TAG_NAMING_URL)) {
                    value = properties.get(TAG_SERVER_PROTOCOL) + "://" +
                        properties.get(TAG_SERVER_HOST) + ":" + 
                        properties.get(TAG_SERVER_PORT) +
                        properties.get(TAG_DEPLOY_URI) + "/namingservice";
                } else if (q.equals(TAG_NOTIFICATION_URL)) {
                    value = properties.get(TAG_SERVER_PROTOCOL) + "://" +
                        properties.get(TAG_SERVER_HOST) + ":" + 
                        properties.get(TAG_SERVER_PORT) +
                        properties.get(TAG_DEPLOY_URI) + "/notificationservice";
                }
            }

            if (q.equals(TAG_APPLICATION_PASSWD)) {
                encodePwd = (String) AccessController.doPrivileged(
                    new EncodeAction(value));
            }
            properties.put(q, value);
        }

        properties.put(TAG_ENCODED_APPLICATION_PASSWD, encodePwd);

        for (Iterator i = properties.keySet().iterator(); i.hasNext(); ) {
            String q = (String)i.next();
            String value = (String)properties.get(q);

            if (q.equals(TAG_KEYPASS) ||
                q.equals(TAG_KEYSTORE) ||
                q.equals(TAG_STOREPASS)) {
                value = value.replaceAll("@BASEDIR@",
                    (String)properties.get(TAG_BASEDIR));
                value = value.replaceAll("@DEPLOY_URI@",
                    (String)properties.get(TAG_DEPLOY_URI));
                properties.put(q, value);
            }
        }
    }

    private void createPropertiesFile()
        throws IOException
    {
        String content = getFileContent(fileClientProperties);
        for (Iterator i = properties.keySet().iterator(); i.hasNext(); ) {
            String tag = (String)i.next();
            content = content.replaceAll("@" + tag + "@",
                (String)properties.get(tag));
        }

        String protocol = (String)properties.get(TAG_SERVER_PROTOCOL);
        if (protocol.equalsIgnoreCase("https")) {
            content += TRUST_ALL_CERTS;
        }

        BufferedWriter out = new BufferedWriter(new FileWriter(
            fileAMConfigProperties));
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
    
    public static void main(String args[]) {
        try {
            new Setup(args[0], args[1]);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MissingResourceException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
