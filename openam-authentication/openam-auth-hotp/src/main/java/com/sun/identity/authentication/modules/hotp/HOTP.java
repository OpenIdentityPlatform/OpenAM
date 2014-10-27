/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: HOTP.java,v 1.1 2009/03/24 23:52:12 pluo Exp $
 *
 * Portions Copyrighted 2012-2014 ForgeRock AS.
 * Portions Copyrighted 2014 Nomura Research Institute, Ltd
 */

package com.sun.identity.authentication.modules.hotp;

import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.service.AMAuthErrorCode;
import com.sun.identity.authentication.spi.AuthErrorCodeException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.PasswordCallback;
import java.security.Principal;
import java.util.Map;
import java.util.ResourceBundle;

public class HOTP extends AMLoginModule {

    protected static final String amAuthHOTP = "amAuthHOTP";
    protected static final Debug debug = Debug.getInstance(amAuthHOTP);
    private static final String FROM_ADDRESS = "sunAMAuthHOTPSMTPFromAddress";
    ResourceBundle bundle = null;

    private String userName = null;
    private String userUUID = null;
    private int currentState;
    private Map sharedState;
    public Map currentConfig;
    protected Principal userPrincipal;

    private String enteredHOTPCode = null;

    // Module specific properties
    private static final String AUTHLEVEL = "sunAMAuthHOTPAuthLevel";
    private static final String GATEWAYSMSImplCLASS = "sunAMAuthHOTPSMSGatewayImplClassName";
    private static final String CODEVALIDITYDURATION = "sunAMAuthHOTPPasswordValidityDuration";
    private static final String CODELENGTH = "sunAMAuthHOTPPasswordLength";
    private static final String CODEDELIVERY = "sunAMAuthHOTPasswordDelivery";
    private static final String ATTRIBUTEPHONE = "openamTelephoneAttribute";
    private static final String ATTRIBUTECARRIER = "openamSMSCarrierAttribute";
    private static final String ATTRIBUTEEMAIL = "openamEmailAttribute";
    private static final String AUTO_CLICKING = "sunAMAuthHOTPAutoClicking";
    private static final String SKIP_HOTP = "skipHOTP";
    private String gatewaySMSImplClass = null;
    private String codeValidityDuration = null;
    private String codeLength = null;
    private String codeDelivery = null;
    private String telephoneAttribute = null;
    private String carrierAttribute = null;
    private String emailAttribute = null;
    private boolean skip = false;
    private boolean hotpAutoClicking = false;

    private int START_STATE = 2;

    private HOTPService hotpService;

    public void init(Subject subject, Map sharedState, Map options) {
        currentConfig = options;
        String authLevel = CollectionHelper.getMapAttr(options, AUTHLEVEL);
        if (authLevel != null) {
            try {
                setAuthLevel(Integer.parseInt(authLevel));
            } catch (Exception e) {
                debug.error("HOTP.init() : " + "Unable to set auth level " + authLevel, e);
            }
        }

        gatewaySMSImplClass = CollectionHelper.getMapAttr(options,
                GATEWAYSMSImplCLASS);
        codeValidityDuration = CollectionHelper.getMapAttr(options,
                CODEVALIDITYDURATION);
        codeLength = CollectionHelper.getMapAttr(options, CODELENGTH);
        codeDelivery = CollectionHelper.getMapAttr(options, CODEDELIVERY);

        telephoneAttribute = CollectionHelper.getMapAttr(options, ATTRIBUTEPHONE);
        carrierAttribute = CollectionHelper.getMapAttr(options, ATTRIBUTECARRIER);
        emailAttribute = CollectionHelper.getMapAttr(options, ATTRIBUTEEMAIL);
        if (debug.messageEnabled()) {
            debug.message("HOTP.init() : " + "telephone attribute=" + telephoneAttribute);
            debug.message("HOTP.init() : " + "carrier attribute=" + carrierAttribute);
            debug.message("HOTP.init() : " + "email attribute=" + emailAttribute);
        }

        java.util.Locale locale = getLoginLocale();
        bundle = amCache.getResBundle(amAuthHOTP, locale);
        if (debug.messageEnabled()) {
            debug.message("HOTP.init() : " + "HOTP resouce bundle locale=" + locale);
        }

        userName = (String) sharedState.get(getUserKey());
        if (userName == null || userName.isEmpty()) {
            try {
                //Session upgrade case. Need to find the user ID from the old session.
                SSOTokenManager mgr = SSOTokenManager.getInstance();
                InternalSession isess = getLoginState("HOTP").getOldSession();
                if (isess == null) {
                    throw new AuthLoginException("amAuth", "noInternalSession", null);
                }
                SSOToken token = mgr.createSSOToken(isess.getID().toString());
                userUUID = token.getPrincipal().getName();
                userName = token.getProperty("UserToken");
                if (debug.messageEnabled()) {
                    debug.message("HOTP.init() : UserName in SSOToken : " + userName);
                }
            } catch (SSOException ssoe) {
                debug.error("HOTP.init() : Unable to retrieve userName from existing session", ssoe);
            } catch (AuthLoginException ale) {
                debug.error("HOTP.init() : Unable to retrieve userName from existing session", ale);
            }
        }
        this.sharedState = sharedState;

        if (sharedState.containsKey(SKIP_HOTP)) {
            skip = (Boolean) sharedState.get(SKIP_HOTP);
        }
      
        hotpAutoClicking = CollectionHelper.getMapAttr(options, AUTO_CLICKING).equals("true");

        HOTPParams hotpParams = new HOTPParams(gatewaySMSImplClass, Long.parseLong(codeValidityDuration),
                telephoneAttribute, carrierAttribute, emailAttribute, codeDelivery, currentConfig,
                Integer.parseInt(codeLength), bundle.getString("messageSubject"), bundle.getString("messageContent"),
                FROM_ADDRESS);
        hotpService = new HOTPService(getAMIdentityRepository(getRequestOrg()), userName, hotpParams);
    }

    public int process(Callback[] callbacks, int state) throws AuthLoginException {
        if (skip) {
            debug.message("Skipping HOTP module");
            return ISAuthConstants.LOGIN_SUCCEED;
        }
        if (userName == null || userName.length() == 0) {
            throw new AuthLoginException("amAuth", "noUserName", null);
        }

        if (state == 1) {
            if(hotpAutoClicking) {
                debug.message("Auto sending OTP code");
                try {
                    hotpService.sendHOTP();
                    substituteHeader(START_STATE, bundle.getString("send.success"));
                } catch (AuthLoginException ale) {
                    throw new AuthErrorCodeException(AMAuthErrorCode.AUTH_ERROR, amAuthHOTP, "send.failure");
                }
            }
            return START_STATE;
        }
        
        currentState = state;
        int action = 0;
        try {    
            if (currentState == START_STATE) {
                // callback[0] is OTP code
                // callback[1] is user selected button index
                // action = 0 is Submit HOTP Code Button
                // action = 1 is Request HOTP Code Button
                if (callbacks != null && callbacks.length == 2) {
                    action =
                        ((ConfirmationCallback)
                        callbacks[1]).getSelectedIndex();
                    if (debug.messageEnabled()) {
                        debug.message("HOTP.process() : " + "LOGIN page button index: " + action);
                    }

                    if (action == 0) { //Submit HOTP Code
                        enteredHOTPCode = String.valueOf(((PasswordCallback) callbacks[0]).getPassword());
                        if (enteredHOTPCode == null || enteredHOTPCode.length() == 0) {
                            if (debug.messageEnabled()) {
                                debug.message("HOTP.process() : " + "invalid HOTP code");
                            }
                            setFailureID(userName); 
                            throw new InvalidPasswordException("amAuth", "invalidPasswd", null);
                        }

                        // Enforce the code validate time HOTP module config
                        if (hotpService.isValidHOTP(enteredHOTPCode)) {
                            return ISAuthConstants.LOGIN_SUCCEED;
                        } else {
                            setFailureID(userName);
                            throw new InvalidPasswordException("amAuth", "invalidPasswd", null);
                        }
                    } else { // Send HOTP Code
                        try {
                            hotpService.sendHOTP();
                            substituteHeader(START_STATE, bundle.getString("send.success"));
                        } catch (AuthLoginException ale) {
                            throw new AuthErrorCodeException(AMAuthErrorCode.AUTH_ERROR, amAuthHOTP, "send.failure");
                        }
                        return START_STATE;
                    }
                } else {
                    setFailureID(userName);
                    throw new AuthLoginException(amAuthHOTP, "authFailed", null);
                }

            } else {
                setFailureID(userName);
                throw new AuthLoginException(amAuthHOTP, "authFailed", null);
            }
        } catch (NumberFormatException ex) {
            debug.error("HOTP.process() : NumberFormatException Exception", ex);
            if (userName != null && userName.length() != 0) {
                setFailureID(userName);
            }
            throw new AuthLoginException(amAuthHOTP, "authFailed", null, ex);
        }
    }

    public java.security.Principal getPrincipal() {
        if (userUUID != null) {
            userPrincipal = new HOTPPrincipal(userUUID);
            return userPrincipal;
        } else if (userName != null) {
            userPrincipal = new HOTPPrincipal(userName);
            return userPrincipal;
        } else {
            return null;
        }
    }

    // cleanup state fields
    public void destroyModuleState() {
        nullifyUsedVars();
    }

    public void nullifyUsedVars() {
        bundle = null;
        userName = null;
        sharedState = null;
        currentConfig = null;
        enteredHOTPCode = null;
    }
}
