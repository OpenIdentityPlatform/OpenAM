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
 * $Id: TokenProvider.java,v 1.3 2008/08/27 19:05:53 mrudul_uchil Exp $
 *
 */

package com.sun.identity.wss.security;

/**
 * The interface <code>TokenProvider</code> represents a Security Token
 * provider for generating the Security Tokens. 
 *
 * <p> Each token provider implementation must implement this interface
 * to generate the <code>SecurityToken</code>s that may be attached to the
 * Web services security (SOAP) headers.
 * 
 * @supported.all.api
 */
public interface TokenProvider {

    /**
     * Initializes the <code>TokenProvider</code> to generate the necessary
     * Security Token. 
     *
     * @param tokenSpec the token specification required to generate the
     *        Security Token. 
     */
    public void init(SecurityTokenSpec tokenSpec);

    /**
     * Returns the <code>SecurityToken</code> for the WS-Security.
     *
     * @return the Security Token.
     *
     * @exception SecurityException if unable to generate the Security Token.
     */
    public  SecurityToken getSecurityToken() throws SecurityException;

}
