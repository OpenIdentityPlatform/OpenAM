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
 * $Id: AuthnSvcRequestHandlerImpl.java,v 1.2 2008/06/25 05:47:06 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.authnsvc;

import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;

import org.w3c.dom.Element;

import com.sun.identity.liberty.ws.authnsvc.mechanism.MechanismHandler;
import com.sun.identity.liberty.ws.authnsvc.protocol.SASLRequest;
import com.sun.identity.liberty.ws.authnsvc.protocol.SASLResponse;
import com.sun.identity.liberty.ws.common.LogUtil;
import com.sun.identity.liberty.ws.soapbinding.Message;
import com.sun.identity.liberty.ws.soapbinding.RequestHandler;

/**
 * The class <code>AuthnSvcRequestHandlerImpl</code> is used to process
 * SASL requests.
 */ 
public final class AuthnSvcRequestHandlerImpl implements RequestHandler {

    /**
     * Default constructor
     */
    public AuthnSvcRequestHandlerImpl() {
        if (AuthnSvcUtils.debug.messageEnabled()) {
            AuthnSvcUtils.debug.message(
                        "AuthnSvcRequestHanderImpl constructor.");
        }
    }
    
    /**
     * Extracts SASL request out of a SOAP Message and processes it.
     * @param request a SOAP Message containing a SASL request
     * @return a SOAP Message containing a SASL response
     * @exception AuthnSvcException if an error occurs while processing the
     *                              SOAP Message
     */
    public Message processRequest(Message request) throws AuthnSvcException {
        List list = request.getBodies(AuthnSvcConstants.NS_AUTHN_SVC,
                                      AuthnSvcConstants.TAG_SASL_REQUEST);

        if (list.isEmpty()) {
            throw new AuthnSvcException("missingSASLRequet");
        } else if (list.size() > 1) {
            throw new AuthnSvcException("tooManySASLRequet");
        }

        SASLRequest saslReq = new SASLRequest((Element)list.get(0));
        saslReq.setMessageID(request.getCorrelationHeader().getMessageID());
        saslReq.setRefToMessageID(request.getCorrelationHeader()
                                         .getRefToMessageID());

        Message message = new Message();
        String respMessageID = message.getCorrelationHeader().getMessageID();
        SASLResponse saslResp = processSASLRequest(saslReq, request,
                                                   respMessageID);
        message.setSOAPBody(saslResp.toElement());
        return message;
    }

    /**
     * Processes a SASL request and returns a SASL response.
     * @param saslReq a SASL request
     * @param message a SOAP Message containing a SASL response
     * @param respMessageID messageID of SOAP Message response that will
     *                      contain returned SASL response
     * @return a SASL response
     * @exception AuthnSvcException if an error occurs while processing the
     *                              SASL request
     */
    private static SASLResponse processSASLRequest(SASLRequest saslReq,
                                                   Message message,
                                                   String respMessageID)
        throws AuthnSvcException {

        String mechanism = saslReq.getMechanism().trim();

        if (AuthnSvcUtils.debug.messageEnabled()) {
            String msg = AuthnSvcUtils.getString("messageID") + "=" +
                     message.getCorrelationHeader().getMessageID() + ", " +
                     AuthnSvcUtils.getString("mechanism") + "=" + mechanism +
                     ", " + AuthnSvcUtils.getString("authzID") + "=" +
                     saslReq.getAuthzID() + ", " +
                     AuthnSvcUtils.getString("advisoryAuthnID") + "=" +
                     saslReq.getAdvisoryAuthnID();
            AuthnSvcUtils.debug.message(msg);
        }


        String[] data = { message.getCorrelationHeader().getMessageID(), 
                          mechanism, 
                          saslReq.getAuthzID(), 
                          saslReq.getAdvisoryAuthnID() };

        if (mechanism.length() == 0) {
            if (AuthnSvcUtils.debug.messageEnabled()) {
                AuthnSvcUtils.debug.message(
                        "AuthnSvcRequestHanderImpl.processSASLRequest: " +
                        "mechanism is empty");
            }
            if (LogUtil.isLogEnabled()) {
                LogUtil.access(Level.INFO,LogUtil.AS_ABORT,data);
            }
            return new SASLResponse(SASLResponse.ABORT);
        }

        MechanismHandler mechanismHandler = null;
        StringTokenizer stz = new StringTokenizer(mechanism);
        while(stz.hasMoreTokens()) {
            String mech = stz.nextToken();
            mechanismHandler = AuthnSvcService.getMechanismHandler(mech);
            if (mechanismHandler != null) {
                break;
            }
        }
                
        if (mechanismHandler == null) {
            if (AuthnSvcUtils.debug.messageEnabled()) {
                AuthnSvcUtils.debug.message(
                        "AuthnSvcRequestHanderImpl.processSASLRequest: " +
                        "Unable to find mechanismHandler");
            }
            if (LogUtil.isLogEnabled()) {
                LogUtil.access(Level.INFO,LogUtil.AS_ABORT,data);
            }
            return new SASLResponse(SASLResponse.ABORT);
        } else {
            if (AuthnSvcUtils.debug.messageEnabled()) {
                AuthnSvcUtils.debug.message(
                        "AuthnSvcRequestHanderImpl.processSASLRequest: " +
                        "mechanismHandler = " + mechanismHandler.getClass());
            }
        }

        SASLResponse saslResp = mechanismHandler.processSASLRequest(
                                           saslReq, message, respMessageID);
        if (LogUtil.isLogEnabled()) {
            String statusCode = saslResp.getStatusCode();

            if (statusCode.equals(SASLResponse.OK)) {
                LogUtil.access(Level.INFO,LogUtil.AS_OK,data);

            } else if (statusCode.equals(SASLResponse.CONTINUE)) {
                LogUtil.access(Level.INFO, LogUtil.AS_CONTINUE,data);

            } else {
                LogUtil.access(Level.INFO, LogUtil.AS_ABORT,data);
            }
        }

        return saslResp;
    }
}
