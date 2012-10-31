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
 * $Id: UMChangeUserPasswordModel.java,v 1.3 2009/09/28 18:59:55 babysunil Exp $
 *
 */

package com.sun.identity.console.user.model;

import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMConsoleException;

/* - NEED NOT LOG - */

public interface UMChangeUserPasswordModel
    extends AMModel
{
    /**
     * Returns user name.
     *
     * @param userId Universal ID of user.
     * @return user name.
     */
    String getUserName(String userId);

    /** 
     * Returns user password.
     *
     * @param userId Universal ID of user.
     * @return user password.
     * @throws AMConsoleException if password cannot be obtained.
     */
    String getPassword(String userId)
        throws AMConsoleException;

    /**
     * Modifies user password.
     *
     * @param userId Universal ID of user.
     * @param password New password.
     * @throws AMConsoleException if password cannot be modified.
     */
    void changePassword(String userId, String password)
        throws AMConsoleException;

    /**
      * Modifies user password after validating old password.
      *
      * @param userId Universal ID of user.
      * @param oldpwd old password.
      * @param newpwd New password.
      * @throws AMConsoleException if password cannot be modified.
      */
     void changePwd(String userId, String oldpwd, String newpwd)
         throws AMConsoleException;
}
