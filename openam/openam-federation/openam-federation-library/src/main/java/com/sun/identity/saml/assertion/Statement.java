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
 * $Id: Statement.java,v 1.2 2008/06/25 05:47:32 qcheng Exp $
 *
 */

package com.sun.identity.saml.assertion;

/**
 * The <code>Statement</code> element is an extension point that allows
 * other assertion-based applications to reuse the SAML assertion framework.
 * Its <code>StatementAbstractType</code> complex type is abstract; extension
 * elements must use <code>xsi:type</code> attribute to indicate the derived
 * type.
 *
 *@supported.all.api
 */
public abstract class Statement {
    /**
     * The Statement is not supported.
     */
    public final static int NOT_SUPPORTED                   = -1;

    /**
     * The Statement is an Authentication Statement.
     */
    public final static int AUTHENTICATION_STATEMENT        = 1;

    /**
     * The Statement is an Authorization Decision Statement.
     */
    public final static int AUTHORIZATION_DECISION_STATEMENT= 2;

    /**
     * The Statement is an Attribute Statement.
     */
    public final static int ATTRIBUTE_STATEMENT             = 3; 
    
    /**
     *Default constructor of the statement 
     */   
    protected Statement() {
    }
    
    /**
     * Returns the real of statement such as
     * <code>AuthenticationStatement</code>, 
     * <code>AuthorizationDecisionStatement</code> or
     * <code>AttributeStatement</code>.
     *
     * @return real type of Statement.
     */
    public abstract int getStatementType(); 

    /** 
     *Creates a String representation of the Statement
     *@param includeNS : Determines whether or not the namespace qualifier
     *                   is prepended to the Element when converted
     *@param declareNS : Determines whether or not the namespace is declared
     *                   within the Element.
     *@return A String representation of the <code>Statement</code> element   
     */
    public abstract String toString(boolean includeNS, boolean declareNS);    
}

