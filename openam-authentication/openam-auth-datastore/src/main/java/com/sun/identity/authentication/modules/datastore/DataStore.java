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
 * $Id: DataStore.java,v 1.4 2008/06/25 05:41:56 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.datastore;

import com.sun.identity.authentication.spi.UserNamePasswordValidationException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.sm.ServiceConfig;
import java.util.Map;
import java.util.ResourceBundle;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import java.security.Principal;

public class DataStore extends AMLoginModule {
    // local variables
    ResourceBundle bundle = null;
    protected String validatedUserID;
    private String userName;
    private String userPassword;
    private ServiceConfig sc;
    private int currentState;
    private String currentConfigName;

    private static String AUTHLEVEL = "sunAMAuthDataStoreAuthLevel";
    private static final String INVALID_CHARS =
            "iplanet-am-auth-ldap-invalid-chars";

    private Map sharedState;
    public Map currentConfig;
    
    protected Debug debug = null;
    protected String amAuthDataStore;
    protected Principal userPrincipal;
    
    public DataStore() {
        amAuthDataStore = "amAuthDataStore";
        debug = Debug.getInstance(amAuthDataStore);
    }
    
    public void init(Subject subject, Map sharedState, Map options) {
        sc = (ServiceConfig) options.get("ServiceConfig");
        currentConfig = options;
        currentConfigName = 
            (String)options.get(ISAuthConstants.MODULE_INSTANCE_NAME);
        String authLevel = CollectionHelper.getMapAttr(options, AUTHLEVEL);
        if (authLevel != null) {
            try {
                setAuthLevel(Integer.parseInt(authLevel));
            } catch (Exception e) {
                debug.error("Unable to set auth level " + authLevel,e);
            }
        }
        java.util.Locale locale = getLoginLocale();
        bundle = amCache.getResBundle(amAuthDataStore, locale);
        if (debug.messageEnabled()) {
            debug.message("DataStore resbundle locale=" + locale);
        }
        this.sharedState = sharedState;
    }
 
    public int process(Callback[] callbacks, int state)
            throws AuthLoginException {
        currentState = state;
        int retVal = 0;
        Callback[] idCallbacks = new Callback[2];
        try {
            
            if (currentState == ISAuthConstants.LOGIN_START) {
                if (callbacks !=null && callbacks.length == 0) {
                    userName = (String) sharedState.get(getUserKey());
                    userPassword = (String) sharedState.get(getPwdKey());
                    if (userName == null || userPassword == null) {
                        return ISAuthConstants.LOGIN_START;
                    }
                    NameCallback nameCallback = new NameCallback("dummy");
                    nameCallback.setName(userName);
                    idCallbacks[0] = nameCallback;
                    PasswordCallback passwordCallback = new PasswordCallback
                        ("dummy",false);
                    passwordCallback.setPassword(userPassword.toCharArray());
                    idCallbacks[1] = passwordCallback;
                } else {
                    idCallbacks = callbacks;
                    //callbacks is not null
                    userName = ( (NameCallback) callbacks[0]).getName();
                    char[] password = ((PasswordCallback) callbacks[1]).getPassword();
                    userPassword = password == null ? null : String.valueOf(password);
                }
                if (userName == null) {
                    debug.message("DataStore.process: Username is null/empty");
                    throw new UserNamePasswordValidationException("amAuth", "InvalidUP", null);
                }
                if (userPassword == null || userPassword.length() == 0) {
                    debug.message("DataStore.process: Password is null/empty");
                    throw new InvalidPasswordException("amAuth", "invalidPasswd", null);
                }
                //store username password both in success and failure case
                storeUsernamePasswd(userName, userPassword);
                /*
                Fix for OPENAM-1872. Reject usernames with illegal characters (e.g. * or ! or ) or ( or & ), just
                like the LDAP LoginModule does. List of invalid characters comes from a new configuration entry (though
                the list of illegal characters does not seem to be processed in validateUserName). I want the invocation
                to be just like the LDAP LoginModule, and to handle the case in which the username format validator
                cannot be successfully loaded in validateUserName.
                 */
                validateUserName(userName, CollectionHelper.getMapAttr(
                        currentConfig, INVALID_CHARS));

                AMIdentityRepository idrepo = getAMIdentityRepository(
                    getRequestOrg());
                boolean success = idrepo.authenticate(idCallbacks);
                if (success) {
                    retVal=ISAuthConstants.LOGIN_SUCCEED;
                    validatedUserID = userName;
                } else {
                    throw new AuthLoginException(amAuthDataStore, "authFailed",
                        null);
                }
            }  else {
                setFailureID(userName);
                throw new AuthLoginException(amAuthDataStore, "authFailed",
                    null);
            }
        } catch (IdRepoException ex) {
            debug.message("idRepo Exception");
            setFailureID(userName);
            throw new AuthLoginException(amAuthDataStore, "authFailed",
                null, ex);
        }
        return retVal;
        
    }
    
    public java.security.Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        }
        else if (validatedUserID != null) {
            userPrincipal = new DataStorePrincipal(validatedUserID);
            return userPrincipal;
        } else {
            return null;
        }
    }
    
    // cleanup state fields
    public void destroyModuleState() {
        validatedUserID = null;
        userPrincipal = null;
    }
    
    public void nullifyUsedVars() {
        bundle = null;
        userName = null ;
        userPassword = null;
        sc = null;
        
        sharedState = null;
        currentConfig = null;
        
        amAuthDataStore = null;
    }
    
}
