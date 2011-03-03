/* The contents of this file are subject to the terms
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
 * $Id: IntegerUtil.java,v 1.2 2009/10/14 17:42:04 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.util;

/**
 * Miscellaneous integer utility methods.
 *
 * @author Paul C. Bryan
 */
public class IntegerUtil
{
    /**
     * Parses a string into an integer value.
     *
     * @param s The string containing the integer to parse.
     * @param val The value to return in the event an integer could not be parsed.
     * @return the integer value, or <tt>val</tt> if it could not be parsed.
     */
    public static final int parseInt(String s, int val) {
        try {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException nfe) {
            return val;
        }
    }
}

