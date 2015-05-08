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
 * $Id: IdentityServices.java,v 1.5 2009/12/15 00:34:57 veiming Exp $
 *
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 */

package com.sun.identity.idsvcs;

import com.iplanet.am.util.Token;

import java.util.List;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Base interface for all security providers.
 */
public interface IdentityServices extends Remote {

    /**
     * Logs a message on behalf of the authenticated app.
     *
     * @param app Token corresponding to the authenticated application.
     * @param subject Optional token identifying the subject for which the log record pertains.
     * @param logName Identifier for the log file, e.g. "MyApp.access".
     * @param message String containing the message to be logged.
     * @throws AccessDenied If app token is not specified.
     * @throws GeneralFailure On any other error.
     */
    void log(Token app, Token subject, String logName, String message) throws AccessDenied, TokenExpired,
            GeneralFailure, RemoteException;

    /**
     * Returns the cookie used by OpenAM Authentication module to store
     * the SSOToken. Can be used for Single-Sign-On by replaying this cookie
     * back to OpenAM for other operations.
     *
     * @return cookie name that contains the SSOToken
     * @throws GeneralFailure on other errors.
     */
    String getCookieNameForToken() throws GeneralFailure, RemoteException;
    
    /**
     * Returns a list of cookie names that are used by OpenAM for
     * authentication and load balancing. Replaying all these cookies
     * during the request is highly recommended.
     *
     * @return <true> if token is valid
     * @throws GeneralFailure on other errors.
     * @throws RemoteException On a remote error.
     */
    List getCookieNamesToForward() throws GeneralFailure, RemoteException;
}
