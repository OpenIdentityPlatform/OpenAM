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
 * $Id: DirStrConverter.java,v 1.2 2008/06/25 05:52:46 qcheng Exp $
 *
 */

package com.iplanet.security.x509;

import java.io.IOException;

import sun.io.CharToByteConverter;

import com.iplanet.security.util.ASN1CharStrConvMap;
import com.iplanet.security.util.DerValue;

/**
 * A DirStrConverter converts a string to a DerValue of ASN.1 Directory String,
 * which is a CHOICE of Printable (subset of ASCII), T.61 (Teletex) or Universal
 * String (UCS-4), and vice versa.
 * 
 * <p>
 * The string to DerValue conversion is done as follows. If the string has only
 * PrintableString characters it is converted to a ASN.1 Printable String using
 * the PrintableString CharToByteConverter from the global default
 * ASN1CharStrConvMap. If it has only characters covered in the PrintableString
 * or T.61 character set it is converted to a ASN.1 T.61 string using the T.61
 * CharToByteConverter from the ASN1CharStrCovnMap. Otherwise it is converted to
 * a ASN.1 UniversalString (UCS-4 character set) which covers all characters.
 * 
 * @see AVAValueConverter
 * @see ASN1CharStrConvMap
 */

public class DirStrConverter implements AVAValueConverter {
    // public constructors

    /**
     * Constructs a DirStrConverter.
     */
    public DirStrConverter() {
    }

    // public functions

    /**
     * Converts a string to a DER encoded ASN1 Directory String, which is a
     * CHOICE of PrintableString, T.61String or UniversalString. The string is
     * taken as is i.e. should not be in Ldap DN string syntax.
     * 
     * @param ds
     *            a string representing a directory string value.
     * 
     * @return a DerValue
     * 
     * @exception IOException
     *                if the string cannot be converted, such as when a
     *                UniversalString CharToByteConverter isn't available and
     *                the string contains characters covered only in the
     *                universal string (or UCS-4) character set.
     */
    private static byte[] DefEncodingOrder = new byte[] {
            DerValue.tag_PrintableString, DerValue.tag_T61String,
            DerValue.tag_UniversalString };

    public static synchronized void setDefEncodingOrder(byte[] defEncodingOrder)
    {
        DefEncodingOrder = defEncodingOrder;
    }

    public DerValue getValue(String ds) throws IOException {
        return getValue(ds, DefEncodingOrder);
    }

    /**
     * Like getValue(String) with specified DER tags as encoding order.
     */
    public DerValue getValue(String ds, byte[] tags) throws IOException {
        // try to convert to printable, then t61 the universal -
        // i.e. from minimal to the most liberal.

        int ret = -1;
        CharToByteConverter cbc;
        byte[] bbuf, derBuf;
        int i;

        if (tags == null || tags.length == 0)
            tags = DefEncodingOrder;

        bbuf = new byte[4 * ds.length()];
        for (i = 0; i < tags.length; i++) {
            try {
                cbc = ASN1CharStrConvMap.getDefault().getCBC(tags[i]);
                if (cbc == null)
                    continue;
                ret = cbc.convert(ds.toCharArray(), 0, ds.length(), bbuf, 0,
                        bbuf.length);
                break;
            } catch (java.io.CharConversionException e) {
                continue;
            } catch (InstantiationException e) {
                throw new IOException("Cannot instantiate CharToByteConverter");
            } catch (IllegalAccessException e) {
                throw new IOException(
                        "Illegal Access loading CharToByteConverter");
            }
        }
        if (ret == -1) {
            throw new IOException(
                    "Cannot convert the directory string value " +
                    "to a ASN.1 type");
        }

        derBuf = new byte[ret];
        System.arraycopy(bbuf, 0, derBuf, 0, ret);
        return new DerValue(tags[i], derBuf);
    }

    /**
     * Creates a DerValue from a BER encoded value, obtained from for example a
     * attribute value in octothorpe form of a Ldap DN string. Checks if the BER
     * encoded value is legal for a DirectoryString.
     * 
     * NOTE: currently only supports DER encoding for the BER encoded value.
     * 
     * @param berByteStream 
     *            Byte array of a BER encoded value.
     * 
     * @return DerValue object.
     * 
     * @exception IOException
     *                If the BER value cannot be converted to a valid Directory
     *                String DER value.
     */
    public DerValue getValue(byte[] berByteStream) throws IOException {
        DerValue value = new DerValue(berByteStream);

        /*
         * if (value.tag != DerValue.tag_PrintableString && value.tag !=
         * DerValue.tag_T61String && value.tag != DerValue.tag_UniversalString)
         * throw new IOException("Invalid Directory String AVA Value");
         */

        return value;
    }

    /**
     * Converts a DerValue to a string. The string is not in any syntax, such as
     * RFC1779 string syntax.
     * 
     * @param avaValue
     *            a DerValue
     * @return a string if the value can be converted.
     * @exception IOException
     *                if a ByteToCharConverter needed for the conversion is not
     *                available.
     */
    public String getAsString(DerValue avaValue) throws IOException {
        /*
         * if (avaValue.tag != DerValue.tag_PrintableString && avaValue.tag !=
         * DerValue.tag_BMPString && avaValue.tag !=
         * DerValue.tag_UniversalString && avaValue.tag !=
         * DerValue.tag_T61String) throw new IllegalArgumentException( "Invalid
         * Directory String value"); // NOTE will return null if a
         * ByteToCharConverter is not available.
         */
        return avaValue.getASN1CharString();
    }

}
