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
 * $Id: LogoutRequest.java,v 1.2 2008/06/25 05:47:56 qcheng Exp $
 *
 */


package com.sun.identity.saml2.protocol;

import com.sun.identity.saml2.common.SAML2Exception;

/**
 * This class represents the <code>LogoutRequest</code> element in
 * SAML protocol schema.
 * A session participant or session authority sends a <code>LogoutRequest</code>
 * message to indicate that a session has been terminated.
 *
 * <pre>
 * &lt;element name="LogoutRequest" type="{urn:oasis:names:tc:SAML:2.0:protocol}LogoutRequestType"/>
 * </pre>
 *
 * <pre>
 * &lt;complexType name="LogoutRequestType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:protocol}RequestAbstractType">
 *       &lt;sequence>
 *         &lt;choice>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}BaseID"/>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}NameID"/>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}EncryptedID"/>
 *         &lt;/choice>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}SessionIndex" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="NotOnOrAfter" type="{http://www.w3.org/2001/XMLSchema}dateTime" />
 *       &lt;attribute name="Reason" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * @supported.all.api
 */
public interface LogoutRequest
extends com.sun.identity.saml2.protocol.RequestAbstract {
    
    /**
     * Returns the value of the notOnOrAfter property.
     *
     * @return <code>java.util.Date</code> value of the notOnOrAfter property
     * @see #setNotOnOrAfter(Date)
     */
    public java.util.Date getNotOnOrAfter();
    
    /**
     * Sets the value of the notOnOrAfter property.
     *
     * @param value <code>java.util.Date</code> value of the notOnOrAfter 
     * property to be set
     *
     * @throws SAML2Exception if the object is immutable
     * @see #getNotOnOrAfter
     */
    public void setNotOnOrAfter(java.util.Date value) throws SAML2Exception;
    
    /**
     * Returns the value of the reason property.
     *
     * @return <code>String</code> value of the reason property
     * @see #setReason(String)
     */
    public java.lang.String getReason();
    
    /**
     * Sets the value of the reason property.
     *
     * @param value <code>String</code> value of the reason property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getReason
     */
    public void setReason(java.lang.String value) throws SAML2Exception;
    
    /**
     * Returns the value of the encryptedID property.
     *
     * @return the value of the encryptedID property
     * @see #setEncryptedID(EncryptedID)
     */
    public com.sun.identity.saml2.assertion.EncryptedID getEncryptedID();
    
    /**
     * Sets the value of the encryptedID property.
     *
     * @param value the value of the encryptedID property to be set
     * @throws SAML2Exception if the object is immutable
     *
     * @see #getEncryptedID
     */
    public void setEncryptedID(
    com.sun.identity.saml2.assertion.EncryptedID value)
    throws SAML2Exception;
    
    /**
     * Returns the value of the nameID property.
     *
     * @return the value of the nameID property
     * @see #setNameID(NameID)
     */
    public com.sun.identity.saml2.assertion.NameID getNameID();
    
    /**
     * Sets the value of the nameID property.
     *
     * @param value the value of the nameID property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getNameID
     */
    public void setNameID(com.sun.identity.saml2.assertion.NameID value)
    throws SAML2Exception;
    
    /**
     * Returns the value of the baseID property.
     *
     * @return the value of the baseID property
     * @see #setBaseID(BaseID)
     */
    public com.sun.identity.saml2.assertion.BaseID getBaseID();
    
    /**
     * Sets the value of the baseID property.
     *
     * @param value the value of the baseID property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getBaseID
     */
    public void setBaseID(com.sun.identity.saml2.assertion.BaseID value)
    throws SAML2Exception;
    
    /**
     * Returns the value of the SessionIndex property.
     *
     * @return list containing objects of type <code>java.lang.String</code>
     * @see #setSessionIndex(List)
     */
    public java.util.List getSessionIndex();
    
    /**
     * Sets the value of the SessionIndex property.
     *
     * @param sessionIndexList list containing objects of 
     *        type <code>java.lang.String</code>
     * @throws SAML2Exception if the object is immutable
     * @see #getSessionIndex
     */
    public void setSessionIndex(java.util.List sessionIndexList)
    throws SAML2Exception;
}
