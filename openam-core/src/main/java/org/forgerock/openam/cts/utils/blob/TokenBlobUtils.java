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

import java.io.UnsupportedEncodingException;

/**
 * Responsible for encoding and decoding binary data for Token Blobs.
 *
 * @author robert.wapshott@forgerock.com
 */
public class TokenBlobUtils {

    public static final String ENCODING = "UTF-8";

    public String getBlobAsString(Token token) {
        try {
            return new String(token.getBlob(), ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Failed to decode blob to " + ENCODING, e);
        }
    }

    public void setBlobFromString(Token token, String blob) {
        try {
            token.setBlob(blob.getBytes(ENCODING));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Failed to encode blob to " + ENCODING, e);
        }
    }
}