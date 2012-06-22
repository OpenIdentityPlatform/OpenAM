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
 * $Id: RequestAbstract.java,v 1.2 2008/06/25 05:47:57 qcheng Exp $
 *
 */


package com.sun.identity.saml2.protocol;

import com.sun.identity.saml.xmlsig.XMLSignatureException;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Exception;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.Signature;
import com.sun.identity.saml2.xmlsig.SigManager;
import java.util.Date;
import org.w3c.dom.Element;

/** 
 * This interface defines methods for setting and retrieving attributes and 
 * elements associated with a SAML request message used in SAML protocols.
 *
 * @supported.all.api
 */

public interface RequestAbstract {
    
    /** 
     * Sets the <code>Issuer</code> object.
     *
     * @param nameID the new <code>Issuer</code> object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getIssuer
     */
    public void setIssuer(Issuer nameID) throws SAML2Exception;
    
    /** 
     * Returns the <code>Issuer</code> Object.
     *
     * @return the <code>Issuer</code> object.
     * @see #setIssuer(Issuer)
     */
    public com.sun.identity.saml2.assertion.Issuer getIssuer();
    
    /** 
     * Returns the <code>Signature</code> Object as a string.
     *
     * @return the <code>Signature</code> object as a string.
     */
    public String getSignature();   
   
   /**
     * Signs the Request.
     *
     * @param privateKey Signing key
     * @param cert Certificate which contain the public key correlated to
     *             the signing key; It if is not null, then the signature
     *             will include the certificate; Otherwise, the signature
     *             will not include any certificate.
     * @throws SAML2Exception if it could not sign the Request.
     */
    public void sign(PrivateKey privateKey, X509Certificate cert)
        throws SAML2Exception; 
    
    /** 
     * Sets the <code>Extensions</code> Object.
     *
     * @param extensions the <code>Extensions</code> object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getExtensions
     */
    public void setExtensions(Extensions extensions) throws SAML2Exception;
    
    /** 
     * Returns the <code>Extensions</code> Object.
     *
     * @return the <code>Extensions</code> object.
     * @see #setExtensions(Extensions)
     */
    public Extensions getExtensions();
    
    /** 
     * Sets the value of the <code>ID</code> attribute.
     *
     * @param id the new value of <code>ID</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getID
     */
    public void setID(String id) throws SAML2Exception;
    
    /** 
     * Returns the value of the <code>ID</code> attribute.
     *
     * @return the value of <code>ID</code> attribute.
     * @see #setID(String)
     */
    public String getID();
    
    /** 
     * Sets the value of the <code>Version</code> attribute.
     *
     * @param version the value of <code>Version</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getVersion
     */
    public void setVersion(String version) throws SAML2Exception;
    
    /** 
     * Returns the value of the <code>Version</code> attribute.
     *
     * @return value of <code>Version</code> attribute.
     * @see #setVersion(String)
     */
    String getVersion();
    
    /** 
     * Sets the value of <code>IssueInstant</code> attribute.
     *
     * @param dateTime new value of the <code>IssueInstant</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getIssueInstant	 
     */
    public void setIssueInstant(Date dateTime) throws SAML2Exception;
    
    /** 
     * Returns the value of <code>IssueInstant</code> attribute.
     *
     * @return value of the <code>IssueInstant</code> attribute.
     * @see #setIssueInstant(Date)
     */
    public java.util.Date getIssueInstant();
    
    /** 
     * Sets the value of the <code>Destination</code> attribute.
     *
     * @param destinationURI new value of <code>Destination</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getDestination
     */
    public void setDestination(String destinationURI) throws SAML2Exception;
    
    /** 
     * Returns the value of the <code>Destination</code> attribute.
     *
     * @return  the value of <code>Destination</code> attribute.
     * @see #setDestination(String)
     */
    public String getDestination();
    
    /** 
     * Sets the value of the <code>Consent</code> attribute.
     *
     * @param consent new value of <code>Consent</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getConsent
     */
    public void setConsent(String consent) throws SAML2Exception;
    
    /** 
     * Returns the value of the <code>Consent</code> attribute.
     *
     * @return value of <code>Consent</code> attribute.
     * @see #setConsent(String)
     */
    public String getConsent();
    
    
    /** 
     * Returns true if message is signed.
     *
     * @return true if message is signed. 
     */
    
    public boolean isSigned();
    
    
    /**
     * Return whether the signature is valid or not.
     *
     * @param senderCert Certificate containing the public key
     *             which may be used for  signature verification;
     *             This certificate may also may be used to check
     *             against the certificate included in the signature
     * @return true if the signature is valid; false otherwise.
     * @throws SAML2Exception if the signature could not be verified
     */
    public boolean isSignatureValid(X509Certificate senderCert)
        throws SAML2Exception;
    
    /** 
     * Returns a String representation of this Object.
     *
     * @return a String representation of this Object.
     * @throws SAML2Exception if it could not create String object
     */
    public String toXMLString() throws SAML2Exception;
    
    /** 
     * Returns a String representation of this Object.
     *
     * @param includeNSPrefix determines whether or not the namespace
     *         qualifier is prepended to the Element when converted
     * @param declareNS determines whether or not the namespace is declared
     *         within the Element.
     * @throws SAML2Exception if it could not create String object.
     * @return a String representation of this Object.
     **/
    
    public String toXMLString(boolean includeNSPrefix,boolean declareNS)
	throws SAML2Exception;
    
        
    /** 
     * Makes this object immutable. 
     */
    public void makeImmutable() ;
    
    /** 
     * Returns true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable();
}
