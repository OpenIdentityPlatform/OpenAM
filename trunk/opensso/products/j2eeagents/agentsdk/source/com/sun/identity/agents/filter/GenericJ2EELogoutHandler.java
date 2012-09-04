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
 * $Id: GenericJ2EELogoutHandler.java,v 1.2 2008/06/25 05:51:44 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.security.Principal;

/**
 * Provides a generic way to logout the user from the J2EE container
 * by destroying the session.
 *
 */
public class GenericJ2EELogoutHandler
                     implements IJ2EELogoutHandler {


        /**
         * Method authenticate
         *
         * Logs out the user who is currently logged in locally.
         *
         * @param request
         * @param response
         * @param extraData
         *
         * @return true if logout was successful, false otherwise.
         *
         */
        public boolean logout(HttpServletRequest request,
                               HttpServletResponse response,
                               Object extraData) {
            boolean retval = false;
            if ( request!= null) {
                 retval = true;
                 HttpSession session = request.getSession(false);
                 if ( session != null) {
                     session.invalidate();
                 }
            }
            return retval;
         }

        /**
         * Method needToLogoutUser
         *
         * Returns true if there is a session mismatch between IS
         * and the J2EE container which should be interpreted as a
         * need to logout the user locally.
         *
         * @param request
         * @param response
         * @param isUserId
         * @param isUserDN
         * @param filterMode
         * @param extraData
         *
         * @return true if the local logout is required
         */
        public boolean needToLogoutUser(HttpServletRequest request,
                                        HttpServletResponse response,
                                        String isUser, String isUserDN,
                                        AmFilterMode filterMode, Object extraData) {
            boolean needToLogout = false;

            if (isUser != null) {
                Principal user = request.getUserPrincipal();
                String userName = null;

                if (user != null) {
                    userName = user.getName();
                }
                if ( userName != null && !userName.equals(isUser)){
                    needToLogout = true;
                }
                if (!needToLogout) {
                    String remoteUser = request.getRemoteUser();
                    if (remoteUser != null && !remoteUser.equals(isUser)) {
                        needToLogout = true;
                    }
                }
            }

            return needToLogout;
        }

}

