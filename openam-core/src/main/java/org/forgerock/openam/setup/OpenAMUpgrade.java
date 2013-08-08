/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

/*
 * Portions Copyrighted 2012-2013 ForgeRock Inc
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */

package org.forgerock.openam.setup;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 *
 * @author steve
 */
public class OpenAMUpgrade {
    private static final String OPENAM_UPGRADE_PROPERTIES =
        "OpenAMUpgrade";
    private static final String SERVER_URL = "SERVER_URL";
    private static final String DEPLOYMENT_URI = "DEPLOYMENT_URI";

    public static void main(String[] args) {
        ResourceBundle rb = ResourceBundle.getBundle(
            OPENAM_UPGRADE_PROPERTIES);

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

        for(Enumeration e = config.keys(); e.hasMoreElements() ;) {
            String key = (String) e.nextElement();
            String val = (String) config.get(key);

            if (val != null) {
                val = val.trim();

                if (val.length() > 0) {
                    if (key.equals(DEPLOYMENT_URI)) {
                        deploymentURI = val;
                    } else if (key.equals(SERVER_URL)) {
                        serverURL = val;
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

        String openamURL = serverURL + deploymentURI;

        ReadProgress rp = new ReadProgress(openamURL);
        Thread t = new Thread(rp);
        t.start();

        DataOutputStream os = null;
        BufferedReader br = null;
        boolean isException = false;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(openamURL + "/config/upgrade/upgrade.htm?actionLink=doUpgrade");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(false);
            conn.connect();

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(
                    conn.getInputStream()));
                String str = null;
                while ((str = br.readLine()) != null) {
                    if (str.equals("true")) {
                        System.out.println("\nUpgrade Complete.");
                    } else {
                        System.out.println("\nUpgrade Failed. Please check the amUpgrade debug file for errors");
                    }
                }
            } else {
                System.out.println(rb.getString("upgradeFailed"));
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
        String openamURL = null;

        public ReadProgress(String openamURL) {
            this.openamURL = openamURL;
        }

        public void run() {
            int retry = 0;

            while (retry <= 1) {
                retry++;
                BufferedReader br = null;
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(openamURL +
                        "/upgrade/setUpgradeProgress?mode=text");
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
