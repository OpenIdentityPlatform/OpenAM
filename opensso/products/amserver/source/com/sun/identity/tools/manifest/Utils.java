/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
* $Id: Utils.java,v 1.2 2008/09/04 22:26:12 kevinserwin Exp $
*/

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.tools.manifest;

import java.io.InputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.ListIterator;

public class Utils implements ManifestConstants {
    
    static byte[] buf = new byte[BUFFER_SIZE];
    
    /**
     * Run the hash with the pass in MessageDigest and InputStream
     *
     * @param md The MessageDigest to be used.
     * @param in The InputStream of the data to be hashed.
     * @return The MessageDigest object after doing the hashing.
     */
    public static MessageDigest hashing(MessageDigest md, InputStream in){
        try {
            DigestInputStream din = new DigestInputStream(in, md);
            synchronized(buf){
                while (din.read(buf) != -1);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return md;
    }
    
    /**
     * Calculate and return the hash value with byte array.
     *
     * @param algorithm The string to indicate the hashing algorithm to be used.
     * @param in The InputStream of the data to be hashed.
     * @return The hash value in byte array.
     */
    
    public static byte[] getHash(String algorithm, InputStream in){
        try {
            MessageDigest md=MessageDigest.getInstance(algorithm);
            return hashing(md, in).digest();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    /**
     * Translate the byte array into Hex String.
     *
     * @param hash The byte array of hash value.
     * @return The string of the hash value in Hex.
     */
    
    public static String translateHashToString(byte[] hash){
        StringBuilder hashBuffer = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            hashBuffer.append(Character.forDigit((hash[i] >> 4) & 0x0F, 16));
            hashBuffer.append(Character.forDigit(hash[i] & 0x0F, 16));
        }
        return hashBuffer.toString();
    }
    
    /**
     * Check whether the string matches the pattern.
     *
     * @param actualString The string to be checked.
     * @param patterns A list of patterns to check for.
     * @param wildCard A character which is used as wild card in the pattern.
     * @return Whether the string matches one of the patterns in the list.
     */
    
    public static boolean isMatch(String actualString, LinkedList patterns,
        char wildCard){
        boolean matched = false;
        for (ListIterator iter = patterns.listIterator(0); iter.hasNext(); ) {
            if(isMatch(actualString, (String) iter.next(), wildCard)){
                matched = true;
                break;
            }
        }
        return matched;
    }
    
    /**
     * Check whether the string matches the pattern.
     *
     * @param actualString The string to be checked.
     * @param pattern A pattern to check for.
     * @param wildCard A character which is used as wild card in the pattern.
     * @return Whether the string matches one of the patterns in the list.
     */
    
    public static boolean isMatch(String actualString, String pattern,
        char wildCard){
        String tempPattern=pattern.trim();
        int matchOffset = 0;
        boolean matched = true;
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < tempPattern.length(); i++) {
            if (tempPattern.charAt(i) != wildCard) {
                buffer.append(tempPattern.charAt(i));
            }
            if ((i == (tempPattern.length() - 1)) ||
                (tempPattern.charAt(i) == wildCard)) {
                if (buffer.length() > 0) {
                    int matchedIndex = actualString.indexOf(buffer.toString(),
                            matchOffset);
                    if (matchedIndex >= matchOffset) {
                        if (i != (tempPattern.length() - 1)) {
                            matchOffset = matchedIndex +
                                    buffer.length();
                        } else {
                            if (tempPattern.charAt(i) != wildCard) {
                                if (actualString.substring(matchedIndex).
                                    length() !=
                                    buffer.length()) {
                                    matched = false;
                                    break;
                                }
                            }
                        }
                    } else {
                        matched = false;
                        break;
                    }
                    buffer = new StringBuffer();
                }
            }
        }
        return matched;
    }
}
