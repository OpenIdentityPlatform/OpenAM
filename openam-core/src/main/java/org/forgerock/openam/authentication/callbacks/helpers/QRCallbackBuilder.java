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
 */
package org.forgerock.openam.authentication.callbacks.helpers;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;

import org.forgerock.openam.utils.qr.GenerationUtils;
import org.forgerock.util.Reject;

import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;

/**
 * Builder class to assist in the creation of a QR code callback.
 */
public class QRCallbackBuilder {

    private static final String CALLBACK_STRING = "callback_";

    private int callbackIndex = -1;
    private String scheme;
    private String host;
    private String path = "";
    private Map<String, String> queryContents = new HashMap<String, String>();
    private String port;

    /**
     * Builds the callback from the provided information.
     *
     * @return a new Callback Object for presenting a QR code.
     */
    public Callback build() {
        Reject.ifTrue(-1 == callbackIndex, "callback index required");
        Reject.ifNull(scheme, "uri scheme is required");
        Reject.ifNull(host, "uri host is required");
        Reject.ifNull(port, "uri port is required");
        final String uri = makeUri();
        final String callback = CALLBACK_STRING + callbackIndex;
        return new ScriptTextOutputCallback(
                GenerationUtils.getQRCodeGenerationJavascriptForAuthenticatorAppRegistration(
                        callback, uri));
    }

    /**
     * Set the callback index to assign to the callback.
     * @param index the new callback index
     * @return this {@link QRCallbackBuilder}
     */
    public QRCallbackBuilder withCallbackIndex(int index){
        this.callbackIndex = index;
        return this;
    }

    /**
     * Set the uri scheme to use for the QR callback.
     * @param scheme the scheme to use in the QR callback uri
     * @return this {@link QRCallbackBuilder}
     */
    public QRCallbackBuilder withUriScheme(String scheme) {
        this.scheme = scheme;
        return this;
    }

    /**
     * Set the host to use for the uri in the QR callback.
     * @param host the host to use in the uri
     * @return this {@link QRCallbackBuilder}
     */
    public QRCallbackBuilder withUriHost(String host) {
        this.host = host;
        return this;
    }

    /**
     * Set the path to use in the iur for the QR callback.
     * @param path the path for the uri
     * @return this {@link QRCallbackBuilder}
     */
    public QRCallbackBuilder withUriPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * Set the port to use in the uri for the QR code callback.
     * @param port the port to use in the uri
     * @return this {@link QRCallbackBuilder}
     */
    public QRCallbackBuilder withUriPort(String port) {
        this.port = port;
        return this;
    }

    /**
     * Add a query component to use in the QR Callback URI.
     * @param key the key of the query object
     * @param value the value of the query object
     * @return this {@link QRCallbackBuilder}
     */
    public QRCallbackBuilder addUriQueryComponent(String key, String value) {
        queryContents.put(key, value);
        return this;
    }

    private String makeUri() {
        StringBuilder sb = new StringBuilder();
        sb.append(scheme)
                .append("://").append(host)
                .append("/").append(path)
                .append(":").append(port)
                .append("?").append(getQueryString(sb));

        return sb.toString();
    }

    private StringBuilder getQueryString(StringBuilder sb) {
        boolean first = true;
        for(Map.Entry<String, String> entries : queryContents.entrySet()) {
            if (first) {
                first = false;
            } else {
                sb.append("&");
            }
            sb.append(entries.getKey());
            sb.append("=");
            sb.append(entries.getValue());
        }
        return sb;
    }
}
