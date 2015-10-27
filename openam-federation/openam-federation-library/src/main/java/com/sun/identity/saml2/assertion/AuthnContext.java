/*
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
 * $Id: AuthnContext.java,v 1.2 2008/06/25 05:47:40 qcheng Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */



package com.sun.identity.saml2.assertion;

import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sun.identity.saml2.assertion.impl.AuthnContextImpl;
import com.sun.identity.saml2.common.SAML2Exception;

/**
 * The <code>AuthnContext</code> element specifies the context of an
 * authentication event. The element can contain an authentication context
 * class reference, an authentication declaration or declaration reference,
 * or both. Its type is <code>AuthnContextType</code>.
 * <p>
 * <pre>
 * &lt;complexType name="AuthnContextType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;choice>
 *           &lt;sequence>
 *             &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *             AuthnContextClassRef"/>
 *             &lt;choice minOccurs="0">
 *               &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *               AuthnContextDecl"/>
 *               &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *               AuthnContextDeclRef"/>
 *             &lt;/choice>
 *           &lt;/sequence>
 *           &lt;choice>
 *             &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *             AuthnContextDecl"/>
 *             &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *             AuthnContextDeclRef"/>
 *           &lt;/choice>
 *         &lt;/choice>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *         AuthenticatingAuthority" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * @supported.all.api
 */
@JsonDeserialize(as=AuthnContextImpl.class)
public interface AuthnContext {

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
     * Returns the value of the <code>AuthnContextClassRef</code> property.
     *
     * @return the value of the <code>AuthnContextClassRef</code>.
     * @see #setAuthnContextClassRef(String)
     */
    public String getAuthnContextClassRef();

    /**
     * Sets the value of the <code>AuthnContextClassRef</code> property.
     *
     * @param value new <code>AuthenticationContextClassRef</code>.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAuthnContextClassRef()
     */
    public void setAuthnContextClassRef(String value)
        throws SAML2Exception;

    /**
     * Returns the value of the <code>AuthnContextDeclRef</code> property.
     *
     * @return A String representing authentication context
     *                 declaration reference.
     * @see #setAuthnContextDeclRef(String)
     */
    public String getAuthnContextDeclRef();

    /**
     * Sets the value of the <code>AuthnContextDeclRef</code> property.
     *
     * @param value A String representation of authentication context
     *                declaration reference.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAuthnContextDeclRef()
     */
    public void setAuthnContextDeclRef(String value)
        throws SAML2Exception;

    /**
     * Returns the value of the <code>AuthnContextDecl</code> property.
     *
     * @return An XML String representing authentication context declaration.
     * @see #setAuthnContextDecl(String)
     */
    public String getAuthnContextDecl();

    /**
     * Sets the value of the <code>AuthnContextDecl</code> property.
     *
     * @param value An xml String representing authentication context
     *                declaration.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAuthnContextDecl()
     */
    public void setAuthnContextDecl(String value)
        throws SAML2Exception;

    /**
     * Sets the value of the <code>AuthenticatingAuthority</code> property.
     *
     * @param value List of Strings representing authenticating authority
     * @throws SAML2Exception if the object is immutable.
     * @see #getAuthenticatingAuthority()
     */
    public void setAuthenticatingAuthority(List<String> value)
        throws SAML2Exception;

    /**
     * Returns the value of the <code>AuthenticatingAuthority</code> property.
     *
     * @return List of Strings representing
     *                <code>AuthenticatingAuthority</code>.
     * @see #setAuthenticatingAuthority(List)
     */
    public List<String> getAuthenticatingAuthority();

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
