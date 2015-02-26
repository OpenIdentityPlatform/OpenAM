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
 * $Id: ISValidation.java,v 1.4 2008/06/25 05:42:07 qcheng Exp $
 *
 */


package com.sun.identity.authentication.util;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.StringTokenizer;
import com.sun.identity.shared.debug.Debug;

/**
  * This class is an utility to detect the specified
  * pattern in the given string 
  * 
  */
public class ISValidation {
    private static Debug debug = null;
    private static String SEPERATOR = "|";
    public ISValidation() {}
    
    /**
     * Checks for invalid characters in the source string .
     *
     * @param str Source string which should be validated.
     * @param regEx Pattern for which to search.
     * @param tmpDebug Debug object used for logging debug info.
     * @return <code>false</code> when it detects specified pattern within
     *         source string which need to be validated OR if source
     *         string is null.
     */
    public static boolean validate(String str, String regEx, Debug tmpDebug) {
        // Assign new Debug object
        debug = tmpDebug;
        if (str == null || str.length() == 0) {
            debug.message("Source string is null or empty");
            return false;
        }
        char [] value = str.toCharArray();
        int count = value.length;
        Set hSet = new HashSet();
        StringTokenizer st = new StringTokenizer(regEx, SEPERATOR);
        while (st.hasMoreTokens()) {
            hSet.add(st.nextToken());
        }
        Iterator itr = hSet.iterator();
        while (itr.hasNext()) {
            String obj = (String) itr.next();
            if (process(value, obj, 0, count) > -1) {
                debug.message("detected invalid chars ...");
                return false;
            }

        }
        return true;
    }
    
    /**
     * Returns the index within this string of the first occurrence of the
     * specified substring, starting at the specified index.  The integer
     * returned is the smallest value <tt>k</tt> for which:
     * <blockquote><pre>
     *     k >= Math.min(fromIndex, str.length()) &&
     *                   this.toString().startsWith(str, k)
     * </pre></blockquot>
     * If no such value of <i>k</i> exists, then -1 is returned.
     *
     * @param   value       the characters being searched.
     * @param   str         the substring for which to search.
     * @param   fromIndex   the index from which to start the search.
     * @return  the index within this string of the first occurrence of the
     *          specified substring, starting at the specified index.
     */
    private static int process(
        char[] value,
        String str,
        int fromIndex,
        int count) {
        return processString(value, 0, count,
                       str.toCharArray(), 0, str.length(), fromIndex);
    }
    
    /**
     * This method is used to do searches. The
     * source is the character array being searched, and the target
     * is the string being searched for.
     *
     * @param   source       the characters being searched.
     * @param   sourceOffset offset of the source string.
     * @param   sourceCount  count of the source string.
     * @param   target       the characters being searched for.
     * @param   targetOffset offset of the target string.
     * @param   targetCount  count of the target string.
     * @param   fromIndex    the index to begin searching from.
     * @return  return the index within this string of the first
     *          occurrence of specified substring, starting at the 
     *          specified index.
     */

    private static int processString(char[] source, int sourceOffset,
                                     int sourceCount,
                                     char[] target, int targetOffset,
                                     int targetCount,int fromIndex) {
        if (fromIndex >= sourceCount) {
            return (targetCount == 0 ? sourceCount : -1);
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (targetCount == 0) {
            return fromIndex;
        }

        char first = target[targetOffset];
        int i = sourceOffset + fromIndex;
        int max = sourceOffset + (sourceCount - targetCount);

        startSearchForFirstChar:while (true) {
            /* Look for first character. */
            while (i <= max && source[i] != first) {
                i++;
            }
            if (i > max) {
                return -1;
            }

            /* Found first character, now look at the rest of v2 */
            int j = i + 1;
            int end = j + targetCount - 1;
            int k = targetOffset + 1;
            while (j < end) {
                if (source[j++] != target[k++]) {
                    i++;
                    /* Look for str's first char again. */
                    continue startSearchForFirstChar;
                }
            }
            return i - sourceOffset; /* Found whole string. */
        }
    }

}
