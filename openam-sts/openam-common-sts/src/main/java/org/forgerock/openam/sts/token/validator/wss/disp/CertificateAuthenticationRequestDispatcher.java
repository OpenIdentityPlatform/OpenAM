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
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import com.google.inject.Inject;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.AuthTargetMapping;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;

import java.net.URI;
import org.slf4j.Logger;

/**
 * Class which encapsulates knowledge as to how to post a x509 certificate to the OpenAM REST authN context.
 *
 * There is currently no OpenAM REST authN context which takes POSTed cert context, and thus this class is currently
 * not used. It will be resurrected and refactored once this context is in place.
 */
public class CertificateAuthenticationRequestDispatcher implements TokenAuthenticationRequestDispatcher<X509Certificate[]> {
    private final Logger logger;

    /**
     * String taken from org.forgerock.openam.forgerockrest.authn.callbackhandlers.RestAuthX509CallbackHandler. This
     * CallbackHandler is responsible for placing the x509 Cert in the Callback, and expects to find it in a form
     * parameter with the following identifier.
     */
    private static final String CERT_PARAM_ID = "javax.servlet.request.X509Certificate";
    private static final String OPEN_BRACKET = "{";
    private static final String CLOSE_BRACKET = "}";
    private static final String COLON = ":";
    private static final String QUOTE = "\"";

    @Inject
    public CertificateAuthenticationRequestDispatcher(Logger logger) {
        this.logger = logger;
    }

    @Override
    public Representation dispatch(URI uri, AuthTargetMapping.AuthTarget target, X509Certificate[] certificates) {
        //TODO: log if there is more than one cert - or do I just dispatch multiple requests, or ??  - for now, just log
        if (certificates.length > 1) {
            StringBuilder stringBuilder = new StringBuilder("Dealing with more than a single certificate. Their DNs:");
            for (int ndx = 0; ndx < certificates.length; ndx++) {
                stringBuilder.append("\n").append(certificates[ndx].getSubjectDN());
            }
            logger.warn(stringBuilder.toString());
        }
        ClientResource clientResource = new ClientResource(uri);
        String base64Cert = null;
        try {
            base64Cert =  javax.xml.bind.DatatypeConverter.printBase64Binary(certificates[0].getEncoded());
            //base64Cert = Base64.encode(certificates[0].getEncoded());
        } catch (CertificateEncodingException e) {
            String message = "Exception caught encoding cert: " + e;
            logger.error(message, e);
            throw new RuntimeException(message);
        }
        /*
        Hack below will go away when POSTed x509 authN module exists and consumption thereof formalized, and this class
        is re-written to adhere to said formalization.TODO
         */
        StringBuilder stringBuilder =
                new StringBuilder()
                .append(OPEN_BRACKET)
                .append(QUOTE)
                .append(CERT_PARAM_ID)
                .append(QUOTE)
                .append(COLON)
                .append(QUOTE)
                .append(base64Cert)
                .append(QUOTE)
                .append(CLOSE_BRACKET);

        StringRepresentation stringRepresentation =
                new StringRepresentation(stringBuilder.toString(), MediaType.APPLICATION_JSON);
        logger.debug("String representation of cert: " + stringRepresentation.getText());
        return clientResource.post(stringRepresentation);
    }
}
