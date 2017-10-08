package org.forgerock.openam.utils;/*
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
* Copyright 2016 ForgeRock AS.
*/

/**
 * Representative of different alphabets.
 */
public enum Alphabet implements CodeGeneratorSource {

    /** Standard set. **/
    ALPHANUMERIC("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"),

    BASE58("123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"),

    ALPHA("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"),

    UPPER_ALPHA("ABCDEFGHIJKLMNOPQRSTUVWXYZ"),

    LOWER_ALPHA("abcdefghijklmnopqrstuvwxyz"),

    DECIMAL_DIGITS("0123456789"),

    HEX_DIGITS("0123456789abcdef");

    final private String chars;

    Alphabet(String chars) {
        this.chars = chars;
    }

    /**
     * Get a String containing the characters which are valid for this alphabet.
     *
     * @return A String containing valid characters.
     */
    public String getChars() {
        return chars;
    }
}

