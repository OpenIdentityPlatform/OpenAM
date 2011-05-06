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
 * $Id: EncryptedElementImpl.java,v 1.2 2008/06/25 05:47:43 qcheng Exp $
 *
 */



package com.sun.identity.saml2.assertion.impl;

import com.sun.identity.saml2.assertion.EncryptedElement;
import com.sun.identity.saml2.common.SAML2Exception;

/**
 * Java content class for EncryptedElementType complex type.
 * <p>The following schema fragment specifies the expected
 *         content contained within this java content object.
 * <p>
 * <pre>
 * &lt;complexType name="EncryptedElementType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.w3.org/2001/04/xmlenc#}EncryptedData"/>
 *         &lt;element ref="{http://www.w3.org/2001/04/xmlenc#}EncryptedKey"
 *         maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 */
public abstract class EncryptedElementImpl implements  EncryptedElement {
    protected String xmlString = null;

    /**
     * Returns a String representation of the element.
     *
     * @return A string containing the valid XML for this element.
     *          By default name space name is prepended to the element name.
     * @throws SAML2Exception if the object does not conform to the schema.
     */
    public String toXMLString()
        throws SAML2Exception
    {
            return xmlString;
    }

    /**
     * Returns a String representation of the element.
     *
     * @param includeNS Determines whether or not the namespace qualifier is
     *          prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *          within the Element.
     * @return A string containing the valid XML for this element
     * @throws SAML2Exception if the object does not conform to the schema.
     */
    public String toXMLString(boolean includeNS, boolean declareNS)
        throws SAML2Exception
    {
            return xmlString;
    }

}
