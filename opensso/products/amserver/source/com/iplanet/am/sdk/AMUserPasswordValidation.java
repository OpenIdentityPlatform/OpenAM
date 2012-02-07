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
 * $Id: AMUserPasswordValidation.java,v 1.3 2008/06/25 05:41:23 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Map;

/**
 * This class provides a <code>userID</code> and password validation plugin
 * mechanism.
 * 
 * <p>
 * The methods of this class need to be overridden by the implementation plugin
 * modules that validate the <code>userID</code> and/or password for the user.
 * The implementation plugin modules will be invoked whenever a
 * <code>userID</code> or password value is being added/modified using
 * Identity Server console, <code>amadmin</code> command line interface or
 * using SDK API's directly.
 * 
 * <p>
 * The plugins that extend this class can be configured per Organization by
 * setting the attribute:
 * <code>iplanet-am-admin-console-user-password-validation-class</code> of
 * <code>iPlanetAMAdminConsoleService</code> Service. If a plugin is not
 * configured at an Organization, then the plugin configured at the global level
 * will be used.
 * 
 * <p>
 * If the validation of the plugin fails, the plugin module can throw an
 * Exception to notify the application to indicate the error in th
 * <code>userID</code> or password supplied by the user. The Exception
 * mechanism provides a means to notify the plugin specific validation error to
 * the user.
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public class AMUserPasswordValidation {

    /**
     * Constructs the <code> AMUserPasswordValidation </code> object
     */
    public AMUserPasswordValidation() {
    }

    /**
     * Method to validate the <code>userID</code>.
     * 
     * @param userID
     *            the value of the user ID.
     * @throws AMException
     *             if an error occurs in supplying password. The operation
     *             (add/modify) in progress will be aborted and the application
     *             is notified about the error through the exception.
     */
    public void validateUserID(String userID) throws AMException {
    }

    /**
     * /** Method to validate the <code>userID</code>.
     * 
     * @param userID
     *            the value of the user ID.
     * @param envParams
     *            the values of the parameters for which the validation is
     *            enforced.
     * @throws AMException
     *             if an error occurs in supplying password. The operation
     *             (add/modify) in progress will be aborted and the application
     *             is notified about the error through the exception.
     */
    public void validateUserID(String userID, Map envParams) 
        throws AMException {
        validateUserID(userID);
    }

    /**
     * Method to validate the Password.
     * 
     * @param password
     *            the password value
     * @throws AMException
     *             if an error occurs in supplying password. The operation
     *             (add/modify) in progress will be aborted and the application
     *             is notified about the error through the exception.
     */
    public void validatePassword(String password) throws AMException {
    }

    /**
     * Method to validate the Password.
     * 
     * @param password
     *            the password value
     * @param envParams
     *            the values of the parameters for which the password is
     *            validated.
     * @throws AMException
     *             if an error occurs in supplying password. The operation
     *             (add/modify) in progress will be aborted and the application
     *             is notified about the error through the exception.
     */
    public void validatePassword(String password, Map envParams)
            throws AMException {
        validatePassword(password);
    }
}
