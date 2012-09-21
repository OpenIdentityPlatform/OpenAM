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
 * $Id: CaseInsensitiveTreeSet.java,v 1.2 2008/06/25 05:42:25 qcheng Exp $
 *
 */

package com.sun.identity.common;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/*
 * Tree set with case insensitive String comparison.
 */
public class CaseInsensitiveTreeSet extends TreeSet {

    //
    // utils
    //

    /**
     * Descending Case Insensitive Comparator
     */
    static private class DescendingComparator implements Comparator {
        public DescendingComparator() {
        }

        public int compare(Object o1, Object o2) {
            String s1 = (o1 instanceof String) ? (String) o1 : String
                    .valueOf(o1);
            String s2 = (o2 instanceof String) ? (String) o2 : String
                    .valueOf(o2);
            return String.CASE_INSENSITIVE_ORDER.compare(s2, s1);
        }

        public boolean equals(Object o) {
            return String.CASE_INSENSITIVE_ORDER.equals(o);
        }
    }

    //
    // public constructors and methods
    //

    public CaseInsensitiveTreeSet() {
        super(String.CASE_INSENSITIVE_ORDER);
    }

    public CaseInsensitiveTreeSet(boolean ascendingOrder) {
        super(ascendingOrder ? String.CASE_INSENSITIVE_ORDER
                : new DescendingComparator());
    }

    public CaseInsensitiveTreeSet(Collection c) {
        this(true, c);
    }

    public CaseInsensitiveTreeSet(boolean ascendingOrder, Collection c) {
        this(ascendingOrder);
        if (c != null) {
            Iterator iter = c.iterator();
            while (iter.hasNext()) {
                add(iter.next());
            }
        }
    }

    public CaseInsensitiveTreeSet(SortedSet set) {
        this(true, set);
    }

    public CaseInsensitiveTreeSet(boolean ascendingOrder, SortedSet set) {
        this(ascendingOrder);
        if (set != null) {
            Iterator iter = set.iterator();
            while (iter.hasNext()) {
                add(iter.next());
            }
        }
    }

    /*
     * public static void main(String[] args) { CaseInsensitiveTreeSet s = new
     * CaseInsensitiveTreeSet(); s.add("OnE"); s.add("TWo"); s.add("thrEE");
     * System.out.println("s has one: "+s.contains("one"));
     * System.out.println("s has two: "+s.contains("two"));
     * System.out.println("s has three: "+s.contains("three")); Iterator iter =
     * s.iterator(); while (iter.hasNext()) { System.out.println("s has item
     * "+(String)iter.next()); } }
     */

}
