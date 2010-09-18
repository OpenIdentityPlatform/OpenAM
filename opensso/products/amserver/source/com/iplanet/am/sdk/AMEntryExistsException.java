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
 * $Id: AMEntryExistsException.java,v 1.4 2008/06/25 05:41:20 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import com.iplanet.sso.SSOToken;
import com.iplanet.ums.UMSException;

/**
 * This <code>AMEntryExistsException</code> is a specific typed exception used
 * to indicate an error encountered because the entry already existing in the
 * Directory. This class is a subclass of the <code>AMException</code> class.
 * 
 * @see java.lang.Exception
 * @see java.lang.Throwable
 * @see AMException
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public class AMEntryExistsException extends AMException {

    /**
     * Constructs a new <code>AMEntryExistsException</code> with detailed
     * message.
     * 
     * @param msg
     *            The detailed message
     * @param errorCode
     *            Error code that matches the appropriate entry in
     *            <code>amProfile.properties</code>.
     */
    public AMEntryExistsException(String msg, String errorCode) {
        super(msg, errorCode);
    }

    /**
     * Protected constructor for convenience.
     * 
     * @param token
     *            Single Sign On Token.
     * @param errorCode
     *            Error code that matches the appropriate entry in
     *            <code>amProfile.properties</code>.
     */
    public AMEntryExistsException(SSOToken token, String errorCode) {
        super(token, errorCode);
    }

    /**
     * Protected constructor for convenience.
     * 
     * @param msg
     *            The detailed message.
     * @param errorCode
     *            Error code that matches the appropriate entry in
     *            <code>amProfile.properties</code>.
     * @param ue
     *            <code>UMSException</code> root cause exception.
     */
    public AMEntryExistsException(
            String msg, String errorCode, UMSException ue) {
        super(msg, errorCode, ue);
    }

    /**
     * Protected constructor for convenience.
     * 
     * @param token
     *            Single Sign On Token.
     * @param errorCode
     *            Error code that matches the appropriate entry in
     *            <code>amProfile.properties</code>.
     * @param ue
     *            <code>UMSException</code> root cause exception.
     */
    public AMEntryExistsException(SSOToken token, String errorCode,
            UMSException ue) {
        super(token, errorCode, ue);
    }

    /**
     * Constructs a new <code>AMException</code> with detailed message.
     * 
     * @param msg
     *            The detailed message.
     * @param errorCode
     *            Error code that matches the appropriate entry in
     *            <code>amProfile.properties</code>.
     * @param args
     *            Array of arguments to be applied in the message.
     */
    public AMEntryExistsException(String msg, String errorCode, Object args[]) {
        super(msg, errorCode, args);
    }

    /**
     * Constructs a new <code>AMException</code> with detailed message.
     * 
     * @param msg
     *            The detailed message.
     * @param errorCode
     *            Error code that matches the appropriate entry in
     *            <code>amProfile.properties</code>.
     * @param args
     *            Array of arguments to be applied in the message.
     * @param ue
     *            <code>UMSException</code> root cause exception.
     */
    public AMEntryExistsException(String msg, String errorCode, Object args[],
            UMSException ue) {
        super(msg, errorCode, args, ue);
    }

}
