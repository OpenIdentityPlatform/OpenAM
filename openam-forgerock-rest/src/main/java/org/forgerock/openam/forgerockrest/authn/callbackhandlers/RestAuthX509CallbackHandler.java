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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.forgerockrest.authn.callbackhandlers;

import com.sun.identity.authentication.spi.X509CertificateCallback;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.security.cert.X509Certificate;

/**
 * Defines methods to update a X509CertificateCallback from the headers and request of a Rest call and methods to
 * convert a Callback to and from a JSON representation.
 */
public class RestAuthX509CallbackHandler extends AbstractRestAuthCallbackHandler<X509CertificateCallback> {

    private static final String CALLBACK_NAME = "X509CertificateCallback";

    /**
     * Checks the request for the presence of a parameter named 'javax.servlet.request.X509Certificate', if present
     * and not null or empty takes the first certificate from the array and sets it on the X509CerificateCallback and
     * returns true.
     *
     * {@inheritDoc}
     */
    public boolean updateCallbackFromRequest(HttpHeaders headers, HttpServletRequest request,
            HttpServletResponse response, X509CertificateCallback callback) {

        X509Certificate[] certificates = (X509Certificate[]) request.getAttribute(
                "javax.servlet.request.X509Certificate");

        if (certificates != null && certificates.length > 0) {
            callback.setCertificate(certificates[0]);
        }

        return true;
    }

    /**
     * This method will never be called as the <code>updateCallbackFromRequest</code> method from
     * <code>AbstractRestAuthCallbackHandler</code> has been overridden.
     *
     * {@inheritDoc}
     */
    boolean doUpdateCallbackFromRequest(HttpHeaders headers, HttpServletRequest request, HttpServletResponse response,
            X509CertificateCallback callback) throws RestAuthCallbackHandlerResponseException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public X509CertificateCallback handle(HttpHeaders headers, HttpServletRequest request, HttpServletResponse response,
            JsonValue postBody, X509CertificateCallback originalCallback) {
        return originalCallback;
    }

    /**
     * {@inheritDoc}
     */
    public String getCallbackClassName() {
        return CALLBACK_NAME;
    }

    /**
     * Will throw a 400 RestAuthException with an UnsupportedOperationException as X509CertificateCallbacks cannot be
     * converted to JSON as certificate needs to be provided in the request.
     *
     * {@inheritDoc}
     */
    public JsonValue convertToJson(X509CertificateCallback callback, int index) {
        throw new RestAuthException(Response.Status.BAD_REQUEST, new UnsupportedOperationException(
                "X509Certificate must be specified in the initial request. Cannot be converted into a JSON "
                        + "representation."));
    }

    /**
     * Will throw a 400 RestAuthException with an UnsupportedOperationException as X509CertificateCallbacks cannot be
     * converted from JSON as certificate needs to be provided in the request.
     *
     * {@inheritDoc}
     */
    public X509CertificateCallback convertFromJson(X509CertificateCallback callback, JsonValue jsonCallback) {
        throw new RestAuthException(Response.Status.BAD_REQUEST, new UnsupportedOperationException(
                "X509Certificate must be specified in the initial request. Cannot be converted from a JSON "
                        + "representation."));
    }
}
