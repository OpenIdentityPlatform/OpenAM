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
 * $Id: BigInt.java,v 1.2 2008/06/25 05:52:43 qcheng Exp $
 *
 */

package com.iplanet.security.util;

import java.math.BigInteger;

/**
 * A low-overhead arbitrary-precision <em>unsigned</em> integer. This is
 * intended for use with ASN.1 parsing, and printing of such parsed values.
 * Convert to "BigInteger" if you need to do arbitrary precision arithmetic,
 * rather than just represent the number as a wrapped array of bytes.
 * 
 * <P>
 * <em><b>NOTE:</b>  This class may eventually disappear, to
 * be supplanted by big-endian byte arrays which hold both signed
 * and unsigned arbitrary-precision integers.
 */
public final class BigInt {

    // Big endian -- MSB first.
    private byte[] places;

    /**
     * Constructs a "Big" integer from a set of (big-endian) bytes. Leading
     * zeroes should be stripped off.
     * 
     * @param data
     *            a sequence of bytes, most significant bytes/digits first.
     *            CONSUMED.
     */
    public BigInt(byte[] data) {
        places = (byte[]) data.clone();
    }

    /**
     * Constructs a "Big" integer from a "BigInteger", which must be positive
     * (or zero) in value.
     */
    public BigInt(BigInteger i) {
        byte[] temp = i.toByteArray();

        if ((temp[0] & 0x80) != 0)
            throw new IllegalArgumentException("negative BigInteger");

        // XXX we assume exactly _one_ sign byte is used...

        if (temp[0] != 0)
            places = temp;
        else {
            // Note that if i = new BigInteger("0"),
            // i.toByteArray() contains only 1 zero.
            if (temp.length == 1) {
                places = new byte[1];
                places[0] = (byte) 0;
            } else {
                places = new byte[temp.length - 1];
                for (int j = 1; j < temp.length; j++)
                    places[j - 1] = temp[j];
            }
        }
    }

    /**
     * Constructs a "Big" integer from a normal Java integer.
     * 
     * @param i
     *            the java primitive integer
     */
    public BigInt(int i) {
        if (i < (1 << 8)) {
            places = new byte[1];
            places[0] = (byte) i;
        } else if (i < (1 << 16)) {
            places = new byte[2];
            places[0] = (byte) (i >> 8);
            places[1] = (byte) i;
        } else if (i < (1 << 24)) {
            places = new byte[3];
            places[0] = (byte) (i >> 16);
            places[1] = (byte) (i >> 8);
            places[2] = (byte) i;
        } else {
            places = new byte[4];
            places[0] = (byte) (i >> 24);
            places[1] = (byte) (i >> 16);
            places[2] = (byte) (i >> 8);
            places[3] = (byte) i;
        }
    }

    /**
     * Converts the "big" integer to a java primitive integer.
     * 
     * @throws NumberFormatException
     *             if 32 bits is insufficient.
     */
    public int toInt() {
        if (places.length > 4)
            throw new NumberFormatException("BigInt.toInt, too big");
        int retval = 0, i = 0;
        for (; i < places.length; i++)
            retval = (retval << 8) + (places[i] & 0xff);
        return retval;
    }

    /**
     * Returns a hexadecimal printed representation. The value is formatted to
     * fit on lines of at least 75 characters, with embedded newlines. Words are
     * separated for readability, with eight words (32 bytes) per line.
     */
    public String toString() {
        return hexify();
    }

    /**
     * Returns a BigInteger value which supports many arithmetic operations.
     * Assumes negative values will never occur.
     */
    public BigInteger toBigInteger() {
        return new BigInteger(1, places);
    }

    /**
     * Returns the length of the data as a byte array.
     */
    public int byteLength() {
        return places.length;
    }

    /**
     * Returns the data as a byte array. The most significant bit of the array
     * is bit zero (as in <code>java.math.BigInteger</code>).
     */
    public byte[] toByteArray() {
        if (places.length == 0) {
            byte zero[] = new byte[1];
            zero[0] = (byte) 0;
            return zero;
        } else {
            return (byte[]) places.clone();
        }
    }

    private static final String digits = "0123456789abcdef";

    private String hexify() {
        if (places.length == 0)
            return "  0  ";

        StringBuffer buf = new StringBuffer(places.length * 2);
        buf.append("    "); // four spaces
        for (int i = 0; i < places.length; i++) {
            buf.append(digits.charAt((places[i] >> 4) & 0x0f));
            buf.append(digits.charAt(places[i] & 0x0f));
            if (((i + 1) % 32) == 0) {
                if ((i + 1) != places.length)
                    buf.append("\n    "); // line after four words
            } else if (((i + 1) % 4) == 0)
                buf.append(' '); // space between words
        }
        return buf.toString();
    }

    /**
     * Returns true iff the parameter is a numerically equivalent BigInt.
     * 
     * @param other
     *            the object being compared with this one.
     */
    public boolean equals(Object other) {
        if (other instanceof BigInt)
            return equals((BigInt) other);
        return false;
    }

    /**
     * Returns true iff the parameter is numerically equivalent.
     * 
     * @param other
     *            the BigInt being compared with this one.
     */
    public boolean equals(BigInt other) {
        if (this == other)
            return true;

        byte[] otherPlaces = other.toByteArray();
        if (places.length != otherPlaces.length)
            return false;
        for (int i = 0; i < places.length; i++)
            if (places[i] != otherPlaces[i])
                return false;
        return true;
    }
}
