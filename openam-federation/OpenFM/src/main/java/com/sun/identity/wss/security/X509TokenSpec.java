/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: X509TokenSpec.java,v 1.3 2008/08/27 19:05:53 mrudul_uchil Exp $
 *
 */

package com.sun.identity.wss.security;


/**
 * This class defines the specification for generating the X509 security tokens.
 * It implements <code>SecurityTokenSpec</code> interface.
 * 
 * @supported.all.api
 */ 
public class X509TokenSpec implements SecurityTokenSpec {

    public static final String DIRECT_REFERENCE = "DirectReference";

    public static final String KEY_IDENTIFIER = "KeyIdentifier";

    public static final String ISSUER_SERIAL = "IssuerSerial";

    private String[] certAlias = null;
    String valueType = null;
    String encodingType = null;
    String referenceType = null;

    /**
     * Constructor that defines this specfication.
     *
     * @param certAlias the array of certificate aliases that can be used
     *        to create the X509 token specification. For example the PKI
     *        PathSecurity token would need ordered list of certificates.
     *
     * @param valueType the token value type. This is to indicate the 
     *        value of the certificate.
     *
     * @param encodingType the token encoding type of the certficates that
     *        are attached in the security header.
     */
    public X509TokenSpec(String[] certAlias, 
          String valueType, String encodingType) {

        this.certAlias = certAlias;
        this.valueType = valueType;
        this.encodingType = encodingType;
    }

    /**
     * Returns the array of certificate aliases defined in this spec.
     *
     * @return String[] the array of subject certificate aliases.
     */ 
    public String[] getSubjectCertAlias() {
        return certAlias;
    }

    /**
     * Returns the X509 token type.
     *
     * @return the X509 token type. 
     */
    public String getValueType() {
        return valueType;
    }

    /**
     * Returns the encoding type.
     *
     *@return the encoding type of the certificates used.
     */
    public String getEncodingType() {
        return encodingType;
    }

    /**
     * Returns the token reference type.
     *
     * @return the reference type for the x509 token.
     */
    public String getReferenceType() {
        return referenceType;
    }

    /**
     * Sets the token reference type.
     */
    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

}
