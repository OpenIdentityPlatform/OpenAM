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
 * $Id: Evidence.java,v 1.2 2008/06/25 05:47:41 qcheng Exp $
 *
 */



package com.sun.identity.saml2.assertion;

import java.util.List;

import com.sun.identity.saml2.common.SAML2Exception;

/**
 * The <code>Evidence</code> element contains one or more assertions or
 * assertion references that the SAML authority relied on in issuing the
 * authorization decision. It has the <code>EvidenceType</code> complex type.
 * <p>
 * <pre>
 * &lt;complexType name="EvidenceType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *         AssertionIDRef"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *         AssertionURIRef"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Assertion"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *         EncryptedAssertion"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * @supported.all.api
 */
public interface Evidence {

    /**
     * Makes the object immutable.
     */
    public void makeImmutable();

    /**
     * Returns the mutability of the object.
     *
     * @return <code>true</code> if the object is mutable;
     *                 <code>false</code> otherwise.
     */
    public boolean isMutable();

    /**
     * Returns the <code>AssertionIDRef</code> in the element.
     *
     * @return List of Strings representing the <code>AssertionIDRef</code>s
     *                in the <code>Evidence</code>.
     * @see #setAssertionIDRef(List)
     */
    public List getAssertionIDRef();

    /**
     * Sets the <code>AssertionIDRef</code>(s) in the element.
     *
     * @param value List of Strings representing new
     *                <code>AssertionIDRef</code>(s).
     * @throws SAML2Exception if the object is immutable.
     * @see #getAssertionIDRef()
     */
    public void setAssertionIDRef(List value)
        throws SAML2Exception;

    /**
     * Returns the <code>AssertionURIRef</code>(s) in the element.
     *
     * @return List of Strings representing the <code>AssertionURIRef</code>(s)
     *                in the <code>Evidence</code>.
     * @see #setAssertionURIRef(List)
     */
    public List getAssertionURIRef();

    /**
     * Sets the <code>AssertionURIRef</code>(s) in the element.
     *
     * @param value List of Strings representing new
     *                <code>AssertionURIRef</code>(s).
     * @throws SAML2Exception if the object is immutable.
     * @see #getAssertionURIRef()
     */
    public void setAssertionURIRef(List value)
        throws SAML2Exception;

    /**
     * Returns the <code>Assertion</code>(s) in the element.
     *
     * @return List of <code>Assertion</code>(s) in the <code>Evidence</code>.
     * @see #setAssertion(List)
     */
    public List getAssertion();

    /**
     * Sets the <code>Assertion</code>(s) in the element.
     *
     * @param value List of new <code>Assertion</code>(s).
     * @throws SAML2Exception if the object is immutable.
     * @see #getAssertion()
     */
    public void setAssertion(List value)
        throws SAML2Exception;

    /**
     * Returns the <code>EncryptedAssertion</code>(s) in the element.
     *
     * @return List of <code>EncryptedAssertion</code>(s) in the
     *                <code>Evidence</code>.
     * @see #setEncryptedAssertion(List)
     */
    public List getEncryptedAssertion();

    /**
     * Sets the <code>EncryptedAssertion</code>(s) in the element.
     *
     * @param value List of new <code>EncryptedAssertion</code>(s).
     * @throws SAML2Exception if the object is immutable.
     * @see #getEncryptedAssertion()
     */
    public void setEncryptedAssertion(List value)
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
     * Returns a String representation of the element.
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

