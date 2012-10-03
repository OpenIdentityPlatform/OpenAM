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
 * $Id: Token.java,v 1.4 2008/06/25 05:43:40 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.log.spi;

import com.sun.identity.log.LogManager;
import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManagerUtil;

/** Class containing static methods that first instantiate a class
 *  for token generation based on configuration and using it for
 *  generating internal tokens.
 */
public class Token {
    
    private static ITokenProvider tokenProvider;
    private static LogManager lmanager = 
        (LogManager)LogManagerUtil.getLogManager();
    
    
    static {
        String tokenClass = lmanager.getProperty(LogConstants.TOKEN_PROVIDER);
        try {
            Class c = Class.forName(tokenClass);
            tokenProvider = (ITokenProvider)c.newInstance();
            if (Debug.messageEnabled()) {
                Debug.message("Succeeded in instantiating TokenProvider");
            }
        } catch(Exception e){
            Debug.error("Could not instantiate class", e);
        }
    }
    /**
     * Generates internal token given a name.
     * @param name a String representing the name to be used for the token.
     * @param password password for user
     * @return <code>SSOToken</code> that is casted to <code>Object</code>
     */
    public static Object createToken(String name, String password) {
        return tokenProvider.createToken(name, password);
    }
}
