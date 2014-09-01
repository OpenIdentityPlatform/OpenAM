/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CertException.java,v 1.2 2008/06/25 05:52:46 qcheng Exp $
 *
 */

package com.iplanet.security.x509;

/**
 * CertException indicates one of a variety of certificate problems.
 * 
 * @see java.security.cert.Certificate
 */
public class CertException extends SecurityException {

    // Zero is reserved.

    /** Indicates that the signature in the certificate is not valid. */
    public static final int verf_INVALID_SIG = 1;

    /** Indicates that the certificate was revoked, and so is invalid. */
    public static final int verf_INVALID_REVOKED = 2;

    /** Indicates that the certificate is not yet valid. */
    public static final int verf_INVALID_NOTBEFORE = 3;

    /** Indicates that the certificate has expired and so is not valid. */
    public static final int verf_INVALID_EXPIRED = 4;

    /**
     * Indicates that a certificate authority in the certification chain is not
     * trusted.
     */
    public static final int verf_CA_UNTRUSTED = 5;

    /** Indicates that the certification chain is too long. */
    public static final int verf_CHAIN_LENGTH = 6;

    /** Indicates an error parsing the ASN.1/DER encoding of the certificate. */
    public static final int verf_PARSE_ERROR = 7;

    /** Indicates an error constructing a certificate or certificate chain. */
    public static final int err_CONSTRUCTION = 8;

    /** Indicates a problem with the public key */
    public static final int err_INVALID_PUBLIC_KEY = 9;

    /** Indicates a problem with the certificate version */
    public static final int err_INVALID_VERSION = 10;

    /** Indicates a problem with the certificate format */
    public static final int err_INVALID_FORMAT = 11;

    /** Indicates a problem with the certificate encoding */
    public static final int err_ENCODING = 12;

    // Private data members
    private int verfCode;

    private String moreData;

    /**
     * Constructs a certificate exception using an error code 
     * (<code>verf_*</code>) and a string describing the context of the error.
     */
    public CertException(int code, String moredata) {
        verfCode = code;
        moreData = moredata;
    }

    /**
     * Constructs a certificate exception using just an error code, without a
     * string describing the context.
     */
    public CertException(int code) {
        verfCode = code;
    }

    /**
     * Returns the error code with which the exception was created.
     */
    public int getVerfCode() {
        return verfCode;
    }

    /**
     * Returns a string describing the context in which the exception was
     * reported.
     */
    public String getMoreData() {
        return moreData;
    }

    /**
     * Return a string corresponding to the error code used to create this
     * exception.
     */
    public String getVerfDescription() {
        switch (verfCode) {
        case verf_INVALID_SIG:
            return "The signature in the certificate is not valid.";
        case verf_INVALID_REVOKED:
            return "The certificate has been revoked.";
        case verf_INVALID_NOTBEFORE:
            return "The certificate is not yet valid.";
        case verf_INVALID_EXPIRED:
            return "The certificate has expired.";
        case verf_CA_UNTRUSTED:
            return "The Authority which issued the certificate is not trusted.";
        case verf_CHAIN_LENGTH:
            return "The certificate path to a trusted authority is too long.";
        case verf_PARSE_ERROR:
            return "The certificate could not be parsed.";
        case err_CONSTRUCTION:
            return "There was an error when constructing the certificate.";
        case err_INVALID_PUBLIC_KEY:
            return "The public key was not in the correct format.";
        case err_INVALID_VERSION:
            return "The certificate has an invalid version number.";
        case err_INVALID_FORMAT:
            return "The certificate has an invalid format.";
        case err_ENCODING:
            return "Problem encountered while encoding the data.";

        default:
            return "Unknown code:  " + verfCode;
        }
    }

    /**
     * Returns a string describing the certificate exception.
     */
    public String toString() {
        return "[Certificate Exception: " + getMessage() + "]";
    }

    /**
     * Returns a string describing the certificate exception.
     */
    public String getMessage() {
        return getVerfDescription()
                + ((moreData != null) ? ("\n  (" + moreData + ")") : "");
    }
}
