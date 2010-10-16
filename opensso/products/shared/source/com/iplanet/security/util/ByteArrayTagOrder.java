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
 * $Id: ByteArrayTagOrder.java,v 1.2 2008/06/25 05:52:43 qcheng Exp $
 *
 */

package com.iplanet.security.util;

import java.util.Comparator;

public class ByteArrayTagOrder implements Comparator {

    /**
     * Compare two byte arrays, by the order of their tags, as defined in ITU-T
     * X.680, sec. 6.4. (First compare tag classes, then tag numbers, ignoring
     * the constructivity bit.)
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

        // tag order is same as byte order ignoring any difference in
        // the constructivity bit (0x02)
        return (bytes1[0] | 0x20) - (bytes2[0] | 0x20);
    }

}
