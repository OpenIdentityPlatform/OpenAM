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
package org.forgerock.openam.saml2.plugins;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPMessage;

import org.forgerock.openam.wsfederation.common.ActiveRequestorException;

import com.iplanet.sso.SSOToken;

/**
 * Authenticates end-users for active requestor profile WS-Federation requests.
 */
public interface WsFedAuthenticator {

    /**
     * Authenticates the end-user for the incoming active WS-Federation request.
     *
     * @param request The HTTP request.
     * @param response The HTTP response.
     * @param soapMessage The SOAP message received by the STS endpoint.
     * @param realm The realm that is associated with the incoming request.
     * @param username The username extracted from the SOAP message.
     * @param password The password extracted from the SOAP message.
     * @return The {@link SSOToken} corresponding to the successful authentication. May not be null.
     * @throws ActiveRequestorException If there was any problem during the authentication, or if the authentication was
     * unsuccessful.
     */
    SSOToken authenticate(HttpServletRequest request, HttpServletResponse response, SOAPMessage soapMessage,
            String realm, String username, char[] password)
            throws ActiveRequestorException;
}
