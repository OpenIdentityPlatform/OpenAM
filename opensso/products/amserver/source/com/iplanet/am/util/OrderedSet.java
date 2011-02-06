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
 * $Id: OrderedSet.java,v 1.3 2008/06/25 05:41:27 qcheng Exp $
 *
 */

package com.iplanet.am.util;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This class extends from AbstractSet. It uses List for storing data so that
 * data is stored in the order input. It overwrites add, iterator and size
 * methods. All other methods it needs are in the base class.
 *
 * @deprecated As of OpenSSO version 8.0
 *             {@link com.sun.identity.shared.datastruct.OrderedSet}
 */
public class OrderedSet extends AbstractSet implements Set, Serializable {
    protected List list_ = new ArrayList();

    /**
     * Adds the specified element to this set if it is not already present
     * (optional operation).
     * 
     * @param o
     *            to be added
     */
    public boolean add(Object o) {
        if (!list_.contains(o)) {
            list_.add(o);
            return true;
        }
        return false;
    }

    /**
     * Returns an iterator over the elements in this set.
     * 
     * @return an iterator over the elements in this set.
     */
    public Iterator iterator() {
        return list_.iterator();
    }

    /**
     * Returns the number of elements in this set (its cardinality).
     * 
     * @return the number of elements in this set
     */
    public int size() {
        return list_.size();
    }

    /**
     * Adds all entries in the given Set to this Set.
     */
    public boolean addAll(Set s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        Iterator it = s.iterator();
        boolean changed = false;
        while (it.hasNext()) {
            boolean c = add(it.next());
            if (c) {
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Deletes entries of a given array of indices.
     * 
     * @param array
     *            Array of indices.
     */
    public void removeAll(Integer[] array) {
        for (int i = (array.length - 1); i >= 0; --i) {
            list_.remove(array[i].intValue());
        }
    }

    /**
     * Returns the object of an given index.
     * 
     * @param index
     *            Index of which object resides
     */
    public Object get(int index) {
        return list_.get(index);
    }

    /**
     * Set value to the list.
     * 
     * @param index
     *            Index of object.
     * @param value
     *            Value of object.
     */
    public void set(int index, Object value) {
        list_.set(index, value);
    }
}
