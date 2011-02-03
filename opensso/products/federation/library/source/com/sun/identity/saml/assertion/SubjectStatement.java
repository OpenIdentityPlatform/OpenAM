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
 * $Id: SubjectStatement.java,v 1.2 2008/06/25 05:47:33 qcheng Exp $
 *
 */


package com.sun.identity.saml.assertion;

import org.w3c.dom.*; 
import java.util.*; 
import com.sun.identity.saml.common.SAMLUtilsCommon;

/**
 * The <code>SubjectStatement</code> element is an extension
 * point that allows other assertion-based applications to reuse the SAML
 * assertion framework. It contains a <code>Subject</code> element
 * that allows issuer to describe a subject. Its
 * <code>SubjectStatementAbstractType</code> complex type is abstract;
 * extension elements must use <code>xsi:type</code> attribute to indicate
 * the derived type. 
 *
 * @supported.all.api
 */
public abstract class SubjectStatement extends Statement {
    //The subject 
    protected Subject _subject = null; 
      
    /**
     *Default Constructor
     */
    protected SubjectStatement() {
    }
    
    /**
     * Returns the Subject within the <code>SubjectStatement</code>.
     *
     * @return The subject within the statement.
     */
    public Subject getSubject() {
        return _subject; 
    }       
    
    /**
     * Set the Subject for the <code>SubjectStatement</code>.
     *
     * @param subject A Subject object.
     * @return true if the operation is successful.
     */
    public boolean setSubject(Subject subject) {
        if (subject == null) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("SubjectStatement:setSubject:" + 
                                        "Input is null." );
            }
            return false; 
        }
        _subject = subject; 
        return true; 
    }
    
    /** 
     * Creates a String representation of the <code>SubjectStatement</code>.
     *
     * @param includeNS Determines whether or not the namespace qualifier 
     *        is prepended to the Element when converted.
     * @param declareNS Determines whether or not the namespace is declared 
     *        within the Element.
     * @return A String representation of the 
     *         <code>&lt;saml:SubjectStatement&gt;</code> element.
    */ 
    public abstract String toString(boolean includeNS, boolean declareNS); 
}

