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
 * $Id: InvalidPasswordException.java,v 1.4 2008/06/25 05:42:06 qcheng Exp $
 *
 */

package com.sun.identity.authentication.spi;

/**
 * Exception that is thrown when the user-entered password token causes the
 * authentication module to be authenticated to <b>fail</b>. Authentication
 * module must throw this exception if it wishes to participate in user lock out
 * due to too many password failure login feature. Prior to throwing this
 * exception, the authentication module should set the <code>userTokenId</code>
 * so that subsequent calls to <code>getUserTokenId</code> will return correct
 * <code>userTokenId</code> that will be use to lock out the user.
 *
 * @supported.all.api
 */
public class InvalidPasswordException extends AuthLoginException {

    String tokenId;

    /**
     * Constructs an <code>InvalidPasswordException</code> object.
     * 
     * @param rbName
     *            Resource bundle name for the message.
     * @param errCode
     *            Key to the message in resource bundle.
     * @param args
     *            Arguments to the message.
     */
    public InvalidPasswordException(String rbName, 
            String errCode, Object[] args) 
    {
        super(rbName, errCode, args);
    }

    /**
     * Constructs an <code>InvalidPasswordException</code> object.
     * 
     * @param message
     *            English message for the exception.
     */
    public InvalidPasswordException(String message) {
        super(message);
    }

    /**
     * Constructs an <code>InvalidPasswordException</code> object.
     * 
     * @param message
     *            English message for the exception.
     * @param tokenId
     *            The <code>userId</code> for which the exception occurred.
     */
    public InvalidPasswordException(String message, String tokenId) {
        super(message);
        this.tokenId = tokenId;
    }

    /**
     * Constructs an <code>InvalidPasswordException</code> object.
     * 
     * @param t
     *            the root cause of the exception
     */
    public InvalidPasswordException(Throwable t) {
        super(t);
        if (t instanceof InvalidPasswordException) {
            this.tokenId = ((InvalidPasswordException) t).tokenId;
        }
    }

    /**
     * Constructs an <code>InvalidPasswordException</code> object.
     * 
     * @param rbName
     *            Resource bundle name for the message.
     * @param errorCode
     *            Key to the message in resource bundle.
     * @param args
     *            Arguments to the message.
     * @param tokenId
     *            <code>userID</code> for which the exception occurred.
     * @param t
     *            The root cause of the exception.
     */
    public InvalidPasswordException(String rbName, String errorCode,
            Object[] args, String tokenId, Throwable t) {
        super(rbName, errorCode, args, t);
        this.tokenId = tokenId;
    }

    /**
     * Returns the token ID.
     * 
     * @return the token ID.
     */
    public String getTokenId() {
        return tokenId;
    }
}
