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

/**
 * Portions Copyrighted 2011-2014 ForgeRock AS
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */

package com.sun.identity.setup;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.inject.Inject;
import org.forgerock.openam.installer.utils.StatusChecker;
import org.forgerock.openam.license.LicensePresenter;
import org.forgerock.openam.license.LicenseRejectedException;


/**
 * Application which accepts a config file, displays licenses to the user
 * and triggers OpenAM's configurator system. Once begun, uses a second thread
 * to connect to the installation progress page and print out its status.
 */
public class OpenSSOConfigurator {

    private static final String OPENSSO_CONFIGURATOR_PROPERTIES = "OpenSSOConfigurator";
    private static final String SERVER_URL = "SERVER_URL";
    private static final String DEPLOYMENT_URI = "DEPLOYMENT_URI";
    private static final String ADMIN_PWD = "ADMIN_PWD";
    private static final String ADMIN_CONFIRM_PWD = "ADMIN_CONFIRM_PWD";
    private static final String AMLDAPUSERPASSWD = "AMLDAPUSERPASSWD";
    private static final String AMLDAPUSERPASSWD_CONFIRM = "AMLDAPUSERPASSWD_CONFIRM";
    private static final String USERSTORE_TYPE = "USERSTORE_TYPE";
    private static final String ACCEPT_LICENSES = "ACCEPT_LICENSES";
    private static final String ACCEPT_LICENSES_PARAM = "acceptLicense";
    private static final String STATUS_LOCATION = "/setup/setSetupProgress?mode=text";

    private final LicensePresenter licensePresenter;
    private final ResourceBundle rb = ResourceBundle.getBundle(OPENSSO_CONFIGURATOR_PROPERTIES);

    private String userStoreType = null;
    private StringBuilder postBodySB = new StringBuilder();
    private boolean acceptLicense = false;

    @Inject
    public OpenSSOConfigurator(LicensePresenter licensePresenter) {
        this.licensePresenter = licensePresenter;
    }

    /**
     * Runs the configurator
     *
     * @param args Program arguments
     */
    public void execute(String[] args) {
        int configArg = 1; //most likely

        if (args.length < 2) {
            System.out.println(rb.getString("usage"));
            System.exit(-1);
        }

        for (int i = 0; i < args.length; i++) {
            if((i < args.length - 1) && ("--file".equals(args[i]) || "--f".equals(args[i]))) {
                configArg = i + 1;
            } else if (args[i].equals("--acceptLicense")) {
                acceptLicense = true;
            }
        }

        Properties config = new Properties();
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(args[configArg]);
            config.load(fis);
        } catch (IOException ex) {
            System.out.println(rb.getString("errorConfig"));
            System.exit(-1);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                    System.out.println(rb.getString("errorConfig"));
                    System.exit(-1);
                }
            }
        }

        String openAmURL = configure(config);

        try {
            licensePresenter.presentLicenses(acceptLicense);
        } catch (LicenseRejectedException e) {
            System.out.println(licensePresenter.getNotice());
            System.exit(-1);
        }

        // User must have accepted all license terms now, so ensure parameter is added to request
        if (postBodySB.length() > 0) {
            postBodySB.append("&");
        }
        postBodySB.append(ACCEPT_LICENSES_PARAM).append("=").append("true");

        StatusChecker sc = new StatusChecker(openAmURL, STATUS_LOCATION);
        Thread readProgressThread = new Thread(sc);
        readProgressThread.start();

        boolean success = postRequestToServer(readProgressThread, openAmURL);

        if (success) {
            System.exit(0);
        } else {
            System.exit(-1);
        }
    }

    /**
     * Sets up the Configurator with the appropriate values read from the supplied file,
     * may also set the acceptLicense instance variable.
     *
     * @param config the properties file to configure from
     * @return the OpenAM URL to post data to
     */
    private String configure(Properties config) {

        String serverURL = config.getProperty(SERVER_URL);
        String deploymentURI = config.getProperty(DEPLOYMENT_URI);
        userStoreType = config.getProperty(USERSTORE_TYPE);

        if (config.getProperty(ACCEPT_LICENSES) != null && !config.getProperty(ACCEPT_LICENSES).isEmpty()) {
            acceptLicense = Boolean.parseBoolean(config.getProperty(ACCEPT_LICENSES));
        }

        for (String key : config.stringPropertyNames()) {

            if (key.equals(USERSTORE_TYPE) || key.equals(ACCEPT_LICENSES) || key.equals(DEPLOYMENT_URI)) {
                continue;
            }

            String val = config.getProperty(key);

            if (val == null || val.length() < 1) {
                continue;
            }

            if (postBodySB.length() > 0) {
                postBodySB.append("&");
            }

            String encodedVal;
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

        return serverURL + deploymentURI;
    }

    /**
     * Talks to the OpenAM server
     *
     * @param readProgress Thread that prints out the current progress of the installation
     * @param openAmURL URL of the OpenAM instance to update
     *
     * @return true if installation succeeds, false otherwise
     */
    private boolean postRequestToServer(Thread readProgress, String openAmURL) {
        DataOutputStream os = null;
        BufferedReader br = null;
        HttpURLConnection conn = null;

        try {
            URL url = new URL(openAmURL + "/config/configurator");
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Length", Integer.toString(postBodySB.length()));
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.connect();

            os = new DataOutputStream(conn.getOutputStream());
            os.writeBytes(postBodySB.toString());
            os.flush();

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(
                        conn.getInputStream()));
                String str;
                while ((str = br.readLine()) != null) {
                    System.out.println(str);
                }
            } else {
                System.out.println(rb.getString("configFailed"));
                if ((userStoreType != null) &&
                        (userStoreType.equals("LDAPv3ForADDC"))) {
                    System.out.println(rb.getString("cannot.connect.to.UM.datastore"));
                }
                return false;
            }
        } catch (ProtocolException ex) {
            ex.printStackTrace();
            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
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
                readProgress.join(5000);
            } catch (InterruptedException e) {
            }
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ex) {
                }
            }
        }

        return true;
    }

}
