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
 * $Id: QuerySignatureUtil.java,v 1.2 2008/06/25 05:47:45 qcheng Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */
package com.sun.identity.saml2.common;

import java.security.GeneralSecurityException;
import java.util.Set;
import java.util.StringTokenizer;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.InvalidKeyException;
import java.security.cert.X509Certificate;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.saml.common.SAMLConstants;
import org.apache.xml.security.Init;
import org.apache.xml.security.algorithms.JCEMapper;
import org.apache.xml.security.signature.XMLSignature;
import org.forgerock.openam.utils.StringUtils;

import java.security.NoSuchAlgorithmException;

/**
 * The <code>QuerySignatureUtil</code> provides methods to
 * sign query string and to verify signature on query string
 */
public class QuerySignatureUtil {

    private static final String SIGNATURE = "Signature";

    static {
        Init.init();
    }

    private QuerySignatureUtil() {
    }

    /**
     * Signs the query string.
     * @param queryString Query String
     * @param privateKey siging key
     * @return String signed query string
     * @exception SAML2Exception if the signing fails
     */
    public static String sign(String queryString, PrivateKey privateKey) throws SAML2Exception {
        
        String classMethod =
            "QuerySignatureUtil.sign: ";
        if (queryString == null ||
            queryString.length() == 0 ||
            privateKey == null) {
            SAML2Utils.debug.error(
                classMethod +
                "Either input query string or private key is null."
            );  
            throw new SAML2Exception( 
                SAML2Utils.bundle.getString("nullInput"));  
        }                                                 
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                classMethod +
                "Input query string:\n" +
                queryString);
        }

        final String querySigAlg;
        final String alg = privateKey.getAlgorithm();
        switch (alg) {
            case "RSA":
                //Defaulting to RSA-SHA1 for the sake of interoperability
                querySigAlg = SystemPropertiesManager.get(SAML2Constants.QUERY_SIGNATURE_ALGORITHM_RSA,
                        XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1);
                break;
            case "DSA":
                //Defaulting to SHA1WithDSA as JDK7 does not support SHA256WithDSA
                querySigAlg = SystemPropertiesManager.get(SAML2Constants.QUERY_SIGNATURE_ALGORITHM_DSA,
                        XMLSignature.ALGO_ID_SIGNATURE_DSA);
                break;
            case "EC":
                querySigAlg = SystemPropertiesManager.get(SAML2Constants.QUERY_SIGNATURE_ALGORITHM_EC,
                        XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA512);
                break;
            default:
                SAML2Utils.debug.error(classMethod + "Private Key algorithm not supported: " + alg);
                throw new SAML2Exception(SAML2Utils.bundle.getString("algorithmNotSupported"));
        }

        Signature sig;
        try {
            sig = Signature.getInstance(JCEMapper.translateURItoJCEID(querySigAlg));
        } catch (NoSuchAlgorithmException nsae) {
            throw new SAML2Exception(nsae);
        }

        if(queryString.charAt(queryString.length()-1)
           != '&'){
            queryString = queryString + "&";
        }
        queryString += SAML2Constants.SIG_ALG + "=" + URLEncDec.encode(querySigAlg);
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                classMethod +
                "Final string to be signed:\n" +
                queryString);
        }

        byte[] sigBytes;
        try {
            sig.initSign(privateKey);
            sig.update(queryString.getBytes());
            sigBytes = sig.sign();
        } catch (GeneralSecurityException gse) {
            throw new SAML2Exception(gse);
        }

        if (sigBytes == null ||
            sigBytes.length == 0) {
            SAML2Utils.debug.error(
                classMethod +
                "Generated signature is null");
            throw new SAML2Exception(
                SAML2Utils.bundle.getString(
                    "nullSigGenerated"
                )
            );
        }
        Base64 encoder = new Base64();
        String encodedSig = encoder.encode(sigBytes);
        queryString +=
            "&" + SAML2Constants.SIGNATURE + "=" +
            URLEncDec.encode(encodedSig);

        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                classMethod +
                "Signed query string:\n" +
                queryString);
        }
        return queryString;
    }
    
    /**
     * Verifies the query string signature.
     *
     * @param queryString Signed query String.
     * @param verificationCerts Verification certificates.
     * @return boolean whether the verification is successful or not.
     * @throws SAML2Exception if there is an error during verification.
     */
    public static boolean verify(
        String queryString, 
        Set<X509Certificate> verificationCerts
    ) throws SAML2Exception {
        
        String classMethod =
            "QuerySignatureUtil.verify: ";
        if (queryString == null ||
            queryString.length() == 0 || verificationCerts.isEmpty()) {
            
            SAML2Utils.debug.error(
                classMethod +
                "Input query string or certificate is null");       
            throw new SAML2Exception( 
                SAML2Utils.bundle.getString("nullInput"));
        }
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                classMethod +
                "Query string to be verifed:\n" + queryString);
        }
        StringTokenizer st = new
            StringTokenizer(queryString, "&");
        String token = null;
        String samlReq = null;
        String samlRes = null;
        String relay = null;
        String sigAlg = null;
        String encSig = null;
        while (st.hasMoreTokens()) {
            token = st.nextToken();
            if (token.startsWith(SAML2Constants.SAML_REQUEST)) {
                samlReq=token;
            } else if (token.startsWith(SAML2Constants.SAML_RESPONSE)) {
                samlRes=token;
            } else if (token.startsWith(SAML2Constants.RELAY_STATE)) {
                relay=token;
            } else if (token.startsWith(SAML2Constants.SIG_ALG)) {
                sigAlg = token;
            } else if (token.startsWith(SAML2Constants.SIGNATURE)) {
                encSig = token;
            }
        }
        if (sigAlg == null || sigAlg.equals("")) {
            SAML2Utils.debug.error(
                classMethod +
                "Null SigAlg query parameter.");
            throw new SAML2Exception( 
                SAML2Utils.bundle.getString("nullSigAlg"));         
        }
        if (encSig == null || encSig.equals("")) {
            SAML2Utils.debug.error(
                classMethod +
                "Null Signature query parameter.");
            throw new SAML2Exception( 
                SAML2Utils.bundle.getString("nullSig"));            
        }       
        // The following manipulation is necessary because
        // other implementations could send the query
        // parameters out of order, i.e., not in the same
        // order when signature is produced
        String newQueryString = null;
        if (samlReq != null) {
            newQueryString = samlReq;
        } else {
            newQueryString = samlRes;
        }
        if (relay != null) {
            newQueryString += "&"+relay;
        }
        newQueryString += "&"+sigAlg;
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                classMethod+
                "Query string to be verifed (re-arranged):\n" +
                newQueryString);
        }
        int sigAlgValueIndex = sigAlg.indexOf('=');
        String sigAlgValue =
            sigAlg.substring(sigAlgValueIndex+1);
        if (sigAlgValue == null || sigAlgValue.equals("")) {
            SAML2Utils.debug.error(
                classMethod +
                "Null SigAlg query parameter value.");
            throw new SAML2Exception( 
                SAML2Utils.bundle.getString("nullSigAlg"));         
        }
        sigAlgValue = URLEncDec.decode(sigAlgValue);
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                classMethod +
                "SigAlg query parameter value: " +
                sigAlgValue);
        }
        int encSigValueIndex = encSig.indexOf('=');
        String encSigValue =
            encSig.substring(encSigValueIndex+1);
        if (encSigValue == null || encSigValue.equals("")) {
            SAML2Utils.debug.message(
                classMethod +
                "Null Signature query parameter value.");
            throw new SAML2Exception( 
                SAML2Utils.bundle.getString("nullSig"));            
        }
        encSigValue = URLEncDec.decode(encSigValue);
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                classMethod +
                "Signature query parameter value:\n" +
                encSigValue);
        }
        // base-64 decode the signature value
        byte[] signature = null;
        Base64 decoder = new Base64();
        signature = decoder.decode(encSigValue);

        // get Signature instance based on algorithm
        if (!SIGNATURE.equals(JCEMapper.getAlgorithmClassFromURI(sigAlgValue))) {
            SAML2Utils.debug.error(classMethod + "Signature algorithm " + sigAlgValue + " is not supported.");
            throw new SAML2Exception(SAML2Utils.bundle.getString("algNotSupported"));
        }

        Signature sig;
        try {
            sig = Signature.getInstance(JCEMapper.translateURItoJCEID(sigAlgValue));
        } catch (NoSuchAlgorithmException nsae) {
            throw new SAML2Exception(nsae);
        }
        return isValidSignature(sig, verificationCerts, newQueryString.getBytes(), signature);
    }

    private static boolean isValidSignature(Signature sig, Set<X509Certificate> certificates, byte[] queryString,
            byte[] signature) throws SAML2Exception {
        final String classMethod = "QuerySignatureUtil.isValidSignature: ";
        Exception firstException = null;
        for (X509Certificate certificate : certificates) {
            try {
                sig.initVerify(certificate);
                sig.update(queryString);
                if (sig.verify(signature)) {
                    return true;
                }
            } catch (InvalidKeyException | SignatureException ex) {
                SAML2Utils.debug.warning(classMethod + "Signature validation failed due to " + ex);
                if (firstException == null) {
                    firstException = ex;
                }
            }
        }
        if (firstException != null) {
            throw new SAML2Exception(firstException);
        }

        return false;
    }
}
