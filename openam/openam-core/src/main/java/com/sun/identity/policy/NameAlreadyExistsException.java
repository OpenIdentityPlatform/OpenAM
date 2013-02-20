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
 * $Id: NameAlreadyExistsException.java,v 1.2 2008/06/25 05:43:44 qcheng Exp $
 *
 */


package com.sun.identity.policy;

/**
 * Exception thrown if a name of an object such as policy, rule or 
 * referral already exists (used by another object of the same type)
 *
 * @supported.all.api
 */
public class NameAlreadyExistsException extends PolicyException {

    private String name;
    private int type;

    /**
     * Constructs a <code>NameAlreadyExistsException</code> instance.
     *
     * @param msg exception message
     * @param name name that already exists
     * @param type type of object whose name already exist
     */
    public NameAlreadyExistsException(String msg, String name, int type) {
        super(msg);
        this.name = name;
        this.type = type;
        fillInStackTrace();
    }

    /**
     * Constructs a <code>NameAlreadyExistsException</code> instance.
     * With localized error message
     *
     * @param rbName Resource bundle name
     * @param errorCode  error code to use access the resource bundle
     * @param args parameters for message formatting
     * @param name name that already exists
     * @param type type of object whose name already exist
     */
    public NameAlreadyExistsException(String rbName,String errorCode,
                                      Object[] args, String name, int type) 
    {
        super(rbName,errorCode,args,null);
        this.name = name;
        this.type = type;
        fillInStackTrace();
    }

    /**
     * Constructs a <code>NameAlreadyExistsException</code> instance.
     * With localized error message
     *
     * @param rbName Resource bundle name
     * @param errorCode  error code to use access the resource bundle
     * @param args parameters for message formatting
     * @param t nested root cause exception
     * @param name name that already exists
     * @param type type of object whose name already exist
     */
    public NameAlreadyExistsException(String rbName,String errorCode,
                      Object[] args, Throwable t, String name, int type) 
    {
        super(rbName,errorCode,args,t);
        this.name = name;
        this.type = type;
        fillInStackTrace();
    }

    /**
     * Constructs a <code>NameAlreadyExistsException</code> instance.
     *
     * @param t nested root cause exception
     * @param name name that already exists
     * @param type type of object whose name already exist
     */
    public NameAlreadyExistsException(Throwable t, String name, int type) {
        super(t);
        this.name = name;
        this.type = type;
        fillInStackTrace();
    }

    /**
     * Gets the name that already exists.
     *
     * @return name that already exists
     */
    public String getName() {
        return (name);
    }

    /**
     * Gets the type of object which encountered
     * the invalid name. The defined objects are <code>POLICY</code>,
     * <code>RULE</code>, <code>ORGANIZATION</code>,
     * <code>USER_COLLECTION</code>
     * <code>RESPONSE_PROVIDER_COLLECTION</code>
     * <code>CONDITION_COLLECTION</code>
     * <code>REFERRAL_COLLECTION</code>
     * <code>REFERRAL_TYPE</code>
     * <code>SUBJECT_TYPE</code>
     * and <code>SERVICE</code>
     *
     * @return type of object that already exists
     */
    public int getObjectType() {
        return (type);
    }
}
