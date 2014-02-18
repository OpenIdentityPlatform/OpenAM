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

package org.forgerock.openam.installer.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Runnable to check the HTTP status of a URI, with a server and path.
 */
public class StatusChecker implements Runnable {

    private final String server;
    private final String path;

    public StatusChecker(String server, String path) {
        this.server = server;
        this.path = path;
    }

    /**
     * Connects to the resource on the supplied server at the requested
     * path. Ensures that the response is HTTP 200 and then prints the
     * contents to the screen.
     */
    public void run() {
        int retry = 0;

        while (retry <= 1) {
            retry++;
            BufferedReader br = null;
            HttpURLConnection conn = null;
            try {
                URL url = new URL(server + path);
                conn = (HttpURLConnection)url.openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.connect();

                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {

                    br = new BufferedReader(new InputStreamReader(
                            conn.getInputStream()));

                    String line;
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