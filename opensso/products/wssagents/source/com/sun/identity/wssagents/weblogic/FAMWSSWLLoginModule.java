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
 * $Id: FAMWSSWLLoginModule.java,v 1.4 2008/08/19 19:15:12 veiming Exp $
 *
 */

package com.sun.identity.wssagents.weblogic;

import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.io.IOException;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.Subject;
import javax.security.auth.spi.LoginModule;
import javax.security.auth.login.LoginException;
import javax.security.auth.login.FailedLoginException;

import weblogic.security.principal.WLSGroupImpl;
import weblogic.security.principal.WLSUserImpl;


/**
 * Class FAMWSSWLLoginModule is a customized LoginModule for Weblogic application
 * server. It is used by OpenSSO webservices security plugins to set the user 
 * principals to the weblogic server.
 */
public class FAMWSSWLLoginModule implements LoginModule {
    
    /**
     * Method declaration
     *
     * @param subject
     * @param callbackHandler
     * @param sharedState
     * @param options
     *
     * @see
     */
    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map sharedState, Map options) {
        if (_logger != null) {
            _logger.log(Level.FINE, "FAMWSSWLLoginModule.initialize.");
        }
        setSubject(subject);
        setCallbackHandler(callbackHandler);
        setSharedState(sharedState);
        setOptions(options);
    }
    
    /**
     * login method
     *
     * @return true if login succeeds, or false if it fails
     *
     * @throws LoginException
     *
     * @see
     */
    public boolean login() throws LoginException {
        boolean     result = false;
        
        Callback[] callbacks = getCallbacks();
        String     userName = getUserName(callbacks);
        
        if (_logger != null) {
            _logger.log(Level.FINE, "FAMWSSWLLoginModule.login:" +
                    "  User Name from Callback " + userName);
        }
        if ((userName!= null) && (userName.length() != 0)) {
            getPrincipalsVector().add(new WLSUserImpl(userName));
            result = true;
            if (_logger != null) {
                _logger.log(Level.FINE, "FAMWSSWLLoginModule.login:" +
                            " user auth succeeded");
            }
        } else {
            if (_logger != null) {
                _logger.log(Level.FINE, "FAMWSSWLLoginModule.login:" +
                            " user auth failed");
            }
        }
        
        setIsLoggedIn(result);
        
        return result;
    }
    
    /**
     * This method should never be called.
     * Hence it always returns true
     *
     * @return
     *
     * @see
     */
    public boolean logout() {
        
        return true;
    }
    
    /**
     * Completes the login by adding the user and the user's groups
     * to the subject.
     *
     * @return A boolean indicating whether or not the commit succeeded.
     */
    public boolean commit() {
        
        if (isLoggedIn()) {
            getSubject().getPrincipals().addAll(getPrincipalsVector());
            setIsCommited(true);
            
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Aborts the login attempt.  Remove any principals we put
     * into the subject during the commit method from the subject.
     *
     * @return A boolean indicating whether or not the abort succeeded.
     */
    public boolean abort() throws LoginException {
        
        if (isCommited()) {
            getSubject().getPrincipals().removeAll(getPrincipalsVector());
            setIsCommited(false);
        }
        
        return true;
    }
    
    // private method used to handle callbacks
    
    /**
     * Method declaration
     *
     * @return
     *
     * @throws LoginException
     *
     * @see
     */
    private Callback[] getCallbacks() throws LoginException {
        if (getCallbackHandler() == null) {
            throw new LoginException("FAMWSSWLLoginModule.getCallbacks: " + 
                    "No CallbackHandler Specified");
        }
        
        Callback[] callbacks = new Callback[2];
        
        // add in the user name callback
        callbacks[0] = new NameCallback("user name: ");
        callbacks[1] = new PasswordCallback("password: ", false);
 
        try {
            getCallbackHandler().handle(callbacks);

        } catch (IOException e) {
            throw new LoginException(e.toString());
        } catch (UnsupportedCallbackException e) {
            throw new LoginException(e.toString() + " "
                    + e.getCallback().toString());
        }
        
        return callbacks;
    }
    
    
    /**
     * Get user name from callbacks
     *
     * @param callbacks
     *
     * @return the user's name
     *
     * @throws LoginException
     *
     * @see
     */
    private String getUserName(Callback[] callbacks) throws LoginException {
        String userName = ((NameCallback) callbacks[0]).getName();
        
        if (userName == null) {
            throw new LoginException(
                "FAMWSSWLLoginModule.getUserName: user name not supplied.");
        }
        
        return userName;
    }
    
    private void setSubject(Subject subject) {
        this.subject = subject;
    }
    
    private Subject getSubject() {
        return this.subject;
    }
    
    private void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }
    
    private CallbackHandler getCallbackHandler() {
        return this.callbackHandler;
    }
    
    private void setSharedState(Map sharedState) {
        this.sharedState = sharedState;
    }
    
    private Map getSharedState() {
        return this.sharedState;
    }
    
    private void setOptions(Map options) {
        this.options = options;
    }
    
    private Map getOptions() {
        return this.options;
    }
    
    private void setIsLoggedIn(boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
    }
   
    private boolean isLoggedIn() {
        return this.isLoggedIn;
    }
    
    private void setIsCommited(boolean isCommited) {
        this.isCommited = isCommited;
    }
    
    private boolean isCommited() {
        return this.isCommited;
    }
    
    private void setPrincipalsVector(Vector principalsVector) {
        this.principalsVector = principalsVector;
    }
    
    private Vector getPrincipalsVector() {
        return this.principalsVector;
    }
    
    static {
        LogManager logManager = LogManager.getLogManager();
        _logger = logManager.getLogger(
            "javax.enterprise.system.core.security");
    }
    
    private static Logger _logger = null;
        
    private Subject	    subject;
    private CallbackHandler callbackHandler;
    private Map		    sharedState;
    private Map		    options;
    
    // to keep track of a successful login
    private boolean	    isLoggedIn = false;
    
    // To keep track of a successful commit
    private boolean	    isCommited = false;
    
    // vector of principals for the subject
    private Vector	    principalsVector = new Vector();
    
}
