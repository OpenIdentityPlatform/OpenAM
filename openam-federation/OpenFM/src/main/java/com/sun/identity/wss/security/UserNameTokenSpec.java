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
 * $Id: UserNameTokenSpec.java,v 1.4 2008/08/27 19:05:53 mrudul_uchil Exp $
 *
 */

package com.sun.identity.wss.security;



/**
 * This class defines the specification for generating the Username security 
 * tokens.
 * It implements <code>SecurityTokenSpec</code> interface.
 * 
 * @supported.all.api
 */ 
public class UserNameTokenSpec implements SecurityTokenSpec {

    private boolean isTimeStamp = false;
    private boolean isNonce = false;
    private String passwordType = null;
    private String password = null;
    private String username = null;

    //Default constructor.
    public UserNameTokenSpec() { }

    /**
     * Returns the password type.
     * @return the password type.
     */
    public String getPasswordType() {
        return passwordType;
    }

    /**
     * Sets the password type.
     * @param passwordType the password type.
     */
    public void setPasswordType(String passwordType) {
        this.passwordType = passwordType;
    }

    /**
     * Sets if the nonce should be used.
     * @param isNonce true if the nonce should be used.
     */
    public void setNonce(boolean isNonce) {
        this.isNonce = isNonce;
    }

    /**
     * Checks if the nonce should be used.
     * @return true if the nonce should be used.
     */
    public boolean isCreateNonce() {
        return isNonce;
    } 

    /**
     * Checks if the creation time stamp should be used.
     * @return true if the create time stamp should be used.
     */
    public boolean isCreateTimeStamp() {
        return isTimeStamp;
    } 

    /**
     * Sets if the create time stamp should be used.
     * @param isTimeStamp true if the time stamp should be used.
     */
    public void setCreateTimeStamp(boolean isTimeStamp) {
        this.isTimeStamp = isTimeStamp;
    }

    /**
     * Sets the username 
     * @param username the username to be used in username token.
     */
    public void setUserName(String username) {
        this.username = username;
    }
 
    /**
     * Returns the username
     * @return the user name.
     */
    public String getUserName() {
        return username;
    }

    /**
     * Sets the user password to be used in username token.
     * @param password the password to be used in username token.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the password.
     * @return the password.
     */
    public String getPassword() {
        return password;
    }
}
