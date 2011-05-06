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
 * $Id: RDN.java,v 1.2 2008/06/25 05:52:46 qcheng Exp $
 *
 */

package com.iplanet.security.x509;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import com.iplanet.security.util.DerInputStream;
import com.iplanet.security.util.DerOutputStream;
import com.iplanet.security.util.DerValue;
import com.iplanet.security.util.ObjectIdentifier;

/**
 * RDNs are a set of {attribute = value} assertions. Some of those attributes
 * are "distinguished" (unique w/in context). Order is never relevant.
 * 
 * Some X.500 names include only a single distinguished attribute per RDN. This
 * style is currently common.
 * 
 * Note that DER-encoded RDNs sort AVAs by assertion OID ... so that when we
 * parse this data we don't have to worry about canonicalizing it, but we'll
 * need to sort them when we expose the RDN class more.
 * 
 * @see X500Name
 * @see AVA
 * @see LdapDNStrConverter
 */

public class RDN {
    // public constructors

    /**
     * Constructs a RDN from a Ldap DN String with one RDN component using the
     * global default LdapDNStrConverter.
     * 
     * @see LdapDNStrConverter
     * @param rdnString
     *            a Ldap DN string with one RDN component, e.g. as defined in
     *            RFC1779.
     * @exception IOException
     *                if error occurs while parsing the string.
     */
    public RDN(String rdnString) throws IOException {
        RDN rdn = LdapDNStrConverter.getDefault().parseRDN(rdnString);
        assertion = rdn.getAssertion();
    }

    /**
     * Like RDN(String) with a DER encoding order given as argument for
     * Directory Strings.
     */
    public RDN(String rdnString, byte[] tags) throws IOException {
        RDN rdn = LdapDNStrConverter.getDefault().parseRDN(rdnString, tags);
        assertion = rdn.getAssertion();
    }

    /**
     * Constructs a RDN from a Ldap DN string with one RDN component using the
     * specified Ldap DN Str converter. For example, RFC1779StrConverter can be
     * passed to parse a Ldap DN string in RFC1779 format.
     * 
     * @see LdapDNStrConverter
     * @param rdnString
     *            Ldap DN string.
     * @param ldapDNStrConverter
     *            a LdapDNStrConverter.
     */
    public RDN(String rdnString, LdapDNStrConverter ldapDNStrConverter)
            throws IOException {
        RDN rdn = ldapDNStrConverter.parseRDN(rdnString);
        assertion = rdn.getAssertion();
    }

    /**
     * Constructs a RDN from a DerValue.
     * 
     * @param set
     *            Der value of a set of AVAs.
     */
    public RDN(DerValue set) throws IOException {
        if (set.tag != DerValue.tag_Set)
            throw new CertParseError("X500 RDN");

        int j_max = 50; // XXX j_max = f(data)!!
        int j;
        int i;

        AVA[] avas = new AVA[j_max];

        // create a temporary array big enough for a huge set of AVA's
        for (j = 0; j < j_max; j++) {
            avas[j] = new AVA(set.data);
            if (set.data.available() == 0)
                break;
        }

        // copy the elements into it
        if (j >= j_max - 1) {
            assertion = new AVA[j + 1];
        } else {
            assertion = new AVA[j + 1];
            for (i = 0; i < (j + 1); i++) {
                assertion[i] = avas[i];
            }
        }

        /*
         * if (set.data.available () != 0) // throw new CertParseError ("X500
         * RDN 2"); System.out.println (" ... RDN parse, ignored bytes = " +
         * set.data.available ());
         */
    }

    /**
     * Constructs a RDN from a Der Input Stream.
     * 
     * @param in
     *            a Der Input Stream.
     */
    public RDN(DerInputStream in) throws IOException {
        /* an RDN is a SET of avas */
        DerValue avaset[] = in.getSet(1);
        int i;
        assertion = new AVA[avaset.length];
        for (i = 0; i < assertion.length; i++)
            assertion[i] = new AVA(avaset[i].data);
    }

    /**
     * Constructs a RDN from an array of AVA.
     * 
     * @param avas
     *            a AVA Array.
     */
    public RDN(AVA avas[]) {
        assertion = (AVA[]) avas.clone();
    }

    /**
     * convenience method.
     */
    public RDN(Vector avaVector) {
        int size = avaVector.size();
        assertion = new AVA[size];
        for (int i = 0; i < size; i++) {
            assertion[i] = (AVA) avaVector.elementAt(i);
        }
    }

    /**
     * returns an array of AVA in the RDN.
     * 
     * @return array of AVA in this RDN.
     */
    public AVA[] getAssertion() {
        return (AVA[]) assertion.clone();
    }

    /**
     * returns the number of AVAs in the RDN.
     * 
     * @return number of AVAs in this RDN.
     */
    public int getAssertionLength() {
        return assertion.length;
    }

    private AVA assertion[];

    private class AVAEnumerator implements Enumeration {
        private int index;

        public AVAEnumerator() {
            index = 0;
        }

        public boolean hasMoreElements() {
            return (index < assertion.length);
        }

        public Object nextElement() {
            if (index >= assertion.length)
                return null;
            return assertion[index++];
        }
    }

    // other public methods.

    /**
     * Checks if this RDN is the same as another by comparing the AVAs in the
     * RDNs.
     * 
     * @param other
     *            the other RDN.
     * @return true iff the other RDN is the same.
     */
    public boolean equals(RDN other) {
        int i;

        if (other == this)
            return true;
        if (assertion.length != other.assertion.length)
            return false;

        for (i = 0; i < assertion.length; i++)
            if (!assertion[i].equals(other.assertion[i]))
                return false;

        return true;
    }

    DerValue findAttribute(ObjectIdentifier oid) {
        int i;

        for (i = 0; i < assertion.length; i++)
            if (assertion[i].oid.equals(oid))
                return assertion[i].value;
        return null;
    }

    /**
     * Encodes this RDN to a Der output stream.
     * 
     * @param out
     *            the Der Output Stream.
     */
    public void encode(DerOutputStream out) throws IOException {
        DerOutputStream tmp = new DerOutputStream();
        int i;

        for (i = 0; i < assertion.length; i++)
            assertion[i].encode(tmp);
        out.write(DerValue.tag_Set, tmp);
    }

    /**
     * returns an enumeration of AVAs that make up this RDN.
     * 
     * @return an enumeration of AVAs that make up this RDN.
     */
    public Enumeration getAVAs() {
        return new AVAEnumerator();
    }

    /**
     * Returns a Ldap DN string with one RDN component using the global default
     * LdapDNStrConverter.
     * 
     * @see LdapDNStrConverter
     * @return the Ldap DN String of this RDN.
     * @exception IOException
     *                if an error occurs during the conversion.
     */
    public String toLdapDNString() throws IOException {
        return LdapDNStrConverter.getDefault().encodeRDN(this);
    }

    /**
     * Returns a Ldap DN String with this RDN component using the specified
     * LdapDNStrConverter.
     * 
     * @see LdapDNStrConverter
     * @param ldapDNStrConverter
     *            a LdapDNStrConverter.
     * @return a Ldap DN String.
     * @exception IOException
     *                if an error occurs in the conversion.
     */
    public String toLdapDNString(LdapDNStrConverter ldapDNStrConverter)
            throws IOException {
        return ldapDNStrConverter.encodeRDN(this);
    }

    /**
     * Returns a Ldap DN string with this RDN component using the global default
     * LdapDNStrConverter.
     * 
     * @see LdapDNStrConverter
     * @return the Ldap DN String with this RDN component, null if an error
     *         occurs in the conversion.
     */
    public String toString() {
        String s;
        try {
            s = toLdapDNString();
        } catch (IOException e) {
            return null;
        }
        return s;
    }

}
