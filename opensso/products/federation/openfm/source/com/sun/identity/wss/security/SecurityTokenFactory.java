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
 * $Id: SecurityTokenFactory.java,v 1.3 2008/08/27 19:05:52 mrudul_uchil Exp $
 *
 */

package com.sun.identity.wss.security;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;

/**
 * This class <code>SecurityTokenFactory</code> represents a factory class to 
 * create WS-Security tokens.
 *
 * This class uses pluggable <code>TokenProvider</code>s to generate the
 * WS-Security tokens. If the <code>TokenProvider</code> is
 * not specified, this class will use the default implementation of the 
 * Token Provider.
 * 
 * @supported.all.api
 */
public class SecurityTokenFactory {

    private TokenProvider tokenProvider = null;

    /**
     * Protected constructor.
     */ 
    protected SecurityTokenFactory(TokenProvider provider) {
        this.tokenProvider = provider;   
    }

    /**
     * Returns the Security Token factory instance. The default 
     * <code>TokenProvider</code> is used to generate the 
     * WS-Security tokens.
     *
     * @param credential The credential of the authenticated subject.
     *
     * @exception SecurityException if unable to create the 
     *         the security token factory.
     */
    public static SecurityTokenFactory getInstance (
                      SSOToken credential)
        throws SecurityException {
        try {
            return new SecurityTokenFactory(new AMTokenProvider(credential)); 

        } catch (SSOException se) {
            WSSUtils.debug.error("SecurityTokenFactory.getInstance: " +
            "Unable to get the factory instance", se);
            throw new SecurityException(se.getMessage());
        }
    }

    /**
     * Returns the Security Token factory instance. 
     *
     * @param provider the token provider implementation.
     *
     * @exception SecurityException if unable to create the 
     *         the security token factory.
     */
    public static SecurityTokenFactory getInstance (
                    TokenProvider provider) throws SecurityException {

        if(provider == null) {
           throw new IllegalArgumentException(
                WSSUtils.bundle.getString("nullTokenProvider"));
        }
        return new SecurityTokenFactory(provider);
    }

    /**
     * Returns / Creates a security token from the security token specificaion.
     *
     * @param tokenSpec Security Token Specification that is needed to 
     *        generate the security tokens.
     *
     * @return SecurityToken the security token.
     *
     * @exception SecurityException if the security token could not be
     *            generated.
     */
    public SecurityToken getSecurityToken(SecurityTokenSpec tokenSpec)
            throws SecurityException {

        tokenProvider.init(tokenSpec);
        return tokenProvider.getSecurityToken();
    }

    /**
     * Returns the provider of this Security Token factory object.
     *
     * @return the token provider.
     */
    public TokenProvider getTokenProvider() {
         return tokenProvider;
    }

}
