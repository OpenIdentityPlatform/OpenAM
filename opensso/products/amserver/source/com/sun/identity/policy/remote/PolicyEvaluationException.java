/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PolicyEvaluationException.java,v 1.2 2008/06/25 05:43:53 qcheng Exp $
 *
 */

package com.sun.identity.policy.remote;

import com.sun.identity.policy.PolicyException;

/**
 * The class <code>PolicyEvaluationException</code> is the exception 
 * for the error happening in policy request XML parsing and policy 
 * request evaluation. 
 *
 * @supported.all.api
 */
public class PolicyEvaluationException extends PolicyException {

    // The id of the policy request
    private String reqId;

    /**
     * Constructs an instance of the <code>PolicyEvaluationException</code>.
     *
     * @param message The message provided by the object that is throwing the
     * exception.
     */
    public PolicyEvaluationException(String message) {
        super(message);
        reqId = "-1";
    }

    /**
     * Constructs an instance of the <code>PolicyEvaluationException</code>
     *
     * @param nestedException the exception caught by the code  block creating
     *        this.
     */
    public PolicyEvaluationException(Throwable nestedException) {
        super(nestedException);
        reqId = "-1";
    }

    /**
     * Constructs an instance of the <code>PolicyEvaluationException</code>
     * class.
     *
     * @param nestedException the exception caught by the code  block creating
     *        this
     * @param reqId The id of the policy request exception.
     */
    public PolicyEvaluationException(Throwable nestedException, String reqId) {
        super(nestedException);
        this.reqId = reqId;
    }

    /**
     * Constructs an instance of <code> PolicyEvaluationException </code> 
     * to pass the localized error message
     * At this level, the locale of the caller is not known and it is
     * not possible to throw localized error message at this level.
     * Instead this constructor provides Resource Bundle name and error code
     * for correctly locating the error message. The default
     * <code>getMessage()</code> will always return English messages only. This
     * is in consistent with current JRE.
     *
     * @param rbName Resource Bundle Name to be used for getting 
     * localized error message.
     * @param errorCode Key to resource bundle. You can use 
     * <pre>
     * ResourceBundle rb = ResourceBunde.getBundle (rbName,locale);
     * String localizedStr = rb.getString(errorCode);
     * </pre>
     * @param args arguments to message. If it is not present pass the
     * as null 
     * @param nestedException the exception caught by the code  block creating 
     * this 
     * @param reqId The id of the policy request exception.
     */
    public PolicyEvaluationException (String rbName, String errorCode, 
        Object[] args, Throwable nestedException, String reqId) {
        super (rbName,errorCode,args,nestedException);
        this.reqId = reqId;
    }

    /**
     * Constructs an instance of <code> PolicyEvaluationException </code> 
     * to pass the localized error message
     * At this level, the locale of the caller is not known and it is
     * not possible to throw localized error message at this level.
     * Instead this constructor provides Resource Bundle name and error code
     * for correctly locating the error message. The default
     * <code>getMessage()</code> will always return English messages only.
     * This is in consistent with current JRE.
     *
     * @param rbName Resource Bundle Name to be used for getting 
     *        localized error message.
     * @param errorCode Key to resource bundle. You can use 
     * <pre>
     * ResourceBundle rb = ResourceBunde.getBundle (rbName,locale);
     * String localizedStr = rb.getString(errorCode):
     * </pre>
     * @param args arguments to message. If it is not present pass the
     * as null 
     * @param nestedException the exception caught by the code  block creating 
     * this 
     */
    public PolicyEvaluationException (String rbName, String errorCode, 
        Object[] args, Throwable nestedException) {
        super (rbName,errorCode,args,nestedException);
    }

    /**
     * Constructs an instance of the <code>PolicyEvaluationException</code>
     * class.
     * @param message The message provided by the object that is throwing the
     *        exception.
     * @param reqId The id of the policy request exception.
     */
    public PolicyEvaluationException(String message, String reqId) {
        super(message);
        this.reqId = reqId;
    }

    /**
     * Constructs an instance of the <code>PolicyEvaluationException</code>
     * class.
     * @param message message of this exception
     * @param nestedException the exception caught by the code  block creating
     *        this exception.
     */
    public PolicyEvaluationException(String message, Throwable nestedException){
        super(message, nestedException);
        reqId = "-1";
    }

    /**
     * Constructs an instance of the <code>PolicyEvaluationException</code>
     * class.
     *
     * @param message message of this exception
     * @param nestedException the exception caught by the code 
     *        block creating this exception
     * @param reqId The id of the policy request
     */
    public PolicyEvaluationException(String message, Throwable nestedException, 
        String reqId) {
        super(message, nestedException);
        this.reqId = reqId;
    }

    /**
     * Returns the request Id.
     *
     * @return the request Id.
     */
    public String getRequestId() {
        return reqId;
    }
}
