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
 * Portions Copyright 2025-2026 3A Systems, LLC.
 */

package org.forgerock.openam.setup;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.ResourceBundle;
import jakarta.inject.Inject;
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

    private boolean preAcceptLicenses;

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

        if (args.length == 1 && ("--help".equals(args[0]) || "-?".equals(args[0]))) {
            System.out.println(rb.getString("usage"));
            return;
        }

        String configFile = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--file":
                case "-f":
                case "--f":
                    if (i + 1 == args.length) {
                        System.out.println(rb.getString("usage"));
                        System.exit(-1);
                    }
                    configFile = args[++i];
                    break;
                case "--acceptLicense":
                    preAcceptLicenses = true;
                    break;
                default:
                    System.out.println(rb.getString("usage"));
                    System.exit(-1);
            }
        }
        if (configFile == null) {
            System.out.println(rb.getString("usage"));
            System.exit(-1);
        }

        Properties config = new Properties();
        try (FileInputStream input = new FileInputStream(configFile)) {
            config.load(input);
        } catch (IOException ex) {
            System.out.println(rb.getString("errorConfig"));
            System.exit(-1);
        }

        String openAmURL = configure(config);

        try {
            licensePresenter.presentLicenses(preAcceptLicenses);
        } catch (LicenseRejectedException e) {
            System.out.println(licensePresenter.getNotice());
            System.exit(-1);
        }

        StatusChecker sc = new StatusChecker(openAmURL, URI_LOCATION);
        Thread readProgressThread = new Thread(sc);
        readProgressThread.start();

        System.exit(getRequestToServer(readProgressThread, openAmURL) ? 0 : -1);
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

        HttpURLConnection conn = null;
        try {
            // The license presenter has already obtained acceptance.
            URL url = new URL(openAmURL + "/config/upgrade/upgrade.htm?actionLink=doUpgrade&acceptLicense=true");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(false);
            conn.connect();

            int responseCode = conn.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.out.println(rb.getString("upgradeFailed") + " HTTP " + responseCode);
                return false;
            }

            try (BufferedReader response = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                if ("true".equals(response.readLine()) && response.readLine() == null) {
                    System.out.println("\nUpgrade Complete.");
                    return true;
                }
            }
            System.out.println("\nUpgrade Failed. Please check the amUpgrade debug file for errors");
            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            try {
                readProgress.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Sets up the Upgrader with the appropriate values read from the supplied file
     *
     * @param config the properties file to configure from
     * @return the OpenAM deployment URL
     */
    private String configure(Properties config) {

        String serverURL = config.getProperty(SERVER_URL);
        String deploymentURI = config.getProperty(DEPLOYMENT_URI);

        if (config.getProperty(ACCEPT_LICENSES) != null && !config.getProperty(ACCEPT_LICENSES).isEmpty()) {
            preAcceptLicenses = Boolean.parseBoolean(config.getProperty(ACCEPT_LICENSES));
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
