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
 * $Id: CertUtils.java,v 1.2 2008/06/25 05:52:46 qcheng Exp $
 *
 */

package com.iplanet.security.x509;

import java.io.IOException;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;

/**
 * This class provides utility methods to read the certificate DN information
 * in a format that can be understandable across OpenSSO.
 */
public class CertUtils {
    
    /**
     * Returns the Subject Name from <code>X509Certificate</code> for
     * OpenSSO  compliant <code>X500Name<code>.
     * @param cert X509 Certificate Object.
     * @return null if the SubjectDN can not be obtained or
     *              invalid certificate.
     */
    public static String getSubjectName(X509Certificate cert) {
        if(cert == null) {
           return null;
        }
        try {
            X500Principal subjectDN = cert.getSubjectX500Principal();
            X500Name certDN = new X500Name(subjectDN.getEncoded());
            return certDN.getName();
        } catch (IOException io) {
           return null;
        }
    }

    /**
     * Returns the Issuer Name from <code>X509Certificate</code> for
     * OpenSSO  compliant <code>X500Name<code>.
     * @param cert X509 Certificate Object.
     * @return null if the IssuerDN can not be obtained or
     *              invalid certificate.
     */
    public static String getIssuerName(X509Certificate cert) {
        if(cert == null) {
           return null;
        }
        try {
            X500Principal issuerDN = cert.getIssuerX500Principal();
            X500Name certDN = new X500Name(issuerDN.getEncoded());
            return certDN.getName();
        } catch (IOException io) {
           return null;
        }
    }
    
}
