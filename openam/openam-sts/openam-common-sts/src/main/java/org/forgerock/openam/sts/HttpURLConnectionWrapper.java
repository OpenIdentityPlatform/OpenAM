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

import java.io.IOException;
import java.net.ProtocolException;
import java.util.Map;

/**
 * Several STS constituents make REST calls to OpenAM. This interface defines the concerns of wrapping the HttpURLConnection
 * so that the boilerplate pertaining to making these calls can be encapsulated in a single place. Note that it is not
 * intended to be a generic Http Client, but rather encapsulate some of the functionality shared across multiple consumers
 * of OpenAM REST APIs in the sts.
 */
public interface HttpURLConnectionWrapper {
    /**
     * Class which encapsulates the result of the HttpURLConnection invocation. The status code will specify the Http response
     * code, and the result will contain the contents of the response, from either the input stream, or the error stream,
     * depending upon whether the statusCode was expected.
     */
    public static class ConnectionResult {
        private int statusCode;
        private String result;

        ConnectionResult(int statusCode, String result) {
            this.statusCode = statusCode;
            this.result = result;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getResult() {
            return result;
        }
    }

    /**
     * Set the response code expected from the connection. Defaults to 200.
     * @param responseCode The expected response code.
     * @return The HttpURLConnectionWrapper to support fluent idiom
     */
    public HttpURLConnectionWrapper setExpectedResponseCode(int responseCode);

    /**
     * @param headers The set of headers to be included in the request.
     * @return The HttpURLConnectionWrapper to support fluent idiom
     */
    public HttpURLConnectionWrapper setRequestHeaders(Map<String, String> headers);

    /**
     * @param requestMethod set the desired request method (GET, POST, etc).
     * @return The HttpURLConnectionWrapper to support fluent idiom
     */
    public HttpURLConnectionWrapper setRequestMethod(String requestMethod) throws ProtocolException;

    /**
     * @param requestPayload the payload to be sent
     * @return The HttpURLConnectionWrapper to support fluent idiom
     */
    public HttpURLConnectionWrapper setRequestPayload(String requestPayload);

    /**
     * @return The ConnectionResult encapsulating the returned statusCode and the input or error stream contents.
     */
    public ConnectionResult makeInvocation() throws IOException;
}
