/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package com.sun.identity.cli.rest;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOTokenID;
import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.Map;

/**
 * Rest command does a rest call to OpenAM
 */
public class RestCommand {

    private final Debug debug = Debug.getInstance("amRest");

    /**
     * Run a rest command with a SSO Token
     * @param ssoTokenID
     * @param url
     * @param requestMethod
     * @param headers
     * @param data
     * @return
     * @throws IOException
     */
    public String sendRestCommand(SSOTokenID ssoTokenID, URL url, String requestMethod, Map<String, String> headers,
            String data) throws IOException {
        headers.put(SystemProperties.get(Constants.AM_COOKIE_NAME, "iPlanetDirectoryPro"), ssoTokenID.toString());
        return sendRestCommand(url, requestMethod, headers, data);
    }

    /**
     * Run a rest command
     * @param url
     * @param requestMethod
     * @param headers
     * @param data
     * @return
     * @throws IOException
     */
    public String sendRestCommand(URL url, String requestMethod, Map<String, String> headers, String data) throws
            IOException {

        HttpURLConnection conn = HttpURLConnectionManager.getConnection(url);

        try {
            conn = HttpURLConnectionManager.getConnection(url);
            conn.setRequestMethod(requestMethod);
            for (Map.Entry<String, String> headersEntry : headers.entrySet()) {
                conn.setRequestProperty(headersEntry.getKey(), headersEntry.getValue());
            }
            conn.setRequestProperty("accept", "*/*");
            if (data != null && !data.isEmpty()) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(data.getBytes("UTF8"));
                os.flush();
                os.close();
            }

            debug.message("Response code '{}'.", conn.getResponseCode());

            InputStream inputStream = conn.getErrorStream();
            if (inputStream != null) {
                return IOUtils.readStream(inputStream);
            }
            debug.message("Error stream is null: there is no error from the server");
            return IOUtils.readStream(conn.getInputStream());

        } catch (IOException e) {
            try {
                int respCode = conn.getResponseCode();
                debug.error("IOException occurred. Response code : {}", respCode, e);

                return IOUtils.readStream(conn.getErrorStream());

            } catch(IOException ex) {
                debug.error("An IOException occurred. Can't get the content of the error stream", e);
                return "";
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}