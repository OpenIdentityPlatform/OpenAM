/* The contents of this file are subject to the terms
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
 * $Id: PPIDHelper.java,v 1.1 2008/03/27 17:09:55 mrudul_uchil Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyright (c) 2006, Chuck Mortimore - xmldap.org
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the names xmldap, xmldap.org, xmldap.com nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.sun.identity.cardfactory;

import org.bouncycastle.asn1.x509.X509Name;
import org.xmldap.exceptions.TokenIssuanceException;
import org.xmldap.util.Base64;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.util.Vector;
import javax.security.auth.x500.X500Principal;

public abstract class PPIDHelper {

    private static boolean isExtendedEvaluationCert(X509Certificate relyingpartyCert) {
        return false;
    }

    private static byte[] rpIdentifier(
            X509Certificate relyingpartyCert,
            X509Certificate[] chain)
            throws TokenIssuanceException {
        if (isExtendedEvaluationCert(relyingpartyCert)) {
            return rpIdentifierEV(relyingpartyCert);
        } else {
            return rpIdentifierNonEV(relyingpartyCert, chain);
        }
    }

    private static String orgIdString(X509Certificate relyingpartyCert)
            throws TokenIssuanceException {
        X500Principal principal = relyingpartyCert.getSubjectX500Principal();
        String dn = principal.getName();
        if (dn == null) {
            PublicKey publicKey = relyingpartyCert.getPublicKey();
            return new String(publicKey.getEncoded());
        }
        X509Name x509Name = new X509Name(dn);
        Vector oids = x509Name.getOIDs();
        Vector values = x509Name.getValues();
        int index = 0;
        StringBuffer orgIdStringBuffer = new StringBuffer("|");
        for (Object oid : oids) {
            if ("O".equals(oid)) {
                String value = (String) values.get(index);
                if (value == null) {
                    orgIdStringBuffer.append("O=\"\"|");
                } else {
                    orgIdStringBuffer.append("O=\"" + value + "\"|");
                }
            } else if ("L".equals(oid)) {
                String value = (String) values.get(index);
                if (value == null) {
                    orgIdStringBuffer.append("L=\"\"|");
                } else {
                    orgIdStringBuffer.append("L=\"" + value + "\"|");
                }
            } else if ("S".equals(oid)) {
                String value = (String) values.get(index);
                if (value == null) {
                    orgIdStringBuffer.append("S=\"\"|");
                } else {
                    orgIdStringBuffer.append("S=\"" + value + "\"|");
                }
            } else if ("C".equals(oid)) {
                String value = (String) values.get(index);
                if (value == null) {
                    orgIdStringBuffer.append("C=\"\"|");
                } else {
                    orgIdStringBuffer.append("C=\"" + value + "\"|");
                }
            } else {
                System.out.println("unused oid (" + oid + "). Value=" + (String) values.get(index));
            }
            index += 1;
        }
        if (orgIdStringBuffer.length() == 1) { // none of OLSC were found
            PublicKey publicKey = relyingpartyCert.getPublicKey();
            return new String(publicKey.getEncoded());
        }
        return orgIdStringBuffer.toString();
    }

    private static byte[] rpIdentifierNonEV(
            X509Certificate relyingpartyCert,
            X509Certificate[] chain)
            throws TokenIssuanceException {
        String orgIdString = orgIdString(relyingpartyCert);

        String qualifiedOrgIdString = qualifiedOrgIdString(chain, orgIdString);
        try {
            byte[] qualifiedOrgIdBytes = qualifiedOrgIdString.getBytes("UTF-8");
            byte[] rpIdentifier = sha256(qualifiedOrgIdBytes);
            return rpIdentifier;
        } catch (UnsupportedEncodingException e) {
            throw new TokenIssuanceException(e);
        }
    }

    /**
     * @param chain
     * @param orgIdString
     */
    private static String qualifiedOrgIdString(X509Certificate[] chain, String orgIdString) {
        StringBuffer qualifiedOrgIdString = new StringBuffer();
        for (int i = chain.length; i < 0; i++) {
            X509Certificate parent = chain[i];
            X500Principal parentPrincipal = parent.getSubjectX500Principal();
            String subjectDN = parentPrincipal.getName(X500Principal.RFC2253);
            // append CertPathString
            qualifiedOrgIdString.append("|ChainElement=\"");
            qualifiedOrgIdString.append(subjectDN);
            qualifiedOrgIdString.append("\"");
        }
        qualifiedOrgIdString.append(orgIdString);
        return qualifiedOrgIdString.toString();
    }

    private static byte[] rpIdentifierEV(X509Certificate relyingpartyCert)
            throws TokenIssuanceException {
        String rpIdentifier = null;
        String orgIdString = orgIdString(relyingpartyCert);

        byte[] digest;
        try {
            digest = sha256(orgIdString.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new TokenIssuanceException(e);
        }
        return digest;
    }

    private static byte[] sha256(byte[] bytes) throws TokenIssuanceException {
        MessageDigest mdAlgorithm;
        try {
            mdAlgorithm = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new TokenIssuanceException(e);
        }
        mdAlgorithm.update(bytes);
        byte[] digest = mdAlgorithm.digest();
        return digest;
    }

    public static String generateRPPPID(
            String cardId,
            X509Certificate relyingPartyCert,
            X509Certificate[] chain)
            throws TokenIssuanceException {
        try {
            byte[] rpIdentifierBytes = sha256(rpIdentifier(relyingPartyCert, chain));
            byte[] canonicalCardIdBytes = sha256(cardId.getBytes("UTF-8"));
            byte[] bytes = new byte[rpIdentifierBytes.length + canonicalCardIdBytes.length];
            System.arraycopy(rpIdentifierBytes, 0, bytes, 0, rpIdentifierBytes.length);
            System.arraycopy(canonicalCardIdBytes, 0, bytes, rpIdentifierBytes.length, canonicalCardIdBytes.length);
            byte[] ppidBytes = sha256(bytes);
            return Base64.encodeBytes(ppidBytes);
        } catch (UnsupportedEncodingException e) {
            throw new TokenIssuanceException(e);
        }
    }
}
