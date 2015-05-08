/*
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
 * $Id: IdentityServicesImpl.java,v 1.20 2010/01/06 19:11:17 veiming Exp $
 *
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 */

package com.sun.identity.idsvcs.opensso;

import java.rmi.RemoteException;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idsvcs.AccessDenied;
import com.sun.identity.idsvcs.GeneralFailure;
import com.sun.identity.idsvcs.LogResponse;
import com.sun.identity.idsvcs.Token;
import com.sun.identity.idsvcs.TokenExpired;
import com.sun.identity.log.AMLogException;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.Logger;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

/**
 * Web Service to provide security based on authentication and authorization support.
 */
public class IdentityServicesImpl implements com.sun.identity.idsvcs.IdentityServicesImpl {

    private static Debug debug = Debug.getInstance("amIdentityServices");

    @Override
    public LogResponse log(Token app, Token subject, String logName, String message) throws AccessDenied, TokenExpired,
            GeneralFailure, RemoteException {
        if (app == null) {
            throw new AccessDenied("No logging application token specified");
        }

        SSOToken appToken;
        SSOToken subjectToken;
        appToken = getSSOToken(app);
        subjectToken = (subject == null) ? appToken : getSSOToken(subject);

        try {
            LogRecord logRecord = new LogRecord(java.util.logging.Level.INFO, message, subjectToken);
            // todo Support internationalization via a resource bundle
            // specification
            Logger logger = (Logger) Logger.getLogger(logName);
            logger.log(logRecord, appToken);
            logger.flush();
        } catch (AMLogException e) {
            debug.error("IdentityServicesImpl:log", e);
            throw new GeneralFailure(e.getMessage());
        }
        return new LogResponse();
    }

    @Override
    public String getCookieNameForToken() throws GeneralFailure, RemoteException {
        return SystemProperties.get(Constants.AM_COOKIE_NAME, "iPlanetDirectoryPro");
    }

    @Override
    public String[] getCookieNamesToForward() throws GeneralFailure, RemoteException {
        String[] cookies;
        String ssoTokenCookie = getCookieNameForToken();
        String lbCookieName = SystemProperties.get(Constants.AM_LB_COOKIE_NAME);
        if (lbCookieName == null) {
            cookies = new String[1];
        } else {
            cookies = new String[2];
            cookies[1] = lbCookieName;
        }
        cookies[0] = ssoTokenCookie;
        return cookies;
    }

    private SSOToken getSSOToken(Token admin) throws TokenExpired {
        SSOToken token;
        try {
            if (admin == null) {
                throw (new TokenExpired("Token is NULL"));
            }
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            token = mgr.createSSOToken(admin.getId());
        } catch (SSOException ex) {
            // throw TokenExpired exception
            throw new TokenExpired(ex.getMessage());
        }
        return token;
    }
}
