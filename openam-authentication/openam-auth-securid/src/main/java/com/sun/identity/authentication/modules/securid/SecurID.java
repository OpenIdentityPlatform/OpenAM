/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SecurID.java,v 1.4 2009/11/10 17:51:48 ericow Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.authentication.modules.securid;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;

import javax.security.auth.*;
import javax.security.auth.callback.*;

import com.rsa.authagent.authapi.AuthAgentException;
import com.rsa.authagent.authapi.AuthSession;
import com.rsa.authagent.authapi.AuthSessionFactory;
import com.rsa.authagent.authapi.PinData;

import com.iplanet.am.util.Misc;

import com.sun.identity.authentication.spi.*;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;




/*
 *  need to implement the following methods:
 *    init(Subject subject, Map sharedState, Map options)
 *    process(Callback[] callbacks, int state)
 *    getPrincipal()
 *    shutdown()
 */

public class SecurID extends AMLoginModule {

    private Map sharedState;
    private Map options;
    private String userTokenId = null;
    private String username;
    private SecurIDPrincipal userPrincipal = null;
    private String wtOrgName = "";

    static ResourceBundle bundle = null;
    private static String bundleName = "amAuthSecurID";
    private static Debug debug = null;
    private static com.sun.identity.shared.locale.Locale locale = null;

    // this is per user auth session, but really per module instance
    private AuthSessionFactory api = null;
    private AuthSession session = null;  // this is per user auth session

    //  configDone contains paths to sdconf.rec that have verified
    private static HashMap configDone = new HashMap();

    private final String SDCONF_FILE = "sdconf.rec";
    public String STR_SECURID_CONFIG_PATH = "";  // per module instance

    private boolean getCredentialsFromSharedState;

    static {
        if (debug == null) {
            debug = Debug.getInstance ("amAuthSecurID");
        }
    }

    /*
     *  called for every new SecurID auth request session
     *  Map options contains the auth module instance's config values.
     *  for SecurID, the one of particular interest is 
     *  iplanet-am-auth-securid-server-config-path, which
     *  is defined by ISAuthConstants.SECURID_CONFIG_PATH.
     *
     *  this probably would have been a good place to get the
     *  AuthSessionFactory and AuthSession instances, but
     *  it doesn't look like you can throw an exception from init().
     *
     *  another thing is that want to use the same config path througout
     *  the user's auth session, so get it here only.  however, the
     *  checking of the path has to be done in process().
     */
    public void init(Subject subject, Map sharedState, Map options) {
        session = null;
        try {
            bundle = amCache.getResBundle("amAuthSecurID", getLoginLocale());
            if (debug.messageEnabled()) {
                debug.message("SecurID resbundle locale="+getLoginLocale());
            }
            this.options = options;
            this.sharedState = sharedState;

            String thisOrg = getRequestOrg();
            if (debug.messageEnabled()) {
                debug.message("SecurID:init:subject = " + subject +
                    "\n\tsharedState = " + sharedState +
                    "\n\toptions = " + options +
                    "\n\torg = " + thisOrg);
            }
            String config_path = Misc.getServerMapAttr (options,
                ISAuthConstants.SECURID_CONFIG_PATH);
            config_path = config_path.trim();
            if (!config_path.endsWith(Constants.FILE_SEPARATOR)) {
                config_path += Constants.FILE_SEPARATOR;
            }
            STR_SECURID_CONFIG_PATH = config_path;
            if (debug.messageEnabled()) {
                debug.message ("SecurID:init:configpath = " +
                    STR_SECURID_CONFIG_PATH);
            }
        } catch (Exception e) {
            debug.error ("SecurID.init:Error...", e);
        }
    }

    private void setDynamicText (boolean isPswd, int state, String prompt) 
        throws AuthLoginException
    {
        if (debug.messageEnabled()) {
            debug.message("SecurID:setDynamic: isPswd=" + isPswd +
                ", state=" + state); 
        }
        Callback[] callbacks = getCallback(state);

        boolean echo = false;
        if (isPswd) {
            echo = ((PasswordCallback)callbacks[0]).isEchoOn();
        }

        if (isPswd) {
            callbacks[0] = new PasswordCallback(prompt, echo);
        } else {
            if (debug.messageEnabled()) {
                debug.message(" prompt=" + prompt);
            }
            callbacks[0] = new NameCallback(prompt);
        }
        replaceCallback(state, 0, callbacks[0]);
    }

    private void verifyConfigPath () throws AuthLoginException {
        //  see if the filepath actually exists
        String filePath = STR_SECURID_CONFIG_PATH + SDCONF_FILE;
        File f = new File (filePath);

        if (debug.messageEnabled()) {
            debug.message (
                "SecurID:verifyConfigPath:checking Server File Path " +
                filePath + " for Org " + wtOrgName);
        }

        if (!f.exists()) {
            debug.error ("SecurID.verifyConfigPath:SecurID Server Path '" +
                filePath + "' does not exist.  Organization = " + wtOrgName);
            throw new AuthLoginException(bundleName, 
                "SecurIDSrvrPathNoExist", null);
        } else {
            if (debug.messageEnabled()) {
                debug.message(
                    "SecurID:verifyConfigPath:found SecurID Server Path = " +
                    filePath + " for Org " + wtOrgName);
            }
        }
    }


    public int process(Callback[] callbacks, int state)
        throws AuthLoginException
    {
        String nextToken;
        int rtnval = -1;
        String tmp_passcode = null;

        /*
         *  state starts at 1, numbering corresponds to order of screens.
         *  return -1 if done, next screen# if another screen
         */

        wtOrgName = getRequestOrg();

        if (debug.messageEnabled()) {
            debug.message ("SecurID:process: Org = " + wtOrgName +
                "\n\tstate = " + state);
        }

        /*
         * see if this org not initialized.
         * the path to sdconf.rec was gotten in init();
         * verify that it exists once.  after that, can
         * get the AuthSessionFactory.getInstance every time,
         * as it will return the same one, given the same path.
         */
        if (!configDone.containsKey(STR_SECURID_CONFIG_PATH)) {
            // verify path to sdconf.rec
            verifyConfigPath();
            configDone.put(STR_SECURID_CONFIG_PATH, "true");
        }

        /*
         *  not particularly pretty getting the
         *  AuthSessionFactory instance every time, but the
         *  SecurID api states that it returns the same instance
         *  for the given path.  plus this way saves having to
         *  keep track of stuff...
         */
        if (api == null) {
            debug.message("SecurID.process:getting Session instance");
            try {
                api = AuthSessionFactory.getInstance(STR_SECURID_CONFIG_PATH);
            } catch (AuthAgentException e) {
                debug.error ("SecurID.process:" + "Unable to get SecurID API.");
                throw new AuthLoginException (bundleName, "SecurIDInitLex",
                    null, e);
            }
        }

        if (debug.messageEnabled()) {
            debug.message ("SecurID:process:after configDone: Org = " +
                wtOrgName +
                "\n\tstate = " + state +
                "\n\tconfig_path = " + STR_SECURID_CONFIG_PATH +
                "\n\tuserTokenId = " + userTokenId +
                "\n\tusername = " + username);
        }

        String newPin = "";
        int authStatus = AuthSession.ACCESS_DENIED;
        PinData pinData = null;

        switch (state) {
            case ISAuthConstants.LOGIN_START:        // initial state (1)
            {
                if (callbacks != null && callbacks.length == 0) {
                   username = (String) sharedState.get(getUserKey()); 
                   tmp_passcode = (String) sharedState.get(getPwdKey()); 
                   if (username == null || tmp_passcode == null) {
                        return 1;
                   }
                   getCredentialsFromSharedState = true;
                } else {
                    username = ((NameCallback)callbacks[0]).getName();
                    // null userid is a no-no
                    if (username == null || username.length() == 0) {
                        throw new AuthLoginException (bundleName, 
                            "SecurIDUserIdNull", null);
                    }

                    tmp_passcode = charToString(
                        ((PasswordCallback)callbacks[1]).getPassword(),
                        callbacks[1]);
                
                    // null passcode is also a no-no
                    if (tmp_passcode == null || tmp_passcode.length() == 0) {
                        throw new AuthLoginException (bundleName, 
                            "SecurIDPasscodeNull", null);
                    }
                }

                if (debug.messageEnabled()) {
                    debug.message("SecurID.process(): username: " + username);
                }
                storeUsernamePasswd (username, tmp_passcode);

                //  got the userid and passcode
                try {
                    session = api.createUserSession();
                } catch (AuthAgentException aaex) {
                    debug.error("SecurID.process:createUserSession() error:"
                        + aaex.getMessage());
                    setFailureID(username);
                    throw new AuthLoginException (bundleName, 
                        "SecurIDInitializeLex",
                        new Object[] { aaex.getMessage()});
                }

                try {
                    authStatus = session.lock(username);

                    if (debug.messageEnabled()) {
                        debug.message(
                            "SecurID.process:session.lock returns = " +
                            authStatus);
                    }

                    authStatus = session.check(username, tmp_passcode);

                    if (debug.messageEnabled()) {
                        debug.message(
                            "SecurID.process:session.check returns = " +
                            authStatus);
                    }
                    /*
                     *  after sending userid and passcode, can get returns:
                     *    ACCESS_OK
                     *    ACCESS_DENIED
                     *    NEW_PIN_REQUIRED
                     *    NEXT_CODE_REQUIRED
                     */

                    switch (authStatus) {
                        case AuthSession.ACCESS_OK:
                            debug.message("SecurID.process:ACCESS_OK");
                            userTokenId = username;
                            rtnval = ISAuthConstants.LOGIN_SUCCEED;
                            break;
                        case AuthSession.NEW_PIN_REQUIRED:  // new PIN mode
                        {
                            debug.message("SecurID.process:NEW_PIN_REQUIRED");
                            pinData = session.getPinData();
                            int pinState = pinData.getUserSelectable();
                            String msg = getNewPinMsg(pinData);
                            //  if user can't choose their own pin
                            if (pinState == PinData.CANNOT_CHOOSE_PIN) {
                                debug.message(
                                    "SecurID.process:CANNOT_CHOOSE_PIN");
                                newPin = pinData.getSystemPin().trim();
                                // submit new PIN
                                if (newPin.length() != 0) {
                                    authStatus = session.pin(newPin);
                                    if (debug.messageEnabled()) {
                                        debug.message(
                                            "SecurID.process:CCP:pin rtns = " +
                                            authStatus);
                                    }
                                    if(authStatus != AuthSession.PIN_ACCEPTED){
                                        /*
                                         * weird that we'd get an error
                                         * submitting the PIN provided by
                                         * the system...
                                         * could do error handling here,
                                         * or having the user submit a
                                         * null pin will make things terminate
                                         * subsequently...
                                         */
                                        debug.error(
                                            "SecurID.process:CCP:sys pin " +
                                            "not accepted!");
                                        if (session != null) {
                                            try {
                                                session.close();
                                            } catch (AuthAgentException aax) {
                                                    debug.error(
                                                    "SecurID.process:NPRCCP:" +
                                                    "close err = " +
                                                    aax.getMessage());
                                            }
                                            session = null;
                                        }
                                        throw new AuthLoginException(
                                            bundleName, "SecurIDLoginFailed",
                                            new Object[]{username});
                                    }
                                } else {
                                    /*
                                     * weird that we'd get a null PIN
                                     * from the system...
                                     */
                                     debug.message(
                                        "SecurID.process:CCP:newPin 0-length");
                                    newPin = "";
                                    if (session != null) {
                                        try {
                                            session.close();
                                        } catch (AuthAgentException aax) {
                                                debug.error(
                                                "SecurID.process:LSNP:" +
                                                "close err = " +
                                                aax.getMessage());
                                        }
                                        session = null;
                                    }
                                    throw new AuthLoginException(
                                        bundleName, "SecurIDLoginFailed",
                                        new Object[]{username});
                                }
                                /*
                                 * then tell user the new PIN, and to do
                                 * next token
                                 */
                                setDynamicText(true,
                                    ISAuthConstants.LOGIN_NEW_PIN_NEXT_TOKEN,
                                    bundle.getString("SecurIDWaitPin") +
                                    bundle.getString("SecurIDNewSysPin") + 
                                    newPin);
                                rtnval =
                                    ISAuthConstants.LOGIN_NEW_PIN_NEXT_TOKEN;
                            } else if (pinState == PinData.USER_SELECTABLE) {
                                // see if user wants user-gen or sys-gen
                                debug.message(
                                    "SecurID.process:USER_SELECTABLE");
                                setDynamicText(false,
                                    ISAuthConstants.LOGIN_SYS_GEN_PIN,
                                    bundle.getString("SecurIDSysGenPin"));
                                rtnval = ISAuthConstants.LOGIN_SYS_GEN_PIN;
                            } else if (pinState == PinData.MUST_CHOOSE_PIN) {
                                debug.message(
                                    "SecurID.process:MUST_CHOOSE_PIN");
                                // user must provide new PIN
                                setDynamicText(true,
                                    ISAuthConstants.LOGIN_CHALLENGE, msg);
                                rtnval = ISAuthConstants.LOGIN_CHALLENGE;
                                if (debug.messageEnabled()) {
                                    debug.message("SecurID.process:prompt = " +
                                        msg);
                                }
                            } else {  // huh?
                                debug.error(
                                    "SecurID.process:NEW_PIN_REQUIRED:" +
                                    "unknown pinState = " + pinState);
                                if (session != null) {
                                    try {
                                        session.close();
                                    } catch (AuthAgentException aax) {
                                            debug.error("SecurID.process:NPRQ:"+
                                            "close err = " +
                                            aax.getMessage());
                                    }
                                    session = null;
                                        setFailureID(username);
                                    throw new AuthLoginException(bundleName,
                                        "SecurIDLoginFailed",
                                        new Object[]{username});
                                }
                            }
                        }
                            break;
                        case AuthSession.NEXT_CODE_REQUIRED: // next token mode
                            debug.message("SecurID.process:NEXT_CODE_REQUIRED");
                            rtnval = ISAuthConstants.LOGIN_NEXT_TOKEN;
                            break;
                        case AuthSession.ACCESS_DENIED:
                            debug.message("SecurID.process:ACCESS_DENIED");
                        default:
                            debug.message("SecurID.process:state == default");
                            if (getCredentialsFromSharedState
                                && !isUseFirstPassEnabled())
                            {
                                getCredentialsFromSharedState = false;
                                rtnval = ISAuthConstants.LOGIN_START;
                                break;
                            }
                            setFailureID(username);
                            if (session != null) {
                                try {
                                    session.close();
                                } catch (AuthAgentException aax) {
                                    debug.error("SecurID.process:LSAD:" +
                                        "close err = "+ aax.getMessage());
                                }
                                session = null;
                            }
                            throw new AuthLoginException(bundleName,
                                "SecurIDLoginFailed", new Object[]{username});
                    }
                } catch (AuthAgentException aaex) {
                    if (session != null) {
                        try {
                            session.close();
                        } catch (AuthAgentException aax) {
                            debug.error("SecurID.process:LS:close err = " +
                                aax.getMessage());
                        }
                        session = null;
                    }
                    debug.error("SecurID.process:session lock/check:" +
                        aaex.getMessage());
                    setFailureID(username);
                    throw new AuthLoginException (bundleName,
                        "SecurIDInitializeLex",
                        new Object[] { aaex.getMessage()});
                }
            }
                break;

            case ISAuthConstants.LOGIN_CHALLENGE:        // new PIN mode (2)
            {
                debug.message("SecurID.process:LOGIN_CHALLENGE");
                // submit new PIN
                String newPIN = charToString(
                    ((PasswordCallback)callbacks[0]).getPassword(),
                    callbacks[0]);

                /*
                 *  if no PIN provided, submit "" as the new PIN, and
                 *  let the ACE/Server handle it (by returning an error)
                 */
                if (newPIN == null) {
                    newPIN = "";  // might not pass the ASCII test below
                }

                if (debug.messageEnabled()) {
                    debug.message("SecurID.process:state2: token length = " +
                        newPIN.length());
                }

                try {
                    if (!newPIN.equals(new String(newPIN.getBytes("ASCII"),
                        "ASCII")))
                    {
                        if (session != null) {
                            try {
                                session.close();
                            } catch (AuthAgentException aax) {
                                debug.error("SecurID.process:LC:close err = " +
                                    aax.getMessage());
                            }
                            session = null;
                        }
                        setFailureID(username);
                        throw new AuthLoginException(bundleName, 
                            "SecurIDNewPINNotASCII", null);
                    }
                } catch (UnsupportedEncodingException ueex) {
                    if (session != null) {
                        try {
                            session.close();
                        } catch (AuthAgentException aax) {
                            debug.error("SecurID.process:LC2:close err = " +
                                aax.getMessage());
                        }
                        session = null;
                    }
                    setFailureID(username);
                    throw new AuthLoginException(bundleName, 
                        "SecurIDInputEncodingException", null);
                }

                try {
                    authStatus = session.pin(newPIN);

                    if (debug.messageEnabled()) {
                        debug.message("SecurID.process:session.pin returns " +
                            authStatus);
                    }
                    if (authStatus == AuthSession.PIN_ACCEPTED) {
                        debug.message("SecurID.process:new pin ACCEPTED");
                        rtnval = ISAuthConstants.LOGIN_NEW_PIN_NEXT_TOKEN;
                        setDynamicText(true,
                            ISAuthConstants.LOGIN_NEW_PIN_NEXT_TOKEN, 
                            bundle.getString("SecurIDWaitPin"));
                        userTokenId = username;
                    } else if (authStatus == AuthSession.PIN_REJECTED) {
                        debug.message(
                            "SecurID:process:New PIN specified is invalid.");
                        if (session != null) {
                            try {
                                session.close();
                            } catch (AuthAgentException aax) {
                                debug.error("SecurID.process:LC3:close err = "+
                                    aax.getMessage());
                            }
                            session = null;
                        }
                        setFailureID(username);
                        throw new AuthLoginException(bundleName,
                            "SecurIDAuthInvNewPin", null);
                    } else {
                        // hmmm...
                        debug.error(
                            "SecurID.process:unsure this pin response value.");
                        if (session != null) {
                            try {
                                session.close();
                            } catch (AuthAgentException aax) {
                                debug.error("SecurID.process:LC4:close err = "+
                                    aax.getMessage());
                            }
                            session = null;
                        }
                    }
                } catch (AuthAgentException aaex) {
                    // probably have to terminate the session
                    debug.error("SecurID.process:session.pin exception: " +
                        aaex.getMessage());
                    if (session != null) {
                        try {
                            session.close();
                        } catch (AuthAgentException aax) {
                            debug.error("SecurID.process:LC5:close err = " +
                                aax.getMessage());
                        }
                        session = null;
                    }
                    setFailureID(username);
                    throw new AuthLoginException(bundleName, 
                        "SecurIDAuthInvNewPin", null);
                }
            }
                break;

            case ISAuthConstants.LOGIN_NEXT_TOKEN:  // next token mode (3)
            {
                // can do PIN+passcode or just passcode
                debug.message("SecurID.process:LOGIN_NEXT_TOKEN");
                // got the next token; submit it
                nextToken = charToString(
                    ((PasswordCallback)callbacks[0]).getPassword(),
                        callbacks[0]);

                // must have something
                if (nextToken == null) {
                    nextToken = "";  // might not pass the ASCII test below
                }
                        
                if (debug.messageEnabled()) {
                    debug.message(
                        "SecurID.process:LOGIN_NEXT_TOKEN:token length = " +
                        nextToken.length());
                }

                try {
                    if(!nextToken.equals(
                        new String(nextToken.getBytes("ASCII"),"ASCII")))
                    {
                        setFailureID(username);
                        if (session != null) {
                            try {
                                session.close();
                            } catch (AuthAgentException aax) {
                                debug.error("SecurID.process:NT:close err = " +
                                    aax.getMessage());
                            }
                            session = null;
                        }
                        debug.error(
                            "SecurID.process:LOGIN_NEXT_TOKEN:" +
                            "nextToken not ascii");
                        throw new AuthLoginException(bundleName, 
                            "SecurIDNextTokenNotASCII", null);
                    }
                } catch (UnsupportedEncodingException ueex) {
                    if (session != null) {
                        try {
                            session.close();
                        } catch (AuthAgentException aax) {
                            debug.error("SecurID.process:NT2:close err = " +
                                aax.getMessage());
                        }
                        session = null;
                    }
                    setFailureID(username);
                    debug.error("SecurID.process:LOGIN_NEXT_TOKEN:" +
                        "nextToken input encoding");
                    throw new AuthLoginException(bundleName, 
                        "SecurIDInputEncodingException", null);
                }

                try {
                    authStatus = session.next(nextToken);
                    if (debug.messageEnabled()) {
                        debug.message("SecurID.process:LOGIN_NEXT_TOKEN:" +
                        "next returns " + authStatus);
                    }
                } catch (AuthAgentException aaex) {
                    debug.error(
                        "SecurID.process:LOGIN_NEXT_TOKEN:next() exception:" +
                        aaex.getMessage());
                    if (session != null) {
                        try {
                            session.close();
                        } catch (AuthAgentException aax) {
                            debug.error("SecurID.process:NT3:close err = " +
                                aax.getMessage());
                        }
                        session = null;
                    }
                    setFailureID(username);
                    throw new AuthLoginException(bundleName, 
                        "SecurIDInvNextToken", null);
                }

                if (authStatus == AuthSession.ACCESS_OK) {
                    // succeed
                    userTokenId = username;
                    rtnval = ISAuthConstants.LOGIN_SUCCEED;
                } else {
                    if (session != null) {
                        try {
                            session.close();
                        } catch (AuthAgentException aax) {
                            debug.error("SecurID.process:NT4:close err = " +
                                aax.getMessage());
                        }
                        session = null;
                    }
                    setFailureID(username);
                    if (debug.messageEnabled()) {
                        debug.message("SecurID.process:LOGIN_NEXT_TOKEN:" +
                            "nextToken failure");
                    }
                    throw new AuthLoginException(bundleName, 
                        "SecurIDInvNextToken", null);
                }
            }
                break;

            case ISAuthConstants.LOGIN_SYS_GEN_PIN: // sys genned PIN answer(4)
            {
                debug.message("SecurID.process:LOGIN_SYS_GEN_PIN");
                // server asked if sys-genned PIN wanted, user said...
                String answer = ((NameCallback)callbacks[0]).getName();

                if (debug.messageEnabled()) {
                    debug.message(
                        "SecurID.process:received answer(state 4) = " +
                        answer);
                }

                // must have something
                boolean sysgenpin = false;
                if (answer == null || answer.length() == 0 ) {
                    sysgenpin = true;        // make it system generated
                    if (debug.messageEnabled()) {
                        debug.message(
                            "SecurID.process:made answer(state 4) = " +
                            sysgenpin);
                    }
                } else if ( answer.startsWith("y") || answer.startsWith("Y")) {
                    sysgenpin = true;
                }

                if (sysgenpin) {
                    debug.message(
                        "SecurID.process:LOGIN_SYS_GEN_PIN:" +
                        "about to getSystemPin");
                    try {
                        pinData = session.getPinData();
                        newPin = pinData.getSystemPin();
                        authStatus = session.pin(newPin);
                        if (debug.messageEnabled()) {
                            debug.message("SecurID.process:LSGP:" +
                                "newPin:pin() response = " + authStatus);
                        }
                        setDynamicText(true,
                            ISAuthConstants.LOGIN_NEW_PIN_NEXT_TOKEN,
                            bundle.getString("SecurIDWaitPin") +
                            bundle.getString("SecurIDNewSysPin") + newPin);
                        userTokenId = username;
                        rtnval = ISAuthConstants.LOGIN_NEW_PIN_NEXT_TOKEN;
                    } catch (AuthAgentException aaex) {
                        // probably have to terminate the session
                        debug.error(
                            "SecurID.process:LSGP:getSystemPin/pin error = " +
                            aaex.getMessage());
                        if (session != null) {
                            try {
                                session.close();
                            } catch (AuthAgentException aax) {
                                debug.error("SecurID.process:SGP:close err = "+
                                    aax.getMessage());
                            }
                            session = null;
                        }
                        setFailureID(username);

                        if (getCredentialsFromSharedState
                            && !isUseFirstPassEnabled())
                        {
                            getCredentialsFromSharedState = false;
                            rtnval = ISAuthConstants.LOGIN_START;
                        }
                        throw new AuthLoginException(bundleName,
                            "SecurIDAuthInvNewPin", null);
                    }
                } else {
                    // user-generated PIN
                    try {
                        String msg = getNewPinMsg(session.getPinData());
                        if (debug.messageEnabled()) {
                            debug.message(
                            "SecurID.process:LOGIN_SYS_GEN_PIN:" +
                            "about to get user-genned PIN, prompt = \n\t"+msg);
                        }
                        setDynamicText(true, ISAuthConstants.LOGIN_CHALLENGE,
                            msg);
                        rtnval = ISAuthConstants.LOGIN_CHALLENGE;
                    } catch (AuthAgentException aaex) {
                        // probably have to terminate the session
                        debug.error("SecurID.process:" +
                            "session.getPinData exception: " +
                            aaex.getMessage());
                        if (session != null) {
                            try {
                                session.close();
                            } catch (AuthAgentException aax) {
                                debug.error("SecurID.process:UGP:close err = "+
                                    aax.getMessage());
                            }
                            session = null;
                        }
                        setFailureID(username);
                        throw new AuthLoginException(bundleName, 
                            "SecurIDAuthInvNewPin", null);
                    }
                }
            }
                break;

            case ISAuthConstants.LOGIN_NEW_PIN_NEXT_TOKEN: // 5
            {
                /*
                 * next token mode : case 2
                 * After new PIN mode, we have lock the user again.
                 */

                if (debug.messageEnabled()) {
                    debug.message("LOGIN_NEW_PIN_NEXT_TOKEN:username = " +
                        username);
                }

                //  username should contain the userid entered earlier

                if (username == null || username.length() == 0) {
                    setFailureID(username);
                    throw new AuthLoginException(bundleName, 
                        "SecurIDPrevUserid", null);
                }
                
                //  only one callback... the new pin + token

                nextToken = charToString(
                    ((PasswordCallback)callbacks[0]).getPassword(),
                    callbacks[0]);

                /*
                 * if nothing provided, 
                 * send a null string, and let the ACE/Server handle it.
                 */
                if (nextToken == null) {
                    nextToken = "";  // might not pass ASCII test below
                }

                try {
                    if(!nextToken.equals(new String(
                        nextToken.getBytes("ASCII"),"ASCII")))
                    {
                        setFailureID(username);
                        if (session != null) {
                            try {
                                session.close();
                            } catch (AuthAgentException aax) {
                                debug.error("SecurID.process:NPT:close err = "+
                                    aax.getMessage());
                            }
                            session = null;
                        }
                        throw new AuthLoginException(bundleName, 
                            "SecurIDNextTokenNotASCII", null);
                    }
                } catch (UnsupportedEncodingException ueex) {
                    if (session != null) {
                        try {
                            session.close();
                        } catch (AuthAgentException aax) {
                            debug.error("SecurID.process:NPT2:close err = " +
                                aax.getMessage());
                        }
                        session = null;
                    }
                    setFailureID(username);
                    throw new AuthLoginException(bundleName, 
                        "SecurIDInputEncodingException", null);
                }

                debug.message("SecurID.process:LNPNT:doing session.check");
                authStatus = AuthSession.ACCESS_DENIED;
                try {
                    authStatus = session.lock(username);
                    if (debug.messageEnabled()) {
                        debug.message("SecurID.process:LNPNT:lock returns " +
                            authStatus);
                    }
                    authStatus = session.check(username, nextToken);
                    if (debug.messageEnabled()) {
                        debug.message("SecurID.process:LNPNT:next returns " +
                            authStatus);
                    }
                } catch (AuthAgentException aaex) {
                    debug.error("SecurID.process:LNPNT:next() gets exception:"+
                        aaex.getMessage());
                    if (session != null) {
                        try {
                            session.close();
                        } catch (AuthAgentException aax) {
                            debug.error("SecurID.process:NPT3:close err = " +
                                aax.getMessage());
                        }
                        session = null;
                    }
                    setFailureID(username);
                    throw new AuthLoginException(bundleName, 
                        "SecurIDLoginFailed", new Object[]{username});
                }

                if (debug.messageEnabled()) {
                    debug.message("SecurID.process:LNPNT:ACCESS_OK = " +
                        AuthSession.ACCESS_OK + ", authStatus = " +
                        authStatus);
                }

                if (authStatus == AuthSession.ACCESS_OK) {
                    // succeed
                    if (debug.messageEnabled()) {
                        debug.message("SecurID.process:LNPNT:next() =" +
                        " LOGIN_SUCCEED, username = " + username);
                    }
                    userTokenId = username;
                    rtnval = ISAuthConstants.LOGIN_SUCCEED;
                } else {
                    // login failed
                    if (debug.messageEnabled()) {
                        debug.message("SecurID.process:LNPNT:next() " +
                        "gets NOT Succeed = " + authStatus);
                    }
                    if (session != null) {
                        try {
                            session.close();
                        } catch (AuthAgentException aax) {
                            debug.error("SecurID.process:NPT4:close err = " +
                                aax.getMessage());
                        }
                        session = null;
                    }
                    setFailureID(username);
                    throw new AuthLoginException(bundleName, 
                        "SecurIDLoginFailed", new Object[]{username});
                } 
            }
                break;

            default:
                if (session != null) {
                    try {
                        session.close();
                    } catch (AuthAgentException aax) {
                        debug.error("SecurID.process:DEF:close err = " +
                            aax.getMessage());
                    }
                    session = null;
                }
                setFailureID(username);
                throw new AuthLoginException(bundleName, "SecurIDAuth", null);
        }

        if (debug.messageEnabled()) {
            debug.message ("process; after process:" +
                "\n\tstate = " + state +
                "\n\tuserTokenId = " + userTokenId +
                "\n\tusername = " + username +
                "\n\trtnval = " + rtnval);
        }

        if (rtnval == ISAuthConstants.LOGIN_SUCCEED) {
            if (session != null) {
                try {
                    session.close();
                } catch (AuthAgentException aax) {
                    debug.error(
                        "SecurID.process:LOGIN_SUCCEED:close err = " +
                        aax.getMessage());
                }
                session = null;
            }
        }

        return (rtnval);

    }        // process


    private String charToString (char [] tmpPassword, Callback cbk ) {
        if (tmpPassword == null) {
            // treat a NULL password as an empty password
            tmpPassword = new char[0];
        }
        char[] pwd = new char[tmpPassword.length];
        System.arraycopy(tmpPassword, 0, pwd, 0, tmpPassword.length);
        ((PasswordCallback)cbk).clearPassword();
        return new String(pwd);
    }

    private String getNewPinMsg(PinData pinData) {
        int pinState = pinData.getUserSelectable();
        int pinMin = pinData.getMinPinLength();
        int pinMax = pinData.getMaxPinLength();
        if (debug.messageEnabled()) {
            debug.message("SecurID.getNewPinMsg:pinState = " + pinState +
                    "\n\tpinMin = " + pinMin + "\n\tpinMax = " + pinMax);
        }
        if (pinData.isAlphanumeric()) {
            return MessageFormat.format(bundle.getString("SecurIDEnterNewPinChars"), pinMin, pinMax);
        } else {
            return MessageFormat.format(bundle.getString("SecurIDEnterNewPinDigits"), pinMin, pinMax);
        }
    }

    public java.security.Principal getPrincipal() {
        if (userPrincipal != null) {
            if (debug.messageEnabled()) {
                debug.message(
                    "SecurID.getPrincipal:userPrincipal not null; " +
                    "userPrincipal = " + userPrincipal);
            }
            return userPrincipal;
        } else if (userTokenId != null) {
            userPrincipal = new SecurIDPrincipal(userTokenId);
            if (debug.messageEnabled()) {
                debug.message(
                    "SecurID.getPrincipal: userPrincipal null, userTokenId = "+
                    userTokenId + ", returning userPrincipal = " +
                    userPrincipal);
            }
            return userPrincipal;
        } else {
            if (debug.messageEnabled()) {
                debug.message("getPrincipal: returning null");
            }
            return null;
        }
    }
    public void destroyModuleState() {
        userTokenId = null;
        userPrincipal = null;
    }

    public void nullifyUsedVars() {
        sharedState = null;
        options = null;
        username = null;
        wtOrgName = null;

        STR_SECURID_CONFIG_PATH = null;
    }

    public void shutdown() {
    }
}

