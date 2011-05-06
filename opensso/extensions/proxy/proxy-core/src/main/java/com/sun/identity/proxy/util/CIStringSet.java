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
 * $Id: CIStringSet.java,v 1.1 2009/10/18 18:41:52 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.util;

import java.util.Collection;
import java.util.HashMap;

/**
 * A collection of case-insensitive strings that contains no duplicate values.
 *
 * @author Paul C. Bryan
 */
public class CIStringSet extends StringSet
{
    /** Maps lowercase elements to the superclass' case-sensitive elements. */
    private final HashMap<String, String> lc = new HashMap<String, String>();

    /**
     * Creates a new, empty case-insensitive string set.
     */
    public CIStringSet() {
        super();
    }

    /**
     * Creates a new case-insensitive string set with the same elements as the
     * specified string collection.
     *
     * @param c the collection of strings whose elements are to be placed into this set.
     */
    public CIStringSet(Collection<String> c) {
        super();
        addAll(c);
    }

    @Override
    public boolean add(String o) {
        String key = o.toLowerCase();
        String cased = lc.get(key);
        if (cased == null) {
            lc.put(key, o);
            cased = o;
        }
        return super.add(cased);
    }

    @Override
    public void clear() {
        lc.clear();
        super.clear();
    }

    @Override
    public boolean contains(Object o) {
        return (o instanceof String && lc.containsKey(((String)o).toLowerCase()));
    }

    @Override
    public boolean remove(Object o) {
        return (o instanceof String && super.remove(lc.remove(((String)o).toLowerCase())));
    }
}

