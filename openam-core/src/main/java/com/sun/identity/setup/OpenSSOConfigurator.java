/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: OpenSSOConfigurator.java,v 1.4 2009/08/11 23:50:42 goodearth Exp $
 */

/*
 * Portions Copyrighted 2011 ForgeRock Inc
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */

package com.sun.identity.setup;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;

public class OpenSSOConfigurator {

    private static final String OPENSSO_CONFIGURATOR_PROPERTIES =
        "OpenSSOConfigurator";
    private static final String SERVER_URL = "SERVER_URL";
    static final String DEPLOYMENT_URI = "DEPLOYMENT_URI";
    private static final String ADMIN_PWD = "ADMIN_PWD";
    private static final String ADMIN_CONFIRM_PWD = "ADMIN_CONFIRM_PWD";
    private static final String AMLDAPUSERPASSWD = "AMLDAPUSERPASSWD";
    private static final String AMLDAPUSERPASSWD_CONFIRM =
        "AMLDAPUSERPASSWD_CONFIRM";
    private static final String USERSTORE_TYPE = "USERSTORE_TYPE";

    public static void main(String[] args) {

        ResourceBundle rb = ResourceBundle.getBundle(
            OPENSSO_CONFIGURATOR_PROPERTIES);

        if ((args.length != 2) ||
            (!(args[0].equals("--file") || args[0].equals("-f")))) {

            System.out.println(rb.getString("usage"));
            System.exit(-1);
        }

        String configFile = args[1];

        Properties config = new Properties();

        FileInputStream fis = null;
        
        try {
            fis = new FileInputStream(configFile);
            config.load(fis);
        } catch (IOException ex) {
            System.out.println(rb.getString("errorConfig"));
            System.exit(-1);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                }
            }
        }

        String serverURL = null;
        String deploymentURI = null;
        String userStoreType = null;

        StringBuilder postBodySB = new StringBuilder();
        for(Enumeration e = config.keys(); e.hasMoreElements() ;) {
            String key = (String)e.nextElement();
            String val = (String)config.get(key);

            if (val != null) {
                val = val.trim();
                if (val.length() > 0) {
                    if (key.equals(DEPLOYMENT_URI)) {
                        deploymentURI = val;
                    } else {
                        if (key.equals(SERVER_URL)) {
                            serverURL = val;
                        }
                        if (postBodySB.length() > 0) {
                            postBodySB.append("&");
                        }
                        String encodedVal = null;
                        try {
                            encodedVal = URLEncoder.encode(val, "UTF-8");
                        } catch (UnsupportedEncodingException ueex) {
                            encodedVal = val;
                        }
                        postBodySB.append(key).append("=").append(encodedVal);
                        if (key.equals(ADMIN_PWD)) {
                            postBodySB.append("&").append(ADMIN_CONFIRM_PWD)
                                .append("=").append(encodedVal);
                        } else if (key.equals(AMLDAPUSERPASSWD)) {
                            postBodySB.append("&")
                                .append(AMLDAPUSERPASSWD_CONFIRM)
                                .append("=").append(encodedVal);
                        }
                    }
                    if (key.equals(USERSTORE_TYPE)) {
                       userStoreType = val;
                    }
                }
            }
        }

        if (serverURL == null) {
            System.out.println(rb.getString("errorServerURL"));
            System.exit(-1);
        }

        if (deploymentURI == null) {
            System.out.println(rb.getString("errorDeploymentURI"));
            System.exit(-1);
        }

        if (!deploymentURI.startsWith("/")) {
            deploymentURI = "/" + deploymentURI;
        }

        if (serverURL.endsWith("/")) {
            serverURL = serverURL.substring(0, serverURL.length() - 1);
        }

        String openssoURL = serverURL + deploymentURI;

        ReadProgress rp = new ReadProgress(openssoURL);
        Thread t = new Thread(rp);
        t.start();

        DataOutputStream os = null;
        BufferedReader br = null;
        boolean isException = false;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(openssoURL + "/config/configurator");
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Length", Integer.toString(
                postBodySB.length()));
            conn.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
            conn.connect();

            os = new DataOutputStream(conn.getOutputStream());
            os.writeBytes(postBodySB.toString());
            os.flush();


            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(
                    conn.getInputStream()));
                String str = null;
                while ((str = br.readLine()) != null) {
                    System.out.println(str);
                }
            } else {
                System.out.println(rb.getString("configFailed"));
                if ((userStoreType != null) &&
                    (userStoreType.equals("LDAPv3ForADDC"))) {
                    System.out.println(rb.getString("cannot.connect.to.UM.datastore"));
                }
            }
        } catch (ProtocolException ex) {
            ex.printStackTrace();
            isException = true;
        } catch (IOException ex) {
            ex.printStackTrace();
            isException = true;
        } finally {
            if (os != null) {
                try {
                    os.close();
               } catch (IOException ex) {
                }
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                }
            }
            try {
                // wait 5 seconds if ReadProgress thread does not finished.
                t.join(5000);
            } catch (InterruptedException e) {
            }
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ex) {
                }
            }
        }
        if (isException) {
            System.exit(-1);
        } else {
            System.exit(0);
        }
    }

    static class ReadProgress implements Runnable {
        String openssoURL = null;

        public ReadProgress(String openssoURL) {
            this.openssoURL = openssoURL;
        }

        public void run() {
            int retry = 0;

            while (retry <= 1) {
                retry++;
                BufferedReader br = null;
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(openssoURL +
                        "/setup/setSetupProgress?mode=text");
                    conn = (HttpURLConnection)url.openConnection();
                    conn.setInstanceFollowRedirects(false);
                    conn.connect();

                    int responseCode = conn.getResponseCode();

                    if (responseCode == 200) {

                        br = new BufferedReader(new InputStreamReader(
                            conn.getInputStream()));

                        String line = null;
                        while ((line = br.readLine()) != null) {
                            System.out.println(line);
                        }

                    } else if (responseCode == 302) {
                        continue;
                    } else {
                        System.out.println(conn.getResponseMessage());
                    }

                } catch (ProtocolException ex) {
                    ex.printStackTrace();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException ex) {
                        }
                    }
                    if (conn != null) {
                        try {
                            conn.disconnect();
                        } catch (Exception ex) {
                        }
                    }
                }
                break;
            }
        }
    }
}