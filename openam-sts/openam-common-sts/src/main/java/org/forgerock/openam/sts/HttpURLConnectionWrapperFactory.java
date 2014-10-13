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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts;

import com.sun.identity.common.HttpURLConnectionManager;
import org.forgerock.openam.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;

/**
 * A factory to create HttpURLConnectionWrapper instances.
 */
public class HttpURLConnectionWrapperFactory {
    /**
     * Private implementation of the HttpURLConnectionWrapper.
     * @see org.forgerock.openam.sts.HttpURLConnectionWrapper
     */
    private static class HttpURLConnectionWrapperImpl implements HttpURLConnectionWrapper {
        private final HttpURLConnection httpURLConnection;
        private String requestPayload;
        private int expectedResponseCode = HttpURLConnection.HTTP_OK;

        private HttpURLConnectionWrapperImpl(URL url) throws IOException {
            httpURLConnection = HttpURLConnectionManager.getConnection(url);
        }

        @Override
        public HttpURLConnectionWrapper setExpectedResponseCode(int responseCode) {
            expectedResponseCode = responseCode;
            return this;
        }

        @Override
        public HttpURLConnectionWrapper setRequestHeaders(Map<String, String> headers) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                httpURLConnection.setRequestProperty(header.getKey(), header.getValue());
            }
            return this;
        }

        @Override
        public HttpURLConnectionWrapper setRequestMethod(String requestMethod) throws ProtocolException {
            httpURLConnection.setRequestMethod(requestMethod);
            return this;
        }

        @Override
        public HttpURLConnectionWrapper setRequestPayload(String requestPayload) {
            httpURLConnection.setDoOutput(true);
            this.requestPayload = requestPayload;
            return this;
        }

        @Override
        public ConnectionResult makeInvocation() throws IOException {
            int responseCode;
            try {
                if (requestPayload == null) {
                    httpURLConnection.connect();
                } else {
                    OutputStreamWriter writer = null;
                    try {
                        writer = new OutputStreamWriter(httpURLConnection.getOutputStream());
                        writer.write(requestPayload);
                    } finally {
                        IOUtils.closeIfNotNull(writer);
                    }
                }
                responseCode = httpURLConnection.getResponseCode();
            } catch (IOException e) {
                /*
                See 'What's new in Tiger' section of http://docs.oracle.com/javase/6/docs/technotes/guides/net/http-keepalive.html
                for an explanation of the logic below. Summary: facilitate connection re-use when an IOException occurs, the
                error stream should be obtained, drained, and then closed. This occurs when getErrorMessage() is called.
                 */
                try {
                    getErrorMessage();
                } catch (IOException ioe) {
                    //ignore - first exception the important exception
                }
                throw e;
            }
            if (responseCode == expectedResponseCode) {
                return new ConnectionResult(responseCode, getSuccessMessage());
            } else {
                return new ConnectionResult(responseCode, getErrorMessage());
            }
        }

        private String getSuccessMessage() throws IOException {
            return readInputStream(httpURLConnection.getInputStream());
        }

        private String getErrorMessage() throws IOException {
            if (httpURLConnection.getErrorStream() != null) {
                return readInputStream(httpURLConnection.getErrorStream());
            } else {
                return readInputStream(httpURLConnection.getInputStream());
            }
        }

        private String readInputStream(InputStream inputStream) throws IOException {
            if (inputStream == null) {
                throw new IOException("Null InputStream.");
            } else {
                return IOUtils.readStream(inputStream);
            }
        }
    }

    /**
     * Method invoked to obtain instances of the HttpURLConnectionWrapper.
     * @param url the URL targeted by the invocation
     * @return a HttpURLConnectionWrapper instance
     * @throws IOException if the HttpURLConnectionWrapper could not be created.
     */
    public HttpURLConnectionWrapper httpURLConnectionWrapper(URL url) throws IOException {
        return new HttpURLConnectionWrapperImpl(url);
    }
}
