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
 * $Id: SSOProvider.java,v 1.2 2008/06/25 05:41:42 qcheng Exp $
 *
 */

package com.iplanet.sso;

import java.security.Principal;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

/**
 * <code>SSOProvider</code> is the interface that the SSO providers or the
 * plug-ins need to implement. The implementation class that implements this
 * interface MUST have the public default no-arg constructor because
 * SSOTokenManger relies on that to dynamically instantiate an object of such
 * class using Java Reflection.
 */
public interface SSOProvider {
    /**
     * Creates an SSOToken.
     * 
     * @param request HttpServletRequest
     * @return SSOToken
     * @exception SSOException is thrown if the SSOToken can't be created.
     */
    public SSOToken createSSOToken(HttpServletRequest request)
            throws UnsupportedOperationException, SSOException;

    /**
     * Creates an SSOToken.
     * 
     * @param user Principal representing a user or service
     * @param password LDAP password of the user or service
     * @return SSOToken
     * @exception An
     *                SSOException is thrown if the SSOToken can't be created.
     * @exception an UnsupporedOperationException is thrown when other errors 
     * occur during the token creation.
     */
    public SSOToken createSSOToken(Principal user, String password)
            throws SSOException, UnsupportedOperationException;

    /** 
     * Creates an SSOToken.
     * @param sid String representing the SSOToken Id
     * @return SSOToken
     * @exception SSOException is thrown if the SSOToken can't be 
     * created.
     * @exception UnsupportedOperationException is thrown when other unsupported
     * operation is performed.
     */
    public SSOToken createSSOToken(String sid) throws SSOException,
            UnsupportedOperationException;

    /**
     * Creates an SSOToken.
     * 
     * @param sid
     *            representing the SSOToken Id
     * @param clientIP
     *            representing the IP address of the client
     * @return SSOToken
     * @exception An
     *                SSOException is thrown if the SSOToken can't be created.
     */
    public SSOToken createSSOToken(String sid, String clientIP)
            throws SSOException, UnsupportedOperationException;

    /**
     * Destroys an SSOToken.
     * 
     * @param token
     *            The SSOToken object to be destroyed
     * @exception An SSOException is thrown if the SSOToken can't be destroyed.
     */
    public void destroyToken(SSOToken token) throws SSOException;

    /**
     * Checks if an SSOToken is valid or not.
     * 
     * @param token
     *            The SSOToken object to be validated.
     * @return true or false, true if the token is valid
     */
    public boolean isValidToken(SSOToken token);

    /**
     * Checks if the SSOToken is valid.
     * 
     * @exception An
     *                SSOException is thrown if the SSOToken is not valid.
     */
    public void validateToken(SSOToken token) throws SSOException;

    /**
     * Refresh the Session corresponding to the SSOToken from the Session
     * Server.
     * 
     * @param token SSOToken
     * @exception An SSOException is thrown if the session cannot be refreshed 
     * for the token
     * 
     */
    public void refreshSession(SSOToken token) throws SSOException;

    /**
     * Destroys an SSOToken.
     * 
     * @param destroyer
     *            The SSOToken object used to authorize the operation
     * @param destroyed
     *            The SSOToken object to be destroyed.
     * @exception A
     *                SSOException is thrown if the there was an error during
     *                communication with session service.
     */
    public void destroyToken(SSOToken destroyer, SSOToken destroyed)
            throws SSOException;

    /**
     * Returns valid Sessions.
     * 
     * @param requester
     *            The SSOToken object used to authorize the operation
     * @param server
     *            The server for which the valid sessions are to be retrieved
     * @return Set The set of Valid Sessions
     * @exception A
     *                SSOException is thrown if the there was an error during
     *                communication with session service.
     */
    public Set getValidSessions(SSOToken requester, String server)
            throws SSOException;

}
