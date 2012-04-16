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
 * $Id: UnicodeInputStreamReader.java,v 1.2 2008/06/25 05:41:28 qcheng Exp $
 *
 */

package com.iplanet.am.util;

import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

/**
 * The <code>UnicodeInputStreamReader</code> class converts from a byte stream
 * that contains Java Unicode encoded characters (\\uXXXX) to Unicode
 * characters. It can be used to read files that have been produced using the
 * native2ascii tool.
 */
public class UnicodeInputStreamReader extends FilterReader {
    /**
     * Creates a Unicode input stream reader that reads from the given stream.
     * 
     * @param is
     *            the InputStream from which to read
     */
    public UnicodeInputStreamReader(InputStream is)
            throws UnsupportedEncodingException {
        super(new InputStreamReader(is, "UTF-8"));
    }

    public boolean markSupported() {
        return false;
    }

    /**
     * Read one character from the stream. See the other read method for
     * information on decoding that is performed.
     * 
     * @return -1 on end of input, otherwise the character that was read
     */
    public int read() throws IOException {
        char cbuf[] = new char[1];
        return read(cbuf, 0, 1) == 1 ? cbuf[0] : -1;
    }

    /**
     * Read up to len characters from the stream and put them in cbuf starting
     * at offset off. As characters are read, the following conversions are
     * performed:
     * 
     * \\uXXXX is converted to one Unicode character having the value
     * represented by the four hex digits. \\ is converted to \ \X any character
     * preceded by \ is converted to that character.
     * 
     * @param cbuf
     *            the array of characters that is filled in
     * @param off
     *            the offset at which to start placing characters
     * @param len
     *            the maximum number of characters to read
     * @return the number of characters read
     */
    public int read(char cbuf[], int off, int len) throws IOException {
        int c;
        char cc;

        for (int i = 0; i < len; i++) {
            c = in.read();

            if (c == -1) {
                return (i > 0) ? i : -1;
            }

            cc = (char) c;

            if (cc == '\\') {
                c = in.read();

                if (c == -1) {
                    return (i > 0) ? i : -1;
                }

                cc = (char) c;

                if (cc == 'u') {
                    // Read the xxxx
                    int value = 0;

                    for (int j = 0; j < 4; j++) {
                        c = in.read();

                        if (c == -1) {
                            return (i > 0) ? i : -1;
                        }

                        cc = (char) c;

                        switch (cc) {

                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            value = (value << 4) + cc - '0';
                            break;

                        case 'a':
                        case 'b':
                        case 'c':
                        case 'd':
                        case 'e':
                        case 'f':
                            value = (value << 4) + 10 + cc - 'a';
                            break;

                        case 'A':
                        case 'B':
                        case 'C':
                        case 'D':
                        case 'E':
                        case 'F':
                            value = (value << 4) + 10 + cc - 'A';
                            break;

                        default:
                            throw new IllegalArgumentException(
                                    "Malformed \\uxxxx encoding.");
                        }
                    }

                    cbuf[off + i] = (char) value;
                } else {
                    cbuf[off + i] = cc;
                }
            } else {
                cbuf[off + i] = cc;
            }
        }

        return len;
    }
}
