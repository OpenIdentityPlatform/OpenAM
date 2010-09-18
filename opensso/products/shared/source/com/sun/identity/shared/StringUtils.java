/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: StringUtils.java,v 1.3 2009/11/20 00:30:40 exu Exp $
 *
 */

package com.sun.identity.shared;

/**
 * This class provides string related helper methods.
 */
public class StringUtils {

     // ampersand character '&'
     public static final String AMPERSAND = "&";
     // HTML escape code for character '&'
     public static final String ESCAPE_AMPERSAND = "&#38;";
     // character '|' used as property value delimiter
     public static final String PROPERTY_VALUE_DELIMITER = "|";
     // HTML escape code for character '|'
     public static final String ESCAPE_DELIMITER = "&#124;";


    private StringUtils() {
    }

    /**
     * Returns substituted string.
     *
     * @param orig Original string.
     * @param pattern String substitution matching pattern.
     * @param str Substituting string.
     * @return substituted string.
     */
    public static String strReplaceAll(String orig, String pattern, String str){
        return orig.replaceAll(pattern, str.replaceAll("[$]", "\\\\\\$"));
    }

    /**
     * Returns special character escaped string.
     *
     * @param value original string.
     * @return special character escaped string.
     */
    public static String getEscapedValue(String value) {
        if (value != null) {
            return value.replaceAll(AMPERSAND, ESCAPE_AMPERSAND).replaceAll(
                         "\\" + PROPERTY_VALUE_DELIMITER, ESCAPE_DELIMITER);
        }
        return null;
    }

    /**
     * Returns special character un-escaped string.
     *
     * @param value special character escaped string.
     * @return un-escaped string.
     */
    public static String getUnescapedValue(String value) {
        if (value != null) {
            return value.replaceAll(ESCAPE_DELIMITER, PROPERTY_VALUE_DELIMITER).                replaceAll(ESCAPE_AMPERSAND, AMPERSAND);
        }
        return null;
    }
}
