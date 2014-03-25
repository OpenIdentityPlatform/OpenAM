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

/**
 * Portions Copyrighted 2012-2014 ForgeRock AS
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
import java.util.Properties;
import java.util.ResourceBundle;
import javax.inject.Inject;
import org.forgerock.openam.installer.utils.StatusChecker;
import org.forgerock.openam.license.LicensePresenter;
import org.forgerock.openam.license.LicenseRejectedException;

/**
 * Application which accepts a config file, displays licenses to the user
 * and triggers OpenAM's update system. Once begun, uses a second thread
 * to connect to the upgrade progress page and print out its status.
 */
public class OpenAMUpgrade {

    private static final String OPENAM_UPGRADE_PROPERTIES = "OpenAMUpgrade";
    private static final String URI_LOCATION = "/upgrade/setUpgradeProgress?mode=text";
    private static final String SERVER_URL = "SERVER_URL";
    private static final String ACCEPT_LICENSES = "ACCEPT_LICENSES";
    private static final String DEPLOYMENT_URI = "DEPLOYMENT_URI";

    private final LicensePresenter licensePresenter;
    private final ResourceBundle rb = ResourceBundle.getBundle(OPENAM_UPGRADE_PROPERTIES);

    private boolean acceptLicense = false;

    @Inject
    public OpenAMUpgrade(LicensePresenter licensePresenter) {
        this.licensePresenter = licensePresenter;
    }

    /**
     * Main method of the class which evaluates supplied arguments,
     * the supplied config file and sends off necessary requests
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

        StatusChecker sc = new StatusChecker(openAmURL, URI_LOCATION);
        Thread readProgressThread = new Thread(sc);
        readProgressThread.start();

        boolean success = getRequestToServer(readProgressThread, openAmURL);

        if (success) {
            System.exit(0);
        } else {
            System.exit(-1);
        }
    }

    /**
     * Talks to the OpenAM server
     *
     * @param readProgress Thread that prints out the current progress of the update
     * @param openAmURL URL of the OpenAM instance to update
     *
     * @return true if update succeeds, false otherwise
     */
    private boolean getRequestToServer(Thread readProgress, String openAmURL) {

        DataOutputStream os = null;
        BufferedReader br = null;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(openAmURL + "/config/upgrade/upgrade.htm?actionLink=doUpgrade&acceptLicense="
                    + acceptLicense);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(false);
            conn.connect();

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(
                        conn.getInputStream()));
                String str;
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

    /**
     * Sets up the Upgrader with the appropriate values read from the supplied file
     *
     * @param config the properties file to configure from
     * @return the OpenAM URL to post data to
     */
    private String configure(Properties config) {

        String serverURL = config.getProperty(SERVER_URL);
        String deploymentURI = config.getProperty(DEPLOYMENT_URI);

        if (config.getProperty(ACCEPT_LICENSES) != null && !config.getProperty(ACCEPT_LICENSES).isEmpty()) {
            acceptLicense = Boolean.parseBoolean(config.getProperty(ACCEPT_LICENSES));
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

}
