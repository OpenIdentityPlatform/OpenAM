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
 * Portions Copyrighted 2014 ForgeRock AS.
 */

package com.iplanet.security.x509;

import com.sun.identity.shared.debug.Debug;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

/**
 * This class provides utility methods to read the certificate DN information in a format that is understandable
 * across OpenAM.
 */
public class CertUtils {

    public static final String COMMON_NAME = "CN";
    public static final String MAIL = "MAIL";
    public static final String EMAIL_ADDRESS = "E";
    public static final String UID = "uid";
    private static final Map<String, String> OID_MAP = new HashMap<String, String>();
    private static final Debug DEBUG = Debug.getInstance("amAuthCert");

    static {
        OID_MAP.put("1.2.840.113549.1.9.1", EMAIL_ADDRESS);
        OID_MAP.put("1.2.840.113549.1.9.2", "unstructuredName");
        OID_MAP.put("1.2.840.113549.1.9.8", "unstructuredAddress");
        OID_MAP.put("2.5.4.4", "sn");
        OID_MAP.put("2.5.4.5", "serialNumber");
        OID_MAP.put("2.5.4.12", "title");
        OID_MAP.put("2.5.4.42", "givenName");
        OID_MAP.put("2.5.4.43", "initials");
        OID_MAP.put("2.5.4.44", "generationQualifier");
        OID_MAP.put("2.5.4.46", "dnQualifier");
    }

    /**
     * Returns the Subject Name from the {@link X509Certificate}'s subject {@link X500Principal}.
     *
     * @param cert X509 Certificate Object.
     * @return null if the SubjectDN can not be obtained.
     */
    public static String getSubjectName(X509Certificate cert) {
        if (cert == null) {
            return null;
        }

        return cert.getSubjectX500Principal().getName(X500Principal.RFC2253, OID_MAP);
    }

    /**
     * Returns the Issuer Name from the {@link X509Certificate}'s issuer {@link X500Principal}.
     *
     * @param cert X509 Certificate Object.
     * @return null if the IssuerDN can not be obtained.
     */
    public static String getIssuerName(X509Certificate cert) {
        if (cert == null) {
            return null;
        }

        return cert.getIssuerX500Principal().getName(X500Principal.RFC2253, OID_MAP);
    }

    /**
     * Retrieves a given attribute value from the provided {@link X500Principal} even if the attribute was enclosed in
     * a multi-valued RDN.
     *
     * @param principal The principal to retrieve the value from.
     * @param attributeName The non-null name of the attribute to retrieve.
     * @return The attribute value from the principal.
     */
    public static String getAttributeValue(X500Principal principal, String attributeName) {
        try {
            LdapName ldapName = new LdapName(principal.getName(X500Principal.RFC2253, OID_MAP));
            for (Rdn rdn : ldapName.getRdns()) {
                Attributes attrs = rdn.toAttributes();
                NamingEnumeration<? extends Attribute> values = attrs.getAll();
                while (values.hasMoreElements()) {
                    Attribute attr = values.next();
                    if (attributeName.equalsIgnoreCase(attr.getID())) {
                        return attr.get() == null ? null : attr.get().toString();
                    }
                }
            }
        } catch (NamingException ne) {
            DEBUG.warning("A naming error occurred while trying to retrieve " + attributeName + " from principal: "
                    + principal, ne);
        }
        return null;
    }
}
