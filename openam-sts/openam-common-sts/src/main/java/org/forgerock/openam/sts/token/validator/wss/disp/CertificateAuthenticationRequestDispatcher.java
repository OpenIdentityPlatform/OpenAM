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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token.validator.wss.disp;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.sun.identity.shared.encode.Base64;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.HttpURLConnectionWrapper;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.slf4j.Logger;

import javax.inject.Named;

/**
 * Class which encapsulates knowledge as to how to post a x509 certificate to the OpenAM REST authN context. This class
 * will initiate the authN process to receive the json callback with a placeholder for an X509Certificate, and set this
 * reference, and return the json callback state.
 */
public class CertificateAuthenticationRequestDispatcher implements TokenAuthenticationRequestDispatcher<X509Certificate[]> {
    private final String crestVersionAuthNService;
    private final HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory;
    private final Logger logger;

    @Inject
    public CertificateAuthenticationRequestDispatcher(@Named(AMSTSConstants.CREST_VERSION_AUTHN_SERVICE) String crestVersionAuthNService,
                                                      HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory,
                                                      Logger logger) {
        this.crestVersionAuthNService = crestVersionAuthNService;
        this.httpURLConnectionWrapperFactory = httpURLConnectionWrapperFactory;
        this.logger = logger;
    }

    @Override
    public String dispatch(URL url, AuthTargetMapping.AuthTarget target, X509Certificate[] certificates) throws TokenValidationException {
        /*
        The common practice in the cxf-sts and wss4j is just to use the first element in the array, as this is the leaf
        cert, and all others correspond to CAs, which will be in the targeted destination's trust store. TODO: this should
        be revisited when OPENAM-4597 is fixed.
         */
        if (certificates.length > 1) {
            StringBuilder stringBuilder = new StringBuilder("Dealing with more than a single certificate. Their DNs:");
            for (int ndx = 0; ndx < certificates.length; ndx++) {
                stringBuilder.append("\n").append(certificates[ndx].getSubjectDN());
            }
            logger.warn(stringBuilder.toString());
        }
        return postCertInHeader(url, certificates[0], target);
    }

    private String postCertInHeader(URL url, X509Certificate certificate, AuthTargetMapping.AuthTarget target)
            throws TokenValidationException {

        final String base64Certificate;
        try {
            base64Certificate = Base64.encode(certificate.getEncoded());
        } catch (CertificateEncodingException e) {
            throw new TokenValidationException(org.forgerock.json.resource.ResourceException.BAD_REQUEST,
                    "Could not obtain the base64-encoded representation of the client certificate: " + e, e);
        }
        if (target == null) {
            throw new TokenValidationException(org.forgerock.json.resource.ResourceException.BAD_REQUEST,
                    "When validatating X509 Certificates, an AuthTarget needs to be configured with a Map containing a String " +
                            "entry referenced by key" + AMSTSConstants.X509_TOKEN_AUTH_TARGET_HEADER_KEY +
                            " which specifies the header name which will reference the client's X509 Certificate.");
        }
        Object headerKey = target.getContext().get(AMSTSConstants.X509_TOKEN_AUTH_TARGET_HEADER_KEY);
        if (!(headerKey instanceof String)) { //checks both for null and String
            throw new TokenValidationException(org.forgerock.json.resource.ResourceException.BAD_REQUEST,
                    "When validatating X509 Certificates, an AuthTarget needs to be configured with a Map containing a String " +
                            "entry referenced by key" + AMSTSConstants.X509_TOKEN_AUTH_TARGET_HEADER_KEY +
                            " which specifies the header name which will reference the client's X509 Certificate.");
        }

        try {
            Map<String, String> headerMap = new HashMap<String, String>();
            headerMap.put(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
            headerMap.put(AMSTSConstants.CREST_VERSION_HEADER_KEY, crestVersionAuthNService);
            headerMap.put((String)headerKey, base64Certificate);
            HttpURLConnectionWrapper.ConnectionResult connectionResult =  httpURLConnectionWrapperFactory
                    .httpURLConnectionWrapper(url)
                    .setRequestHeaders(headerMap)
                    .setRequestMethod(AMSTSConstants.POST)
                    .makeInvocation();
            final int responseCode = connectionResult.getStatusCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new TokenValidationException(responseCode, "Non-200 response from posting x509 token " +
                        "to rest authN.");
            } else {
                return connectionResult.getResult();
            }
        } catch (IOException e) {
            throw new TokenValidationException(org.forgerock.json.resource.ResourceException.INTERNAL_ERROR,
                    "Exception caught posting x509 token to rest authN: " + e, e);
        }
    }
}
