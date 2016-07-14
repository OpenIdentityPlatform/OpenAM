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
 * $Id: StatusResponse.java,v 1.2 2008/06/25 05:47:58 qcheng Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */
package com.sun.identity.saml2.protocol;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.impl.StatusResponseImpl;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Set;

/**
 * This class represents the <code>StatusResponseType</code> complex type in
 * SAML protocol schema.
 * All SAML responses are of types that are derived from the
 * <code>StatusResponseType</code> complex type. This type defines common
 * attributes and elements that are associated with all SAML responses.
 *
 * <pre>
 * &lt;complexType name="StatusResponseType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Issuer" minOccurs="0"/>
 *         &lt;element ref="{http://www.w3.org/2000/09/xmldsig#}Signature" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}Extensions" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}Status"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Consent" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="Destination" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="ID" use="required" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;attribute name="InResponseTo" type="{http://www.w3.org/2001/XMLSchema}NCName" />
 *       &lt;attribute name="IssueInstant" use="required" type="{http://www.w3.org/2001/XMLSchema}dateTime" />
 *       &lt;attribute name="Version" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * @supported.all.api
 */
@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS,
        defaultImpl = StatusResponseImpl.class)
public interface StatusResponse {
    
    /**
     * Returns the value of the version property.
     *
     * @return the value of the version property
     * @see #setVersion(String)
     */
    public java.lang.String getVersion();
    
    /**
     * Sets the value of the version property.
     *
     * @param value the value of the version property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getVersion
     */
    public void setVersion(java.lang.String value) throws SAML2Exception;
    
    /**
     * Returns the value of the issueInstant property.
     *
     * @return the value of the issueInstant property
     * @see #setIssueInstant(java.util.Date)
     */
    public java.util.Date getIssueInstant();
    
    /**
     * Sets the value of the issueInstant property.
     *
     * @param value the value of the issueInstant property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getIssueInstant
     */
    public void setIssueInstant(java.util.Date value) throws SAML2Exception;
    
    /**
     * Returns the value of the destination property.
     *
     * @return the value of the destination property
     * @see #setDestination(String)
     */
    public java.lang.String getDestination();
    
    /**
     * Sets the value of the destination property.
     *
     * @param value the value of the destination property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getDestination
     */
    public void setDestination(java.lang.String value) throws SAML2Exception;
    
    /**
     * Returns the signature element, the <code>StatusResponse</code> contains
     * as <code>String</code>.  A null value is returned if the 
     * <code>StatusResponse</code> has no signature.
     *
     * @return <code>String</code> representation of the signature.
     */
    public String getSignature();
    
    /**
     * Returns the value of the extensions property.
     *
     * @return the value of the extensions property
     * @see #setExtensions(Extensions)
     */
    public com.sun.identity.saml2.protocol.Extensions getExtensions();
    
    /**
     * Sets the value of the extensions property.
     *
     * @param value the value of the extensions property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getExtensions
     */
    public void setExtensions(com.sun.identity.saml2.protocol.Extensions value)
    throws SAML2Exception;
    
    /**
     * Returns the value of the consent property.
     *
     * @return the value of the consent property
     * @see #setConsent(String)
     */
    public java.lang.String getConsent();
    
    /**
     * Sets the value of the consent property.
     *
     * @param value the value of the consent property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getConsent
     */
    public void setConsent(java.lang.String value) throws SAML2Exception;
    
    /**
     * Returns the value of the inResponseTo property.
     *
     * @return the value of the inResponseTo property
     * @see #setInResponseTo(String)
     */
    public java.lang.String getInResponseTo();
    
    /**
     * Sets the value of the inResponseTo property.
     *
     * @param value the value of the inResponseTo property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getInResponseTo
     */
    public void setInResponseTo(java.lang.String value) throws SAML2Exception;
    
    /**
     * Returns the value of the status property.
     *
     * @return the value of the status property
     * @see #setStatus(Status)
     */
    public com.sun.identity.saml2.protocol.Status getStatus();
    
    /**
     * Sets the value of the status property.
     *
     * @param value the value of the status property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getStatus
     */
    public void setStatus(com.sun.identity.saml2.protocol.Status value)
    throws SAML2Exception;
    
    /**
     * Returns the value of the id property.
     *
     * @return the value of the id property
     * @see #setID(String)
     */
    public java.lang.String getID();
    
    /**
     * Sets the value of the id property.
     *
     * @param value the value of the id property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getID
     */
    public void setID(java.lang.String value) throws SAML2Exception;
    
    /**
     * Returns the value of the issuer property.
     *
     * @return the value of the issuer property
     * @see #setIssuer(Issuer)
     */
    public com.sun.identity.saml2.assertion.Issuer getIssuer();
    
    /**
     * Sets the value of the issuer property.
     *
     * @param value the value of the issuer property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getIssuer
     */
    public void setIssuer(com.sun.identity.saml2.assertion.Issuer value)
    throws SAML2Exception;
    
    /**
     * Returns whether the <code>StatusResponse</code> is signed or not.
     * @return true if the <code>StatusResponse</code> is signed
     *         false otherwise.
     */
    public boolean isSigned();
    
    /**
     * Returns whether the signature on the <code>StatusResponse</code>
     * is valid or not.
     *
     * @param verificationCerts Certificates containing the public keys which may be used for signature verification;
     *                          This certificate may also may be used to check against the certificate included in the
     *                          signature.
     * @return true if the signature is valid; false otherwise.
     * @throws SAML2Exception if the signature could not be verified
     */
    public boolean isSignatureValid(Set<X509Certificate> verificationCerts)
        throws SAML2Exception;
    
    /**
     * Signs the <code>StatusResponse</code>.
     *
     * @param privateKey Signing key
     * @param cert Certificate which contain the public key correlated to
     *             the signing key; It if is not null, then the signature
     *             will include the certificate; Otherwise, the signature
     *             will not include any certificate.
     * @throws SAML2Exception if it could not sign the StatusResponse.
     */
    public void sign(PrivateKey privateKey, X509Certificate cert)
        throws SAML2Exception; 
        
    /**
     * Returns the <code>StatusResponse</code> in an XML document String format
     * based on the <code>StatusResponse</code> schema described above.
     *
     * @return An XML String representing the <code>StatusResponse</code>.
     * @throws SAML2Exception if some error occurs during conversion to
     *         <code>String</code>.
     */
    public String toXMLString() throws SAML2Exception;
    
    /**
     * Returns the <code>StatusResponse</code> in an XML document String format
     * based on the <code>StatusResponse</code> schema described above.
     * @param includeNSPrefix Determines whether or not the namespace qualifier 
     * is prepended to the Element when converted
     *
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A XML String representing the <code>StatusResponse</code>.
     * @throws SAML2Exception if some error occurs during conversion to
     *         <code>String</code>.
     */
    public String toXMLString(boolean includeNSPrefix, boolean declareNS)
    throws SAML2Exception;
    
    /**
     * Makes the object immutable
     */
    public void makeImmutable();
    
    /**
     * Returns true if the object is mutable false otherwise
     *
     * @return true if the object is mutable false otherwise
     */
    public boolean isMutable();
}
