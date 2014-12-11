/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AuthnSvcClient.java,v 1.2 2008/06/25 05:47:05 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS.
 */


package com.sun.identity.liberty.ws.authnsvc;

import java.util.List;
import org.w3c.dom.Element;

import com.sun.identity.liberty.ws.authnsvc.protocol.SASLResponse;
import com.sun.identity.liberty.ws.authnsvc.protocol.SASLRequest;
import com.sun.identity.liberty.ws.soapbinding.Client;
import com.sun.identity.liberty.ws.soapbinding.Message;

/**
 * The <code>AuthnSvcClient</code> class provides web service clients with
 * a method to <code>SASL</code> request to the Authentication Service and
 * receive <code>SASL</code> response.
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class AuthnSvcClient {

    /**
     * Sends a <code>SASL</code> request to the Authentication Service SOAP
     * endpoint and returns a <code>SASL</code> response.
     *
     * @param saslReq a <code>SASL</code> request
     * @param connectTo the SOAP endpoint URL
     * @return a <code>SASL</code> response from the Authentication Service
     * @exception AuthnSvcException if authentication service is not available
     *            or there is an error in <code>SASL</code> request
     */
    public static SASLResponse sendRequest(
        SASLRequest saslReq,
        String connectTo
    ) throws AuthnSvcException {
        Message req = new Message();
        req.setSOAPBody(saslReq.toElement());
        req.getCorrelationHeader()
           .setRefToMessageID(saslReq.getRefToMessageID());

        Message resp = null;
        try {
            resp = Client.sendRequest(req, connectTo);
        } catch (Exception ex) {
            AuthnSvcUtils.debug.error("AuthnSvcClient.sendRequest:", ex);
            throw new AuthnSvcException(ex);  
        }

        List list = resp.getBodies(AuthnSvcConstants.NS_AUTHN_SVC,
                                   AuthnSvcConstants.TAG_SASL_RESPONSE);
        if (list.isEmpty()) {
            throw new AuthnSvcException("missingSASLResponse");
        } else if (list.size() > 1) {
            throw new AuthnSvcException("tooManySASLResponse");
        }

        SASLResponse saslResp = new SASLResponse((Element)list.get(0));
        saslResp.setMessageID(resp.getCorrelationHeader().getMessageID());
        saslResp.setRefToMessageID(resp.getCorrelationHeader()
                                       .getRefToMessageID());

        return saslResp;
    }
}
