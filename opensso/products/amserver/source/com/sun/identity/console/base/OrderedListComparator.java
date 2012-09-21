/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: OrderedListComparator.java,v 1.1 2008/07/02 17:21:45 veiming Exp $
 */

package com.sun.identity.console.base;

import java.util.Comparator;

public class OrderedListComparator implements Comparator {

    public int compare(Object o1, Object o2) {
        String item1 = (String)o1;
        String item2 = (String)o2;
        
        int idx1 = getIndex((String)o1);
        int idx2 = getIndex((String)o2);
        if (idx1 == idx2) {
            return 0;
        }
        return (idx1 < idx2) ? -1 : 1;
    }

    private int getIndex(String str) {
        int idx = str.indexOf("]");
        return Integer.parseInt(str.substring(1, idx));
    }
}
