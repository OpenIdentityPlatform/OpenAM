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
package org.forgerock.openam.saml2;

import com.sun.identity.saml2.common.SAML2Utils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * The SAML2ActorFactory provides creation services for SAML IDP actors.
 */
public class SAML2ActorFactory {

    /**
     * Gets an IDPRequestValidator
     *
     * @param reqBinding the request binding.
     * @param isFromECP true indicates that the request came from an ECP.
     * @return an IDPRequestValidator for performing teh validation request.
     */
    public IDPRequestValidator getIDPRequestValidator(final String reqBinding, final boolean isFromECP) {
        return new UtilProxyIDPRequestValidator(
                reqBinding, isFromECP, SAML2Utils.debug, SAML2Utils.getSAML2MetaManager());
    }

    /**
     * Gets a SAMLAuthenticator object.
     *
     * @param reqData the details of the federation request.
     * @param request the Http request object.
     * @param response the http response object.
     * @param out the output.
     * @param isFromECP true indicates that the request came from an ECP.
     * @return a SAMLAuthenticator object.
     */
    public SAMLAuthenticator getSAMLAuthenticator(final IDPSSOFederateRequest reqData,
                                                  final HttpServletRequest request,
                                                  final HttpServletResponse response,
                                                  final PrintWriter out,
                                                  final boolean isFromECP) {
        return new UtilProxySAMLAuthenticator(reqData, request, response, out, isFromECP);
    }

    /**
     * Get a SAMLAuthenticatorLookup object.
     *
     * @param reqData the details of the federation request.
     * @param request the Http request object.
     * @param response the http response object.
     * @param out the output.
     * @return a SAMLAuthenticatorLookup object.
     */
    public SAMLAuthenticatorLookup getSAMLAuthenticatorLookup(final IDPSSOFederateRequest reqData,
                                                              final HttpServletRequest request,
                                                              final HttpServletResponse response,
                                                              final PrintWriter out) {
        return new UtilProxySAMLAuthenticatorLookup(reqData, request, response, out);
    }
}
