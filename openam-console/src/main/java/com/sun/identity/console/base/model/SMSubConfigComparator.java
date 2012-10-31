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
 * $Id: SMSubConfigComparator.java,v 1.2 2008/06/25 05:42:50 qcheng Exp $
 *
 */

package com.sun.identity.console.base.model;

import java.text.Collator;
import java.util.Comparator;

/* - NEED NOT LOG - */

/**
 * Sub Configuration Display Object Comparator.
 */
public class SMSubConfigComparator
    implements Comparator
{
    private Collator collator = null;

    /**
     * Creates an instance of <code>SMSubConfigComparator</code>.
     *
     * @param collator for locale base sorting
     */
    public SMSubConfigComparator(Collator collator) {
        this.collator = collator;
    }

    /**
     * Compares two <code>SMSubConfig</code> object. Ordered by
     * its type and then name.
     *
     * @return 0 if their types and names are the same.
     *         -1 if <code>o1</code>'s type is lexicographically less than
     *         <code>o2</code>'s type. compares their name if their type is 
     *         the same.
     *         1 if <code>o2</code>'s type is lexicographically less than
     *         <code>o2</code>'s type. compares their name if their type is 
     *         the same.
     */
    public int compare(Object o1, Object o2) {
        SMSubConfig s1 = (SMSubConfig)o1;
        SMSubConfig s2 = (SMSubConfig)o2;

        int compareType = collator.compare(s1.getType(), s2.getType());
        return (compareType != 0)
            ? compareType : collator.compare(s1.getName(), s2.getName());
    }
}
