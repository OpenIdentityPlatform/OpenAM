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
 * $Id: Condition.java,v 1.2 2008/06/25 05:47:32 qcheng Exp $
 *
 */

package com.sun.identity.saml.assertion;

/**
 *This is an abstract class which servers as an extension point for new 
 *conditions.  This is one of the element within the <code>Conditions</code> 
 *object.  Extension elements based on this class MUST use xsi:type attribute 
 *to indicate the derived type.
 *
 *@supported.all.api
 */

public abstract class Condition {


    /**
     * The Condition is invalid.
     */
    public static int INVALID = -1;

    /**
     * The Condition is indeterminate.
     */
    public static int INDETERMINATE = 0;

    /**
     * The Condition is valid.
     */
    public static int VALID = 1;

    /**  
     * Returns a String representation of the
     * <code>&lt;saml:Conditions&gt;</code> element.
     *
     * @param IncludeNS Determines whether or not the namespace qualifier is 
     *        prepended to the Element when converted
     * @param DeclareNS Determines whether or not the namespace is declared 
     *        within the Element.
     * @return A string containing the valid XML for this element
    */                       
    public abstract String toString(boolean IncludeNS, boolean DeclareNS) ;

    /** 
     * Evaluates this condition 
     * An abstract method which can be implemented by any condition extending
     * this Condition object, to provide means of evaluating the condition.
     *
     * @return evaluation state.
     */
    public abstract int  evaluate() ;

}

