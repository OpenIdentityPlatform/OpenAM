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
 * $Id: Assertion.java,v 1.2 2008/06/25 05:47:39 qcheng Exp $
 *
 */


package com.sun.identity.saml2.assertion;

import java.util.Date;
import java.util.List;
import java.security.Key;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import com.sun.identity.saml2.common.SAML2Exception;

/**
 * The <code>Assertion</code> element is a package of information
 * that supplies one or more <code>Statement</code> made by an issuer. 
 * There are three kinds of assertions: Authentication, Authorization Decision,
 * and Attribute assertions.
 * @supported.all.api
 */

public interface Assertion {

    /**
     * Returns the version number of the assertion.
     *
     * @return The version number of the assertion.
     */
    public String getVersion();

    /**
     * Sets the version number of the assertion.
     *
     * @param version the version number.
     * @exception SAML2Exception if the object is immutable
     */
    public void setVersion(String version) throws SAML2Exception;

    /**
     * Returns the time when the assertion was issued
     *
     * @return the time of the assertion issued
     */
    public Date getIssueInstant();

    /**
     * Sets the time when the assertion was issued
     *
     * @param issueInstant the issue time of the assertion
     * @exception SAML2Exception if the object is immutable
    */
    public void setIssueInstant(Date issueInstant) throws SAML2Exception;

    /**
     * Returns the subject of the assertion
     *
     * @return the subject of the assertion
     */
    public Subject getSubject();

    /**
     * Sets the subject of the assertion
     *
     * @param subject the subject of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    public void setSubject(Subject subject) throws SAML2Exception;

    /**
     * Returns the advice of the assertion
     *
     * @return the advice of the assertion
     */
    public Advice getAdvice();

    /**
     * Sets the advice of the assertion
     *
     * @param advice the advice of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    public void setAdvice(Advice advice) throws SAML2Exception;

    /**
     * Returns the signature of the assertion
     *
     * @return the signature of the assertion
     */
    public String getSignature();

    /**
     * Returns the conditions of the assertion
     *
     * @return the conditions of the assertion
     */
    public Conditions getConditions();

    /**
     * Sets the conditions of the assertion
     *
     * @param conditions the conditions of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    public void setConditions(Conditions conditions) throws SAML2Exception;

    /**
     * Returns the id of the assertion
     *
     * @return the id of the assertion
     */
    public String getID();

    /**
     * Sets the id of the assertion
     *
     * @param id the id of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    public void setID(String id) throws SAML2Exception;

    /**
     * Returns the statements of the assertion
     *
     * @return the statements of the assertion
     */
    public List getStatements();

    /**
     * Returns the <code>AuthnStatements</code> of the assertion
     *
     * @return the <code>AuthnStatements</code> of the assertion
     */
    public List getAuthnStatements();

    /**
     * Returns the <code>AuthzDecisionStatements</code> of the assertion
     *
     * @return the <code>AuthzDecisionStatements</code> of the assertion
     */
    public List getAuthzDecisionStatements();

    /**
     * Returns the attribute statements of the assertion
     *
     * @return the attribute statements of the assertion
     */
    public List getAttributeStatements();

    /**
     * Sets the statements of the assertion
     *
     * @param statements the statements of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    public void setStatements(List statements) throws SAML2Exception;

    /**
     * Sets the <code>AuthnStatements</code> of the assertion
     *
     * @param statements the <code>AuthnStatements</code> of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    public void setAuthnStatements(List statements) throws SAML2Exception;

    /**
     * Sets the <code>AuthzDecisionStatements</code> of the assertion
     *
     * @param statements the <code>AuthzDecisionStatements</code> of 
     *        the assertion
     * @exception SAML2Exception if the object is immutable
     */
    public void setAuthzDecisionStatements(List statements)
        throws SAML2Exception;

    /**
     * Sets the attribute statements of the assertion
     *
     * @param statements the attribute statements of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    public void setAttributeStatements(List statements) throws SAML2Exception;

    /**
     * Returns the issuer of the assertion
     *
     * @return the issuer of the assertion
     */
    public Issuer getIssuer();

    /**
     * Sets the issuer of the assertion
     *
     * @param issuer the issuer of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    public void setIssuer(Issuer issuer) throws SAML2Exception;

    /**
     * Return true if the assertion is signed 
     *
     * @return true if the assertion is signed
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
     * Gets the validity of the assertion evaluating its conditions if
     * specified.
     *
     * @return false if conditions is invalid based on it lying between
     *         <code>NotBefore</code> (current time inclusive) and
     *         <code>NotOnOrAfter</code> (current time exclusive) values 
     *         and true otherwise or if no conditions specified.
     */
    public boolean isTimeValid();

    /**
     * Signs the Assertion.
     *
     * @param privateKey Signing key
     * @param cert Certificate which contain the public key correlated to
     *             the signing key; It if is not null, then the signature
     *             will include the certificate; Otherwise, the signature
     *             will not include any certificate
     * @exception SAML2Exception if it could not sign the assertion.
     */
    public void sign(
        PrivateKey privateKey,
        X509Certificate cert
    ) throws SAML2Exception;

    /**
     * Returns an <code>EncryptedAssertion</code> object.
     *
     * @param recipientPublicKey Public key used to encrypt the data encryption
     *                           (secret) key, it is the public key of the
     *                           recipient of the XML document to be encrypted.
     * @param dataEncAlgorithm Data encryption algorithm.
     * @param dataEncStrength Data encryption strength.
     * @param recipientEntityID Unique identifier of the recipient, it is used
     *                          as the index to the cached secret key so that
     *                          the key can be reused for the same recipient;
     *                          It can be null in which case the secret key will
     *                          be generated every time and will not be cached
     *                          and reused. Note that the generation of a secret
     *                          key is a relatively expensive operation.
     * @return <code>EncryptedAssertion</code> object
     * @throws SAML2Exception if error occurs during the encryption process.
     */
    public EncryptedAssertion encrypt(
        Key recipientPublicKey,
        String dataEncAlgorithm,
        int dataEncStrength,
        String recipientEntityID
    ) throws SAML2Exception;

   /**
    * Returns a String representation
    * @param includeNSPrefix Determines whether or not the namespace qualifier
    *        is prepended to the Element when converted
    * @param declareNS Determines whether or not the namespace is declared
    *        within the Element.
    * @return A String representation
    * @exception SAML2Exception if something is wrong during conversion
     */
    public String toXMLString(boolean includeNSPrefix, boolean declareNS)
     throws SAML2Exception;

   /**
    * Returns a String representation
    *
    * @return A String representation
    * @exception SAML2Exception if something is wrong during conversion
    */
    public String toXMLString() throws SAML2Exception;

   /**
    * Makes the object immutable
    */
    public void makeImmutable();

   /**
    * Returns true if the object is mutable
    *
    * @return true if the object is mutable
    */
    public boolean isMutable();

}
