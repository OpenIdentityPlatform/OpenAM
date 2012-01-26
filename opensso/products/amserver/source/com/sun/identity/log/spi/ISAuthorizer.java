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
 * $Id: ISAuthorizer.java,v 1.6 2008/06/25 05:43:40 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.log.spi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.shared.Constants;


/**
 * This class implements the authorization plugin interface.
 * <p>
 * When a LogRecord is passed into the logging framework it
 * has to be verified if the client has the necessary authorization
 * to perform this log operation. This class provides a model
 * implementation for the authorization plugin.
 */

public class ISAuthorizer implements IAuthorizer {
    /**
     * Returns <code>true</code> if a given log record should be published.
     *
     * @param logName Log name on which operation is to be performed.
     * @param operation The log operation to be performed.
     * @param credential The credential to be authorized.
     * @return <code>true</code> if the credential is authorized.
     */
    public boolean isAuthorized(
        String logName,
        String operation, 
        Object credential
    ) {
        SSOToken ssoToken = null;
        if (credential instanceof SSOToken) {
            ssoToken = (SSOToken)credential;
        }
        
        if (ssoToken == null) {
            Debug.error("ISAuthorizer.isAuthorized(): SSO Token is null ");
            return false;
        }
        
        try {
            String tmpID = ssoToken.getPrincipal().getName();
            if (Debug.messageEnabled()) {
                Debug.message(
                    "ISAuthorizer.isAuthorized():logName = " + logName +
                    ", op = " + operation + ", uid = " + tmpID);
            }

            String thisSubConfig = "LogWrite";
            if (operation.equalsIgnoreCase("READ")) {
                thisSubConfig = "LogRead";
            }

            SSOTokenManager ssoMgr = SSOTokenManager.getInstance();
            if (ssoMgr.isValidToken(ssoToken)) {
                Map tmap = new HashMap();
                Set actSet;
                actSet = Collections.singleton(operation);
                try {
                    String amRealm =
                        ssoToken.getProperty(Constants.ORGANIZATION);

                    DelegationPermission dp =
                        new DelegationPermission(amRealm,   // realm
                                "iPlanetAMLoggingService",  // service name
                                "1.0",                      // version
                                "application",              // config type
                                thisSubConfig,              // subConfig name
                                actSet,                     // actions
                                tmap);                      // extensions
                    DelegationEvaluator de = new DelegationEvaluator();
                    if (de.isAllowed(ssoToken, dp, null)) {
                        return true;
                    } else {
                        Debug.error(logName +
                            ":ISAuthorizer.isAuthorized():log rqt to " +
                            operation + " by " + tmpID + " denied.");
                    }
                } catch (DelegationException dex) {
                    String loggedByID = ssoToken.getPrincipal().getName();
                    Debug.error(
                        "ISAuthorizer.isAuthorized():delegation error: " + 
                        "user: " + loggedByID + ", logName = " + logName +
                        ", op = " + operation + ", msg = " +
                        dex.getMessage());
                }
            } else {
                String loggedByID = ssoToken.getPrincipal().getName();
                Debug.error("ISAuthorizer.isAuthorized(): access denied " + 
                    "for user : " + loggedByID);
            }
        } catch (SSOException ssoe) {
            Debug.error("ISAuthorizer.isAuthorized(): SSOException: ", ssoe);
        }
        return false;
    }
    
    /**
     * Returns <code>true</code> if given subject is authorized to change the
     * password.
     *
     * @param credential Credential to be checked for authorization.
     * @return <code>true</code> if given subject is authorized to change the
     *         password.
     */
    public boolean isAuthorized(Object credential) {
        return true;
    }
}
