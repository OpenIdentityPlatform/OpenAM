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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.forgerockrest.jwt;

import com.sun.identity.shared.encode.Base64;

/**
 * Simple object to represent a JWT as a String.
 */
public class JwtString {

    private final String header;
    private final String content;
    private final byte[] thirdPart;

    /**
     * Constructs a JWTString.
     *
     * @param jwt The JWT as a String.
     */
    public JwtString(String jwt) {

        int headerEndIndex = jwt.indexOf(".");
        String encodedHeader = jwt.substring(0, headerEndIndex);
        this.header = new String(Base64.decode(encodedHeader));

        int contentEndIndex = jwt.indexOf(".", headerEndIndex + 1);
        String encodedContent = jwt.substring(headerEndIndex + 1, contentEndIndex);
        this.content = new String(Base64.decode(encodedContent));

        String encodedThirdPart = jwt.substring(contentEndIndex + 1);
        this.thirdPart = Base64.decode(encodedThirdPart);
    }

    /**
     * Gets the header String.
     *
     * @return The JWT header.
     */
    public String getHeader() {
        return header;
    }

    /**
     * Gets the content String.
     *
     * @return The JWT content.
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets the third part String.
     *
     * @return The JWT third part.
     */
    public byte[] getThirdPart() {
        return thirdPart;
    }
}
