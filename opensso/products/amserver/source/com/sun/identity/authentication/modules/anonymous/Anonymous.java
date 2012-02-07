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
 * $Id: Anonymous.java,v 1.4 2009/06/17 21:53:19 ericow Exp $
 *
 */


package com.sun.identity.authentication.modules.anonymous;

import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.authentication.spi.PagePropertiesCallback;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;

/*
 * This module can be called in this way:
 * <protocol>://<server host>:<server port>/<server deploy uri>
 * /UI/login?org=<my org>&IDToken1=<Anonymous user name>&module=Anonymous
 *  OR  &module=Anonymous
 */
public class Anonymous extends AMLoginModule {

    private static com.sun.identity.shared.debug.Debug debug = null;
    private static final int DEFAULT_ANONYMOUS_AUTH_LEVEL = 0;
    private static final String amAuthAnonymous = "amAuthAnonymous";

    private  ResourceBundle bundle = null;
    private Map sharedState;
    private String userTokenId;
    private String defaultAnonUser;
    private Set validAnonUsernames;
    private int authLevel;
    private String errorMsg = null;
    private String usernameParam = null;
    // the authentication status
    private boolean succeeded = false;
    private boolean commitSucceeded = false;

    // username and password
    private AnonymousAuthPrincipal userPrincipal;
    private CallbackHandler callbackHandler;

    // whether to perform case sensitive authentication
    private boolean isCaseSensitive = false;
    boolean useSharedstate = false;

    public Anonymous() {
    }

    public void init(Subject subject, Map sharedState, Map options) {
        if (debug == null) {
            debug = com.sun.identity.shared.debug.Debug.getInstance(
                amAuthAnonymous);
        }
        this.sharedState = sharedState;
        java.util.Locale locale = getLoginLocale();
        bundle = amCache.getResBundle(amAuthAnonymous, locale);
        if (debug.messageEnabled()) {
            debug.message("Anonymous resbundle locale="+locale);
        }
        try {

            validAnonUsernames = (Set)options.get(
                "iplanet-am-auth-anonymous-users-list");
            if (validAnonUsernames == null) {
                debug.error("No Anonymous Service Template Created");
                errorMsg = "AnonValidateEx";
            }
            defaultAnonUser = CollectionHelper.getMapAttr(options,
                "iplanet-am-auth-anonymous-default-user-name");

            String tmp = CollectionHelper.getMapAttr(options,
                "iplanet-am-auth-anonymous-auth-level");

            if (tmp == null || tmp.length() == 0) {
                authLevel = DEFAULT_ANONYMOUS_AUTH_LEVEL;
            } else {
                try {
                    authLevel = Integer.parseInt(tmp);
                } catch (Exception e) {
                    debug.error("Invalid auth level " + tmp);
                    authLevel = DEFAULT_ANONYMOUS_AUTH_LEVEL;
                }
            }
            callbackHandler = getCallbackHandler();

            isCaseSensitive = Boolean.valueOf(CollectionHelper.getMapAttr(
                options, "iplanet-am-auth-anonymous-case-sensitive", "false")
                ).booleanValue();
            if (debug.messageEnabled()) {
                debug.message("isCaseSensitive: "+isCaseSensitive);
            }
        } catch(Exception ex) {
            if (debug.messageEnabled()) {
                debug.message("possible exception is ",ex);
            }
            debug.error(
                "Failed getting anonymous attributes for organization ");
            errorMsg = "AnonValidateEx";
        }
    }


    public int process (Callback[] callbacks, int state)
        throws AuthLoginException {
        if (errorMsg != null) {
            throw new AuthLoginException(amAuthAnonymous, errorMsg, null);
        }
        useSharedstate = isSharedStateEnabled();
        try {
            if (useSharedstate) {
                usernameParam = (String) sharedState.get(getUserKey());
                if (processAnonUser(usernameParam)) {
                    setAuthLevel(authLevel);
                    return ISAuthConstants.LOGIN_SUCCEED;
                }
            }

            if (callbacks !=null && callbacks.length > 0) {
                if (callbacks[0] instanceof NameCallback) {
                    usernameParam = ((NameCallback)callbacks[0]).getName();
                    if (debug.messageEnabled()) {
                        debug.message("Anonymous:process received NameCallback "
                                + usernameParam);
                    }
                    if (processAnonUser(usernameParam)) {
                        setAuthLevel(authLevel);
                        return ISAuthConstants.LOGIN_SUCCEED;
                    }
                }
            }

            if (validAnonUsernames !=null && !(validAnonUsernames.isEmpty())) {
                usernameParam = sendCallback();
            } else {
                usernameParam = defaultAnonUser;
            }
            storeUsernamePasswd(usernameParam, null);
            processAnonUser(usernameParam);
            setAuthLevel(authLevel);
            if (debug.messageEnabled()) {
                debug.message (
                    "Set auth level: " + authLevel +
                    "\nAnonymous userid: " + userTokenId);
            }

        } catch (Exception e) {
            debug.error("login: User not found in valid Anon List");
            setFailureID(usernameParam);
            throw new AuthLoginException(
                amAuthAnonymous, "AnonValidateEx", null);
        }
        return ISAuthConstants.LOGIN_SUCCEED;

    }

    /**
     * check if the userName is a valid anonymous user name
     * in either case sensitive or insensitive cases.
     */
    private boolean isValidAnonUserName() {
        if (isCaseSensitive) {
            return validAnonUsernames.contains(usernameParam);
        } else {
            for (Iterator it = validAnonUsernames.iterator(); it.hasNext(); ) {
                String name = (String) it.next();
                if (name != null && name.equalsIgnoreCase(usernameParam)) {
                    usernameParam = name;
                    return true;
                }
            }
        }
        return false;
     }

    public java.security.Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        } else if (userTokenId != null) {
            userPrincipal = new AnonymousAuthPrincipal(userTokenId);
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
        defaultAnonUser = null;
        validAnonUsernames = null;
        errorMsg = null;
        usernameParam = null;
        callbackHandler = null;
    }

    private String  sendCallback() throws AuthLoginException {
        if (callbackHandler == null) {
            throw new AuthLoginException(amAuthAnonymous, "NoCallbackHandler",
                null);
        }
        String username = null;
        try {
            Callback[] callbacks = new Callback[2];
            String header = bundle.getString("moduleHeader");
            PagePropertiesCallback ppc = new PagePropertiesCallback(
                null, header, null, 0, null, false, null);
            callbacks[0] = ppc;
            callbacks[1] = new NameCallback (bundle.getString("username"));
            if (debug.messageEnabled()) {
                debug.message("Callback 0 is.. :" + callbacks[0]);
                debug.message("Callback 1 is.. :" + callbacks[1]);
            }
            callbackHandler.handle(callbacks);
            username = ((NameCallback)callbacks[1]).getName();
            return username;
        } catch (IllegalArgumentException ill) {
            debug.message("message type missing");
            throw new AuthLoginException(amAuthAnonymous, "IllegalArgs", null);
        } catch (java.io.IOException ioe) {
            throw new AuthLoginException(ioe);
        } catch (UnsupportedCallbackException uce) {
            throw new AuthLoginException(amAuthAnonymous, "NoCallbackHandler",
                null);
        }
    }

    private boolean processAnonUser(String usernameParam)
            throws AuthLoginException {
        if (usernameParam == null || usernameParam.length() == 0) {
            debug.message("User Name entered is either NULL or Empty");
            if (useSharedstate) {
                return false;
            }
            throw new AuthLoginException(amAuthAnonymous,"UserError", null);
        } else if (usernameParam.equalsIgnoreCase(defaultAnonUser)) {
            debug.message("User Type: default Anon User");
            if (isCaseSensitive && !usernameParam.equals(defaultAnonUser)) {
                if (useSharedstate) {
                    return false;
                }
                setFailureID(usernameParam);
                throw new AuthLoginException(amAuthAnonymous, "AnonValidateEx",
                    null);
            }
            userTokenId = usernameParam;
        } else if (validAnonUsernames != null && isValidAnonUserName()) {
            debug.message("user is in anonymoususer's list");
            debug.message("UserType: non default Anon User");
            userTokenId = usernameParam;
        } else {
            if (useSharedstate) {
                return false;
            }
            setFailureID(usernameParam);
            throw new AuthLoginException(amAuthAnonymous, "AnonValidateEx",
                null);
        }
        return true;
    }
}
