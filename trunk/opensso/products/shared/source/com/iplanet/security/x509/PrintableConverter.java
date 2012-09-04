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
 * $Id: PrintableConverter.java,v 1.2 2008/06/25 05:52:46 qcheng Exp $
 *
 */

package com.iplanet.security.x509;

import java.io.IOException;

import sun.io.CharToByteConverter;

import com.iplanet.security.util.ASN1CharStrConvMap;
import com.iplanet.security.util.DerValue;

/**
 * A AVAValueConverter that converts a Printable String attribute to a DerValue
 * and vice versa. An example an attribute that is a printable string is "C".
 * 
 * @see ASN1CharStrConvMap
 * @see AVAValueConverter
 * 
 */
public class PrintableConverter implements AVAValueConverter {
    // public constructors.

    public PrintableConverter() {
    }

    /**
     * Converts a string with ASN.1 Printable characters to a DerValue.
     * 
     * @param valueString
     *            a string with Printable characters.
     * 
     * @return a DerValue.
     * 
     * @exception IOException
     *                if a Printable CharToByteConverter is not available for
     *                the conversion.
     */
    public DerValue getValue(String valueString) throws IOException {
        return getValue(valueString, null);
    }

    public DerValue getValue(String valueString, byte[] encodingOrder)
            throws IOException {
        CharToByteConverter printable;
        byte[] bbuf = new byte[valueString.length()];
        try {
            printable = ASN1CharStrConvMap.getDefault().getCBC(
                    DerValue.tag_PrintableString);
            if (printable == null) {
                throw new IOException("No CharToByteConverter for printable");
            }
            printable.convert(valueString.toCharArray(), 0, valueString
                    .length(), bbuf, 0, bbuf.length);
        } catch (java.io.CharConversionException e) {
            throw new IllegalArgumentException(
                    "Invalid Printable String AVA Value");
        } catch (InstantiationException e) {
            throw new IOException("Cannot instantiate CharToByteConverter");
        } catch (IllegalAccessException e) {
            throw new IOException("Cannot load CharToByteConverter");
        }
        return new DerValue(DerValue.tag_PrintableString, bbuf);
    }

    /**
     * Converts a BER encoded value of PrintableString to a DER encoded value.
     * Checks if the BER encoded value is a PrintableString. NOTE only DER
     * encoded values are currently accepted on input.
     * 
     * @param berStream
     *            A byte array of the BER encoded value.
     * 
     * @return A DerValue.
     * 
     * @exception IOException
     *                if the BER value cannot be converted to a PrintableString
     *                DER value.
     */
    public DerValue getValue(byte[] berStream) throws IOException {
        DerValue value = new DerValue(berStream);
        if (value.tag != DerValue.tag_PrintableString)
            throw new IOException("Invalid Printable String AVA Value");
        return value;
    }

    /**
     * Converts a DerValue of PrintableString to a java string with
     * PrintableString characters.
     * 
     * @param avaValue
     *            a DerValue.
     * 
     * @return a string with PrintableString characters.
     * 
     * @exception IOException
     *                if the DerValue is not a PrintableString i.e. The DerValue
     *                cannot be converted to a string with PrintableString
     *                characters.
     */
    public String getAsString(DerValue avaValue) throws IOException {
        return avaValue.getPrintableString();
    }

}
