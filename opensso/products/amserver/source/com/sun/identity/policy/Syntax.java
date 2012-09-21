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
 * $Id: Syntax.java,v 1.3 2008/06/25 05:43:45 qcheng Exp $
 *
 */


package com.sun.identity.policy;

/**
 * Provides an enum like support for the syntax of values
 * such as ANY, NONE, LIST, CONSTANT, SINGLE_CHOICE, MULTIPLE_CHOICE
 * In other words, provides access to a set of finite values and enforces 
 * new values can not be created by users
 *
 * @supported.all.api
 */
public final class Syntax {

    /**
     * value is a free form text, would be typically shown in as editable 
     *  text field
     */
    public static final Syntax ANY = new Syntax("ANY");

    /**
     * value is a free form multi list text field
     */
    public static final Syntax LIST = new Syntax("LIST");

    /**
     * value is a free form text, could also search from a large set of values
     */
    public static final Syntax ANY_SEARCHABLE = new Syntax("ANY_SEARCHABLE");

    /**
     * no value is allowed
     */
    public static final Syntax NONE = new Syntax("NONE");

    /**
     * value is a constant string, would be typically shown as non 
     *  editable text
     */
    public static final Syntax CONSTANT = new Syntax("CONSTANT");

    /**
     * value is a single  choice from a list
     */
    public static final Syntax SINGLE_CHOICE = new Syntax("SINGLE_CHOICE");

    /**
     * value is multiple choice from list
     */
    public static final Syntax MULTIPLE_CHOICE = new Syntax("MULTIPLE_CHOICE");

    private String _type;

    private Syntax(String type) {
        _type = type;
    }

    /**
     * Returns the string representation of this object.
     *  
     *  @return string representation of this Syntax
     */
    public String toString() {
        return _type;
    }

    /**
     * Checks whether the argument object is equal to this Syntax
     * 
     * @param arg Syntax object for comparison.
     * @return <code>true</code> if the argument object is equal
     *         to this Syntax, else <code>false</code>
     */
    public boolean equals(Object arg) {
        boolean equalObjects = false;
        if ( arg == null ) {
            equalObjects = false;
        } else if ( arg == this ) {
            equalObjects = true;
        } else if ( !(arg instanceof Syntax) ) {
            equalObjects = false;
        } else if ( _type.equals(((Syntax)arg)._type)) {
            equalObjects = true;
        }
        return equalObjects;
   }
}
