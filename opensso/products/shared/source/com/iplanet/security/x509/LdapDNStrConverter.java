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
 * $Id: LdapDNStrConverter.java,v 1.2 2008/06/25 05:52:46 qcheng Exp $
 *
 */

package com.iplanet.security.x509;

import java.io.IOException;

/**
 * Abstract class that converts a Ldap DN String to an X500Name, RDN or AVA and
 * vice versa, except the string is a java string in unicode.
 * 
 */
public abstract class LdapDNStrConverter {
    // 
    // public parsing methods.
    //

    /**
     * Converts a Ldap DN string to a X500Name object.
     * 
     * @param dn
     *            a Ldap DN String.
     * 
     * @return an X500Name object for the Ldap DN String.
     */
    public abstract X500Name parseDN(String dn) throws IOException;

    /**
     * Like parseDN with a specified DER encoding order for Directory Strings.
     */
    public abstract X500Name parseDN(String dn, byte[] tags) throws IOException;

    /**
     * Converts a Ldap DN string to a RDN object.
     * 
     * @param rdn
     *            a Ldap DN String
     * 
     * @return an RDN object.
     */
    public abstract RDN parseRDN(String rdn) throws IOException;

    /**
     * Like parseRDN with a specified DER encoding order for Directory Strings.
     */
    public abstract RDN parseRDN(String rdn, byte[] tags) throws IOException;

    /**
     * Converts a Ldap DN string to a AVA object.
     * 
     * @param ava
     *            a Ldap DN string.
     * @return an AVA object.
     */
    public abstract AVA parseAVA(String ava) throws IOException;

    /**
     * Like parseAVA with a specified DER encoding order for Directory Strings.
     */
    public abstract AVA parseAVA(String rdn, byte[] tags) throws IOException;

    //
    // public encoding methods.
    //

    /**
     * Converts a X500Name object to a Ldap dn string.
     * 
     * @param dn
     *            an X500Name object.
     * @return a Ldap DN String.
     */
    public abstract String encodeDN(X500Name dn) throws IOException;

    /**
     * Converts an RDN object to a Ldap dn string.
     * 
     * @param rdn
     *            an RDN object.
     * @return a Ldap dn string.
     */
    public abstract String encodeRDN(RDN rdn) throws IOException;

    /**
     * Converts an AVA object to a Ldap dn string.
     * 
     * @param ava
     *            An AVA object.
     * @return A Ldap dn string.
     */
    public abstract String encodeAVA(AVA ava) throws IOException;

    //
    // public static methods
    //

    /**
     * Gets a global default Ldap DN String converter. Currently it is
     * LdapV3DNStrConverter object using the default X500NameAttrMap and accepts
     * unknown OIDs.
     * 
     * @see LdapV3DNStrConverter
     * 
     * @return The global default LdapDNStrConverter instance.
     */
    public static LdapDNStrConverter getDefault() {
        return defaultConverter;
    }

    /**
     * Set the global default LdapDNStrConverter object.
     * 
     * @param defConverter
     *            A LdapDNStrConverter object to become the global default.
     */
    public static void setDefault(LdapDNStrConverter defConverter) {
        if (defConverter == null)
            throw new IllegalArgumentException(
                    "The default Ldap DN String converter cannot " +
                    "be set to null.");
        defaultConverter = defConverter;
    }

    //
    // private static variables
    //

    private static LdapDNStrConverter defaultConverter = 
        new LdapV3DNStrConverter();
}
