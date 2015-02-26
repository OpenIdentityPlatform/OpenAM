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
 * $Id: X500NameAttrMap.java,v 1.2 2008/06/25 05:52:46 qcheng Exp $
 *
 */

package com.iplanet.security.x509;

import java.util.Enumeration;
import java.util.Hashtable;

import com.iplanet.security.util.ObjectIdentifier;

/**
 * Maps an attribute name in an X500 AVA to its OID and a converter for the
 * attribute type. The converter converts from a string to its DER encoded
 * attribute value. * For example, "CN" maps to its OID of 2.5.4.3 and the
 * Directory String Converter. The Directory String Converter converts from a
 * string to a DerValue with tag Printable, T.61 or UniversalString.
 * 
 */
public class X500NameAttrMap {
    //
    // public constructors.
    //

    /**
     * Construct a X500NameAttrMap.
     */
    public X500NameAttrMap() {
    }

    //
    // public get methods.
    //

    /**
     * Get the attribute name (keyword) of the specified OID.
     * 
     * @param oid
     *            An ObjectIdentifier
     * 
     * @return An attribute name (keyword string) for the OID.
     */
    public String getName(ObjectIdentifier oid) {
        // XXX assert oid != null
        return (String) oid2Name.get(oid);
    }

    /**
     * Get the ObjectIdentifier of the attribute name.
     * 
     * @param name
     *            An attribute name (string of ascii characters)
     * 
     * @return An ObjectIdentifier for the attribute.
     */
    public ObjectIdentifier getOid(String name) {
        // XXX assert name != null
        return (ObjectIdentifier) name2OID.get(name.toUpperCase());
    }

    /**
     * Get the Attribute Value Converter for the specified attribute name.
     * 
     * @param name
     *            An attribute name
     * 
     * @return An attribute value converter for the attribute name
     */
    public AVAValueConverter getValueConverter(String name) {
        ObjectIdentifier oid = (ObjectIdentifier) name2OID.get(name
                .toUpperCase());
        if (oid == null)
            return null;
        return (AVAValueConverter) oid2ValueConverter.get(oid);
    }

    /**
     * Get the Attribute Value Converter for the specified ObjectIdentifier.
     * 
     * @param oid
     *            An ObjectIdentifier
     * 
     * @return An AVAValueConverter for the OID.
     */
    public AVAValueConverter getValueConverter(ObjectIdentifier oid) {
        return (AVAValueConverter) oid2ValueConverter.get(oid);
    }

    /**
     * Get an Enumeration of all attribute names in this map.
     * 
     * @return An Enumeration of all attribute names.
     */
    public Enumeration getAllNames() {
        return name2OID.keys();
    }

    /**
     * Get an Enumeration of all ObjectIdentifiers in this map.
     * 
     * @return An Enumeration of all OIDs in this map.
     */
    public Enumeration getAllOIDs() {
        return oid2Name.keys();
    }

    /**
     * Get the ObjectIdentifier object in the map for the specified OID.
     * 
     * @param oid
     *            An ObjectIdentifier.
     * @return The ObjectIdentifier object in this map for the OID.
     */
    public ObjectIdentifier getOid(ObjectIdentifier oid) {
        String name = (String) oid2Name.get(oid);
        if (name == null)
            return null;
        return (ObjectIdentifier) name2OID.get(name);
    }

    //
    // public add methods.
    //

    /**
     * Adds a attribute name, ObjectIdentifier, AVAValueConverter entry to the
     * map.
     * 
     * @param name
     *            An attribute name (string of ascii chars)
     * @param oid
     *            The ObjectIdentifier for the attribute.
     * @param valueConverter
     *            An AVAValueConverter object for converting an value for this
     *            attribute from a string to a DerValue and vice versa.
     */
    public void addNameOID(String name, ObjectIdentifier oid,
            AVAValueConverter valueConverter) {
        // normalize name for case insensitive compare.
        ObjectIdentifier theOid;
        Class expValueConverter;

        theOid = (ObjectIdentifier) name2OID.get(name);
        if (theOid != null) {
            expValueConverter = oid2ValueConverter.get(theOid).getClass();
            if (!theOid.equals(oid)
                    || expValueConverter != valueConverter.getClass()) {
                throw new IllegalArgumentException(
                        "Another keyword-oid-valueConverter triple already "
                                + "exists in the X500NameAttrMap ");
            }
            return;
        }
        name2OID.put(name.toUpperCase(), oid);
        oid2Name.put(oid, name.toUpperCase());
        oid2ValueConverter.put(oid, valueConverter);
    }

    //
    // public static methods.
    // 

    /**
     * Get the global default X500NameAttrMap.
     * 
     * @return The global default X500NameAttrMap.
     */
    public static X500NameAttrMap getDefault() {
        return defMap;
    }

    /**
     * Set the global default X500NameAttrMap.
     * 
     * @param newDefault
     *            The new default X500NameAttrMap.
     */
    public static void setDefault(X500NameAttrMap newDefault) {
        // XXX assert newDef != null
        defMap = newDefault;
    }

    //
    // private variables
    //

    Hashtable name2OID = new Hashtable();

    Hashtable oid2Name = new Hashtable();

    Hashtable oid2ValueConverter = new Hashtable();

    //
    // global defaults.
    //

    private static X500NameAttrMap defMap;

    /*
     * Create the default map on initialization.
     */
    static {
        defMap = new X500NameAttrMap();
        AVAValueConverter directoryStr = new DirStrConverter(), ia5Str = 
            new IA5StringConverter();
        defMap.addNameOID("CN", new ObjectIdentifier("2.5.4.3"), directoryStr);
        defMap.addNameOID("OU", new ObjectIdentifier("2.5.4.11"), directoryStr);
        defMap.addNameOID("O", new ObjectIdentifier("2.5.4.10"), directoryStr);
        // serialNumber added for CEP support
        defMap.addNameOID("SERIALNUMBER", new ObjectIdentifier("2.5.4.5"),
                new PrintableConverter());
        defMap.addNameOID("C", new ObjectIdentifier("2.5.4.6"),
                new PrintableConverter());
        defMap.addNameOID("L", new ObjectIdentifier("2.5.4.7"), directoryStr);
        defMap.addNameOID("ST", new ObjectIdentifier("2.5.4.8"), directoryStr);
        defMap.addNameOID("STREET", new ObjectIdentifier("2.5.4.9"),
                directoryStr);
        defMap.addNameOID("TITLE", new ObjectIdentifier("2.5.4.12"),
                directoryStr);
        // RFC 1274 UserId, rfc822MailBox
        defMap.addNameOID("UID", new ObjectIdentifier(
                "0.9.2342.19200300.100.1.1"), directoryStr);
        defMap.addNameOID("MAIL", new ObjectIdentifier(
                "0.9.2342.19200300.100.1.3"), ia5Str);
        // PKCS9 e-mail address
        defMap.addNameOID("E", new ObjectIdentifier("1.2.840.113549.1.9.1"),
                ia5Str);

        // DC definition from draft-ietf-asid-ldap-domains-02.txt
        defMap.addNameOID("DC", new ObjectIdentifier(
                "0.9.2342.19200300.100.1.25"), ia5Str);

        // more defined in RFC2459 used in Subject Directory Attr extension
        defMap.addNameOID("SN", // surname
                new ObjectIdentifier("2.5.4.4"), directoryStr);
        defMap.addNameOID("GIVENNAME", new ObjectIdentifier("2.5.4.42"),
                directoryStr);
        defMap.addNameOID("INITIALS", new ObjectIdentifier("2.5.4.43"),
                directoryStr);
        defMap.addNameOID("GENERATIONQUALIFIER", new ObjectIdentifier(
                "2.5.4.44"), directoryStr);
        defMap.addNameOID("DNQUALIFIER", new ObjectIdentifier("2.5.4.46"),
                directoryStr);

        // these two added mainly for CEP support
        // PKCS9 unstructured name
        defMap.addNameOID("UNSTRUCTUREDNAME", new ObjectIdentifier(
                "1.2.840.113549.1.9.2"), ia5Str);
        // PKCS9 unstructured address
        defMap.addNameOID("UNSTRUCTUREDADDRESS", new ObjectIdentifier(
                "1.2.840.113549.1.9.8"), new PrintableConverter());
    }

}
