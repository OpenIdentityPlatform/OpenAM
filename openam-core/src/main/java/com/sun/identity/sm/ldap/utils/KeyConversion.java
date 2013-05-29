/**
 * Copyright 2013 ForgeRock, Inc.
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
package com.sun.identity.sm.ldap.utils;

import com.iplanet.dpro.session.SessionID;
import com.sun.identity.session.util.SessionUtils;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;

/**
 * Responsible for perform encoding and decoding of a given Token.
 */
public class KeyConversion {
    /**
     * Encode the given key to Hexadecimal.
     *
     * @param key Non null nor empty key to encode to hexadecimal.
     * @return The non null encoded key.
     */
    public String encodeKey(final String key) {
        if (StringUtils.isEmpty(key)) {
            throw new IllegalArgumentException("Key cannot be empty");
        }
        try {
            return Hex.encodeHexString(key.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalStateException(uee);
        }
    }

    /**
     * Decode the given hexadecimal key.
     *
     * @param hexEncodedKey Non null nor empty key to decode.
     * @return Non null decoded key.
     */
    public String decodeKey(final String hexEncodedKey) {
        if (StringUtils.isEmpty(hexEncodedKey)) {
            throw new IllegalArgumentException("hexEncodedKey cannot be empty");
        }

        try {
            return new String(Hex.decodeHex(hexEncodedKey.toCharArray()));
        } catch (DecoderException de) {
            throw new IllegalStateException(de);
        }
    }

    /**
     * Encrypt the Session key.
     * @param key Non null.
     * @return Non null encrypted Session key.
     */
    public String encryptKey(SessionID key) {
        try {
            return SessionUtils.getEncryptedStorageKey(key);
        } catch (Exception e) {
            String message = MessageFormat.format(
                    "Failed to create encrypted storage key for:\n" +
                            "Session ID: {0}",
                    key);
            throw new IllegalStateException(message, e);
        }
    }
}
