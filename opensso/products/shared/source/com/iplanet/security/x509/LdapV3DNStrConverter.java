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
 * $Id: LdapV3DNStrConverter.java,v 1.2 2008/06/25 05:52:46 qcheng Exp $
 *
 */

package com.iplanet.security.x509;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

import sun.io.ByteToCharConverter;

import com.iplanet.security.util.DerValue;
import com.iplanet.security.util.ObjectIdentifier;

/**
 * A converter that converts Ldap v3 DN strings as specified in
 * draft-ietf-asid-ldapv3-dn-03.txt to a X500Name, RDN or AVA and vice versa.
 * 
 * @see LdapDNStrConverter
 * @see X500Name
 * @see RDN
 * @see AVA
 * @see X500NameAttrMap
 * 
 */
public class LdapV3DNStrConverter extends LdapDNStrConverter {
    //
    // Constructors
    //

    /**
     * Constructs a LdapV3DNStrConverter using the global default
     * X500NameAttrMap and accept OIDs not in the default X500NameAttrMap.
     * 
     * @see X500NameAttrMap
     */
    public LdapV3DNStrConverter() {
        attrMap = X500NameAttrMap.getDefault();
        acceptUnknownOids = true;
    }

    /**
     * Constructs a LdapV3DNStrConverter using the specified X500NameAttrMap and
     * a boolean indicating whether to accept OIDs not listed in the
     * X500NameAttrMap.
     * 
     * @param attributeMap
     *            a X500NameAttrMap
     * @param doAcceptUnknownOids
     *            whether to convert unregistered OIDs (oids not in the
     *            X500NameAttrMap)
     * @see X500NameAttrMap
     */
    public LdapV3DNStrConverter(X500NameAttrMap attributeMap,
            boolean doAcceptUnknownOids) {
        attrMap = attributeMap;
        acceptUnknownOids = doAcceptUnknownOids;
    }

    //
    // public parsing methods
    // From LdapDNStrConverter interface
    //

    /**
     * Parse a Ldap v3 DN string to a X500Name.
     * 
     * @param dn
     *            a LDAP v3 DN String
     * @return a X500Name
     * @exception IOException
     *                if an error occurs during the conversion.
     */
    public X500Name parseDN(String dn) throws IOException {
        return parseDN(dn, null);
    }

    /**
     * Like parseDN(String) with a DER encoding order given as argument for
     * Directory Strings.
     */
    public X500Name parseDN(String dn, byte[] encodingOrder) throws IOException 
    {
        StringReader dn_reader = new StringReader(dn);
        PushbackReader in = new PushbackReader(dn_reader, 5);

        return parseDN(in, encodingOrder);
    }

    /**
     * Parse a Ldap v3 DN string with a RDN component to a RDN
     * 
     * @param rdn
     *            a LDAP v3 DN String
     * @return a RDN
     * @exception IOException
     *                if an error occurs during the conversion.
     */
    public RDN parseRDN(String rdn) throws IOException {
        return parseRDN(rdn, null);
    }

    /**
     * Like parseRDN(String) with a DER encoding order given as argument for
     * Directory Strings.
     */
    public RDN parseRDN(String rdn, byte[] encodingOrder) throws IOException {
        StringReader rdn_reader = new StringReader(rdn);
        PushbackReader in = new PushbackReader(rdn_reader, 5);

        return parseRDN(in, null);
    }

    /**
     * Parse a Ldap v3 DN string with a AVA component to a AVA.
     * 
     * @param ava
     *            a LDAP v3 DN string
     * @return a AVA
     */
    public AVA parseAVA(String ava) throws IOException {
        return parseAVA(ava, null);
    }

    /**
     * Like parseDN(String) with a DER encoding order given as argument for
     * Directory Strings.
     */
    public AVA parseAVA(String ava, byte[] encodingOrder) throws IOException {
        StringReader ava_reader = new StringReader(ava);
        PushbackReader in = new PushbackReader(ava_reader, 5);

        return parseAVA(in, encodingOrder);
    }

    //
    // public parsing methods called by other methods.
    //

    /**
     * Parses a Ldap DN string in a string reader to a X500Name.
     * 
     * @param in
     *            Pushback string reader for a Ldap DN string. The pushback
     *            reader must have a pushback buffer size > 2.
     * 
     * @return a X500Name
     * 
     * @exception IOException
     *                if any reading or parsing error occurs.
     */
    public X500Name parseDN(PushbackReader in) throws IOException {
        return parseDN(in, null);
    }

    /**
     * Like parseDN(PushbackReader in) with a DER encoding order given as
     * argument for Directory Strings.
     */
    public X500Name parseDN(PushbackReader in, byte[] encodingOrder)
            throws IOException {
        RDN rdn;
        int lastChar;
        Vector rdnVector = new Vector();
        RDN names[];
        int i, j;

        do {
            rdn = parseRDN(in, encodingOrder);
            rdnVector.addElement(rdn);
            lastChar = in.read();
        } while (lastChar == ',' || lastChar == ';');

        names = new RDN[rdnVector.size()];
        for (i = 0, j = rdnVector.size() - 1; i < rdnVector.size(); i++, j--)
            names[j] = (RDN) rdnVector.elementAt(i);
        return new X500Name(names);
    }

    /**
     * Parses Ldap DN string with a rdn component from a string reader to a RDN.
     * The string reader will point to the separator after the rdn component or
     * -1 if at end of string.
     * 
     * @param in
     *            Pushback string reader containing a Ldap DN string with at
     *            least one rdn component. The pushback reader must have a
     *            pushback buffer size > 2.
     * 
     * @return RDN object of the first rdn component in the Ldap DN string.
     * 
     * @exception IOException
     *                if any read or parse error occurs.
     */
    public RDN parseRDN(PushbackReader in) throws IOException {
        return parseRDN(in, null);
    }

    /**
     * Like parseRDN(PushbackReader) with a DER encoding order given as argument
     * for Directory Strings.
     */
    public RDN parseRDN(PushbackReader in, byte[] encodingOrder)
            throws IOException {
        Vector avaVector = new Vector();
        AVA ava;
        int lastChar;
        AVA assertion[];

        do {
            ava = parseAVA(in, encodingOrder);
            avaVector.addElement(ava);
            lastChar = in.read();
        } while (lastChar == '+');

        if (lastChar != -1)
            in.unread(lastChar);

        assertion = new AVA[avaVector.size()];
        for (int i = 0; i < avaVector.size(); i++)
            assertion[i] = (AVA) avaVector.elementAt(i);
        return new RDN(assertion);
    }

    /**
     * Parses a Ldap DN string with a AVA component from a string reader to an
     * AVA. The string reader will point to the AVA separator after the ava
     * string or -1 if end of string.
     * 
     * @param in
     *            a Pushback reader containg a Ldap string with at least one AVA
     *            component. The Pushback reader must have a pushback buffer
     *            size > 2.
     * 
     * @return AVA object of the first AVA component in the Ldap DN string.
     */
    public AVA parseAVA(PushbackReader in) throws IOException {
        return parseAVA(in, null);
    }

    /**
     * Like parseAVA(PushbackReader) with a DER encoding order given as argument
     * for Directory Strings.
     */
    public AVA parseAVA(PushbackReader in, byte[] encodingOrder)
            throws IOException {
        int c;
        ObjectIdentifier oid;
        DerValue value;
        StringBuffer keywordBuf;
        StringBuffer valueBuf;
        ByteArrayOutputStream berStream;
        char hexChar1, hexChar2;
        CharArrayWriter hexCharsBuf;
        String endChars;

        /*
         * First get the keyword indicating the attribute's type, and map it to
         * the appropriate OID.
         */
        keywordBuf = new StringBuffer();
        for (;;) {
            c = in.read();
            if (c == '=')
                break;
            if (c == -1)
                throw new IOException("Bad AVA format: Missing '='");
            keywordBuf.append((char) c);
        }
        oid = parseAVAKeyword(keywordBuf.toString());

        /*
         * Now parse the value. "#hex", a quoted string, or a string terminated
         * by "+", ",", ";", ">". Whitespace before or after the value is
         * stripped.
         */
        for (c = in.read(); c == ' '; c = in.read())
            continue;
        if (c == -1)
            throw new IOException("Bad AVA format: Missing attribute value");

        if (c == '#') {
            /*
             * NOTE per LDAPv3 dn string ietf standard the value represented by
             * this form is a BER value. But we only support DER value here
             * which is only a form of BER.
             */
            berStream = new ByteArrayOutputStream();
            int b;
            for (;;) {
                hexChar1 = (char) (c = in.read());
                if (c == -1 || octoEndChars.indexOf(c) > 0) // end of value
                    break;
                hexChar2 = (char) (c = in.read());
                if (hexDigits.indexOf(hexChar1) == -1
                        || hexDigits.indexOf(hexChar2) == -1)
                    throw new IOException("Bad AVA value: bad hex value.");
                b = (Character.digit(hexChar1, 16) << 4)
                        + Character.digit(hexChar2, 16);
                berStream.write(b);
            }
            if (berStream.size() == 0)
                throw new IOException("bad AVA format: invalid hex value");

            value = parseAVAValue(berStream.toByteArray(), oid);

            while (c == ' ' && c != -1)
                c = in.read();
        } else {
            valueBuf = new StringBuffer();
            boolean quoted = false;
            if (c == '"') {
                quoted = true;
                endChars = quotedEndChars;
                if ((c = in.read()) == -1)
                    throw new IOException("Bad AVA format: Missing attrValue");
            } else {
                endChars = valueEndChars;
            }

            // QUOTATION * ( quotechar / pair ) QUOTATION
            // quotechar = any character except '\' or QUOTATION
            // pair = '\' ( special | '\' | QUOTATION | hexpair )
            while (c != -1 && endChars.indexOf(c) == -1) {
                if (c == '\\') {
                    if ((c = in.read()) == -1)
                        throw new IOException("Bad AVA format: expecting "
                                + "escaped char.");
                    // expect escaping of special chars, space and CR.
                    if (specialChars.indexOf((char) c) != -1 || c == '\n'
                            || c == '\\' || c == '"' || c == ' ') {
                        valueBuf.append((char) c);
                    } else if (hexDigits.indexOf(c) != -1) {
                        hexCharsBuf = new CharArrayWriter();
                        // handle sequence of '\' hexpair
                        do {
                            hexChar1 = (char) c;
                            hexChar2 = (char) (c = in.read());
                            if (hexDigits.indexOf((char) c) == -1)
                                throw new IOException("Bad AVA format: "
                                        + "invalid escaped hex pair");
                            hexCharsBuf.write(hexChar1);
                            hexCharsBuf.write(hexChar2);
                            // read ahead to next '\' hex-char if any.
                            if ((c = in.read()) == -1)
                                break;
                            if (c != '\\') {
                                in.unread(c);
                                break;
                            }
                            if ((c = in.read()) == -1)
                                throw new IOException("Bad AVA format: "
                                        + "expecting escaped char.");
                            if (hexDigits.indexOf((char) c) == -1) {
                                in.unread(c);
                                in.unread('\\');
                                break;
                            }
                        } while (true);
                        valueBuf.append(getStringFromHexpairs(hexCharsBuf
                                .toCharArray()));
                    } else {
                        throw new IOException("Bad AVA format: "
                                + "invalid escaping");
                    }
                } else
                    valueBuf.append((char) c);
                c = in.read();
            }

            value = parseAVAValue(valueBuf.toString().trim(), oid,
                    encodingOrder);

            if (quoted) { // move to next non-white space
                do {
                    c = in.read();
                } while (c == ' ');
                if (c != -1 && valueEndChars.indexOf(c) == -1)
                    throw new IOException(
                            "Bad AVA format: " +
                            "separator expected at end of ava.");
            }
        }

        if (c != -1)
            in.unread(c);

        return new AVA(oid, value);
    }

    /**
     * Converts a AVA keyword from a Ldap DN string to an ObjectIdentifier from
     * the attribute map or, if this keyword is an OID not in the attribute map,
     * create a new ObjectIdentifier for the keyword if acceptUnknownOids is
     * true.
     * 
     * @param avaKeyword
     *            AVA keyword from a Ldap DN string.
     * 
     * @return a ObjectIdentifier object
     * @exception IOException
     *                if the keyword is an OID not in the attribute map and
     *                acceptUnknownOids is false, or if an error occurs during
     *                conversion.
     */
    public ObjectIdentifier parseAVAKeyword(String avaKeyword)
            throws IOException {
        String keyword = avaKeyword.toUpperCase().trim();
        String oid_str = null;
        ObjectIdentifier oid, new_oid;

        // get oid for keyword from OID registry

        if (Character.digit(keyword.charAt(0), 10) != -1) {
            // value is an oid string of 1.2.3.4
            oid_str = keyword;
        } else if (keyword.startsWith("oid.") || keyword.startsWith("OID.")) {
            // value is an oid string of oid.1.2.3.4 or OID.1.2...
            oid_str = keyword.substring(4);
        }

        if (oid_str != null) {
            // value is an oid string of 1.2.3.4 or oid.1.2.3.4 or OID.1.2...
            new_oid = new ObjectIdentifier(oid_str);
            oid = attrMap.getOid(new_oid);
            if (oid == null) {
                if (!acceptUnknownOids)
                    throw new IOException("Unknown AVA OID.");
                oid = new_oid;
            }
        } else {
            oid = attrMap.getOid(keyword);
            if (oid == null)
                throw new IOException("Unknown AVA keyword '" + keyword + "'.");
        }

        return oid;
    }

    /**
     * Converts a AVA value from a Ldap dn string to a DerValue according the
     * attribute type. For example, a value for CN, OU or O is expected to be a
     * Directory String and will be converted to a DerValue of ASN.1 type
     * PrintableString, T61String or UniversalString. A Directory String is a
     * ASN.1 CHOICE of Printable, T.61 or Universal string.
     * 
     * @param avaValueString
     *            a attribute value from a Ldap DN string.
     * @param oid
     *            OID of the attribute.
     * 
     * @return DerValue for the value.
     * 
     * @exception IOException
     *                if an error occurs during conversion.
     * @see AVAValueConverter
     */
    public DerValue parseAVAValue(String avaValueString, ObjectIdentifier oid)
            throws IOException {
        return parseAVAValue(avaValueString, oid, null);
    }

    /**
     * Like parseAVAValue(String) with a DER encoding order given as argument
     * for Directory Strings.
     */
    public DerValue parseAVAValue(String avaValueString, ObjectIdentifier oid,
            byte[] encodingOrder) throws IOException {
        AVAValueConverter valueConverter = attrMap.getValueConverter(oid);
        if (valueConverter == null) {
            if (!acceptUnknownOids) {
                throw new IllegalArgumentException(
                        "Unrecognized OID for AVA value conversion");
            } else {
                valueConverter = new GenericValueConverter();
            }
        }
        return valueConverter.getValue(avaValueString, encodingOrder);
    }

    /**
     * Converts a value in BER encoding, for example given in octothorpe form in
     * a Ldap v3 dn string, to a DerValue. Checks if the BER encoded value is a
     * legal value for the attribute.
     * <p>
     * <strong><i>NOTE:</i></strong> only DER encoded values are supported
     * for the BER encoded value.
     * 
     * @param berValue
     *            a value in BER encoding
     * @param oid
     *            ObjectIdentifier of the attribute.
     * 
     * @return DerValue for the BER encoded value
     * @exception IOException
     *                if an error occurs during conversion.
     */
    public DerValue parseAVAValue(byte[] berValue, ObjectIdentifier oid)
            throws IOException {
        AVAValueConverter valueConverter = attrMap.getValueConverter(oid);
        if (valueConverter == null && !acceptUnknownOids) {
            throw new IllegalArgumentException(
                    "Unrecognized OID for AVA value conversion");
        } else {
            valueConverter = new GenericValueConverter();
        }
        return valueConverter.getValue(berValue);
    }

    //
    // public encoding methods.
    //

    /**
     * Converts a X500Name object to a Ldap v3 DN string (except in unicode).
     * 
     * @param x500name
     *            a X500Name
     * 
     * @return a Ldap v3 DN String (except in unicode).
     * 
     * @exception IOException
     *                if an error is encountered during conversion.
     */
    public String encodeDN(X500Name x500name) throws IOException {
        RDN[] rdns = x500name.getNames();
        // String fullname = null;
        StringBuffer fullname = new StringBuffer();
        String s;
        int i;
        if (rdns.length == 0)
            return "";
        i = rdns.length - 1;
        fullname.append(encodeRDN(rdns[i--]));
        while (i >= 0) {
            s = encodeRDN(rdns[i--]);
            fullname.append(",");
            fullname.append(s);
        }
        return fullname.toString();
    }

    /**
     * Converts a RDN to a Ldap v3 DN string (except in unicode).
     * 
     * @param rdn
     *            a RDN
     * 
     * @return a LDAP v3 DN string (except in unicode).
     * 
     * @exception IOException
     *                if an error is encountered during conversion.
     */
    public String encodeRDN(RDN rdn) throws IOException {
        AVA[] avas = rdn.getAssertion();
        // String relname = null;
        StringBuffer relname = new StringBuffer();
        String s;
        int i = 0;

        relname.append(encodeAVA(avas[i++]));
        while (i < avas.length) {
            s = encodeAVA(avas[i++]);
            relname.append("+");
            relname.append(s);
        }
        return relname.toString();
    }

    /**
     * Converts a AVA to a Ldap v3 DN String (except in unicode).
     * 
     * @param ava
     *            an AVA
     * 
     * @return a Ldap v3 DN string (except in unicode).
     * 
     * @exception IOException
     *                If an error is encountered during exception.
     */
    public String encodeAVA(AVA ava) throws IOException {
        ObjectIdentifier oid = ava.getOid();
        DerValue value = ava.getValue();
        String keyword, valueStr;

        // get attribute name

        keyword = encodeOID(oid);
        valueStr = encodeValue(value, oid);

        return keyword + "=" + valueStr;
    }

    /**
     * Converts an OID to a attribute keyword in a Ldap v3 DN string - either a
     * keyword if known or a string of "1.2.3.4" syntax.
     * 
     * @param oid
     *            a ObjectIdentifier
     * 
     * @return a keyword to use in a Ldap V3 DN string.
     * 
     * @exception IOException
     *                if an error is encountered during conversion.
     */
    public String encodeOID(ObjectIdentifier oid) throws IOException {
        String keyword = attrMap.getName(oid);
        if (keyword == null) {
            if (acceptUnknownOids)
                keyword = oid.toString();
            else
                throw new IOException("Unknown OID");
        }
        return keyword;
    }

    /**
     * Converts a value as a DerValue to a string in a Ldap V3 DN String. If the
     * value cannot be converted to a string it will be encoded in octothorpe
     * form.
     * 
     * @param attrValue
     *            a value as a DerValue.
     * @param oid
     *            OID for the attribute.
     * @return a string for the value in a LDAP v3 DN String
     * @exception IOException
     *                if an error occurs during conversion.
     */
    public String encodeValue(DerValue attrValue, ObjectIdentifier oid)
            throws IOException {
        /*
         * Construct the value with as little copying and garbage production as
         * practical.
         */
        StringBuffer retval = new StringBuffer(30);
        int i;
        String temp = null;
        AVAValueConverter valueConverter;

        valueConverter = attrMap.getValueConverter(oid);
        if (valueConverter == null) {
            if (acceptUnknownOids)
                valueConverter = new GenericValueConverter();
            else
                throw new IOException("Unknown AVA " +
                        "type for encoding AVA value");
        }

        try {
            temp = valueConverter.getAsString(attrValue);
            if (temp == null) {
                // convert to octothorpe form.
                byte data[] = attrValue.toByteArray();

                retval.append('#');
                for (i = 0; i < data.length; i++) {
                    retval.append(hexDigits.charAt((data[i] >> 4) & 0x0f));
                    retval.append(hexDigits.charAt(data[i] & 0x0f));
                }

            } else {

                retval.append(encodeString(temp));

            }
        } catch (IOException e) {
            throw new IllegalArgumentException("malformed AVA DER Value");
        }

        return retval.toString();
    }

    /**
     * converts a raw value string to a string in Ldap V3 DN string format.
     * 
     * @param valueStr
     *            a 'raw' value string.
     * @return a attribute value string in Ldap V3 DN string format.
     */
    public String encodeString(String valueStr) {
        int i, j;
        int len;
        StringBuffer retval = new StringBuffer();

        /*
         * generate string according to ldapv3 DN. escaping is used. Strings
         * generated this way are acceptable by rfc1779 implementations.
         */
        len = valueStr.length();

        // get index of first space at the end of the string.
        for (j = len - 1; j >= 0 && valueStr.charAt(j) == ' '; j--)
            continue;

        // escape spaces at the beginning of the string.
        for (i = 0; i <= j && valueStr.charAt(i) == ' '; i++) {
            retval.append('\\');
            retval.append(valueStr.charAt(i));
        }

        // escape special characters in the middle of the string.
        for (; i <= j; i++) {
            if (valueStr.charAt(i) == '\\') {
                retval.append('\\');
                retval.append(valueStr.charAt(i));
            } else if (specialChars.indexOf(valueStr.charAt(i)) != -1) {
                retval.append('\\');
                retval.append(valueStr.charAt(i));
            } else
                retval.append(valueStr.charAt(i));
        }

        // esacape spaces at the end.
        for (; i < valueStr.length(); i++) {
            retval.append('\\');
            retval.append(' ');
        }

        return retval.toString();
    }

    //
    // public get/set methods
    //

    /**
     * gets the X500NameAttrMap used by the converter.
     * 
     * @return X500NameAttrMap used by this converter.
     */
    public X500NameAttrMap getAttrMap() {
        return attrMap;
    }

    /**
     * returns true if the converter accepts unregistered attributes i.e. OIDS
     * not in the X500NameAttrMap.
     * 
     * @return true if converter converts attributes not in the X500NameAttrMap.
     */
    public boolean getAcceptUnknownOids() {
        return acceptUnknownOids;
    }

    //
    // private and protected variables
    //

    protected X500NameAttrMap attrMap;

    protected boolean acceptUnknownOids;

    //
    // private and protected static variables & methods.
    //

    protected static final String specialChars = ",+=<>#;";

    protected static final String valueEndChars = "+,;>";

    protected static final String quotedEndChars = "\"";

    protected static final String octoEndChars = " " + valueEndChars;

    /*
     * Values that aren't printable strings are emitted as BER-encoded hex data.
     */
    protected static final String hexDigits = "0123456789ABCDEFabcdef";

    /**
     * Parse a sequence of hex pairs, each pair a UTF8 byte to a java string.
     * For example, "4C75C48D" is "Luc", the last c with caron.
     */
    protected static char[] getStringFromHexpairs(char[] hexPairs)
            throws UnsupportedEncodingException {
        ByteToCharConverter utf8_bcc;
        byte utf8_buf[];
        char char_buf[];
        int ret;
        int i, j;

        try {
            utf8_bcc = ByteToCharConverter.getConverter("UTF8");
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedEncodingException(
                    "No UTF8 byte to char converter to use for "
                            + "parsing LDAP DN String");
        }
        utf8_bcc.setSubstitutionMode(false);

        utf8_buf = new byte[hexPairs.length / 2];
        char_buf = new char[utf8_buf.length * utf8_bcc.getMaxCharsPerByte()];

        for (i = 0, j = 0; i < hexPairs.length; i++, j++) {
            utf8_buf[j] = (byte) ((Character.digit(hexPairs[i++], 16) << 4) 
                    + Character.digit(hexPairs[i], 16));
        }
        try {
            ret = utf8_bcc.convert(utf8_buf, 0, utf8_buf.length, char_buf, 0,
                    char_buf.length);
        } catch (java.io.CharConversionException e) {
            throw new IllegalArgumentException(
                    "Invalid hex pair in LDAP DN String.");
        }

        char[] out_buf = new char[ret];
        System.arraycopy(char_buf, 0, out_buf, 0, ret);
        return out_buf;
    }

}
