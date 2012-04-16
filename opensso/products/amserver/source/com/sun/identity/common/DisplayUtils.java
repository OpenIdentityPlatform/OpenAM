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
 * $Id: DisplayUtils.java,v 1.2 2008/06/25 05:42:25 qcheng Exp $
 *
 */

package com.sun.identity.common;

/**
 * This class provides utility methods user interfaces.
 */
public abstract class DisplayUtils {
    /**
     * Returns true if the given pattern is contained in the string.
     * 
     * @param string
     *            to examine
     * @param pattern
     *            to match
     * @return true if string matches <code>filter</code>
     */
    public static boolean wildcardMatch(String string, String pattern) {
        string = string.toLowerCase();
        pattern = pattern.toLowerCase();

        if (pattern.equals("*") || pattern.equals(string)) {
            return true;
        }

        int length = pattern.length();
        int wildCardIndex = pattern.indexOf("*");

        if (wildCardIndex >= 0) {
            String patternSubStr = pattern.substring(0, wildCardIndex);

            if (!string.startsWith(patternSubStr, 0)) {
                return false;
            }

            int beginIndex = patternSubStr.length() + 1;
            int stringIndex = 0;

            if (wildCardIndex > 0) {
                stringIndex = beginIndex;
            }

            String sub = pattern.substring(beginIndex, length);

            while ((wildCardIndex = pattern.indexOf("*", beginIndex)) != -1) {
                patternSubStr = pattern.substring(beginIndex, wildCardIndex);

                if (string.indexOf(patternSubStr, stringIndex) == -1) {
                    return false;
                }

                beginIndex = wildCardIndex + 1;
                stringIndex = stringIndex + patternSubStr.length() + 1;
                sub = pattern.substring(beginIndex, length);
            }

            if (string.endsWith(sub)) {
                return true;
            }
        }
        return false;
    }
}
