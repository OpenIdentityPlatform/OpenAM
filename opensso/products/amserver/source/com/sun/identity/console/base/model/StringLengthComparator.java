/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: StringLengthComparator.java,v 1.2 2008/06/25 05:42:50 qcheng Exp $
 *
 */

package com.sun.identity.console.base.model;

import java.util.Comparator;

/* - NEED NOT LOG - */

/**
 * This compares the length of two string.
 */
public class StringLengthComparator implements Comparator {
    /**
     * Performs string comparison on string length.
     *
     * @param s1 first string.
     * @param s2 second string.
     * @return 0 if both strings have same length; less than 0 if
     *         length of <code>s1</code> is longer than length of
     *         <code>s2</code>; greater than 0 if length of <code>s1</code> is
     *         shorted than length of <code>s2</code>.
     */
    public int compare(Object s1, Object s2) {
        int result = 0;
        String str1 = (String)s1;
        String str2 = (String)s2;
        int len1 = ((String)s1).length();
        int len2 = ((String)s2).length();

        if (len1 > len2) {
            result = -1;
        } else if (len1 < len2) {
            result = 1; 
        }
        return result;
    }
}
