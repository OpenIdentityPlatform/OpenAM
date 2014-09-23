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
 */

package com.sun.identity.common;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * An implementation of Case Insensitive Hash Set with case preservation.
 * Strings are converted to case insensitive strings before being hashed, but
 * original case is preserved. The iterator returns the case preserved Strings
 * that can be casted to a String, no different from an iterator from the
 * standard Hash Set.
 */
public class CaseInsensitiveHashSet extends HashSet {

    static private class CaseInsensitiveKeyIterator implements Iterator {
        Iterator mIterator;

        public CaseInsensitiveKeyIterator(Iterator iterator) {
            mIterator = iterator;
        }

        public boolean hasNext() {
            return mIterator.hasNext();
        }

        public Object next() {
            Object nextIter = mIterator.next();
            if (nextIter instanceof CaseInsensitiveKey) {
                return ((CaseInsensitiveKey) nextIter).toString();
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

    public boolean add(Object o) {
        boolean retval;
        if (o instanceof String) {
            CaseInsensitiveKey ciKey = new CaseInsensitiveKey((String) o);
            retval = super.add(ciKey);
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
    public Iterator iterator() {
        // The CaseInsensitiveKeyIterator allows the iterator to return
        // elements as regular strings.
        return new CaseInsensitiveKeyIterator(super.iterator());
    }

    public Object[] toArray() {
        return toArray(null);
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
