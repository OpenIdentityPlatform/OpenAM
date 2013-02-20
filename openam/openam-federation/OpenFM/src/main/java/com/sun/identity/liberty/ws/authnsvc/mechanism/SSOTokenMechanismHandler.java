/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SSOTokenMechanismHandler.java,v 1.3 2008/06/25 05:49:56 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.authnsvc.mechanism;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import org.w3c.dom.Element;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcConstants;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcUtils;
import com.sun.identity.liberty.ws.authnsvc.protocol.SASLRequest;
import com.sun.identity.liberty.ws.authnsvc.protocol.SASLResponse;
import com.sun.identity.liberty.ws.soapbinding.Message;
import com.sun.identity.sm.SMSEntry;

/**
 * The <code>SSOTokenMechanismHandler</code> is a handler for 'SSOTOKEN'
 * mechanism.
 */
public class SSOTokenMechanismHandler implements MechanismHandler {
    private static String defaultOrg = SMSEntry.getRootSuffix();
    private static String MECHANISM_SSOTOKEN = "SSOTOKEN";


    /**
     * Generates a SASL response according to the SASL request
     * @param saslReq a SASL request
     * @param message a SOAP Message containing the SASL request
     * @param respMessageID messageID of SOAP Message response that will
     *                      contain returned SASL response
     * @return a SASL response
     */
    public SASLResponse processSASLRequest(SASLRequest saslReq,
                                       Message message, String respMessageID) {

        if (AuthnSvcUtils.debug.messageEnabled()) {
            AuthnSvcUtils.debug.message(
                          "SSOTokenMechanismHandler.processSASLRequest: ");
        }

        String refToMessageID = saslReq.getRefToMessageID();
        boolean isFirstRequest = (refToMessageID == null ||
                                  refToMessageID.length() == 0);

        if (AuthnSvcUtils.debug.messageEnabled()) {
            AuthnSvcUtils.debug.message(
                          "SSOTokenMechanismHandler.processSASLRequest: " + 
                          "refToMessageID = " + refToMessageID);
        }

        SASLResponse saslResp = null;

        byte[] data = saslReq.getData();

        if (data == null) {
            if (isFirstRequest) {
                saslResp = new SASLResponse(SASLResponse.CONTINUE);
                saslResp.setServerMechanism(MECHANISM_SSOTOKEN);
            } else {
                saslResp = new SASLResponse(SASLResponse.ABORT);
            }
        } else {
            String dataStr = null;
            try {
                dataStr = new String(data, "UTF-8");
            } catch (Exception ex) {
                AuthnSvcUtils.debug.error(
                          "SSOTokenMechanismHandler.processSASLRequest: ", ex);
            }

            if (dataStr == null) {
                saslResp = new SASLResponse(SASLResponse.ABORT);
            } else {
                saslResp = authenticate(dataStr, message);
            }

            if (isFirstRequest) {
                saslResp = new SASLResponse(SASLResponse.CONTINUE);
                saslResp.setServerMechanism(MECHANISM_SSOTOKEN);
            }
        }
        return saslResp;
    }

    private SASLResponse authenticate(String data, Message message) {


        if (AuthnSvcUtils.debug.messageEnabled()) {
            AuthnSvcUtils.debug.message(
                          "SSOTokenMechanismHandler.authenticate: " + 
                          "SSOTokenID = " + data);
        }

        try {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            SSOToken token = manager.createSSOToken(data); 
            manager.validateToken(token);
            String userDN = token.getPrincipal().getName();
            
            SASLResponse saslResp = new SASLResponse(SASLResponse.OK);
            if (!AuthnSvcUtils.setResourceOfferingAndCredentials(
                saslResp, message, userDN)) {            
                return new SASLResponse(SASLResponse.ABORT);
            }

            return saslResp;
        } catch (Exception ex) {
            AuthnSvcUtils.debug.error(
                          "SSOTokenMechanismHandler.authenticate: ", ex);
            return new SASLResponse(SASLResponse.ABORT);
        }

    }

}
