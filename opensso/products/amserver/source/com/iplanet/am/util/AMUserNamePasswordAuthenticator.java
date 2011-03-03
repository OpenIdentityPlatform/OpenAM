/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMUserNamePasswordAuthenticator.java,v 1.1 2009/03/24 23:52:12 pluo Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.util;

import javax.mail.PasswordAuthentication;

/*
 * This is an implementation for javax.mail.Authenticator to do
 * user name and password authentication. This class will be
 * used to authenticate to mail server.
 */

public class AMUserNamePasswordAuthenticator
        extends javax.mail.Authenticator {
    private String userName;
    private String userPassword;

    /**
     * Public constructor that takes user name and user password.
     *
     * @param user The user name
     * @param password The user password
     */

    public AMUserNamePasswordAuthenticator(String user, String password) {
        userName = user;
        userPassword = password;
    }

    /**
     * Gets a PasswordAuthentication object
     *
     * @return An PasswordAuthentication object based on the user name and password 
     *         passed in from the constructor.
     */

    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(userName, userPassword);
    }
}
