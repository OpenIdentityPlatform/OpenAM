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
 * $Id: NameNotFoundException.java,v 1.2 2008/06/25 05:43:44 qcheng Exp $
 *
 */



package com.sun.identity.policy;

/**
 * Exception thrown if an object such as policy, rule or 
 * referral for the given name does not exist.
 *
 * @supported.all.api
 */
public class NameNotFoundException extends PolicyException {

    private String name;
    private int type;

    /**
     * Constructor
     * @param msg exception message
     * @param name name that was not found
     * @param type type of object whose name was not found
     */
    public NameNotFoundException(String msg, String name, int type) {
        super(msg);
        this.name = name;
        this.type = type;
        fillInStackTrace();
    }

    /**
     * Constructor
     * @param rbName Resource bundle name of exception message
     * @param errCode id used with resource bundle to get error message
     * @param args any arguments for message formatting. null if no
     * additional arguments are necessary
     * @param name name that was not found
     * @param type type of object whose name was not found
     */
    public NameNotFoundException(String rbName, String errCode,         
        Object[] args, String name, int type) 
    {
        super(rbName, errCode, args,null);
        fillInStackTrace();
        this.name = name;
        this.type = type;
    }

    /**
     * Constructor
     * @param rbName Resource bundle name of exception message
     * @param errCode id used with resource bundle to get error message
     * @param args any arguments for message formatting. null if no
     * additional arguments are necessary
     * @param t nested root cause exception
     * @param name name that was not found
     * @param type type of object whose name was not found
     */
    public NameNotFoundException(String rbName, String errCode,         
        Object[] args, Throwable t, String name, int type) 
    {
        super(rbName, errCode, args,t);
        fillInStackTrace();
        this.name = name;
        this.type = type;
    }

    /**
     * Constructor
     * @param t nested root cause exception
     * @param name name that was not found
     * @param type type of object whose name was not found
     */
    public NameNotFoundException(Throwable t, String name, int type) {
        super(t);
        this.name = name;
        this.type = type;
        fillInStackTrace();
    }

    /**
     * Gets the name that was not found.
     *
     * @return name that was not found.
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
     * @return type of object for which name was not found
     */
    public int getObjectType() {
        return (type);
    }
}
