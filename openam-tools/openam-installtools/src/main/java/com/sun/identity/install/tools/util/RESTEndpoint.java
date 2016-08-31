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
 * Copyright 2016 ForgeRock AS.
 *
 * Portions Copyright 2008 Sun Microsystems Inc.
 */
package com.sun.identity.install.tools.util;

import org.forgerock.openam.utils.StringUtils;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Construct REST endpoints and call them.
 */
public final class RESTEndpoint {

    public static final String AUTHENTICATION_URI = "/json/{REALM}authenticate";
    public static final String AUTHENTICATION_URI_API_VERSION = "1.0";

    public static final String CREATE_PROFILE_URI = "/json/{REALM}agents";
    public static final String CREATE_PROFILE_URI_ACTION_VALUE = "create";
    public static final String CREATE_PROFILE_URI_API_VERSION = "1.0";

    public static final String SERVER_INFO_URI = "/json/serverinfo/*";
    public static final String SERVER_INFO_URI_API_VERSION = "1.0";

    private static final String AUTH_INDEX_TYPE_NAME = "authIndexType";
    private static final String AUTH_INDEX_TYPE_VALUE = "module";
    private static final String AUTH_INDEX_VALUE_NAME = "authIndexValue";
    private static final String AUTH_INDEX_VALUE_VALUE = "Application";

    private enum HTTPMethod {
        GET, POST;
    }

    private final String path;
    private final Map<String, String> parameters;
    private final String postData;
    private final HTTPMethod httpMethod;
    private final Map<String, String> headers;

    private RESTEndpoint(RESTEndpointBuilder builder) {
        this.path = builder.getPath();
        this.parameters = builder.parameters;
        this.postData = builder.postData;
        this.httpMethod = builder.httpMethod;
        this.headers = builder.headers;
    }

    /**
     * Call a REST endpoint, returning its response.
     * @return RESTResponse object containing status and text of returned value.
     * @throws IOException
     */
    public RESTResponse call() throws IOException {

        HttpURLConnection urlConnect = null;
        RESTResponse response = new RESTResponse();
        List<String> returnList = new ArrayList<>();

        try {
            URL serviceURL = new URL(path + paramsToString());
            urlConnect = (HttpURLConnection) serviceURL.openConnection();
            if (httpMethod == HTTPMethod.GET) {
                urlConnect.setRequestMethod("GET");
            } else {
                urlConnect.setRequestMethod("POST");
                urlConnect.setDoOutput(true);
            }
            urlConnect.setUseCaches(false);
            if (!headers.isEmpty()) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    urlConnect.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            if (httpMethod == HTTPMethod.POST) {
                // post data
                try (DataOutputStream output = new DataOutputStream(urlConnect.getOutputStream())){
                    output.writeBytes(postData);
                    output.flush();
                }
            }

            // read response
            response.setResponseCode(urlConnect.getResponseCode());

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnect.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    returnList.add(line);
                }
            }

        } catch (FileNotFoundException | UnknownHostException | ConnectException ex) {
            throw ex;

        } catch (IOException ex) {
            if (urlConnect != null) {
                InputStream is = urlConnect.getErrorStream();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                    String line;
                    while((line = br.readLine()) != null) {
                        returnList.add(line);
                    }
                }
            }
        }
        response.setContent(returnList);

        return response;
    }

    /**
     * Convert the parameters into a string of ?name=value&othername=othervalue
     * @return the parameters as a string
     */
    private String paramsToString() {
        StringBuilder result = new StringBuilder();
        if (!parameters.isEmpty()) {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                if (result.length() == 0) {
                    result.append("?");
                } else {
                    result.append("&");
                }
                result.append(entry.getKey());
                result.append("=");
                result.append(entry.getValue());
            }
        }
        return result.toString();
    }

    /**
     * ONLY EVER USE THIS FUNCTION FOR DEBUGGING PURPOSES.  IT HIDES PASSWORDS.  This obviously won't be what you
     * want in real life.
     *
     * @return A string representing the headers of this endpoint, with a clumsy attempt to knock out clear text
     * passwords
     */
    private String headersToString() {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            result.append(entry.getKey());
            result.append(": ");
            if (entry.getKey().toLowerCase().contains("password")) {
                result.append("***************");
            } else {
                result.append(entry.getValue());
            }
            result.append("\n");
        }
        return result.toString();
    }

    /**
     * Turn this RESTEndpoint into a string - ONLY for debugging purposes.
     * @return a representation of this endpoint.
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("path+params=");
        result.append(path);
        result.append(paramsToString());
        result.append(" method: ");
        result.append(httpMethod.toString());

        if (postData.length() > 0) {
            result.append(" post data ");
            if (postData.toLowerCase().contains("password")) {
                result.append(postData.length() + " bytes of POST data (hidden as it appears to contain a password)");
            } else {
                result.append(postData);
            }
        }

        if (!headers.isEmpty()) {
            result.append(" headers:");
            result.append("\n");
            result.append(headersToString());
        }
        return result.toString();
    }

    /**
     * For a little less than total debugging, try this:
     * @return The path of the endpoint
     */
    public String getPath() {
        return path + paramsToString();
    }

    /**
     * Build a RESTEndpoint
     */
    public static class RESTEndpointBuilder {
        private StringBuilder path;
        private Map<String, String> parameters;
        private String realm;
        private String postData;
        private HTTPMethod httpMethod;
        private Map<String, String> headers;

        public RESTEndpointBuilder() {
            path = new StringBuilder();
            parameters = new LinkedHashMap<>();
            headers = new LinkedHashMap<>();
            realm = null;
            httpMethod = HTTPMethod.POST;
            postData = "";
        }

        /**
         * This is where we assemble the path (straightforward in itself) but substitute the realm.  For a number of
         * URLs we use, the realm is not involved (none of the OIDC calls use it) but for others (like the identity
         * endpoint) it is very important.  Unfortunately substituting it is painful as we can accidentally change
         * <p/>
         * path1/{REALM}/path2
         * <p/>
         * to
         * <p/>
         * path1//path2
         * <p/>
         * when the realm is undefined (i.e. it is the root realm), or even worse:
         * <p/>
         * path1///path2
         * <p/>
         * when the realm is set to "/".
         *
         * @return the carefully assembled path
         */
        public String getPath() {
            String result = path.toString();

            // Trim the realm.  Note that in this way if the caller set the realm to "/" (root realm), we trim it
            // such that it becomes zero length.
            //
            if (realm != null) {
                if (realm.startsWith("/")) {
                    realm = realm.substring(1);
                }
                if (realm.endsWith("/")) {
                    realm = realm.substring(0, realm.length() - 1);
                }
            }

            if (result.contains("{REALM}/")) {
                result = result.replace("{REALM}/", "{REALM}");
            }
            if (result.contains("{REALM}")) {
                if (StringUtils.isBlank(realm)) {
                    result = result.replace("{REALM}", "");
                } else {
                    result = result.replace("{REALM}", realm + "/");
                }
            }
            return result;
        }

        /**
         * Add the specified value to the path carefully.  We must never end up gluing together two "/" characters
         * (one from the end of the previous path and another from the start of the next path).
         * @param incoming the value to append to the path.
         * @return the rest call builder object for fluency.
         */
        public RESTEndpointBuilder path(String incoming) {
            if (StringUtils.isBlank(incoming)) {
                return this;
            }
            if (incoming.startsWith("/")) {
                incoming = incoming.substring(1);
            }
            if (incoming.endsWith("/")) {
                incoming = incoming.substring(0, incoming.length() - 1);
            }
            if (this.path.length() > 0) {
                this.path.append("/");
            }
            this.path.append(incoming);

            return this;
        }

        /**
         * Add the specified realm.
         * @param s The realm.
         * @return the rest call builder object for fluency.
         */
        public RESTEndpointBuilder realm(String s) {
            realm = s;
            return this;
        }

        /**
         * Add the specified name/value pair to the list of parameters.  The list of parameters is preserved in order,
         * even though this technically may not be necessary.
         *
         * @param name the name of the parameter
         * @param value the value of the parameter
         * @return the rest call builder object for added fluency
         */
        public RESTEndpointBuilder parameter(String name, String value) {
            if (name.startsWith("&") || name.startsWith("?")) {
                name = name.substring(1);
            }
            parameters.put(name, value);
            return this;
        }

        /**
         * Add the auth index name and auth index value to the list of parameters.
         * @return the current rest call builder object
         */
        public RESTEndpointBuilder addModuleParameters() {
            parameters.put(AUTH_INDEX_TYPE_NAME, AUTH_INDEX_TYPE_VALUE);
            parameters.put(AUTH_INDEX_VALUE_NAME, AUTH_INDEX_VALUE_VALUE);
            return this;
        }

        /**
         * Add HTTP POST data.  No checking is done in the case where we're actually building a GET and the post
         * data will still be written.
         * @return the current rest call builder object
         */
        public RESTEndpointBuilder postData(String s) {
            this.postData = s;
            return this;
        }

        /**
         * Set the HTTP method to GET.
         * @return the current rest call builder object
         */
        public RESTEndpointBuilder get() {
            this.httpMethod = HTTPMethod.GET;
            return this;
        }

        /**
         * Set the HTTP method to POST
         * @return the current rest call builder object
         */
        public RESTEndpointBuilder post() {
            this.httpMethod = HTTPMethod.POST;
            return this;
        }

        /**
         * Add the name/value pair to the outgoing headers.
         * @param header The header name
         * @param value The header value
         * @return the current rest call builder object
         */
        public RESTEndpointBuilder header(String header, String value) {
            headers.put(header, value);
            return this;
        }

        /**
         * Set the API version for this endpoint.
         * @param apiVersion The specified API version
         * @return the current rest call builder object
         */
        public RESTEndpointBuilder apiVersion(String apiVersion) {
            headers.put("Accept-API-Version", "protocol=1.0,resource=" + apiVersion);
            return this;
        }

        /**
         * Build the rest endpoint object
         * @return the built rest endpoint
         */
        public RESTEndpoint build() {
            return new RESTEndpoint(this);
        }
    }

    /**
     * Class to encapsulate the response from REST API.
     */
    public static class RESTResponse {
        private int responseCode = -1;
        private List<String> content = null;

        /**
         * @return All the text returned by the endpoint, as a list of lines
         */
        public List<String> getContent() {
            return content;
        }

        /**
         * Set the content, as a list of lines, returned by the endpoint
         * @param content The content, as a list of lines
         */
        public void setContent(List<String> content) {
            this.content = content;
        }

        /**
         * @return the response code of the endpoint
         */
        public int getResponseCode() {
            return responseCode;
        }

        /**
         * set the response code of the endpoint
         * @param responseCode the response code to set
         */
        public void setResponseCode(int responseCode) {
            this.responseCode = responseCode;
        }

        /**
         * @return A string representation of the endpoint's response
         */
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
}
