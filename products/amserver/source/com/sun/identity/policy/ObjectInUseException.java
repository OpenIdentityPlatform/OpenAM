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
 * $Id: ObjectInUseException.java,v 1.2 2008/06/25 05:43:44 qcheng Exp $
 *
 */




package com.sun.identity.policy;


/**
 * Exception thrown to indicate that an object you are trying to
 * remove is in use and therefore can not be removed.
 *
 * @supported.all.api
 */
public class ObjectInUseException extends PolicyException {

    private String name;
    private Object user;

    /**
     * Constructs an <code>ObjectInUseException</code> object
     * @param msg exception message
     * @param name name of the object that is in use 
     * @param user object that is using the object you are trying to remove
     */
    public ObjectInUseException(String msg, String name, Object user) {
        super(msg);
        this.name = name;
        this.user = user;
    }

    /**
     * Constructs an <code>ObjectInUseException</code> object
     * @param rbName Resource bundle name of exception message
     * @param errCode id used with resource bundle to get error message
     * @param args any arguments for message formatting. null if no
     * additional arguments are necessary
     * @param name name of the object that is in use
     * @param user object that is using the object you are trying to remove
     */
    public ObjectInUseException(String rbName, String errCode,         
        Object[] args, String name, Object user) 
    {
        super(rbName, errCode, args,null);
        this.name = name;
        this.user = user;
    }

    /**
     * Constructs an <code>ObjectInUseException</code> object
     * @param rbName Resource bundle name of exception message
     * @param errCode id used with resource bundle to get error message
     * @param args any arguments for message formatting. null if no
     * additional arguments are necessary
     * @param t nested root cause exception
     * @param name name of the object that is in use
     * @param user object that is using the object you are trying to remove
     */
    public ObjectInUseException(String rbName, String errCode,         
        Object[] args, Throwable t, String name, Object user) 
    {
        super(rbName, errCode, args,t);
        this.name = name;
        this.user = user;
    }

    /**
     * Constructs an <code>ObjectInUseException</code> object
     * @param t nested root cause exception
     * @param name name of the object that is in use 
     * @param user object that is using the object you are trying to remove
     */
    public ObjectInUseException(Throwable t, String name, Object user) {
        super(t);
        this.name = name;
        this.user = user;
    }

    /**
     * Gets the name that is in use.
     *
     * @return name of the object that is in use
     */
    public String getName() {
        return (name);
    }
}
