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
 * $Id: InvalidFormatException.java,v 1.2 2008/06/25 05:43:43 qcheng Exp $
 *
 */



package com.sun.identity.policy;

/**
 * Exception thrown if a name of an object such as policy, rule or 
 * referral has invalid format
 *
 * @supported.all.api
 */
public class InvalidFormatException extends PolicyException {

    private String name;
    private int type;

    /**
     * Constructs a <code>InvalidFormatException</code> instance.
     *
     * @param msg exception message
     * @param name name that was in invalid format
     * @param type type of object that had invalid name format
     */
    public InvalidFormatException(String msg, String name, int type) {
        super(msg);
        fillInStackTrace();
        this.name = name;
        this.type = type;
    }

    /**
     * Constructs a <code>InvalidFormatException</code> instance.
     *
     * @param t nested root cause exception
     * @param name name that was in invalid format
     * @param type type of object that had invalid name format
     */
    public InvalidFormatException(Throwable t, String name, int type) {
        super(t);
        fillInStackTrace();
        this.name = name;
        this.type = type;
    }

    /**
     * Constructs a <code>InvalidFormatException</code> instance.
     *
     * @param rbName Resource bundle name of exception message
     * @param errCode id used with resource bundle to get error message
     * @param args any arguments for message formatting. null if no
     * additional arguments are necessary
     * @param t nested root cause exception
     * @param name name that was in invalid format
     * @param type type of object that had invalid name format
     */
    public InvalidFormatException(String rbName,String errCode,
        Object [] args, Throwable t, String name, int type) 
    {
        super(rbName,errCode,args,t);
        fillInStackTrace();
        this.name = name;
        this.type = type;
    }

    /**
     * Constructs a <code>InvalidFormatException</code> instance.
     *
     * @param rbName Resource bundle name of exception message
     * @param errCode id used with resource bundle to get error message
     * @param args any arguments for message formatting. null if no
     * additional arguments are necessary
     * @param name name that was in invalid format
     * @param type type of object that had invalid name format
     */
    public InvalidFormatException(String rbName,String errCode,
        Object [] args, String name, int type) 
    {
        super(rbName,errCode,args,null);
        fillInStackTrace();
        this.name = name;
        this.type = type;
    }

    /**
     * Gets the name of the object that encountered an invalid format.
     *
     * @return name that was in invalid format
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
     * @return type of object that had invalid name format
     */
    public int getObjectType() {
        return (type);
    }
}
