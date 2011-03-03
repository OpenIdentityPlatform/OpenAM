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
 * $Id: AssertionIDReference.java,v 1.2 2008/06/25 05:47:31 qcheng Exp $
 *
 */


package com.sun.identity.saml.assertion;

import org.w3c.dom.*; 
import com.sun.identity.saml.common.*; 
import com.sun.identity.shared.xml.XMLUtils;

/**
 * <code>AssertionIDReference</code> element makes reference to a SAML
 * assertion.
 *
 * @supported.all.api
 */
public class AssertionIDReference {     
    protected String assertionID = null;
    
    /**
     * Constructs an <code>AssertionIDReference</code> element from an existing 
     * XML block.
     *
     * @param element representing a DOM tree element.
     * @exception SAMLException if there is an error in the sender or in the
     *            element definition.
     */
    public AssertionIDReference(Element element) throws SAMLException{
        // make sure that the input xml block is not null
        if (element == null) {
            SAMLUtilsCommon.debug.message(
            "AssertionIDReference: Input is null.");
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("nullInput"));
        }
        // Make sure this is as AssertionIDReference.
        String tag = null;
        tag = element.getLocalName(); 
        if ((tag == null) || (!tag.equals("AssertionIDReference"))) {
            SAMLUtilsCommon.debug.message("AssertionIDReference: wrong input");
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("wrongInput"));
        }
        assertionID = XMLUtils.getElementValue(element); 
        // check if the AssertionIDReference is null.
        if (assertionID == null || assertionID.length() == 0) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("AssertionIDReference is null.");
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("missingElementValue"));
        }
    }
    
    /**
     *Default constructor 
     */
    public AssertionIDReference() {
        assertionID = SAMLUtils.generateAssertionID(); 
    }
    
    /**
     *Constructs of <code>AssertionIDReference</code>.
     *@param id A String representing an existing assertion id.  
     */
    public AssertionIDReference(String id) {
        if (id == null || id.length() == 0) {
            assertionID = SAMLUtils.generateAssertionID(); 
        } else 
            assertionID = id; 
    }
    
    /**
     *Returns a String representing the Assertion id.  
     *@return A string representation of the assertion id.    
     */           
    public String getAssertionIDReference() {
        return assertionID; 
    }
    
    /**
     * Translates the <code>AssertionID</code> to an XML String,
     * @return A String representing the <code>AssertionID</code>
     */
    public String toString() {
        return (toString(true, false)); 
    }
   
    /**
     * Returns a String representation of the <code>AssertionIDReference</code> 
     * element.
     *
     * @param includeNS Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A string representation of the
     *         <code>&lt;saml:AssertionIDReference&gt;</code> element.
     */
    public String toString(boolean includeNS, boolean declareNS) {
        StringBuffer result = new StringBuffer(100);
        String prefix = "";
        String uri = "";
    
        if (includeNS) {
            prefix = SAMLConstants.ASSERTION_PREFIX;
        }
        if (declareNS) {
            uri = SAMLConstants.assertionDeclareStr;
        }
        result.append("<").append(prefix).append("AssertionIDReference").
               append(uri).append(">").append(assertionID).append("</").
               append(prefix).append("AssertionIDReference>\n");
        return (result.toString());
    }
}

