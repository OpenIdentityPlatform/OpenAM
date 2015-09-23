/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package org.forgerock.openam.radius.common;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Debugging tool for serializing into space separated hex values for each byte of a sequence of octets or for reading
 * such a formatted string into a sequence of octets. For example, rfc 2865 includes examples that show such space
 * delimited hex strings in sections 7.1 through 7.3. These can readily be used in unit tests as sources for sequences
 * to test instantiation from such wire sequences our set of java objects.
 */
public final class Utils {

    /**
     * Private constructor to prevent utility class from being instantiated.
     */
    private Utils() {

    }

    /**
     * Converts a ByteBuffer's contents to a spaced hex string with each byte represented as a two hex characters with
     * each pair of characters except for the last pair separated from the following pair by a space character such as
     * "01 06 6e 65 6d 6f". This particluar sequence happens to be the wire representation of a UserNameAttribute
     * containing a name of "nemo".
     *
     * @param bfr The buffer to convert to hexadecimal character form.
     * @return The converted representation of the buffer contents.
     */
    public static final String toSpacedHex(ByteBuffer bfr) {
        bfr.mark();
        final StringBuilder sb = new StringBuilder();
        boolean firstTime = true;

        while (bfr.hasRemaining()) {
            final byte b = bfr.get();
            final int j = (b) & 0xFF; // trim off sign-extending bits
            String bt = Integer.toHexString(j);
            if (bt.length() == 1) { // prefix single chars with '0'
                bt = "0" + bt;
            }
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(" ");
            }
            sb.append(bt);
        }
        bfr.reset();
        return sb.toString();
    }

    /**
     * Convert the spaced hex form of a String into a byte array.
     *
     * @param spacedHex the string containing spaced hexadecimal values to be converted to a byte array.
     * @return a byte array holding the binary representation of the spaced hex values.
     */
    public static byte[] toByteArray(String spacedHex) {
        int idx = 0;
        final int len = spacedHex.length();
        final StringBuilder sb = new StringBuilder();
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        while (idx < len) {
            final char chr = spacedHex.charAt(idx++);

            if (chr != ' ') {
                sb.setLength(0);
                sb.append(chr);
                sb.append(spacedHex.charAt(idx++));
                final int i = Integer.parseInt(sb.toString(), 16);
                final byte b = ((byte) i);
                bytes.write(b);
            }
        }

        return bytes.toByteArray();
    }

    /**
     * Convert the spaced hex form of a String into a ByteBuffer.
     *
     * @param spacedHex the string containing spaced hexadecimal values to be converted to a byte array.
     * @return a ByteBuffer holding the binary representation of the spaced hex values.
     */
    public static ByteBuffer toBuffer(String spacedHex) {
        return ByteBuffer.wrap(toByteArray(spacedHex));
    }

    /**
     * Returns a String that reflects as much as we can safely print to output having two sequences. The first sequence
     * being a square brace delimited array of hexadecimal character values separated by spaces delimited with one pair
     * for each byte. The second is delineated by braces and contains ascii characters for values 40 to 126 and a period
     * character for all other values with each character representing on of the hexadecimal pairs. Example
     *
     * @param bytes the binary bytes to be converted to hexadecimal string representation.
     * @return the string of hexadecimal characters representing the content of the bytes.
     */
    public static String toHexAndPrintableChars(byte[] bytes) {
        final StringBuilder s = new StringBuilder().append("bytes [ ").append(Utils.toSpacedHex(ByteBuffer.wrap(bytes)))
                .append(" ] chars (");
        for (int i = 0; i < bytes.length; i++) {
            final byte b = bytes[i];
            if (b >= 32 && b < 127) {
                s.append((char) b);
            } else {
                s.append('.');
            }
        }
        s.append(")");
        return s.toString();
    }

}
