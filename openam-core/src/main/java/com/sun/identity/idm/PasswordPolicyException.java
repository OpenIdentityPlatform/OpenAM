/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package com.sun.identity.idm;


import org.forgerock.opendj.ldap.ResultCode;

/**
 * The exception class whose instance is thrown if there is any error related with password issue.
 */
public class PasswordPolicyException extends IdRepoException {

    /**
     * Create a new password policy exception with a custom error message
     * @param msg the error message
     */
    public PasswordPolicyException(String msg) {
        super(msg);
    }

    /**
     * Create a new Password policy exception
     * @param resultCode the LDAP error code
     * @param errorCode the error code
     * @param args the arguments required for this error code.
     */
    public PasswordPolicyException(ResultCode resultCode, String errorCode, Object... args) {
        super(IdRepoBundle.BUNDLE_NAME, errorCode, String.valueOf(resultCode.intValue()), args);
    }

    /**
     * Create a password policy exception from an id repo exception.
     * @param e the id repo exception
     */
    public PasswordPolicyException(IdRepoException e) {
        super(IdRepoBundle.BUNDLE_NAME, e.getErrorCode(), e.getLDAPErrorCode(), e.getMessageArgs());
    }

    public PasswordPolicyException(String errorCode, Object[] args) {
        super(IdRepoBundle.BUNDLE_NAME, errorCode, args);
    }
}
