/**
 * Copyright 2013 ForgeRock AS.
 *
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
 */
package org.forgerock.openam.cts.utils.blob;

import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.util.Reject;

import java.io.UnsupportedEncodingException;
import java.lang.IllegalStateException;

/**
 * Responsible for handling the encoding and decoding of the binary object format
 * the CTS Token.
 *
 * @see org.forgerock.openam.cts.api.tokens.Token#getBlob()
 */
public class TokenBlobUtils {

    public static final String ENCODING = "UTF-8";

    /**
     * Retrieve the blob data of the Token in String format.
     *
     * @param token Non null.
     * @return maybe null if the Token has not binary data assigned.
     * @throws IllegalStateException If there was a problem decoding the string.
     */
    public String getBlobAsString(Token token) {
        Reject.ifNull(token);
        byte[] blob = token.getBlob();
        if (blob == null) {
            return null;
        }
        try {
            return toUTF8(blob);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Failed to decode blob to " + ENCODING, e);
        }
    }

    /**
     * Assign the given String to the Token by converting it to a byte[] first.
     *
     * @param token Non null Token.
     * @param blob Non null String data to convert to binary and store in Token.
     * @throws IllegalStateException If there was a problem encoding the String.
     */
    public void setBlobFromString(Token token, String blob) {
        Reject.ifNull(token, blob);
        try {
            token.setBlob(fromUTF8(blob));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Failed to encode blob to " + ENCODING, e);
        }
    }

    /**
     * Convert a byte array into a String using the UTF-8 encoding.
     *
     * @param data Non null byte[] to be converted.
     * @return A non null but possibly empty.
     * @throws UnsupportedEncodingException indicates there was a problem decoding the data.
     */
    public String toUTF8(byte[] data) throws UnsupportedEncodingException {
        Reject.ifNull(data);
        return new String(data, ENCODING);
    }

    /**
     * Convert the String contents into an encoded binary representation.
     *
     * @param contents Non null contents to convert.
     * @return Non null response.
     * @throws UnsupportedEncodingException If there was an error performing the conversion.
     */
    public byte[] fromUTF8(String contents) throws UnsupportedEncodingException {
        Reject.ifNull(contents);
        return contents.getBytes(ENCODING);
    }
}