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
 * $Id: Federation.java,v 1.3 2009/01/28 05:35:10 ww203982 Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.authentication.modules.federation;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.datastruct.CollectionHelper;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.plugin.session.impl.FMSessionProvider;

import java.io.IOException;
import java.util.Map;
import java.util.ResourceBundle;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.servlet.http.HttpServletRequest;

// import com.sun.identity.shared.ldap.util.DN;

public class Federation extends AMLoginModule {
    
    private String userName = null;
    private Principal userPrincipal = null;
    private static final String fmAuthFederation = "fmAuthFederation";
    private static Debug debug = Debug.getInstance(fmAuthFederation);
    private ResourceBundle bundle = null;
    
    static private String AUTH_LEVEL = ISAuthConstants.AUTH_ATTR_PREFIX_NEW +
    "FederationAuthLevel";
    
    private CallbackHandler callbackHandler;
    
    /**
     * Constructor
     */
    public Federation() {
        debug.message("Federation()");
    }
    
    /**
     * Initialize parameters.
     */
    public void init(Subject subject, Map sharedState, Map options) {
        debug.message("in initialize...");
        java.util.Locale locale  = getLoginLocale();
        bundle = amCache.getResBundle(fmAuthFederation, locale);
        
        if (debug.messageEnabled()) {
            debug.message(
                "fmAuthFederation Authentication resource bundle locale="+
                locale);
        }
        this.callbackHandler = getCallbackHandler();
        
        if (options != null) {
            String authLevelStr = CollectionHelper.getMapAttr(
                options, AUTH_LEVEL);
            if (authLevelStr != null) {
                try {
                    setAuthLevel(Integer.parseInt(authLevelStr));
                } catch (Exception e) {
                    debug.error("Unable to set auth level " +
                                authLevelStr,e);
                }
            }
        }
    }
    
    /**
     * Process the authentication request.
     * @return ISAuthConstants.LOGIN_SUCCEED as succeeded;
     *         ISAuthConstants.LOGIN_IGNORE as failed.
     * @exception AuthLoginException upon any failure. login state should be
     * kept on exceptions for status check in auth chaining.
     */
    public int process(Callback[] callbacks, int state)
    throws AuthLoginException {

        String randomSecret = null;
        String principalName = null;
        String authLevel = null;        
        try {
            Callback[] cbs = new Callback[3];
            cbs[0] = new NameCallback(FMSessionProvider.RANDOM_SECRET);
            cbs[1] = new NameCallback(SessionProvider.PRINCIPAL_NAME);
            cbs[2] = new NameCallback(SessionProvider.AUTH_LEVEL);
            callbackHandler.handle(cbs);
            randomSecret = ((NameCallback)cbs[0]).getName();
            principalName = ((NameCallback)cbs[1]).getName();
            authLevel = ((NameCallback)cbs[2]).getName();
        } catch (IllegalArgumentException ill) {
            throw new AuthLoginException(fmAuthFederation, "IllegalArgs", null);
        } catch (IOException ioe) {
            throw new AuthLoginException(ioe);
        } catch (UnsupportedCallbackException uce) {
            throw new AuthLoginException(fmAuthFederation, "UnsupportedCallback",
                null);
        }

        if (!FMSessionProvider.matchSecret(randomSecret)) {

            throw new AuthLoginException(fmAuthFederation, "NoMatchingSecret",
                                         null);
        }

        HttpServletRequest request = getHttpServletRequest();
        if (request != null) {
            Map<String, Set<String>> attrs = (Map<String, Set<String>>) request.getAttribute(SessionProvider.ATTR_MAP);
            if (attrs != null) {
                setUserAttributes(attrs);
                request.removeAttribute(SessionProvider.ATTR_MAP);
            }
        }

        // TBD: This piece may or may not be needed
        /*
            DN dnObject = new DN(userName);
            String [] array = dnObject.explodeDN(true);
            userName = array[0];
        */
        debug.message("Module is successful");
        storeUsernamePasswd(principalName, null);
        userName = principalName;
        if (authLevel != null && authLevel.length() != 0) {
            try {
                int authLevelInt = Integer.parseInt(authLevel);
                setAuthLevel(authLevelInt);
            } catch (Exception e) {
                debug.error("Unable to set auth level " +
                            authLevel,e);
            }
        }
        return ISAuthConstants.LOGIN_SUCCEED;
        
    }
    
    /**
     * Returns principal of the authenticated user.
     * @return Principal of the authenticated user.
     */
    public Principal getPrincipal() {

        if (userPrincipal == null && userName != null) {
            userPrincipal = new FederationPrincipal(userName);
        }
        return userPrincipal;
    }
    
    /**
     * Clean up the login state.
     */
    public void destroyModuleState() {
        debug.message("clean up module state");
        userName = null;
        userPrincipal = null;
    }
}
