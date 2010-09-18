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
 * $Id: Authorizer.java,v 1.3 2008/06/25 05:43:39 qcheng Exp $
 *
 */

package com.sun.identity.log.spi;

import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManager;
import com.sun.identity.log.LogManagerUtil;

/**
 * This class is used to verify the authorization of the
 * of the person who is trying to perform a log operation.
 * @supported.all.api
 */

public class Authorizer {
    private static IAuthorizer authorizer;
    private static LogManager lmanager =
        (LogManager)LogManagerUtil.getLogManager();
    
    static {
        String authzClass = lmanager.getProperty(LogConstants.AUTHZ);
        try {
            Class c = Class.forName(authzClass);
            authorizer = (IAuthorizer)c.newInstance();
        } catch(Exception e) {
            Debug.error("Authorizer ", e);
        }
    }
    
    /**
     * Returns true if a given log record should be published.
     *
     * @param logName Log name on which operation is to be performed.
     * @param operation Log operation to be performed.
     * @param credential Credential to be authorized.
     * @return true if the credential is authorized.
     */
    public static boolean isAuthorized(
        String logName,
        String operation,
        Object credential)
    {
        return authorizer.isAuthorized (logName, operation, credential);
    }

    /**
     * Returns true if given subject is authorized to change the password.
     *
     * @param credential Credential to be checked for authorization.
     * @return true if given subject is authorized to change the password.
     */
    public static boolean isAuthorized (Object credential) {
        return authorizer.isAuthorized (credential);
    }
}
