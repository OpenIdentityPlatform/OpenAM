/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: NameIDImplWithoutSPNameQualifier.java,v 1.2 2008/06/25 05:47:44 qcheng Exp $
 *
 */

package com.sun.identity.saml2.assertion.impl;

import java.security.Key;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.xmlenc.EncManager;

/**
 *  The <code>NameID</code> is used in various SAML assertion constructs
 *  such as <code>Subject</code> and <code>SubjectConfirmation</code>
 *  elements, and in various protocol messages.
 */
public class NameIDImplWithoutSPNameQualifier extends NameIDImpl 
    implements NameID {

    /** 
     * Default constructor
     */
    public NameIDImplWithoutSPNameQualifier() {
    }

    /**
     * This constructor is used to build <code>NameID</code> object from a
     * XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        a <code>NameID</code> object
     * @exception SAML2Exception if it could not process the XML string
     */
    public NameIDImplWithoutSPNameQualifier(String xml) throws SAML2Exception {
        super(xml);
    }

    /**
     * This constructor is used to build <code>NameID</code> object from a
     * block of existing XML that has already been built into a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>NameID</code> object
     * @exception SAML2Exception if it could not process the Element
     */
    public NameIDImplWithoutSPNameQualifier(Element element) 
        throws SAML2Exception 
    {
        super(element);
    }

   /*
    * Returns a String representation
    * @param includeNSPrefix Determines whether or not the namespace 
    *        qualifier is prepended to the Element when converted
    * @param declareNS Determines whether or not the namespace is 
    *        declared within the Element.
    * @return A String representation
    * @exception SAML2Exception if something is wrong during conversion
    */
    public String toXMLString(boolean includeNSPrefix, boolean declareNS)
        throws SAML2Exception 
    {
        SAML2SDKUtils.debug.message("NameIDImplWithoutSPNameQualifier.toXML");
        StringBuffer sb = new StringBuffer(2000);
        String NS = "";
        String appendNS = "";
        if (declareNS) {
            NS = SAML2Constants.ASSERTION_DECLARE_STR;
        }
        if (includeNSPrefix) {
            appendNS = SAML2Constants.ASSERTION_PREFIX;
        }
        sb.append("<").append(appendNS).append(NAME_ID_ELEMENT).append(NS);
        String nameQualifier = getNameQualifier();
        if ((nameQualifier != null)
            && (nameQualifier.trim().length() != 0)) {
            sb.append(" ").append(NAME_QUALIFIER_ATTR).append("=\"").
                append(nameQualifier).append("\"");
        } 

        // ignore SPNameQualifier

        String format = getFormat();
        if ((format != null) && (format.trim().length() != 0)) {
            sb.append(" ").append(FORMAT_ATTR).append("=\"").
                append(format).append("\"");
        } 
        String spProvidedID = getSPProvidedID();
        if ((spProvidedID != null)
            && (spProvidedID.trim().length() != 0)) {
            sb.append(" ").append(SP_PROVIDED_ID_ATTR).append("=\"").
                append(spProvidedID).append("\"");
        } 
        sb.append(">");
        String value = getValue();
        if ((value != null) && (value.trim().length() != 0)) {
            sb.append(value);
        } else {
            SAML2SDKUtils.debug.error(
                "NameIDImplWithoutSPNameQualifier.toXMLString(): " +
                "name identifier is missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_name_identifier"));
        } 
        sb.append("</").append(appendNS).append(NAME_ID_ELEMENT).
            append(">");
        return sb.toString();
    }
}
