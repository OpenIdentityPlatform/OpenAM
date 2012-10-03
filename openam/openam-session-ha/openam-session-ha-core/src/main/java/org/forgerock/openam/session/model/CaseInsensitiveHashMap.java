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
 * $Id: CaseInsensitiveHashMap.java,v 1.4 2009/01/13 18:08:54 leiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package org.forgerock.openam.session.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A case insensitive hash map with case preservation. If key is a String, a
 * case insensitive hash code is used for hashing but original case of the key
 * is preserved.
 */
public class CaseInsensitiveHashMap extends HashMap {
    public CaseInsensitiveHashMap() {
        super();
    }

    public CaseInsensitiveHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public CaseInsensitiveHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public CaseInsensitiveHashMap(Map t) {
        putAll(t);
    }

    @Override
    public boolean containsKey(Object key) {
        boolean retval;
        if (key instanceof String) {
            CaseInsensitiveKey ciKey = new CaseInsensitiveKey((String) key);
            retval = super.containsKey(ciKey);
        } else {
            retval = super.containsKey(key);
        }
        return retval;
    }

    @Override
    public Object get(Object key) {
        Object retval;
        if (key instanceof String) {
            CaseInsensitiveKey ciKey = new CaseInsensitiveKey((String) key);
            retval = super.get(ciKey);
        } else {
            retval = super.get(key);
        }
        return retval;
    }

    /**
     * @return a <code>Set</Code> of keys.
     */
    @Override
    public Set keySet() {
        Set keys = super.keySet();
        Set set = new CaseInsensitiveHashSet();
        set.addAll(keys);
        return set;
    }

    /**
     * Returns set view of mappings in this map
     * 
     * @return a <code>Set</Code> of map entries
     */
    @Override
    public Set entrySet() {
        Set entries = super.entrySet();
        HashSet set = new HashSet();
        Iterator iter = entries.iterator();
        while (iter.hasNext()) {
            // keys are CaseInsensitiveKey's,
            // hence needs to switched to String
            Map.Entry entry = (Map.Entry) iter.next();
            set.add(new Entry(entry));
        }
        return set;
    }

    @Override
    public Object put(Object key, Object value) {
        Object retval;
        if (key instanceof String) {
            CaseInsensitiveKey ciKey = new CaseInsensitiveKey((String) key);
            retval = super.put(ciKey, value);
        } else {
            retval = super.put(key, value);
        }
        return retval;
    }

    @Override
    public void putAll(Map map) {
        if (map == null || map.isEmpty()) {
            return;
        }
        for (Iterator iter = map.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry)iter.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Object remove(Object key) {
        Object retval;
        if (key instanceof String) {
            CaseInsensitiveKey ciKey = new CaseInsensitiveKey((String) key);
            retval = super.remove(ciKey);
        } else {
            retval = super.remove(key);
        }
        return retval;
    }

    static private class Entry implements Map.Entry {

        Map.Entry entry;

        Entry(Map.Entry entry) {
            this.entry = entry;
        }

        @Override
        public Object getKey() {
            // Since key would CaseInsensitiveKey,
            // need to convert it to String
            return (entry.getKey().toString());
        }

        @Override
        public Object getValue() {
            return (entry.getValue());
        }

        @Override
        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(Object o) {
            return (entry.equals(o));
        }

        @Override
        public Object setValue(Object o) {
            return (entry.setValue(o));
        }

        @Override
        public int hashCode() {
            return (entry.hashCode());
        }
    }
}
