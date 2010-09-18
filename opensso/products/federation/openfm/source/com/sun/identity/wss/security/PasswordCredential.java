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
 * $Id: PasswordCredential.java,v 1.3 2008/08/27 19:05:52 mrudul_uchil Exp $
 *
 */

package com.sun.identity.wss.security;

import com.iplanet.services.util.Crypt;

/**
 * This class <code>PasswordCredential</code> is a place holder for the
 * username/password object. 
 * 
 * @supported.all.api
 */
public class PasswordCredential {

    private String username = null;
    private String password = null;

    /**
     * Construct a credential with the specified password and user.
     * @param username the user name.
     * @param password the password.
     */
    public PasswordCredential(String username, String password) {
	this.username = username;
	this.password = Crypt.encrypt(password);
    }

    /**
     * Returns the user name.
     * @return the user name.
     */
    public String getUserName() {
        return username;
    }

    /**
     * Returns the password.
     * @return the password.
     */
    public String getPassword() {
        return Crypt.decrypt(password);
    }

}
