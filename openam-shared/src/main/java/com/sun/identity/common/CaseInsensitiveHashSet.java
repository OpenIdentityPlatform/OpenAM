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
 * $Id: CaseInsensitiveHashSet.java,v 1.4 2008/06/25 05:42:25 qcheng Exp $
 *
 * Portions copyright 2015 ForgeRock AS.
 */

package com.sun.identity.common;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * An implementation of Case Insensitive Hash Set with case preservation.
 * Strings are converted to case insensitive strings before being hashed, but
 * original case is preserved. The iterator returns the case preserved Strings
 * that can be casted to a String, no different from an iterator from the
 * standard Hash Set.
 */
public class CaseInsensitiveHashSet<T> extends HashSet<T> {

    static private class CaseInsensitiveKeyIterator<T> implements Iterator<T> {
        Iterator<T> mIterator;

        public CaseInsensitiveKeyIterator(Iterator<T> iterator) {
            mIterator = iterator;
        }

        public boolean hasNext() {
            return mIterator.hasNext();
        }

        public T next() {
            T nextIter = mIterator.next();
            if (nextIter instanceof CaseInsensitiveKey) {
                return (T) ((CaseInsensitiveKey) nextIter).toString();
            } else {
                return nextIter;
            }
        }

        public void remove() {
            mIterator.remove();
        }
    }

    public CaseInsensitiveHashSet() {
        super();
    }
    
    public CaseInsensitiveHashSet(Collection c) {
        super(c);
    }

    public CaseInsensitiveHashSet(int initialCapacity) {
        super(initialCapacity);
    }

    public CaseInsensitiveHashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public boolean add(T o) {
        boolean retval;
        if (o instanceof String) {
            CaseInsensitiveKey ciKey = new CaseInsensitiveKey((String) o);
            retval = super.add((T) ciKey);
        } else {
            retval = super.add(o);
        }
        return retval;
    }

    public boolean contains(Object o) {
        boolean retval;
        if (o instanceof String) {
            retval = super.contains(new CaseInsensitiveKey((String) o));
        } else {
            retval = super.contains(o);
        }
        return retval;
    }

    public boolean remove(Object o) {
        boolean retval;
        if (o instanceof String) {
            retval = super.remove(new CaseInsensitiveKey((String) o));
        } else {
            retval = super.remove(o);
        }
        return retval;
    }

    /**
     * @return an iterator of objects in the set, no different than iterator
     *         from the standard HashSet.
     */
    public Iterator<T> iterator() {
        // The CaseInsensitiveKeyIterator allows the iterator to return
        // elements as regular strings.
        return new CaseInsensitiveKeyIterator(super.iterator());
    }

    public Object[] toArray() {
        return toArray((Object[])null);
    }
    
    /**
     * Removes all elements specified in the parameter collection from the current set.
     *
     * @param c The collection of elements that need to be removed from this set.
     * @return <code>true</code> if at least one element has been removed from this set.
     * @see java.util.AbstractSet#removeAll(java.util.Collection)
     */
    @Override
    public boolean removeAll(Collection c) {
        // Override to ensure that the collection we are removing
        // is an instance of CaseInsensitiveHashSet 
        if (c instanceof CaseInsensitiveHashSet) {
            return super.removeAll(c);
        } else {
            Set<String> ciHashSet = new CaseInsensitiveHashSet(c);
            return super.removeAll(ciHashSet);
        }           
    }

    /**
     * @return an array containing objects in the set, no different than an
     *         array returned from the standard HashSet.
     */
    public Object[] toArray(Object[] a) {
        Object[] ret = null;
        Object[] arr = super.toArray();
        if (a != null && a.length >= arr.length) {
            ret = a;
        } else {
            ret = new Object[arr.length];
        }
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] instanceof CaseInsensitiveKey) {
                ret[i] = ((CaseInsensitiveKey) arr[i]).toString();
            } else {
                ret[i] = arr[i];
            }
        }
        if (a != null && ret == a && a.length > arr.length) {
            ret[arr.length] = null;
        }
        return ret;
    }
}
