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
 * $Id: AVAValueConverter.java,v 1.2 2008/06/25 05:52:46 qcheng Exp $
 *
 */

package com.iplanet.security.x509;

import java.io.IOException;

import com.iplanet.security.util.DerValue;

/**
 * Interface for classes that convert a attribute value string to a DER encoded
 * ASN.1 value and vice versa. The converters are associated with attribute
 * types, such as directory string, ia5string, etc.
 * 
 * <P>
 * For example, to convert a string, such as an organization name for the "O"
 * attribute to a DerValue, the "O" attribute is mapped to the DirStrConverter
 * which is used to convert the organization name to a DER encoded Directory
 * String which is a DerValue of a ASN.1 PrintableString, T.61String or
 * UniversalString for the organization name.
 */
public interface AVAValueConverter {
    /**
     * Converts a string to a DER encoded attribute value.
     * 
     * @param valueString
     *            An AVA value string not encoded in any form.
     * 
     * @return A DerValue object.
     * 
     * @exception IOException
     *                if an error occurs during the conversion.
     */
    public DerValue getValue(String valueString) throws IOException;

    /**
     * Converts a string to a DER encoded attribute value. Specify the order of
     * DER tags to use if more than one encoding is possible. Currently
     * Directory Strings can have different order for backwards compatibility.
     * By 2003 all should be UTF8String.
     * 
     * @param valueString
     *            An AVA value string not encoded in any form.
     * 
     * @return A DerValue object.
     * 
     * @exception IOException
     *                if an error occurs during the conversion.
     */
    public DerValue getValue(String valueString, byte[] tags)
            throws IOException;

    /**
     * Converts a BER encoded value to a DER encoded attribute value.
     * 
     * @param berStream
     *            A byte array of the BER encoded AVA value.
     * @return A DerValue object.
     */
    public DerValue getValue(byte[] berStream) throws IOException;

    /**
     * Converts a DER encoded value to a string, not encoded in any form.
     * 
     * @param avaValue
     *            A DerValue object.
     * 
     * @return A string for the value or null if it can't be converted.
     * 
     * @exception IOException
     *                if an error occurs during the conversion.
     */
    public String getAsString(DerValue avaValue) throws IOException;
}
