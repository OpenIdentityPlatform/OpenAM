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
 * $Id: PolicyException.java,v 1.2 2008/06/25 05:43:44 qcheng Exp $
 *
 */


package com.sun.identity.policy;

/**
 * The class <code>PolicyException</code> is the basic exception for the
 * the policy component. All other exceptions in this package are derived
 * from this exception.
 *
 * @supported.all.api
 */
public class PolicyException extends ChainedException {
    /**
     * The constant variable specifies the exception is
     * with respect to policy.
     */
    public final static int POLICY = 1;

    /**
     * The constant variable specifies the exception is
     * with respect to rule.
     */
    public final static int RULE = 2;

    /**
     * The constant variable specifies the exception is
     * with respect to services.
     */
    public final static int SERVICE = 3;

    /**
     * The constant variable specifies the exception is
     * with respect to organization, sub-organization or
     * the container.
     */
    public final static int ORGANIZATION = 4;

    /**
     * The constant variable specifies the exception is
     * with respect to user collection.
     */
    public final static int USER_COLLECTION = 5;

    /**
     * The constant variable specifies the exception is
     * with respect to constraint collection.
     */
    public final static int CONDITION_COLLECTION = 6;

    /**
     * The constant variable specifies the exception is
     * with respect to response provider collection.
     */
    public final static int RESPONSE_PROVIDER_COLLECTION = 7;

    /**
     * The constant variable specifies the exception is
     * with respect to referral collection.
     */
    public final static int REFERRAL_COLLECTION = 8;

    /**
     * The constant variable specifies the exception is
     * with respect to Referral Type
     */
    public final static int REFERRAL_TYPE = 9;

    /**
     * The constant variable specifies the exception is
     * with respect to Subject Type
     */
    public final static int SUBJECT_TYPE = 10;

    /**
     * Constructs an instance of the <code>PolicyException</code> class.
     * @param message The message provided by the object that is throwing the
     * exception.
     */
    public PolicyException(String message) {
        super(message);
        //fillInStackTrace();
    }

    /**
     * Constructs an instance of the <code>PolicyException</code> class.
     * @param nestedException the exception caught by the code  block creating 
     * this exception
     * 
     */
    public PolicyException(Throwable nestedException) {
        super(nestedException);
    }

    /**
     * Constructs an instance of the <code>PolicyException</code> class.
     * @param message message of this exception
     * @param nestedException the exception caught by the code  block creating 
     * this  exception
     * 
     */
    public PolicyException(String message, Throwable nestedException) {
        super(message, nestedException);
    }

    /**
     * Constructs an instance of <code>PolicyException</code> to pass the 
     * localized error message
     * At this level, the locale of the caller is not known and it is
     * not possible to throw localized error message at this level.
     * Instead this constructor provides Resource Bundle name and error code
     * for correctly locating the error message. The default
     * <code>getMessage()</code> will always return English messages only. This
     * is in consistent with current JRE
     * @param rbName Resource Bundle Name to be used for getting 
     * localized error message.
     * @param errorCode  Key to resource bundle. You can use 
     *        <pre>ResourceBundle rb = ResourceBunde.getBundle (rbName,locale);
     *    String localizedStr = rb.getString(errorCode);</pre>
     * @param args arguments to message. If it is not present pass the
     * as null 
     * @param nestedException The root cause of this error
     */
    public PolicyException(String rbName, String errorCode, Object[] args,
        Throwable nestedException) {
        super (rbName,errorCode,args,nestedException);
    }
}
