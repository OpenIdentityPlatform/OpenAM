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
 * $Id: SetupClientSDKSamples.java,v 1.5 2008/08/19 19:12:25 veiming Exp $
 *
 */

package com.sun.identity.setup;

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

/**
 * This setup the OpenSSO client SDK samples.
 */
public class SetupClientSDKSamples {
    private Map properties = new HashMap();
    private Map labels = new HashMap();

    private static final String FILE_AMCONFIG_PROPERTIES_TEMPLATE = 
        "resources/AMConfig.properties.template";
    private static final String FILE_AMCONFIG_PROPERTIES =
        "resources/AMConfig.properties";
    private static final String TAG_DEBUG_DIR = "DEBUG_DIR";
    private static final String TAG_APPLICATION_PASSWD =
        "ENCODED_APPLICATION_PASSWORD";
    private static final String TAG_NAMING_URL = "NAMING_URL";
    private static final String TAG_SERVER_PROTOCOL = "SERVER_PROTOCOL";
    private static final String TAG_SERVER_HOST = "SERVER_HOST";
    private static final String TAG_SERVER_PORT = "SERVER_PORT";
    private static final String TAG_DEPLOY_URI = "DEPLOY_URI";
    private static final String TAG_CLIENT_ENC_KEY = "ENCRYPTION_KEY_LOCAL";
    private static final String TAG_SESSION_PROVIDER_CLASS = 
        "SESSION_PROVIDER_CLASS";
    private static final String SESSION_PROVIDER_CLASS = 
        "com.sun.identity.plugin.session.impl.FMSessionProvider";
    private static final String TAG_CONFIGURATION_PROVIDER_CLASS = 
        "CONFIGURATION_PROVIDER_CLASS";
    private static final String CONFIGURATION_PROVIDER_CLASS = 
        "com.sun.identity.plugin.configuration.impl.ConfigurationInstanceImpl";
    private static final String TAG_DATASTORE_PROVIDER_CLASS =
        "DATASTORE_PROVIDER_CLASS";
    private static final String DATASTORE_PROVIDER_CLASS =
        "com.sun.identity.plugin.datastore.impl.IdRepoDataStoreProvider";
    private static final String TRUST_ALL_CERTS =
        "com.iplanet.am.jssproxy.trustAllServerCerts=true\n";
    private static final String CLIENT_ENC_KEY =
        "com.sun.identity.client.encryptionKey";
    private static final String AM_ENC_KEY = "am.encryption.pwd";

    private static List questions = new ArrayList();
    private static List clientQuestions = new ArrayList();

    static {
        questions.add(TAG_DEBUG_DIR);
        questions.add(TAG_APPLICATION_PASSWD);
        questions.add(TAG_SERVER_PROTOCOL);
        questions.add(TAG_SERVER_HOST);
        questions.add(TAG_SERVER_PORT);
        questions.add(TAG_DEPLOY_URI);
        questions.add(TAG_NAMING_URL);
    }
    
    public SetupClientSDKSamples()
        throws IOException, MissingResourceException
    {
        getDefaultValues();
        promptForServerAnswers();
        createPropertiesFile();
    }

    private void getDefaultValues() 
        throws MissingResourceException
    {
        ResourceBundle rb = ResourceBundle.getBundle("clientDefault");
        for (Enumeration e = rb.getKeys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            String value = (String)rb.getString(key);

            if (value.startsWith("[")) {
                labels.put(key, value.substring(1, value.length() -1));
            } else {
                properties.put(key, value);
            }
        }

        // add defaults to properties
        properties.put(TAG_DATASTORE_PROVIDER_CLASS, 
            DATASTORE_PROVIDER_CLASS);
        properties.put(TAG_CONFIGURATION_PROVIDER_CLASS,
            CONFIGURATION_PROVIDER_CLASS);
        properties.put(TAG_SESSION_PROVIDER_CLASS,
            SESSION_PROVIDER_CLASS);

        System.setProperty(CLIENT_ENC_KEY,
            (String)properties.get(TAG_CLIENT_ENC_KEY));
        System.setProperty(AM_ENC_KEY,
            (String)properties.get(TAG_CLIENT_ENC_KEY));
    }

    private void promptForServerAnswers()
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
                }
            }

            if (q.equals(TAG_APPLICATION_PASSWD)) {
                properties.put(q, (String) AccessController.doPrivileged(
                    new EncodeAction(value)));
            } else {
                properties.put(q, value);
            }
        }
    }

    private void createPropertiesFile()
        throws IOException
    {
        String content = getFileContent(FILE_AMCONFIG_PROPERTIES_TEMPLATE);
        for (Iterator i = properties.keySet().iterator(); i.hasNext(); ) {
            String tag = (String)i.next();
            String value = (String)properties.get(tag);
            value = value.replaceAll("\\\\", "\\\\\\\\");
            content = content.replaceAll("@" + tag + "@", value);
        }

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
    
    public static void main(String args[]) {
        try {
            SetupClientSDKSamples main = new SetupClientSDKSamples();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MissingResourceException e) {
            e.printStackTrace();
        }
    }
}
