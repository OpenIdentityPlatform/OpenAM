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
 * $Id: RESTUtils.java,v 1.5 2008/08/19 19:13:03 veiming Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */
package com.sun.identity.install.tools.util;

import org.forgerock.openam.utils.IOUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Utility class to call REST APIs on OpenSSO.
 */
public class RESTUtils {

    public static final String AUTHENTICATION_URI = "/json/authenticate"
            + "?authIndexType=module"
            + "&authIndexValue=Application";
    public static final String AUTHENTICATION_URI_API_VERSION = "1.0";

    public static final String CREATE_PROFILE_URI = "/json/agents/?_action=create"
            + "&authIndexType=module"
            + "&authIndexValue=Application";
    public static final String CREATE_PROFILE_URI_API_VERSION = "1.0";

    public static final String SERVER_INFO_URI = "/json/serverinfo/*";
    public static final String SERVER_INFO_URI_API_VERSION = "1.0";

    /**
     * GET from the specified REST service URL and return its response, taking into account request headers.
     * @param url the URL to be called with GET
     * @headers name value pairs to be added into the HTTP headers
     * @return the response from called URL
     */
    public static RESTResponse getServiceURL(String url,
                                             String... headers) throws IOException {
        return callServiceURL(HTTPMethod.GET, url, null, headers);
    }

    /**
     * POST to the specified REST service URL and return its response, taking into account request headers.
     * @param url the URL to be POSTed to
     * @postData the data to be POSTed
     * @headers name value pairs to be added into the HTTP headers
     * @return the response from called URL
     */
    public static RESTResponse postServiceURL(String url,
                                              String postData,
                                              String... headers) throws IOException {
        return callServiceURL(HTTPMethod.POST, url, postData, headers);
    }

    /**
     * call the REST service URL and return its response, taking into account request headers.
     * @param url the URL to be called
     * @postData the data to be posted over the url
     * @headers name value pairs to be added into the HTTP headers
     * @return the response from called URL
     */
    public static RESTResponse callServiceURL(HTTPMethod method,
                                              String url,
                                              String postData,
                                              String... headers)
            throws IOException {

        HttpURLConnection urlConnect = null;
        DataOutputStream output = null;
        RESTResponse response = new RESTResponse();
        ArrayList returnList = new ArrayList();
        try {
            URL serviceURL = new URL(url);
            urlConnect = (HttpURLConnection)serviceURL.openConnection();
            if (method == HTTPMethod.GET) {
                urlConnect.setRequestMethod("GET");
            } else {
                urlConnect.setRequestMethod("POST");
                urlConnect.setDoOutput(true);
            }
            urlConnect.setUseCaches(false);
            if (headers.length > 0) {
                for (int i = 0; i + 1 < headers.length; i += 2) {
                    String header = headers[i];
                    String value = headers[i + 1];
                    urlConnect.setRequestProperty(header, value);
                }
            }

            if (method == HTTPMethod.POST) {
                // post data
                output = new DataOutputStream(urlConnect.getOutputStream());
                output.writeBytes(postData);
                output.flush();
            }

            // read response
            response.setResponseCode(urlConnect.getResponseCode());

            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(urlConnect.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    returnList.add(line);
                }
            } finally {
                IOUtils.closeIfNotNull(reader);
            }

        } catch (FileNotFoundException|UnknownHostException|ConnectException ex) {
            throw ex;

        } catch (IOException ex) {
            BufferedReader br = null;
            try {
                if (urlConnect != null) {
                    InputStream is = urlConnect.getErrorStream();
                    br = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = br.readLine()) != null) {
                        returnList.add(line);
                    }
                }
            } finally {
                IOUtils.closeIfNotNull(br);
            }

        } finally {
            IOUtils.closeIfNotNull(output);
        }
        response.setContent(returnList);

        return response;
    }

    /**
     * Inner public class to encapsulate the response from REST API.
     */
    public static class RESTResponse {
        private int responseCode = -1;
        private ArrayList content = null;

        public ArrayList getContent() {
            return content;
        }

        public void setContent(ArrayList content) {
            this.content = content;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public void setResponseCode(int responseCode) {
            this.responseCode = responseCode;
        }

        public String toString() {
            StringBuilder buffer = new StringBuilder();
            if (content != null) {
                for (int i = 0; i < content.size(); i++) {
                    buffer.append(content.get(i) + "\n");
                }
            }
            return buffer.toString();
        }
    }

    private enum HTTPMethod {
        GET, POST;
    }
}
