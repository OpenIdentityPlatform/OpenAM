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
 * $Id: IJ2EELogoutHandler.java,v 1.2 2008/06/25 05:51:46 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Interface IJ2EELogoutHandler
 *
 * Provides a generic way to logout the user from the J2EE container
 * by destroying the session.
 */
public interface IJ2EELogoutHandler {

    /**
     * Method authenticate
     *
     * Logs out the user who is currently logged in locally.
     *
     * @param request the HttpServletRequest object of the user session
     * @param response the HttpServletResponse object of the user session
     * @param extraData some extra data used for logging out the user
     *
     * @return true if the user is logged out successfully
     *
     */
    public boolean logout(HttpServletRequest request,
                          HttpServletResponse response,
                          Object extraData);

    /**
     * Method needToLogoutUser
     *
     * Returns true if there is a session mismatch between IS
     * and the J2EE container which should be interpreted as a
     * need to logout the user locally.
     *
     * @param request the HttpServletRequest object of the user session
     * @param response the HttpServletResponse object of the user session
     * @param userId the user id
     * @param userDN the user DN
     * @param filterMode the agent filter mode
     * @param extraData some extra data used for logging out the user
     * @return true if there is a session mismatch between IS and the J2EE container
     */
    public boolean needToLogoutUser(HttpServletRequest request,
                                    HttpServletResponse response,
                                    String userId, String userDN,
                                    AmFilterMode filterMode, Object extraData);
}
