/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Unix.java,v 1.4 2008/12/23 21:57:26 manish_rustagi Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.unix;

import java.util.*;
import java.io.*;

import javax.security.auth.*;
import javax.security.auth.callback.*;

import com.iplanet.am.util.Misc;

import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;
import java.security.Principal;
import javax.servlet.http.HttpServletRequest;

public class Unix extends AMLoginModule {

    private static int helper_config_done = 0;
    private static java.util.Locale locale = null;
    private static String amAuthUnix = "amAuthUnix";
    private static Debug debug = null;

    private static int UNIX_HELPER_PORT;
    private static final String DEFAULT_UNIX_HELPER_PORT = "57946";
    private static final String DEFAULT_UNIX_TIMEOUT = "3";
    private static final String DEFAULT_UNIX_THREADS = "5";
    private int UNIX_CONFIG_PORT = 58946;
    private String str_UNIX_TIMEOUT;
    private String str_UNIX_THREADS;
    private String str_UNIX_HELPER_PORT;
    private String password;
    
    private Map sharedState;
    private ResourceBundle bundle = null;
    private String userTokenId;
    private Principal userPrincipal = null;
    private String user;
    private String serviceModule;
    private String clientIPAddr;
    private UnixHelper unixClient;
    private Map options = null;
    private boolean getCredentialsFromSharedState = false;
    private boolean needInit = true;

    private static String PAM_SERVICE_ATTR = 
        "iplanet-am-auth-unix-pam-service-name";
    private static String CONFIG_PORT_ATTR = 
        "iplanet-am-auth-unix-config-port";
    private static String HELPER_PORT_ATTR = 
        "iplanet-am-auth-unix-helper-port";
    private static String HELPER_TIMEOUT_ATTR = 
        "iplanet-am-auth-unix-helper-timeout";
    private static String HELPER_THREADS_ATTR = 
        "iplanet-am-auth-unix-helper-threads";
    private static String AUTH_LEVEL_ATTR = 
        "iplanet-am-auth-unix-auth-level";


    public Unix() throws AuthLoginException{
        try {
            debug = Debug.getInstance(amAuthUnix);
            debug.message("Unix constructor called");
        } catch (Exception e) {
            debug.error("this is an error ", e);
        }
    }


    public void init_helper () throws AuthLoginException {
        //  use the attribute values already read from the profile server.
        //  eventually there'll need to be something to re-config when
        //  changes are made to the auth module's attributes, but for
        //  now, go with what was originally retrieved.
        try {
            unixClient = new UnixHelper (UNIX_CONFIG_PORT, amAuthUnix);

            debug.message("Re-initializing helper.");

            int ires = unixClient.configHelper (str_UNIX_HELPER_PORT,
                str_UNIX_TIMEOUT, str_UNIX_THREADS, debug, bundle);

            unixClient.destroy(bundle);

            if (ires != 0) {
                debug.message("Unable to contact helper to re-initialize(1).");
                throw new AuthLoginException(amAuthUnix, 
                    "UnixconfigHelper", null);
            }
            Thread.sleep (1000);
        } catch (AuthLoginException lex) {
            debug.message ("Unable to contact helper to re-initialize(2).");
            throw new AuthLoginException(amAuthUnix, "UnixInitializeLex", 
                null, lex);
        } catch (Exception ex) {
            debug.message ("Unable to contact helper to re-initialize(3).");
            throw new AuthLoginException (amAuthUnix, "UnixInitializeEx", 
                null, ex);
        }
    }


    public void init(Subject subject, Map sharedState, Map options) {

        try {
            debug.message("in init ...");
            java.util.Locale locale = getLoginLocale();
            bundle = amCache.getResBundle(amAuthUnix, locale);
            if (debug.messageEnabled()) {
                debug.message("Unix resource bundle locale="+locale);
            }
            this.options = options;
            serviceModule= Misc.getMapAttr(options, PAM_SERVICE_ATTR);

            if (debug.messageEnabled()) {
                debug.message("serviceModule is : " + serviceModule);
            }
            this.sharedState = sharedState;

            String authLevel = Misc.getMapAttr(options, AUTH_LEVEL_ATTR);
            if (authLevel != null) {
                try {
                    setAuthLevel(Integer.parseInt(authLevel));
                } catch (Exception e) {
                    debug.error("Unable to set auth level " + authLevel,e);
                }
            }
        } catch (Exception e) {
            debug.error("Error....", e);
        }
    }

    public int process(Callback[] callbacks, int state) 
        throws AuthLoginException{

        if (needInit) {
            initialize_helper();
            debug.message("initialized helper");
        }
        HttpServletRequest servletRequest = getHttpServletRequest();
        if (servletRequest != null) {
            clientIPAddr = getHttpServletRequest().getRemoteAddr();
            if (debug.messageEnabled()) {
                debug.message("Unix client IPAddr = " + clientIPAddr);
            }
        }
        
        // there is only one state defined in Unix Login module
        if (state != 1) {
            debug.message("Inavlid login state");
            throw new AuthLoginException(amAuthUnix, "UnixInvalidState",
                 new Object[]{new Integer(state)});
        }
        // there are three Callbacks in this state:
        // Callback[0] is for user name,
        // Callback[1] is for user password
        if (callbacks !=null && callbacks.length == 0) {                
            user = (String) sharedState.get(getUserKey());
            password = (String) sharedState.get(getPwdKey());
            if (user == null || password == null) {
                return ISAuthConstants.LOGIN_START;
            }
            getCredentialsFromSharedState = true;
        } else {
            user = ((NameCallback)callbacks[0]).getName();
            if (debug.messageEnabled()) {
                debug.message("user is.. " + user);
            }
            if (callbacks.length > 1) {
                char[] tmpPassword = 
                    ((PasswordCallback)callbacks[1]).getPassword();
                if (tmpPassword == null) {
                    // treat a NULL password as an empty password
                    tmpPassword = new char[0];
                }
                password = new String(tmpPassword);
                ((PasswordCallback)callbacks[1]).clearPassword();
            }
        }

        // store username, password both success and failure cases
        storeUsernamePasswd(user, password);

        if (user == null || user.length() == 0) {
            debug.message("user id empty....");
            throw new AuthLoginException(amAuthUnix, "UnixUserIdNull", null); 
        }
        try {
            if (!user.equals(new String(user.getBytes("ASCII"), "ASCII"))) {
                debug.message("enter ascii for user");
                setFailureID(user);
                throw new AuthLoginException(amAuthUnix, "UnixUseridNotASCII",
                    null);
            }
        } catch (UnsupportedEncodingException ueex) {
            if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                getCredentialsFromSharedState = false;
                return ISAuthConstants.LOGIN_START;
            }
            debug.message("unsupported encodidng..");
            throw new AuthLoginException(amAuthUnix, 
                "UnixInputEncodingException", null);
        }

        // null passwd may be ok, make sure it is an empty string
        if (password == null) {
            password= "";
        } else {
            try {
               if (!password.equals(new String(password.getBytes("ASCII"), 
                    "ASCII"))) {
                    throw new AuthLoginException(amAuthUnix, 
                        "UnixPasswordNotASCII", null);
                }
            } catch (UnsupportedEncodingException ueex) {
                if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                    getCredentialsFromSharedState = false;
                    return ISAuthConstants.LOGIN_START;
                }
                setFailureID(user);
                throw new AuthLoginException(amAuthUnix, 
                    "UnixInputEncodingException", null);
            }
        }
        
        debug.message("before calling unixClient...");
        int ires = -1;
        try {
            if (debug.messageEnabled()) {
                debug.message("unixClient is... " + unixClient);
            }
            ires = unixClient.authenticate(user, password, 
                serviceModule, clientIPAddr, bundle); 
            unixClient.destroy(bundle);
        } catch (Exception e) {
            debug.error("Exception unixClient... :"+ e.getMessage());
            if (debug.messageEnabled()) {
                debug.message("Stack: ", e);
            }
        }
        if (debug.messageEnabled()) {
            debug.message("ires...... is... " + ires);
        }
        
        if (ires != 0) {
            if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                getCredentialsFromSharedState = false;
                return ISAuthConstants.LOGIN_START;
            }
            setFailureID(user);
            if (ires == -1) {
                if (debug.messageEnabled()) {
                    debug.message("Auth failed for user " + user);
                }
                throw new InvalidPasswordException(amAuthUnix, 
                    "UnixLoginFailed", new Object[]{user}, user, null);
            } else if (ires == 2) {
                if (debug.messageEnabled()) {
                    debug.message("Auth failed for user " + user + 
                         ". Password expired.");
                }
                return ISAuthConstants.LOGIN_CHALLENGE;
            }
        } else {
            userTokenId = user;
        }
        if (debug.messageEnabled()) {
            debug.message("Authentication for " + user + " succeeded!!");
        }
        // authentication successful, send -1 means done
        return ISAuthConstants.LOGIN_SUCCEED;
    }

    public java.security.Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        } else if (userTokenId != null) {
            userPrincipal = new com.sun.identity.authentication.modules.unix.UnixPrincipal(userTokenId);
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
        sharedState = null;
        bundle = null;
        str_UNIX_HELPER_PORT = null;
        str_UNIX_TIMEOUT = null;
        str_UNIX_THREADS = null;
        user = null;
        password = null;
        serviceModule = null;
        clientIPAddr = null;
        password = null;
        unixClient = null;
        options = null;
    }

    public void getDaemonParams() {
        String config_port = Misc.getMapAttr(options, CONFIG_PORT_ATTR);
        str_UNIX_HELPER_PORT = Misc.getMapAttr(options, HELPER_PORT_ATTR);
        str_UNIX_TIMEOUT = Misc.getMapAttr(options, HELPER_TIMEOUT_ATTR);
        str_UNIX_THREADS = Misc.getMapAttr(options, HELPER_THREADS_ATTR);

        //  get the helper daemon config port. use the default value if 
        //  it is not set.
        if (config_port != null) {
            try {
                UNIX_CONFIG_PORT = Integer.parseInt(config_port);
            } catch (NumberFormatException nex) {
                //ignore;
            }
        }

        if (str_UNIX_HELPER_PORT == null || 
            str_UNIX_HELPER_PORT.length() == 0) {
            str_UNIX_HELPER_PORT = DEFAULT_UNIX_HELPER_PORT;
        }
            
        try {
            UNIX_HELPER_PORT = Integer.parseInt (str_UNIX_HELPER_PORT);
        } catch (NumberFormatException nex) {
            // this should not happen, guaranteed by the input from console.
        }

        if (str_UNIX_TIMEOUT == null || str_UNIX_TIMEOUT.length() == 0) {
            str_UNIX_TIMEOUT = DEFAULT_UNIX_TIMEOUT;
        }

        if (str_UNIX_THREADS == null || str_UNIX_THREADS.length() == 0) {
            str_UNIX_THREADS = DEFAULT_UNIX_THREADS;
        }
    }


    public void initialize_helper() throws AuthLoginException{
        // initialize any configured options

        getDaemonParams();
        if (helper_config_done == 0) {
            init_helper();
            helper_config_done = 1;
            
        }

        try {
            unixClient = new UnixHelper (UNIX_HELPER_PORT, amAuthUnix);
        } catch (AuthenticationException lex) {
            //  try config'ing the helper again. might be that the daemon
            //  port has changed.
            debug.message("Unable to connect to auth port; Try init again.");
            try {
                // grab changes in parameters.
                getDaemonParams();
                init_helper();
                debug.message("Successfully re-initialized helper.");
                try {
                    unixClient = new UnixHelper (UNIX_HELPER_PORT,amAuthUnix);
                    debug.message ("Re-opened auth port tohelper(2).");
                } catch (AuthenticationException lex1) {
                    debug.error("Unable to open auth port to helper(2)", lex1);
                    throw new AuthLoginException(amAuthUnix, 
                        "UnixInitializeLex", null, lex1);
                }
            } catch (AuthLoginException lex2) {
                debug.error("Unable to contact helper to re-init", lex2);
                throw new AuthLoginException(amAuthUnix, "UnixInitLex", null);
            }
        } catch (Exception ex) {
            debug.error("Exception... ", ex);
            throw new AuthLoginException (amAuthUnix, "UnixInitEx", null);
        }
        needInit = false;
    }
}
