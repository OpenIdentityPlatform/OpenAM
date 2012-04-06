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
 * $Id: DoNotCacheCondition.java,v 1.3 2008/06/25 05:47:32 qcheng Exp $
 *
 */

package com.sun.identity.saml.assertion;

import com.sun.identity.saml.common.SAMLUtilsCommon;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;
import org.w3c.dom.Element;

/**
 *This is an implementation of the abstract <code>Condition</code> class, which
 *specifes that the assertion this <code>DoNotCacheCondition</code> is part of, 
 *is the new element in SAML 1.1, that allows an assertion party to express  
 *that an assertion should not be cached by the relying party for future use. 
 *In another word, such an assertion is meant only for "one-time" use by the
 *relying party.
 *
 * @supported.all.api
 */
public class DoNotCacheCondition extends Condition {
    private SAMLConstants sc;

    /**
     * Constructs a new <code>DoNotCacheCondition</code>.
     *
     */
    public DoNotCacheCondition() {
    } 

    /**
     * Constructs a <code>DoNotCacheCondition</code> element from 
     * an existing XML block.
     *
     * @param doNotCacheConditionElement A
     *        <code>org.w3c.dom.Element</code> representing DOM tree
     *        for <code>DoNotCacheCondition</code> object.
     * @exception SAMLException if it could not process the 
     *            <code>org.w3c.dom.Element</code> properly, implying that
     *            there is an error in the sender or in the element definition.
     */
    public DoNotCacheCondition(
        org.w3c.dom.Element doNotCacheConditionElement)  
        throws SAMLException
    {
        Element elt = (Element)doNotCacheConditionElement;
        String eltName = elt.getLocalName();
        if (eltName == null)  {
            if (SAMLUtilsCommon.debug.messageEnabled())  { 
                SAMLUtilsCommon.debug.message("DoNotCacheCondition: "
                    + "null condition ");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "nullInput"));
        }
        if (!(eltName.equals("DoNotCacheCondition"))) {
            if (!(eltName.equals("Condition"))) {
                if (SAMLUtilsCommon.debug.messageEnabled())  { 
                    SAMLUtilsCommon.debug.message("DoNotCacheCondition: "
                        + "unsupported condition ");
                }
                throw new SAMLRequesterException(
                    SAMLUtilsCommon.bundle.getString(
                    "unsupportedCondition"));
            }
        }
        if (eltName.equals("Condition")) { // seems like extension type
            String type = elt.getAttribute("xsi:type");
            if (!(type.equals("DoNotCacheCondition"))) {
                if (SAMLUtilsCommon.debug.messageEnabled()) {
                    SAMLUtilsCommon.debug.message(
                        "DoNotCacheCondition: invalid condition");
                }
                throw new SAMLRequesterException(
                    SAMLUtilsCommon.bundle.getString("invalidElement"));
            }
        } 
    }       
     
    /**
     * Creates a String representation of the element.
     *
     * @return A string containing the valid XML for this element
     * By default name space name is prepended to the element name 
     * example <code>&lt;saml:DoNotCacheCondition&gt;</code>.
     */
    public java.lang.String toString() {
        // call toString() with includeNS true by default and declareNS false
        String xml = this.toString(true, false);
        return xml;
    }

    /**  
     * Returns a String representation of the
     * <code>&lt;DoNotCacheCondition&gt;</code> element.
     *
     * @param includeNS Determines whether or not the namespace qualifier is 
     *        prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A string containing the valid XML for this element
     */                       
    public java.lang.String toString(boolean includeNS, boolean declareNS) {
        StringBuffer xml = new StringBuffer(300);
        String o = SAMLUtilsCommon.makeStartElementTagXML(
                        "DoNotCacheCondition", includeNS, declareNS);
        xml.append(o);
        
        o = SAMLUtilsCommon.makeEndElementTagXML(
                "DoNotCacheCondition",includeNS);
        xml.append(o);
        return xml.toString();
    }

    /**
     * Evaluates the Conditions
     * A method which can be overridden by a plug-in maybe which provides
     * means of evaluating this condition
     *
     * @return evaluation state.
     */
    public int  evaluate() {
        return Condition.INDETERMINATE;
    }

}

