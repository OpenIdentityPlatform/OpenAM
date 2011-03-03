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
 * $Id: SafeWord.java,v 1.3 2008/10/23 22:38:46 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.safeword;

import java.io.*;
import java.util.*;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.util.Misc;
import com.sun.identity.authentication.spi.AMLoginModule;

import javax.security.auth.*;
import javax.security.auth.callback.*;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;

import securecomputing.swec.SafeWordClient;
import securecomputing.swec.AuthenState;
import securecomputing.swec.EasspMessage;
import securecomputing.swec.AuthenticatorData;
import securecomputing.swec.FixedPwdData;
import securecomputing.swec.DynamicPwdData;
import securecomputing.swec.SwecConfig;
import securecomputing.ssl.SimpleSSLClient;


public class SafeWord extends AMLoginModule {
    
    private  ResourceBundle bundle = null;
    private static Debug debug = null;
    private Map sharedState;
    
    private static final String ATTRIBUTE_SERVER_SPECIFICATION =
    "iplanet-am-auth-safeword-server-specification";
    private static final String ATTRIBUTE_SYSTEM_NAME =
    "iplanet-am-auth-safeword-system-name";
    private static final String ATTRIBUTE_SRVR_VERIF_PATH =
    "iplanet-am-auth-safeword-srvr-verif-path";
    private static final String ATTRIBUTE_LOG_ENABLE =
    "iplanet-am-auth-safeword-log-enable";
    private static final String ATTRIBUTE_LOG_LEVEL =
    "iplanet-am-auth-safeword-log-level";
    private static final String ATTRIBUTE_LOG_PATH =
    "iplanet-am-auth-safeword-log-path";
    private static final String ATTRIBUTE_AUTH_LEVEL =
    "iplanet-am-auth-safeword-auth-level";
    private static final String ATTRIBUTE_CLIENT_TYPE =
    "iplanet-am-auth-safeword-client-type";
    private static final String ATTRIBUTE_MINIMUM_STRENGTH =
    "iplanet-am-auth-safeword-minimum-strength";
    private static final String ATTRIBUTE_EASSP_VERSION =
    "iplanet-am-auth-safeword-eassp-version";
    private static final String ATTRIBUTE_TIMEOUT =
    "iplanet-am-auth-safeword-timeout";
    
    // Default values
    private static final String DEFAULT_EASSP_VERSION = "101";
    private static final String DEFAULT_SERVER_SPECIFICATION =
    "localhost 7482";
    private static final String DEFAULT_TIMEOUT = "120";
    private static final String DEFAULT_MINIMUM_STRENGTH = "5";
    private static final String DEFAULT_LOG_LEVEL = "DEBUG";
    // 
    private static final String DEFAULT_VAR_DIR =
       "com.iplanet.am.install.vardir";
    //  SystemProperties.get(Constants.AM_INSTALL_VARDIR);
    
    // auth config parameters
    private String serverSpec;
    private String serverVerifFilesPath = null;
    private String statusLogLevel;
    private String logEnabled = "ON";
    private String statusLogFilePath = null;
    private String authLevel;
    private String clientType;
    private String minimumStrength;
    private String version;
    
    // SafeWord's variables
    private SafeWordClient swClient = null;
    private AuthenState aState = null;
    private String challengeID;
    private String timeOut;
    private boolean flag = false;
    
    // user name and principal
    private String userTokenId;
    private SafeWordPrincipal userPrincipal;
    
    // configurations
    private Map options;
    
    //page states
    private static final int PAGE_USERNAME = 1;
    private static final int PAGE_PASSWORD = 2;
    private static final String amAuthSafeWord = "amAuthSafeWord";
    private boolean getCredentialsFromSharedState;
    
    static {
        if (debug == null) {
            debug = Debug.getInstance(amAuthSafeWord);
        }
    }
    
    /**
     * Initialize this <code>LoginModule</code>.
     *
     * @param subject the <code>Subject</code> to be authenticated
     * @param sharedState shared <code>LoginModule</code> state
     * @param options options specified in the login
     *                <code>Configuration</code> for this particular
     *                <code>LoginModule</code>
     */
    public void init(Subject subject, Map sharedState, Map options) {
        java.util.Locale locale = getLoginLocale();
        bundle = amCache.getResBundle(amAuthSafeWord, locale);
        if (debug.messageEnabled()) {
            debug.message("SafeWord resource bundle locale = " + locale);
        }
        this.options = options;
        this.sharedState = sharedState;
    }
    
    /**
     * This method takes an array of submitted <code>Callback</code>,
     * process them and decide the order of next state to go.
     * Return -1 if the login is successful, return 0 if the
     * LoginModule should be ignored.
     *
     * @param callbacks an array of <code>Callback</cdoe> for this Login state
     * @param state order of state. State order starts with 1
     * @return int order of next state. Return -1 if authentication
     *         is successful, return 0 if the LoginModule should be ignored
     */
    public int process(Callback[] callbacks, int state) throws AuthLoginException {
        try {
            if (state == PAGE_USERNAME) {
                // gets initial parameters
                initAuthConfig();
                if (callbacks !=null && callbacks.length == 0) {
                    userTokenId  = (String) sharedState.get(getUserKey());
                    
                    if (userTokenId  == null ) {
                        return PAGE_USERNAME;
                    }
                    getCredentialsFromSharedState = true;
                } else {
                    // gets the user login name
                    userTokenId = getUserName(callbacks);
                }
                
                // instantiates SafeWord Client
                initSafeWordClient();
                
                // if true sends request and gets the challenge ID from the
                // SafeWord Server
                if (sendRequestForChallengeID()) {
                    
                    // set to display challenge as dynamic text in state 2
                    setDynamicText(PAGE_PASSWORD);
                }
                if (version != null && version.equals("101")) {
                    
                    long time = System.currentTimeMillis();
                    new TimeoutThread(time).start();
                }
                // goto state 2
                return PAGE_PASSWORD;
            } else if (state == PAGE_PASSWORD) {
                // get the user password(challenge response)
                String password = getPassword(callbacks);
                storeUsernamePasswd(userTokenId, password);
                flag = true;
                
                // authenticate to the SafeWord Server
                authenticate(password);
                
                // succeeded
                return ISAuthConstants.LOGIN_SUCCEED;
                
            } else {
                if (debug.messageEnabled()) {
                    debug.message("Invalid login state: " + state);
                }
                setFailureID(userTokenId);
                throw new AuthLoginException(amAuthSafeWord,
                "SafeWordInvalidState", new Object[]{ new Integer(state)});
            }
        } catch (AuthLoginException e) {
            if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                getCredentialsFromSharedState = false;
                return PAGE_USERNAME;
            }
            setFailureID(userTokenId);
            throw e;
        }
    }
    
    /**
     * Returns <code>java.security.Principal</code>.
     *
     * @return <code>java.security.Principal</code>
     */
    public java.security.Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        } else if (userTokenId != null) {
            userPrincipal = new SafeWordPrincipal(userTokenId);
            return userPrincipal;
        } else {
            return null;
        }
    }
    
    public void destroyModuleState() {
        userTokenId = null;
        userPrincipal = null;
    }

    public void nullifyUsedVars() {
        bundle = null;
        sharedState = null;
        serverSpec = null;
        serverVerifFilesPath = null;
        statusLogLevel = null;
        logEnabled = null;
        statusLogFilePath = null;
        authLevel = null;
        clientType = null;
        minimumStrength = null;
        version = null;
        aState = null;
        challengeID = null;
        options = null;
    }
    
    /**
     * Gets SafeWord auth config parameters.
     */
    private void initAuthConfig() throws AuthLoginException {
        if(options != null) {
            serverSpec = Misc.getMapAttr(options,
            ATTRIBUTE_SERVER_SPECIFICATION, DEFAULT_SERVER_SPECIFICATION);
            
            version = Misc.getMapAttr(options, ATTRIBUTE_EASSP_VERSION,
            DEFAULT_EASSP_VERSION );
            
            serverVerifFilesPath = Misc.getMapAttr(options,
            ATTRIBUTE_SRVR_VERIF_PATH);
            if (serverVerifFilesPath == null) {
                serverVerifFilesPath = getServerConfigPath();
            }
            
            String strlogEnabled = Misc.getMapAttr(options,
            ATTRIBUTE_LOG_ENABLE, "true");
            if (strlogEnabled.equals("false")) {
                logEnabled = "OFF";
            }
            
            statusLogLevel = Misc.getMapAttr(options,
            ATTRIBUTE_LOG_LEVEL, DEFAULT_LOG_LEVEL);
            
            statusLogFilePath = Misc.getMapAttr(options,
            ATTRIBUTE_LOG_PATH);
            if (statusLogFilePath == null) {
                statusLogFilePath = getServerLogPath();
            }
            
            authLevel = Misc.getMapAttr(options,
            ATTRIBUTE_AUTH_LEVEL);
            clientType = Misc.getMapAttr(options, ATTRIBUTE_CLIENT_TYPE);
            minimumStrength = Misc.getMapAttr(options,
            ATTRIBUTE_MINIMUM_STRENGTH, DEFAULT_MINIMUM_STRENGTH);
            timeOut = Misc.getMapAttr(options,ATTRIBUTE_TIMEOUT,
            DEFAULT_TIMEOUT);
            
            if (debug.messageEnabled()) {
                debug.message(
                "SafeWord Auth config parameters:\n" +
                ATTRIBUTE_SERVER_SPECIFICATION + ": " + serverSpec + "\n" +
                ATTRIBUTE_SRVR_VERIF_PATH + ": " +
                serverVerifFilesPath + "\n" +
                ATTRIBUTE_TIMEOUT + ": " +
                " timeOut: "+ timeOut+"\n"+
                ATTRIBUTE_LOG_ENABLE + ": " + logEnabled + "\n" +
                ATTRIBUTE_LOG_LEVEL + ": " + statusLogLevel + "\n" +
                ATTRIBUTE_LOG_PATH + ": " + statusLogFilePath + "\n" +
                ATTRIBUTE_EASSP_VERSION + ": " + version + "\n" +
                ATTRIBUTE_CLIENT_TYPE + ": " + clientType + "\n" +
                ATTRIBUTE_AUTH_LEVEL + ": " + authLevel + "\n");
            }
            
        } else {
            debug.error("options is null");
            throw new AuthLoginException(amAuthSafeWord, "SafeWordOptInit", null);
        }
    }
    
    /**
     * Gets the user login name.
     */
    private String getUserName(Callback[] callbacks) throws AuthLoginException {
        
        // there are 1 Callback in this array of callbacks:
        // callback[0] is for user name
        return ((NameCallback)callbacks[0]).getName();
    }
    
    /**
     * Instantiates SafeWordClient object.
     */
    private void initSafeWordClient() throws AuthLoginException {
        SwecConfig config = new SwecConfig();
        config.setDefaults();
        config.setProperty(SwecConfig.EASSP_VERSION, version);
        config.setProperty(SwecConfig.SERVER_SPEC, serverSpec);
        config.setProperty(SwecConfig.SERVER_VERIFICATION_FILES_PATH,
        serverVerifFilesPath);
        config.setProperty(SwecConfig.STATUS_LOG_FILE_PATH, statusLogFilePath);
        config.setProperty(SwecConfig.SOCKET_TIMEOUT, timeOut);
        config.setProperty(SwecConfig.FILE_STATUS_LOG_ENABLE, logEnabled);
        config.setProperty(SwecConfig.GLOBAL_MESSAGE_LEVEL, statusLogLevel);
        config.setProperty(SwecConfig.SOCKET_TIMEOUT, timeOut);
        
        if ((version != null) && (version.equals("201") || version.equals("200"))) {
            if (debug.messageEnabled()) {
                debug.message("Set 20x specific configuration - EASSP Ver: "+
                version);
            }
            config.setProperty(SwecConfig.SSL_ENABLE,"ON");
            SimpleSSLClient.seedRandomGenerator();
        }
        
        debug.message("About to get new SafeWordClient");
        try {
            swClient = new SafeWordClient(config);
            if (debug.messageEnabled()) {
                debug.message("New SafeWordClient: " +
                swClient.getResultText());
            }
        } catch (Exception ex) {
            debug.error("Failed to create new SafeWordClient.", ex);
            throw new AuthLoginException(amAuthSafeWord,
            "SafeWordNewSWClient", null, ex);
        }
        debug.message("Done init new SafeWordClient");
    }
    
    /**
     * Sends request message to SafeWord server and gets the
     * challenge ID from SafeWord server.
     */
    private boolean sendRequestForChallengeID() throws AuthLoginException {
        if (userTokenId == null || userTokenId.length() == 0) {
            closeClient();
            throw new AuthLoginException(amAuthSafeWord,
            "SafeWordUserIdNull", null);
        }
        try {
            if (!userTokenId.equals(
            new String(userTokenId.getBytes("ASCII"), "ASCII"))) {
                closeClient();
                throw new AuthLoginException(amAuthSafeWord,
                "SafeWordUseridNotASCII", null);
            }
        } catch (UnsupportedEncodingException ueex) {
            closeClient();
            throw new AuthLoginException(amAuthSafeWord,
            "SafeWordInputEncodingException", null);
        }
        
        EasspMessage returnMsg;
        try {
            //  submit user ID to SafeWord server
            EasspMessage requestMsg = swClient.createRequestMsg(userTokenId,
            "name");
            
            if (debug.messageEnabled()) {
                debug.message("Submitting requestMsg for userID: " +
                userTokenId);
            }
            requestMsg.setAgentName(clientType);
            requestMsg.setClientType(clientType);
            boolean requireSession = true;
            String authenticationService = null;
            requestMsg.setAuthenticationRequirements(requireSession,
            minimumStrength,
            authenticationService);
            returnMsg = swClient.sendMessage(requestMsg);
        } catch (Exception e) {
            debug.error("Failed to send/receive eassp message :" + e.getMessage());
            closeClient();
            throw new AuthLoginException(amAuthSafeWord,
            "SafeWordEasspError", null);
        }
        
        //  see what message type returned from SafeWord server
        String id = returnMsg.getIdData();
        String statusMsg = returnMsg.getStatusText();
        switch (returnMsg.getMessageType()) {
            case EasspMessage.AUTHENTICATION_CHALLENGE:
                //
                // this is the expected return type
                //
                if (debug.messageEnabled()) {
                    debug.message("Received challenge to auth request by " +
                    id);
                }
                
                EasspMessage challengeMsg = returnMsg;
                aState = new AuthenState(challengeMsg);
                
                // assume one authenticator
                AuthenticatorData data = aState.getCurrentAuthenticator();
                try {
                    if (data instanceof FixedPwdData) {
                        debug.message("Current Authenticator Fixed Password");
                        return false;
                    }
                    else if (data instanceof DynamicPwdData) {
                        debug.message("Current Authenticator Dynamic Password");
                        DynamicPwdData ddata = (DynamicPwdData) data;
                        
                        // assign the dynamic challenge ID
                        challengeID = ddata.getChallenge();
                        return true;
                    }
                }
                catch (Exception e) {
                    debug.error("Received Non-Dynamic Authenticator");
                    setFailureID(userTokenId);
                    closeClient();
                    throw new AuthLoginException(amAuthSafeWord,
                    "SafeWordUnsupportedAuthenticator", null, e);
                }
                
            case EasspMessage.AUTHENTICATION_RESULT:
                //
                //  unexpected return value
                //
                closeClient();
                
                if (returnMsg.passedCheck()) {
                    debug.error("Successful Authentication, but only id " +
                    "sent. Msg: " + statusMsg);
                    setFailureID(userTokenId);
                    throw new AuthLoginException(amAuthSafeWord,
                    "SafeWordSuccessOnlyUserID", new Object[]{statusMsg});
                } else {
                    //  must have failed.  many times means that user has been
                    //  locked out at the SafeWord server
                    debug.error("Authentication Failed, only id sent. Check " +
                    "for lockout on server. Msg: " + statusMsg);
                    setFailureID(userTokenId);
                    throw new AuthLoginException(amAuthSafeWord,
                    "SafeWordLoginFailed", new Object[]{statusMsg});
                }
                
            default:
                //
                //  unknown return value
                //
                closeClient();
                setFailureID(userTokenId);
                debug.error("Authentication Failed, unknown return value: " +
                returnMsg.getMessageType());
                throw new AuthLoginException(amAuthSafeWord,
                "SafeWordLoginFailedUnknown", new Object[]{statusMsg});
        }
    }
    
    /**
     * Sets the text(challenge) in the callbacks belong to this state.
     */
    private void setDynamicText(int state) throws AuthLoginException {
        Callback[] callbacks = getCallback(state);
        String prompt = ((PasswordCallback)callbacks[0]).getPrompt();
        boolean echo = ((PasswordCallback)callbacks[0]).isEchoOn();
        if (debug.messageEnabled()) {
            debug.message("Set dynamic text: challengeID: " + challengeID);
        }
        if (challengeID != null) {
            prompt += "[" + challengeID + "]: ";
        }
        callbacks[0] = new PasswordCallback(prompt, echo);
        replaceCallback(state, 0, callbacks[0]);
    }
    
    /**
     * Gets the password after prompting the challenge ID(if there is one).
     */
    private String getPassword(Callback[] callbacks) throws AuthLoginException {
        // there are 1 Callback in this array of callbacks:
        // callback[0] is for password(also display challenge text)
        char[] tmpPassword =
        ((PasswordCallback)callbacks[0]).getPassword();
        if (tmpPassword == null) {
            // treat a NULL password as an empty password
            tmpPassword = new char[0];
        }
        char[] pwd = new char[tmpPassword.length];
        System.arraycopy(tmpPassword, 0, pwd, 0, tmpPassword.length);
        ((PasswordCallback)callbacks[0]).clearPassword();
        
        return (new String(pwd));
    }
    
    /**
     * Authenticates to SafeWord server.
     */
    private void authenticate(String challengeResponse) throws AuthLoginException {
        if (challengeResponse == null || challengeResponse.length() == 0) {
            if (debug.messageEnabled()) {
                debug.message(userTokenId + " supplied no challenge response");
            }
            closeClient();
            throw new AuthLoginException(amAuthSafeWord, "SafeWordNoChallRsp",
            null);
        }
        try {
            if (!challengeResponse.equals(
            new String(challengeResponse.getBytes("ASCII"), "ASCII"))) {
                closeClient();
                throw new AuthLoginException(amAuthSafeWord,
                "SafeWordChalRspNotASCII", null);
            }
        } catch (UnsupportedEncodingException ueex) {
            closeClient();
            throw new AuthLoginException(amAuthSafeWord,
            "SafeWordInputEncodingException", null);
        }
        
        // submit challenge response to SafeWord server
        int ccount = aState.getAuthenComboCount();
        if (ccount > 1) {
            if (debug.messageEnabled()) {
                debug.message("Authenticator Combo (" + ccount +
                " authenticators) not supported");
            }
        }
        
        if (debug.messageEnabled()) {
            debug.message("Anthenticator Combo count = " + ccount);
        }
        
        // assume just one authenticator
        AuthenticatorData data = aState.getCurrentAuthenticator();
        debug.message("Checking challenge response return message type");
        try {
            if (data instanceof FixedPwdData) {
                // get a fixed password
                FixedPwdData fdata;
                fdata = (FixedPwdData)data;
                fdata.setPwd(challengeResponse);
                
            } else if (data instanceof DynamicPwdData) {
                // get a dynamic password
                DynamicPwdData ddata;
                ddata = (DynamicPwdData)data;
                ddata.setPwd(challengeResponse);
            }
        } catch (Exception e) {
            closeClient();
            debug.error("Received unknown Authenticator");
            throw new AuthLoginException(amAuthSafeWord,
            "SafeWordUnsupportedAuthenticator", null, e);
        }
        
        
        //  create an Authentication Response message
        debug.message("Challenge response return message type Dynamic");
        
        EasspMessage responseMsg = swClient.createResponseMsg(aState);
        
        debug.message("After creating new responseMsg");
        responseMsg.setAgentName(clientType);
        responseMsg.setClientType(clientType);
        boolean requireSession = true;
        String authenticationService = null;
        responseMsg.setAuthenticationRequirements(requireSession,
        minimumStrength,
        authenticationService);
        EasspMessage returnMsg = swClient.sendMessage(responseMsg);
        
        debug.message("After creating new returnMsg");
        String id = returnMsg.getIdData();
        String statusMsg = returnMsg.getStatusText();
        
        if (debug.messageEnabled()) {
            debug.message("Challenge response returns '" + statusMsg +
            "' for userid " + id);
        }
        
        closeClient();
        
        // check if authentication succeeded
        if (returnMsg.passedCheck()) {
            if (debug.messageEnabled()) {
                debug.message("Authentication successful for userid = " +
                userTokenId + ", id = " + id);
            }
            this.setAuthLevel(Integer.parseInt(authLevel));
        } else {
            if (debug.messageEnabled()) {
                debug.message("SafeWord authentication failed for userid = " +
                    userTokenId + ", id = " + id);
            }
            throw new InvalidPasswordException(amAuthSafeWord,
            "SafeWordChallFailed", null, userTokenId, null);
        }
    }
    
    private String getServerConfigPath() {
        if (serverVerifFilesPath == null) {
            StringBuilder buf = new StringBuilder();
            buf.append(DEFAULT_VAR_DIR)
            .append("/auth/safeword/serverVerification");
            serverVerifFilesPath=buf.toString();
        }
        return serverVerifFilesPath;
    }
    
    private String getServerLogPath() {
        if (statusLogFilePath == null) {
            StringBuilder buf = new StringBuilder();
            buf.append(DEFAULT_VAR_DIR)
            .append("/auth/safeword/safe.log");
            statusLogFilePath=buf.toString();
        }
        return statusLogFilePath;
    }
    
    private void closeClient() {
        swClient.close();
        swClient = null;
    }
    
    class TimeoutThread extends Thread {
        long start;
        int iTimeOut = Integer.parseInt(timeOut);
        public TimeoutThread(long time) {
            start = time;
        }
        public void run() {
            while(true) {
                long time = System.currentTimeMillis();
                try {
                    if (time-start >= (iTimeOut*1000) && !flag) {
                        closeClient();
                        break;
                    } else if (flag) {
                        break;
                    }
                    sleep(5000);
                } catch (InterruptedException e) {
                    if (debug.messageEnabled()) {
                        debug.message("Error in timeout thread run : " + e);
                    }
                }
            }
        }
    }
    
}

