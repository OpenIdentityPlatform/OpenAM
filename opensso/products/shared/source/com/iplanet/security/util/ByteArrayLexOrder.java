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
 * $Id: ByteArrayLexOrder.java,v 1.2 2008/06/25 05:52:43 qcheng Exp $
 *
 */

package com.iplanet.security.util;

import java.util.Comparator;

/**
 * Compare two byte arrays in lexicographical order.
 */
public class ByteArrayLexOrder implements Comparator {

    /**
     * Perform lexicographical comparison of two byte arrays, regarding each
     * byte as unsigned. That is, compare array entries in order until they
     * differ--the array with the smaller entry is "smaller". If array entries
     * are equal till one array ends, then the longer array is "bigger".
     * 
     * @param obj1
     *            first byte array to compare.
     * @param obj2
     *            second byte array to compare.
     * @return negative number if obj1 < obj2, 0 if obj1 == obj2, positive
     *         number if obj1 > obj2.
     * 
     * @exception <code>ClassCastException</code> if either argument is not a
     *                byte array.
     */
    public final int compare(Object obj1, Object obj2) {

        byte[] bytes1 = (byte[]) obj1;
        byte[] bytes2 = (byte[]) obj2;

        int diff;
        for (int i = 0; i < bytes1.length && i < bytes2.length; i++) {
            diff = (bytes1[i] & 0xFF) - (bytes2[i] & 0xFF);
            if (diff != 0) {
                return diff;
            }
        }
        // if array entries are equal till the first ends, then the
        // longer is "bigger"
        return bytes1.length - bytes2.length;
    }

}
