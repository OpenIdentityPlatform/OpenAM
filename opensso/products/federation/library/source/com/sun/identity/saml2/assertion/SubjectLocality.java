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
 * $Id: SubjectLocality.java,v 1.2 2008/06/25 05:47:42 qcheng Exp $
 *
 */



package com.sun.identity.saml2.assertion;

import com.sun.identity.saml2.common.SAML2Exception;

/**
 * The <code>SubjectLocality</code> element specifies the DNS domain name
 * and IP address for the system entity that performed the authentication.
 * It exists as part of <code>AuthenticationStatement</code> element.
 * <p>
 * <pre>
 * &lt;complexType name="SubjectLocalityType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="Address" 
 *       type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="DNSName"
 *       type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * @supported.all.api
 */
public interface SubjectLocality {
    
    /**
     * Makes the object immutable.
     */
    public void makeImmutable();

    /**
     * Returns the mutability of the object.
     *
     * @return <code>true</code> if the object is mutable; 
     *                <code>false</code> otherwise.
     */
    public boolean isMutable();

    /**
     * Returns the value of the <code>DNSName</code> attribute.
          *
     * @return the value of the <code>DNSName</code> attribute.
     * @see #setDNSName(String)
     */
    public String getDNSName();

    /**
     * Sets the value of the <code>DNSName</code> attribute.
     *
     * @param value new value of the <code>DNSName</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getDNSName()
     */
    public void setDNSName(String value)
        throws SAML2Exception;

    /**
     * Returns the value of the <code>Address</code> attribute.
     *
     * @return the value of the <code>Address</code> attribute.
     * @see #setAddress(String)
     */
    public String getAddress();

    /**
     * Sets the value of the <code>Address</code> attribute.
     *
     * @param value new value of <code>Address</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAddress()
     */
    public void setAddress(String value)
        throws SAML2Exception;

    /**
     * Returns a String representation of the element.
     *
     * @return A string containing the valid XML for this element.
     *         By default name space name is prepended to the element name.
     * @throws SAML2Exception if the object does not conform to the schema.
     */
    public String toXMLString()
        throws SAML2Exception;

    /**
     * Returns a String representation of the
     * <code>SubjectLocality</code> element.
     *
     * @param includeNS Determines whether or not the namespace qualifier is
     *                prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *                within the Element.
     * @return A string containing the valid XML for this element
     * @throws SAML2Exception if the object does not conform to the schema.
     */
    public String toXMLString(boolean includeNS, boolean declareNS)
        throws SAML2Exception;
}
