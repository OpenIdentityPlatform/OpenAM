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
 * $Id: AMPreCallBackException.java,v 1.4 2008/06/25 05:41:22 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import com.iplanet.sso.SSOToken;
import com.iplanet.ums.UMSException;

/**
 * <code>AMPreCallBackException</code> is a subclass of
 * <code>AMException</code> which is used by all implementations of
 * <code>AMCallback</code>. This exception should be thrown by the
 * <code>callback</code> plugins in case they want to abort the operation
 * being performed by SDK. This helps distinguish a <code>pre-callback</code>
 * exception from normal SDK exceptions and can be handled differently by any
 * applications, if they wish to.
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public class AMPreCallBackException extends AMException {

    /**
     * Constructs a new <code>AMPreCallBackException</code> with detailed
     * message.
     * 
     * @param msg
     *            The detailed message
     * @param errorCode
     *            Matches the appropriate entry in
     *            <code>amProfile.properties</code>.
     */
    public AMPreCallBackException(String msg, String errorCode) {
        super(msg, errorCode);
    }

    /**
     * Constructs a new <code>AMPreCallBackException</code> with detailed
     * message.
     * 
     * @param token
     *            A valid SSO token of the user performing the operation
     * @param errorCode
     *            Matches the appropriate entry in
     *            <code>amProfile.properties</code>.
     */
    public AMPreCallBackException(SSOToken token, String errorCode) {
        super(token, errorCode);
    }

    /**
     * Constructs a new <code>AMPreCallBackException</code> with detailed
     * message.
     * 
     * @param msg
     *            The detailed message
     * @param errorCode
     *            Matches the appropriate entry in
     *            <code>amProfile.properties</code>.
     * @param ue
     *            if the root cause is a <code>UMSException</code>.
     */
    public AMPreCallBackException(String msg, String errorCode, UMSException ue)
    {
        super(msg, errorCode, ue);
    }

    /**
     * Constructs a new <code>AMPreCallBackException</code> with detailed
     * message.
     * 
     * @param token
     *            a valid single sign on token of the user performing the
     *            operation.
     * @param errorCode
     *            Matches the appropriate entry in
     *            <code>amProfile.properties</code>.
     * @param ue
     *            if the root cause is a <code>UMSException</code>.
     */
    public AMPreCallBackException(SSOToken token, String errorCode,
            UMSException ue) {
        super(token, errorCode, ue);
    }

    /**
     * Constructs a new <code>AMPreCallBackException</code> with detailed
     * message.
     * 
     * @param msg
     *            The detailed message.
     * @param errorCode
     *            Matches the appropriate entry in
     *            <code>amProfile.properties</code>.
     * @param args
     *            if the error message needs specific values to be set.
     */
    public AMPreCallBackException(String msg, String errorCode, Object[] args) {
        super(msg, errorCode, args);
    }

    /**
     * Constructs a new <code>AMPreCallBackException</code> with detailed
     * message.
     * 
     * @param msg
     *            The detailed message
     * @param errorCode
     *            Matches the appropriate entry in
     *            <code>amProfile.properties</code>.
     * @param args
     *            if the error message needs specific values to be set.
     * @param ue
     *            if the root cause is a <code>UMSException</code>.
     */
    public AMPreCallBackException(String msg, String errorCode, Object[] args,
            UMSException ue) {
        super(msg, errorCode, args, ue);
    }
}
