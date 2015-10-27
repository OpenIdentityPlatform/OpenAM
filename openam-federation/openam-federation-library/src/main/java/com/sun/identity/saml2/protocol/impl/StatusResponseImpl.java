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
 * $Id: StatusResponseImpl.java,v 1.4 2008/06/25 05:48:01 qcheng Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */
package com.sun.identity.saml2.protocol.impl;

import java.security.PublicKey;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;

import com.sun.identity.shared.DateUtils;
import com.sun.identity.saml.xmlsig.XMLSignatureException;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.Extensions;
import com.sun.identity.saml2.protocol.Status;
import com.sun.identity.saml2.protocol.StatusResponse;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Set;

import com.sun.identity.saml2.xmlsig.SigManager;
import org.w3c.dom.Element;
import com.sun.identity.shared.xml.XMLUtils;


/**
 * This class defines methods for setting and retrieving attributes and
 * elements associated with a SAML response message used in SAML protocols.
 * This class is the base class for all SAML Responses.
 */

public abstract class StatusResponseImpl implements StatusResponse {
    
    protected String version = null;
    protected Date issueInstant = null;
    protected String destination = null;
    protected String signatureString = null;
    protected Extensions extensions = null;
    protected String consent = null;
    protected String inResponseTo = null;
    protected Status status = null;
    protected String responseId = null;
    protected Issuer issuer = null;
    protected boolean isSigned = false;
    protected Boolean isSignatureValid = null;
    protected boolean isMutable = false;
    protected PublicKey publicKey = null;
    protected String  signedXMLString = null;

    /**
     * Returns the value of the version property.
     *
     * @return the value of the version property
     * @see #setVersion(String)
     */
    public java.lang.String getVersion() {
        return version;
    }
    
    /**
     * Sets the value of the version property.
     *
     * @param value the value of the version property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getVersion
     */
    public void setVersion(java.lang.String value) throws SAML2Exception {
        if (isMutable) {
            this.version = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the value of the issueInstant property.
     *
     * @return the value of the issueInstant property
     * @see #setIssueInstant(java.util.Date)
     */
    public java.util.Date getIssueInstant() {
        return issueInstant;
    }
    
    /**
     * Sets the value of the issueInstant property.
     *
     * @param value the value of the issueInstant property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getIssueInstant
     */
    public void setIssueInstant(java.util.Date value) throws SAML2Exception {
        if (isMutable) {
            this.issueInstant = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the value of the destination property.
     *
     * @return the value of the destination property
     * @see #setDestination(String)
     */
    public java.lang.String getDestination() {
        return destination;
    }
    
    /**
     * Sets the value of the destination property.
     *
     * @param value the value of the destination property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getDestination
     */
    public void setDestination(java.lang.String value) throws SAML2Exception {
        if (isMutable) {
            this.destination = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the signature element, the <code>StatusResponse</code> contains
     * as <code>String</code>.
     * It returns null if the <code>StatusResponse</code> has no signature.
     *
     * @return <code>String</code> representation of the signature.
     *
     */
    public String getSignature() {
        return signatureString;
    }
    
    /** Signs the StatusResponse
     *
     * @param privateKey Signing key
     * @param cert Certificate which contain the public key correlated to
     *             the signing key; It if is not null, then the signature
     *             will include the certificate; Otherwise, the signature
     *             will not include any certificate.
     * @throws SAML2Exception if it could not sign the Request.
     */
     public void sign(PrivateKey privateKey, X509Certificate cert)
        throws SAML2Exception  {
        Element signatureEle = SigManager.getSigInstance().sign(
            toXMLString(true, true),
            getID(),
            privateKey,
            cert
        );
        signatureString = XMLUtils.print(signatureEle);
        signedXMLString = XMLUtils.print(signatureEle.getOwnerDocument().
           getDocumentElement(), "UTF-8");
        isSigned =true;
        makeImmutable();
    }

    /**
     * Returns the value of the extensions property.
     *
     * @return the value of the extensions property
     * @see #setExtensions(Extensions)
     */
    public Extensions getExtensions() {
        return extensions;
    }
    
    /**
     * Sets the value of the extensions property.
     *
     * @param value the value of the extensions property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getExtensions
     */
    public void setExtensions(Extensions value) throws SAML2Exception {
        if (isMutable) {
            this.extensions = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the value of the consent property.
     *
     * @return the value of the consent property
     * @see #setConsent(String)
     */
    public java.lang.String getConsent() {
        return consent;
    }
    
    /**
     * Sets the value of the consent property.
     *
     * @param value the value of the consent property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getConsent
     */
    public void setConsent(java.lang.String value) throws SAML2Exception {
        if (isMutable) {
            this.consent = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the value of the inResponseTo property.
     *
     * @return the value of the inResponseTo property
     * @see #setInResponseTo(String)
     */
    public java.lang.String getInResponseTo() {
        return inResponseTo;
    }
    
    /**
     * Sets the value of the inResponseTo property.
     *
     * @param value the value of the inResponseTo property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getInResponseTo
     */
    public void setInResponseTo(java.lang.String value) throws SAML2Exception {
        if (isMutable) {
            this.inResponseTo = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the value of the status property.
     *
     * @return the value of the status property
     * @see #setStatus(Status)
     */
    public com.sun.identity.saml2.protocol.Status getStatus() {
        return status;
    }
    
    /**
     * Sets the value of the status property.
     *
     * @param value the value of the status property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getStatus
     */
    public void setStatus(com.sun.identity.saml2.protocol.Status value)
    throws SAML2Exception {
        if (isMutable) {
            this.status = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the value of the id property.
     *
     * @return the value of the id property
     * @see #setID(String)
     */
    public java.lang.String getID() {
        return responseId;
    }
    
    /**
     * Sets the value of the id property.
     *
     * @param value the value of the id property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getID
     */
    public void setID(java.lang.String value) throws SAML2Exception {
        if (isMutable) {
            this.responseId = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the value of the issuer property.
     *
     * @return the value of the issuer property
     * @see #setIssuer(Issuer)
     */
    public Issuer getIssuer() {
        return issuer;
    }
    
    /**
     * Sets the value of the issuer property.
     *
     * @param value the value of the issuer property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getIssuer
     */
    public void setIssuer(Issuer value)
    throws SAML2Exception {
        if (isMutable) {
            this.issuer = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns whether the <code>StatusResponse</code> is signed or not.
     *
     * @return true if the <code>StatusResponse</code> is signed.
     */
    public boolean isSigned() {
        return isSigned;
    }

    @Override
    public boolean isSignatureValid(Set<X509Certificate> verificationCerts)
        throws SAML2Exception { 	
        if (isSignatureValid == null) {
             isSignatureValid = SigManager.getSigInstance().verify(signedXMLString, getID(), verificationCerts);
         }
         return isSignatureValid.booleanValue();
    }   
    
    /**
     * Returns the <code>StatusResponse</code> in an XML document String format
     * based on the <code>StatusResponse</code> schema described above.
     *
     * @return An XML String representing the <code>StatusResponse</code>.
     * @throws SAML2Exception if some error occurs during conversion to
     *         <code>String</code>.
     */
    public String toXMLString() throws SAML2Exception {
        return toXMLString(true,false);
    }
    
    /**
     * Returns the <code>StatusResponse</code> in an XML document String format
     * based on the <code>StatusResponse</code> schema described above.
     *
     * @param includeNSPrefix Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A XML String representing the <code>StatusResponse</code>.
     * @throws SAML2Exception if some error occurs during conversion to
     *         <code>String</code>.
     */
    public String toXMLString(boolean includeNSPrefix,
    boolean declareNS) throws SAML2Exception {
        
        StringBuffer xmlString = new StringBuffer(1000);
        
        if (declareNS) {
            xmlString.append(SAML2Constants.PROTOCOL_DECLARE_STR)
            .append(SAML2Constants.NEWLINE);
            
        }
        
        xmlString.append(SAML2Constants.ID).append(SAML2Constants.EQUAL)
        .append(SAML2Constants.QUOTE)
        .append(responseId).append(SAML2Constants.QUOTE)
        .append(SAML2Constants.SPACE)
        .append(SAML2Constants.VERSION).append(SAML2Constants.EQUAL)
        .append(SAML2Constants.QUOTE)
        .append(version).append(SAML2Constants.QUOTE)
        .append(SAML2Constants.SPACE)
        .append(SAML2Constants.ISSUE_INSTANT)
        .append(SAML2Constants.EQUAL)
        .append(SAML2Constants.QUOTE)
        .append(DateUtils.toUTCDateFormat(issueInstant))
        .append(SAML2Constants.QUOTE);
        
        if ((destination != null) && (destination.length() > 0)) {
            xmlString.append(SAML2Constants.SPACE)
            .append(SAML2Constants.DESTINATION)
            .append(SAML2Constants.EQUAL)
            .append(SAML2Constants.QUOTE)
            .append(destination)
            .append(SAML2Constants.QUOTE);
        }
        
        if ((consent != null) && (consent.length() > 0)) {
            xmlString.append(SAML2Constants.SPACE)
            .append(SAML2Constants.CONSENT)
            .append(SAML2Constants.EQUAL)
            .append(SAML2Constants.QUOTE)
            .append(consent)
            .append(SAML2Constants.QUOTE);
        }
        
        if ((inResponseTo != null) && (inResponseTo.length() > 0)) {
            xmlString.append(SAML2Constants.SPACE)
            .append(SAML2Constants.INRESPONSETO)
            .append(SAML2Constants.EQUAL)
            .append(SAML2Constants.QUOTE)
            .append(inResponseTo)
            .append(SAML2Constants.QUOTE);
        }
        
        xmlString.append(SAML2Constants.END_TAG);
        
        if (issuer != null) {
            String issuerString = issuer.toXMLString(includeNSPrefix,declareNS);
            xmlString.append(SAML2Constants.NEWLINE).append(issuerString);
        }
        if ((signatureString != null) && (signatureString.length() > 0)) {
            xmlString.append(SAML2Constants.NEWLINE).append(signatureString);
        }
        if (extensions != null) {
            xmlString.append(SAML2Constants.NEWLINE)
            .append(extensions.toXMLString(includeNSPrefix,declareNS));
        }
        if (status != null) {
            xmlString.append(SAML2Constants.NEWLINE)
            .append(status.toXMLString(includeNSPrefix,declareNS));
        }
        
        return xmlString.toString();
    }
    
    /**
     * Makes this object immutable.
     */
    public void makeImmutable() {
        if (isMutable) {
            if ((issuer != null) && (issuer.isMutable())) {
                issuer.makeImmutable();
            }
            if ((extensions != null) && (extensions.isMutable())) {
                extensions.makeImmutable();
            }
            if ((status != null) && (status.isMutable())) {
                status.makeImmutable();
            }
            isMutable = false;
        }
    }
    
    /**
     * Returns true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable() {
        return isMutable;
    }
    
   /* Validates the responseId in the SAML Response. */
    protected void validateID(String responseId) throws SAML2Exception {
        if ((responseId == null) || (responseId.length() == 0 )) {
            SAML2SDKUtils.debug.message("ID is missing in the SAMLResponse");
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("missingIDAttr"));
        }
    }
    
    
   /* Validates the version in the SAML Response. */
    protected void validateVersion(String version) throws SAML2Exception {
        if ((version == null) || (version.length() == 0) ) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
            "missingVersion"));
        } else if (!version.equals(SAML2Constants.VERSION_2_0)) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
            "incorrectVersion"));
        }
    }
    
   /* Validates the IssueInstant attribute in the SAML Response. */
    protected void validateIssueInstant(String issueInstantStr)
    throws SAML2Exception {
        if ((issueInstantStr == null || issueInstantStr.length() == 0)) {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("missingIssueInstant"));
        } else {
            try {
                issueInstant = DateUtils.stringToDate(issueInstantStr);
            } catch (ParseException e) {
                SAML2SDKUtils.debug.message("Error parsing IssueInstant", e);
                throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("incorrectIssueInstant"));
            }
        }
    }
    
    /* Validates the Status element in the SAML Response. */
    protected void validateStatus()
    throws SAML2Exception {
        if (status == null) {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("missingStatus"));
        }
    }    
    
   /* Validates the required elements in the SAML Response. */
    protected void validateData() throws SAML2Exception {
        validateID(responseId);
        validateVersion(version);
        if (issueInstant == null) {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("incorrectIssueInstant"));
        }
        validateIssueInstant(DateUtils.dateToString(issueInstant));
        validateStatus();        
    }
}
