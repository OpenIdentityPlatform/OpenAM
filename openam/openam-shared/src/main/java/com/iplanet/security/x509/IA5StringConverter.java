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
 * $Id: IA5StringConverter.java,v 1.2 2008/06/25 05:52:46 qcheng Exp $
 *
 */

package com.iplanet.security.x509;

import java.io.IOException;

import sun.io.CharToByteConverter;

import com.iplanet.security.util.ASN1CharStrConvMap;
import com.iplanet.security.util.DerValue;

/**
 * A AVAValueConverter that converts a IA5String attribute to a DerValue and
 * vice versa. An example an attribute that is a IA5String string is "E".
 * 
 * @see AVAValueConverter
 */
public class IA5StringConverter implements AVAValueConverter {
    // public constructors

    /*
     * Contructs a IA5String Converter.
     */
    public IA5StringConverter() {
    }

    /*
     * Converts a string with ASN.1 IA5String characters to a DerValue.
     * 
     * @param valueString a string with IA5String characters.
     * 
     * @return a DerValue.
     * 
     * @exception IOException if a IA5String CharToByteConverter is not
     * available for the conversion.
     */
    public DerValue getValue(String valueString) throws IOException {
        return getValue(valueString, null);
    }

    public DerValue getValue(String valueString, byte[] encodingOrder)
            throws IOException {
        ASN1CharStrConvMap map;
        CharToByteConverter cbc;
        byte[] bbuf = new byte[valueString.length()];
        map = ASN1CharStrConvMap.getDefault();
        try {
            cbc = map.getCBC(DerValue.tag_IA5String);
            if (cbc == null)
                throw new IOException("No CharToByteConverter for IA5String");
            cbc.convert(valueString.toCharArray(), 0, valueString.length(),
                    bbuf, 0, bbuf.length);
        } catch (java.io.CharConversionException e) {
            throw new IllegalArgumentException(
                    "Invalid IA5String AVA Value string");
        } catch (InstantiationException e) {
            throw new IOException("Cannot instantiate CharToByteConverter");
        } catch (IllegalAccessException e) {
            throw new IOException("Illegal access loading CharToByteConverter");
        }
        return new DerValue(DerValue.tag_IA5String, bbuf);
    }

    /*
     * Converts a BER encoded value of IA5String to a DER encoded value. Checks
     * if the BER encoded value is a IA5String. NOTE only DER encoding is
     * currently supported on for the BER encoded value.
     * 
     * @param berStream a byte array of the BER encoded value.
     * 
     * @return a DerValue.
     * 
     * @exception IOException if the BER value cannot be converted to a
     * IA5String DER value.
     */
    public DerValue getValue(byte[] berStream) throws IOException {
        DerValue value = new DerValue(berStream);
        if (value.tag != DerValue.tag_IA5String)
            throw new IOException("Invalid IA5String AVA Value.");
        return value;
    }

    /*
     * Converts a DerValue of IA5String to a java string with IA5String
     * characters.
     * 
     * @param avaValue a DerValue.
     * 
     * @return a string with IA5String characters.
     * 
     * @exception IOException if the DerValue is not a IA5String i.e. The
     * DerValue cannot be converted to a string with IA5String characters.
     */
    public String getAsString(DerValue avaValue) throws IOException {
        return avaValue.getIA5String();
    }

}
