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
 * $Id: IdRepoFatalException.java,v 1.3 2008/06/25 05:43:28 qcheng Exp $
 *
 */

package com.sun.identity.idm;

/**
 * The exception class whose instance is thrown if there is any error during the
 * operation of objects of the <code>com.sun.identity.sms</code> package. This
 * class maps the exception that occurred at a lower level to a high level
 * error. Using the exception status code <code>getExceptionCode()</code> the
 * errors are categorized as a <code>ABORT</code>, <code>RETRY</code>,
 * <code>CONFIG_PROBLEM</code> or <code>LDAP_OP_FAILED</code> (typically a
 * bug).
 *
 * @supported.all.api
 */
public class IdRepoFatalException extends IdRepoException {

    /**
     * @param msg
     *            The message provided by the object which is throwing the
     *            exception
     */
    public IdRepoFatalException(String msg) {
        super(msg);

    }

    public IdRepoFatalException(String msg, String errorCode) {
        super(msg, errorCode);
    }

    /**
     * This constructor is used to pass the localized error message At this
     * level, the locale of the caller is not known and it is not possible to
     * throw localized error message at this level. Instead this constructor
     * provides Resource Bundle name and error code for correctly locating the
     * error message. The default <code>getMessage()</code> will always return
     * English messages only. This is in consistent with current JRE.
     * 
     * @param rbName
     *            Resource bundle Name to be used for getting localized error
     *            message.
     * @param errorCode
     *            Key to resource bundle. You can use <code>ResourceBundle rb =
     *        ResourceBunde.getBundle(rbName,locale);
     *        String localizedStr = rb.getString(errorCode)</code>.
     * @param args
     *            arguments to message. If it is not present pass the as null.
     */
    public IdRepoFatalException(String rbName, String errorCode, Object[] args)
    {
        super(rbName, errorCode, args);
    }

}
