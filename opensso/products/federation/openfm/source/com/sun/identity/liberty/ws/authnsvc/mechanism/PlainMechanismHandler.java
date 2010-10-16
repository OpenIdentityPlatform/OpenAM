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
 * $Id: PlainMechanismHandler.java,v 1.6 2008/08/06 17:29:24 exu Exp $
 *
 */


package com.sun.identity.liberty.ws.authnsvc.mechanism;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcConstants;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcService;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcUtils;
import com.sun.identity.liberty.ws.authnsvc.protocol.SASLRequest;
import com.sun.identity.liberty.ws.authnsvc.protocol.SASLResponse;
import com.sun.identity.liberty.ws.soapbinding.Message;
import com.sun.identity.sm.SMSEntry;

/**
 * The <code>PlainMechanismHandler</code> is a handler for 'PLAIN'
 * mechanism.
 */
public class PlainMechanismHandler implements MechanismHandler {
    public static Debug debug = Debug.getInstance("libIDWSF");


    /**
     * Generates a SASL response according to the SASL request.
     * @param saslReq a SASL request
     * @param message a SOAP Message containing the SASL request
     * @param respMessageID messageID of SOAP Message response that will
     *                      contain returned SASL response
     * @return a SASL response
     */
    public SASLResponse processSASLRequest(SASLRequest saslReq,
                                       Message message, String respMessageID) {

        if (debug.messageEnabled()) {
            debug.message("PlainMechanismHandler.processSASLRequest: ");
        }

        String refToMessageID = saslReq.getRefToMessageID();
        boolean isFirstRequest = (refToMessageID == null ||
                                  refToMessageID.length() == 0);

        if (debug.messageEnabled()) {
            debug.message("PlainMechanismHandler.processSASLRequest: " + 
                "refToMessageID = " + refToMessageID);
        }

        SASLResponse saslResp = null;

        byte[] data = saslReq.getData();

        if (data == null) {
            if (isFirstRequest) {
                saslResp = new SASLResponse(SASLResponse.CONTINUE);
                saslResp.setServerMechanism(AuthnSvcConstants.MECHANISM_PLAIN);
            } else {
                saslResp = new SASLResponse(SASLResponse.ABORT);
            }
        } else {
            String dataStr = null;
            try {
                dataStr = new String(data, "UTF-8");
            } catch (Exception ex) {
                debug.error("PlainMechanismHandler.processSASLRequest: ", ex);
            }

            if (dataStr == null) {
                saslResp = new SASLResponse(SASLResponse.ABORT);
            } else {
                saslResp = authenticate(dataStr, message);
            }

            if (isFirstRequest) {
                saslResp.setServerMechanism(AuthnSvcConstants.MECHANISM_PLAIN);
            }
        }
        return saslResp;
    }

    private SASLResponse authenticate(String data, Message message) {

        int indexNul = data.indexOf('\0');
        if (indexNul == -1) {
            return new SASLResponse(SASLResponse.ABORT);
        }

        int indexNul2 = data.indexOf('\0', indexNul + 1);
        if (indexNul2 == -1) {
            return new SASLResponse(SASLResponse.ABORT);
        }

        String authzID = data.substring(0, indexNul);
        String authnID = data.substring(indexNul + 1, indexNul2);
        String password = data.substring(indexNul2 + 1);

        if (authnID == null) {
            return new SASLResponse(SASLResponse.ABORT);
        }

        if (debug.messageEnabled()) {
            debug.message("PlainMechanismHandler.authenticate: " + 
                "authzID = " + authzID + ", authnID = " + authnID);
        }

        String authModule =
            AuthnSvcService.getPlainMechanismAuthenticationModule();

        if (debug.messageEnabled()) {
            debug.message("PlainMechanismHandler.authenticate: " + 
                "authModule = " + authModule);
        }

        AuthContext authContext = null;
        try {
            authContext = new AuthContext(SMSEntry.getRootSuffix());
            authContext.login(AuthContext.IndexType.MODULE_INSTANCE,
                authModule);
        } catch (AuthLoginException le) {
            debug.error("PlainMechanismHandler.authenticate: ", le);
            return new SASLResponse(SASLResponse.ABORT);
        }

        if (authContext.hasMoreRequirements()) {
            Callback[] callbacks = authContext.getRequirements();

            if (callbacks != null) {
                fillInCallbacks(callbacks, authnID, password);
                authContext.submitRequirements(callbacks);
            }
        }

        AuthContext.Status loginStatus = authContext.getStatus();
        if (debug.messageEnabled()) {
            debug.message(
                "PlainMechanismHandler.authenticate: login status = " + 
                loginStatus);
        }
        if (loginStatus != AuthContext.Status.SUCCESS) {
            return new SASLResponse(SASLResponse.ABORT);
        }

        try {
            SSOToken token = authContext.getSSOToken();
            String userDN = token.getPrincipal().getName();
            SASLResponse saslResp = new SASLResponse(SASLResponse.OK);

            try {
                SSOTokenManager.getInstance().destroyToken(token);
            } catch (SSOException ssoex) {
                if (AuthnSvcUtils.debug.warningEnabled()) {
                    AuthnSvcUtils.debug.warning(
                        "PlainMechanismHandler.authenticate:", ssoex);
                }
            }

            if (!AuthnSvcUtils.setResourceOfferingAndCredentials(
                saslResp, message, userDN)) {
                return new SASLResponse(SASLResponse.ABORT);
            }
            return saslResp;
        } catch (Exception ex) {
            debug.error("PlainMechanismHandler.authenticate: ", ex);
            return new SASLResponse(SASLResponse.ABORT);
        }

    }

    private static void fillInCallbacks(Callback[] callbacks,
                                        String username,
                                        String password) {
        if (debug.messageEnabled()) {
            debug.message("PlainMechanismHandler.fillInCallbacks:");
        }

        for(int i = 0; i < callbacks.length; i++) {
            Callback callback = callbacks[i];

            if (callback instanceof NameCallback) {
                ((NameCallback)callback).setName(username);
            } else if (callback instanceof PasswordCallback) {
                ((PasswordCallback)callback).setPassword(
                                                  password.toCharArray());
            } 
        }
    }

}
