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
 * $Id: AMHashMap.java,v 1.7 2009/11/03 00:06:31 hengming Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;

/**
 * This class will lower the case of the key that is used to access the HashMap.
 * Primarily this implementation of HashMap is customized for storing and
 * returning attributes.
 * 
 * In the case of String values, the values are stored in a Set, whereas in case
 * of byte values the values are stored as byte array. The special byte methods
 * in this class should be used to do specific byte operations.
 * 
 * This HashMap stores information about the type of values, (String or byte).
 * All the operations are performed on this basis. The default case is that it
 * stores String values.
 * 
 * Note: For byte values there is no negative caching done.
 * 
 */
public class AMHashMap extends CaseInsensitiveHashMap {

    private static final long serialVersionUID = 6468554078141700418L;

    private boolean byteValues = false;

    // Use a Collections.EMPTY_SET, as you want to avoid instantiating a Set
    // for every AMHashMap created.
    private Set byteNegativeAttrs = Collections.EMPTY_SET;

    public AMHashMap() {
        super();
    }

    /**
     * Creates a new AMHashMap
     * 
     * @param forByteValues
     *            if true then, this map is used for storing byte values. if
     *            false, then used for storing String values
     */
    public AMHashMap(boolean forByteValues) {
        super();
        byteValues = forByteValues;
    }

    public AMHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public AMHashMap(int initialCapacity, boolean forByteValues) {
        super(initialCapacity);
        byteValues = forByteValues;
    }

    public AMHashMap(HashMap map) {
        super();
        if (map != null) {
            Iterator it = map.keySet().iterator();
            while (it.hasNext()) {
                String n = (String) it.next();
                put(n, map.get(n));
            }
        }
    }

    /*
     * Override the default isEmpty(). This map is not empty, if the type is
     * byteValues and if either the map is not empty or the negative set is not
     */
    public boolean isEmpty() {
        if (!byteValues) {
            return super.isEmpty();
        } else {
            return (super.isEmpty() && byteNegativeAttrs.isEmpty());
        }
    }

    /*
     * Override the default clear(). This method will clear the negative
     * attributes set too, if the type is byteValues
     */
    public void clear() {
        super.clear();
        if (byteValues) {
            clearNegativeByteAttrs();
        }
    }

    private void addNegativeByteAttr(String name) {
        if (byteNegativeAttrs == Collections.EMPTY_SET) {
            byteNegativeAttrs = new CaseInsensitiveHashSet();
        }
        byteNegativeAttrs.add(name);
    }

    private void removeNegativeByteAttr(String name) {
        if (byteNegativeAttrs != Collections.EMPTY_SET) {
            byteNegativeAttrs.remove(name);
        }
    }

    protected void clearNegativeByteAttrs() {
        if (byteNegativeAttrs != Collections.EMPTY_SET) {
            byteNegativeAttrs.clear();
        }
    }

    private boolean isNegativeByteAttr(String name) {
        // Collections.EMPTY_SET implements contains and returns false. So, we
        // are okay here.
        return byteNegativeAttrs.contains(name);
    }

    /*
     * Methods to get and get negative set of AMHashMap
     */
    protected void setNegativeByteAttr(Set n) {
        byteNegativeAttrs = n;
    }

    public Set getNegativeByteAttrClone() {
        if (byteNegativeAttrs == Collections.EMPTY_SET) {
            return Collections.EMPTY_SET;
        }
        Set res = new HashSet();
        Iterator it = byteNegativeAttrs.iterator();
        while (it.hasNext()) {
            res.add(it.next());
        }
        return res;
    }

    /**
     * This compares all the elements of the specified and returns a Set which
     * contains all the keys missing in the map. If a key is missing, this
     * method adds puts a new element in this map with the missing key and empty
     * Set.
     * 
     * @param keys
     *            the Set of keys that will be compared against the keys in this
     *            map.
     * @return a set of keys which are missing
     */
    public Set getMissingKeys(Set keys) {
        Set missAttrNames = new HashSet();
        Iterator itr = keys.iterator();
        while (itr.hasNext()) {
            String name = (String) itr.next();
            if (get(name) == null) {
                if (byteValues) {
                    if (!isNegativeByteAttr(name)) {
                        // need to mark it as negative
                        missAttrNames.add(name);
                        addNegativeByteAttr(name);
                    }
                } else { // String values
                    missAttrNames.add(name);
                    put(name, new HashSet());
                }
            }
        }
        return missAttrNames;
    }

    /**
     * This compares all the elements of the specified and returns a Set which
     * contains all the keys missing in the map and also those keys whose values
     * are empty. If a key is missing, this method adds puts a new element in
     * this map with the missing key and empty Set.
     * 
     * @param keys
     *            the Set of keys that will be compared against the keys in this
     *            map.
     * @return a set of keys which are missing and keys whose values are empty
     *         Sets.
     */
    public Set getMissingAndEmptyKeys(Set keys) {
        Set missAttrNames = new HashSet();
        Iterator itr = keys.iterator();
        if (!byteValues) { // String values
            while (itr.hasNext()) {
                String name = (String) itr.next();
                Set values = (Set) get(name);
                if (values == null) {
                    missAttrNames.add(name);
                    put(name, new HashSet());
                } else if (values.isEmpty()) {
                    missAttrNames.add(name);
                }
            }
        } else { // byte values
            while (itr.hasNext()) {
                String name = (String) itr.next();
                byte[][] values = (byte[][]) get(name);
                if (values == null) {
                    missAttrNames.add(name);
                    addNegativeByteAttr(name);
                } else if (isNegativeByteAttr(name)) {
                    missAttrNames.add(name);
                }
            }
        }
        return missAttrNames;
    }

    public void removeKeys(Set keys) {
        if ((keys != null) && (!keys.isEmpty())) {
            Iterator itr = keys.iterator();
            while (itr.hasNext()) {
                String t = (String) itr.next();
                remove(t);
                removeNegativeByteAttr(t);
            }
        }
    }

    /**
     * Copies the contents of the specified map to this map. This operation
     * overwrites the values in this map.
     * 
     * @param map
     *            the map that needs to be copied
     */
    public void copy(Map map) {
        if (map == null || map.isEmpty()) {
            return;
        }
        // Replicating to avoid comparison inside loop
        Iterator itr = map.keySet().iterator();
        if (!byteValues) { // String values
            while (itr.hasNext()) {
                String name = (String) itr.next();
                Set values = (Set) map.get(name);
                this.put(name, getSetCopy(values));
            }
        } else { // byte values
            while (itr.hasNext()) {
                String name = (String) itr.next();
                byte[][] values = (byte[][]) map.get(name);
                this.put(name, values);
                this.removeNegativeByteAttr(name);
            }
            if (map instanceof AMHashMap) {
                this.setNegativeByteAttr(((AMHashMap) map)
                        .getNegativeByteAttrClone());
            }
        }
    }

    /**
     * Copies the contents of the specified map to this map. This operation
     * overwrites the values in this map with new contents. This method copies
     * only Sets with values and not empty sets. NOTE: This method will not
     * worry about any negative attributes. Hence will be used in CacheBlock
     * when the entries are being stored.
     * 
     * @param map
     *            the map that needs to be copied
     * 
     * @return a set of attributes which have empty sets stored with them
     */
    public Set copyValuesOnly(Map map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        Set attrsWithValues = new HashSet();
        // Replicating to avoid comparison inside loop
        Iterator itr = map.keySet().iterator();
        if (!byteValues) { // String values
            while (itr.hasNext()) {
                String name = (String) itr.next();
                Set values = (Set) map.get(name);
                if ((values == null) || (values.isEmpty())) {
                    this.put(name, Collections.EMPTY_SET);
                    attrsWithValues.add(name);
                } else {
                    this.put(name, getSetCopy(values));
                    attrsWithValues.add(name);
                }
            }
        } else { // byte values
            while (itr.hasNext()) {
                String name = (String) itr.next();
                byte[][] values = (byte[][]) map.get(name);
                this.put(name, values);
                attrsWithValues.add(name);
            }
        }
        return attrsWithValues;
    }

    /**
     * Merges all the values from the specified map to the current map. Adds new
     * values into the current map if not already present. The method assumes
     * that the values in the map are of Set type.
     * 
     * @param map
     *            the map whose values need to merge and added to the current
     *            map.
     * @throws NullPointerException
     *             if the map is null.
     */
    public void merge(Map map) {
        if (map == null) {
            throw new NullPointerException();
        }

        if (!byteValues) { // For String values merge
            Iterator itr = map.keySet().iterator();
            while (itr.hasNext()) {
                String name = (String) itr.next();
                Set values = (Set) this.get(name);
                if (values != null) {
                    values.addAll((Set) map.get(name));
                } else {
                    this.put(name, (Set) map.get(name));
                }
            }
        } else { // For byte values replace ??
            putAll(map);
        }
    }

    /**
     * Method which adds new entries to the map with empty set as values
     * 
     * @param names
     *            new entries to be added to the map
     */
    public void addEmptyValues(Set names) {
        Iterator itr = names.iterator();
        while (itr.hasNext()) {
            String name = (String) itr.next();
            if (!byteValues) {
                put(name, new HashSet());
            } else {
                addNegativeByteAttr(name);
            }
        }
    }

    /**
     * Removes all the empty values (empty sets) from the map.
     */
    public void removeEmptyValues() {
        // We are removing negative attributes here
        Set emptyKeys = new HashSet();
        if (!byteValues) {
            Iterator itr = keySet().iterator();
            while (itr.hasNext()) {
                String name = (String) itr.next();
                Set values = (Set) get(name);
                if (values.isEmpty()) {
                    emptyKeys.add(name);
                }
            }
            removeKeys(emptyKeys);
        }
        // Remove the negative byte attrs
        clearNegativeByteAttrs();
    }

    /**
     * This method is to some what clone() behaviour. However, this AMHashMap is
     * designed to mostly store attribute names as keys and Set of values
     * corresponding to each key, a new protected method has been created for
     * this purpose.
     * 
     * @return a new AMHashMap that has a copy of all the elements in this map
     */
    public Map getCopy() {
        if (!byteValues) { // String values
            Map map = new AMHashMap(size(), false);
            if (!isEmpty()) {
                Iterator itr = keySet().iterator();
                while (itr.hasNext()) {
                    String name = (String) itr.next();
                    Set values = (Set) get(name);
                    map.put(name, getSetCopy(values));
                }
            }
            return map;
        } else { // byte values
            Map map = new AMHashMap(size(), true);
            if (!isEmpty()) {
                Iterator itr = keySet().iterator();
                while (itr.hasNext()) {
                    String name = (String) itr.next();
                    byte[][] values = (byte[][]) get(name);
                    map.put(name, getByteArrayCopy(values));
                }
            }
            ((AMHashMap) map).setNegativeByteAttr(getNegativeByteAttrClone());
            return map;
        }
    }

    /**
     * This method is to some what clone() behaviour. However, this AMHashMap is
     * designed to mostly store attribute names as keys and Set of values
     * corresponding to each key, a new protected method has been created for
     * this purpose.
     * 
     * @param names
     *            the names of the attributes that need to be obtained from Map
     * 
     * @return a new AMHashMap that has a copy of all the elements in this map
     */
    public Map getCopy(Set names) {
        if (!byteValues) { // String values
            Map map = new AMHashMap(false);
            if (!isEmpty() && !names.isEmpty()) {
                Iterator itr = names.iterator();
                while (itr.hasNext()) {
                    String name = (String) itr.next();
                    Set values = (Set) get(name);
                    if (values != null) {
                        map.put(name, getSetCopy(values));
                    }
                }
            }
            return map;
        } else { // byte values
            Map map = new AMHashMap(true);
            if (!isEmpty() && !names.isEmpty()) {
                Iterator itr = names.iterator();
                while (itr.hasNext()) {
                    String name = (String) itr.next();
                    byte[][] values = (byte[][]) get(name);
                    if (values != null) {
                        map.put(name, getByteArrayCopy(values));
                    }
                }
            }
            ((AMHashMap) map).setNegativeByteAttr(getNegativeByteAttrClone());

            return map;
        }
    }

    private Set getSetCopy(Set values) {
        if (values == null) {
            return null;
        }
        Set copyValues = new HashSet(values.size());
        if (!values.isEmpty()) {
            Iterator itr = values.iterator();
            while (itr.hasNext()) {
                String value = (String) itr.next();
                copyValues.add(value);
            }
        }
        return copyValues;
    }

    private byte[][] getByteArrayCopy(byte[][] values) {
        int count1 = values.length;
        byte[][] copyValues = new byte[count1][];
        for (int i = 0; i < count1; i++) {
            int count2 = values[i].length;
            copyValues[i] = new byte[count2];
            for (int j = 0; j < count2; j++) {
                copyValues[i][j] = values[i][j];
            }
        }
        return copyValues;
    }
}
