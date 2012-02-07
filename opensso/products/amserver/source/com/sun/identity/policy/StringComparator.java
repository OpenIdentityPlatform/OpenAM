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
 * $Id: StringComparator.java,v 1.2 2008/06/25 05:43:45 qcheng Exp $
 *
 */




package com.sun.identity.policy;

import java.util.Comparator;

/**
 * The class <code>StringComparator</code> is used to
 * create a sorted set of strings.
 */
class StringComparator implements Comparator {

    private static StringComparator comp = null;

    /**
     * Returns an instance of <code>StringComparator</code>
     */
    public static StringComparator getInstance() {
	if (comp == null)
	    comp = new StringComparator();
	return (comp);
    }

    /**
     * Compares two objects
     * @param o1 object one
     * @param o2 object 2
     * @return comparison result, 
     * 0 if <code>o1</code> and <code>02</code> are equal,
     * positive integer if <code>o1</code>  follows <code>02</code>,
     * negative integer if <code>o1</code>  precedes <code>02</code>
     */
    public int compare(Object o1, Object o2) {
	String s1 = (String)o1;
	return (s1.compareTo((String)o2));
    }

    /**
     * Checks whether an object  is equal to this object
     * @return <code>true</code> if the object equals this object,
     * otherwise <code>false</code>
     */
    public boolean equals(Object o1) {
	if (o1 instanceof StringComparator)
	    return (true);
	else
	    return (false);
    }
}
