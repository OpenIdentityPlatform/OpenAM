/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Cache.java,v 1.4 2009/12/12 00:03:13 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.entitlement.opensso;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// IMPORTANT NOTE: The cache has be implemented by modifing the existing
// java.util.Hashtable code. Its has added functionality of a built in
// replacement strategy which in this case is based on Least Recently Used
// concept. The synchronization functionality that exists in a Hashtable has
// been removed for better performance

/**
 * IMPORTANT NOTE: The cache has be implemented by modifing the existing
 * java.util.Hashtable code. Its has added functionality of a built in
 * replacement strategy which in this case is based on Least Recently Used
 * concept. The synchronization functionality that exists in a Hashtable has
 * been removed to improve performance.
 * <p>
 * 
 * The class <code>Cache</code> provides the functionality to cache objects
 * based on their usage. The maximum size of the cache can be set using the
 * constructor. If the maximum size is not set the default cache size for
 * <code>Cache</code> will be obtained from the config file <code>???</code>
 * file, defined using the key <code>???</code>. The 
 * object that needs to be cached can be supplied to the instance of this class
 * using the put method. The object can be obtained by invoking the get method
 * on the instance. Each object that is cached is tracked based on its usage. 
 * If a new object needs to added to the cache and the maximum size limit of 
 * the cache is reached, then the least recently used object is replaced.
 *
 * This class implements a Cache, which maps keys to values. Any 
 * non-<code>null</code> object can be used as a key or as a value. <p>
 *
 * To successfully store and retrieve objects from a Cache, the 
 * objects used as keys must implement the <code>hashCode</code> 
 * method and the <code>equals</code> method. <p>
 *
 * An instance of <code>Cache</code> has two parameters that affect its
 * performance: <i>capacity</i> and <i>load factor</i>.  The
 * <i>capacity</i> is the number of <i>buckets</i> in the hash table, and the
 * <i>capacity</i> is simply the capacity at the time the hash table
 * is created.  Note that the hash table is <i>open</i>: in the case a "hash
 * collision", a single bucket stores multiple entries, which must be searched
 * sequentially.  The <i>load factor</i> is a measure of how full the hash
 * table is allowed to get before its capacity is automatically increased.
 * When the number of entries in the Cache exceeds the product of the load
 * factor and the current capacity, the capacity is increased by calling the
 * <code>rehash</code> method.<p>
 *
 * Generally, the default load factor (.75) offers a good tradeoff between
 * time and space costs.  Higher values decrease the space overhead but
 * increase the time cost to look up an entry (which is reflected in most
 * <tt>Cache</tt> operations, including <tt>get</tt> and <tt>put</tt>).<p>
 *
 * The capacity controls a tradeoff between wasted space and the
 * need for <code>rehash</code> operations, which are time-consuming.
 * No <code>rehash</code> operations will <i>ever</i> occur if the 
 * capacity is greater than the maximum number of entries the
 * <tt>Cache</tt> will contain divided by its load factor.  However,
 * setting the capacity too high can waste space.<p>
 *
 * If many entries are to be made into a <code>Cache</code>, 
 * creating it with a sufficiently large capacity may allow the 
 * entries to be inserted more efficiently than letting it perform 
 * automatic rehashing as needed to grow the table. <p>
 *
 * This class has been retrofitted to implement Map, so that it becomes a 
 * part of Java's collection framework.  
 *
 * The Iterators returned by the iterator and listIterator methods
 * of the Collections returned by all of Cache's "collection view methods"
 * are <em>fail-fast</em>: if the Cache is structurally modified
 * at any time after the Iterator is created, in any way except through the
 * Iterator's own remove or add methods, the Iterator will throw a
 * ConcurrentModificationException.  Thus, in the face of concurrent
 * modification, the Iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the future.
 * The Enumerations returned by Cache's keys and values methods are
 * <em>not</em> fail-fast.
 *
 * @see     Object#equals(java.lang.Object)
 * @see     Object#hashCode()
 * @see     Collection
 * @see            Map
 * @since JDK1.0
 */
public class Cache extends Dictionary implements Map, java.io.Serializable {

    // Default Cache size.
    private final static int DEFAULT_CACHE_SIZE = 10000;

    /**
     * The hash table data.
     */
    private transient Entry table[];

    private transient int maxSize;

    /**
     * A circular doubly linked list which maintains the entry list in the cache
     * (table) based on their usage. The listed is updated everytime an entry is
     * accessed the table such that the most recent entry accessed will be
     * placed at the end of the list. This way, the least recently used entry
     * will be always at the front of the list.
     */
    private transient LRUList lruTracker;

    /**
     * The total number of entries in the hash table.
     */
    private transient int count;

    /**
     * The table is rehashed when its size exceeds this threshold. (The value of
     * this field is (int)(capacity * loadFactor).)
     * 
     * @serial
     */
    private int threshold;

    /**
     * The load factor for the Cache.
     * 
     * @serial
     */
    private float loadFactor;

    /**
     * The number of times this Cache has been structurally modified Structural
     * modifications are those that change the number of entries in the Cache or
     * otherwise modify its internal structure (e.g., rehash). This field is
     * used to make iterators on Collection-views of the Cache fail-fast. (See
     * ConcurrentModificationException).
     */
    private transient int modCount = 0;

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    private static final long serialVersionUID = 1421746759512286392L;


    private transient ReadWriteLock rwlock = new ReentrantReadWriteLock();

    private String name;

    /**
     * Constructs a new, empty Cache with the specified capacity and the
     * specified load factor.
     *
     * @param name Name of cache.
     * @param capacity
     *            the capacity of the Cache.
     * @param loadFactor
     *            the load factor of the Cache.
     * @exception IllegalArgumentException
     *                if the capacity is less than zero, or if the load factor
     *                is nonpositive.
     */
    public Cache(String name, int capacity, float loadFactor) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + capacity);
        }
        if (loadFactor <= 0) {
            throw new IllegalArgumentException("Illegal Load: " + loadFactor);
        }

        if (capacity == 0) {
            capacity = 1;
        }

        this.name = name;
        this.loadFactor = loadFactor;
        table = new Entry[capacity];
        threshold = (int) (capacity * loadFactor);

        maxSize = capacity;
        lruTracker = new LRUList();
    }

    /**
     * Constructs a new, empty Cache with the specified capacity and default
     * load factor, which is <tt>0.75</tt>.
     *
     * @param name Name of cache.
     * @param capacity
     *            the capacity of the Cache.
     * @exception IllegalArgumentException
     *                if the capacity is less than zero.
     */
    public Cache(String name, int capacity) {
        this(name, capacity, 0.75f);
        maxSize = capacity;
        lruTracker = new LRUList();
    }

    /**
     * Constructs a new, empty Cache with a default capacity and load factor,
     * which is <tt>0.75</tt>.
     */
    public Cache(String name) {
        // Obtain the cache size
        this(name, DEFAULT_CACHE_SIZE, 0.75f);
        maxSize = DEFAULT_CACHE_SIZE;
        lruTracker = new LRUList();
    }

    // required for serializable
    public Cache() {
    }

    /**
     * Returns name.
     *
     * @return name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the number of keys in this Cache.
     * 
     * @return the number of keys in this Cache.
     */
    public int size() {
        return count;
    }

    /**
     * Tests if this Cache maps no keys to values.
     * 
     * @return <code>true</code> if this Cache maps no keys to values;
     *         <code>false</code> otherwise.
     */
    public boolean isEmpty() {
        return count == 0;
    }

    /**
     * Returns an enumeration of the keys in this Cache.
     * 
     * @return an enumeration of the keys in this Cache.
     * @see Enumeration
     * @see #elements()
     * @see #keySet()
     * @see Map
     */
    public synchronized Enumeration keys() {
        return new Enumerator(KEYS, false);
    }

    /**
     * Returns an enumeration of the values in this Cache. Use the Enumeration
     * methods on the returned object to fetch the elements sequentially.
     * 
     * @return an enumeration of the values in this Cache.
     * @see java.util.Enumeration
     * @see #keys()
     * @see #values()
     * @see Map
     */
    public synchronized Enumeration elements() {
        return new Enumerator(VALUES, false);
    }

    /**
     * Tests if some key maps into the specified value in this Cache. This
     * operation is more expensive than the <code>containsKey</code> method.
     * <p>
     * 
     * Note that this method is identical in functionality to containsValue,
     * (which is part of the Map interface in the PolicyCollections framework).
     * 
     * @param value
     *            a value to search for.
     * @return <code>true</code> if and only if some key maps to the
     *         <code>value</code> argument in this Cache as determined by the
     *         <tt>equals</tt> method; <code>false</code> otherwise.
     * @exception NullPointerException
     *                if the value is <code>null</code>.
     * @see #containsKey(Object)
     * @see #containsValue(Object)
     * @see Map
     */
    public boolean contains(Object value) {
        if (value == null) {
            throw new NullPointerException();
        }

        boolean found = false;
        rwlock.readLock().lock();

        try {
            Entry tab[] = table;
            for (int i = tab.length; i-- > 0;) {
                for (Entry e = tab[i]; (e != null) && !found; e = e.next) {
                    found = e.value.equals(value);
                }
            }
        } finally {
            rwlock.readLock().unlock();
        }
        return found;
    }

    /**
     * Returns true if this Cache maps one or more keys to this value.
     * <p>
     * 
     * Note that this method is identical in functionality to contains (which
     * predates the Map interface).
     * 
     * @param value
     *            value whose presence in this Cache is to be tested.
     * @see Map
     * @since JDK1.2
     */
    public boolean containsValue(Object value) {
        return contains(value);
    }

    /**
     * Tests if the specified object is a key in this Cache.
     * 
     * @param key
     *            possible key.
     * @return <code>true</code> if and only if the specified object is a key
     *         in this Cache, as determined by the <tt>equals</tt> method;
     *         <code>false</code> otherwise.
     * @see #contains(Object)
     */
    public boolean containsKey(Object key) {
        rwlock.readLock().lock();
        boolean found = false;

        try {
            Entry tab[] = table;
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % tab.length;
            for (Entry e = tab[index]; (e != null) && !found; e = e.next) {
                found = ((e.hash == hash) && e.key.equals(key));
            }
        } finally {
            rwlock.readLock().unlock();
        }

        return found;
    }

    /**
     * Returns the value to which the specified key is mapped in this Cache.
     * 
     * @param key
     *            a key in the Cache.
     * @return the value to which the key is mapped in this Cache;
     *         <code>null</code> if the key is not mapped to any value in this
     *         Cache.
     * @see #put(Object, Object)
     */
    public Object get(Object key) {
        rwlock.readLock().lock();
        Object value = null;

        try {
            Entry tab[] = table;
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % tab.length;
            for (Entry e = tab[index]; (e != null) && (value == null);
                e = e.next) {
                if ((e.hash == hash) && e.key.equals(key)) {
                    // Since the entry is accessed, move it to the end of the
                    // list
                    lruTracker.replaceLast(e);
                    value = e.value;
                }
            }
        } finally {
            rwlock.readLock().unlock();
        }
        return value;
    }

    /**
     * Increases the capacity of and internally reorganizes this Cache, in order
     * to accommodate and access its entries more efficiently. This method is
     * called automatically when the number of keys in the Cache exceeds this
     * Cache's capacity and load factor.
     */
    protected void rehash() {
        int oldCapacity = table.length;
        Entry oldMap[] = table;

        int newCapacity = oldCapacity * 2 + 1;
        Entry newMap[] = new Entry[newCapacity];

        modCount++;
        threshold = (int) (newCapacity * loadFactor);
        table = newMap;
        for (int i = oldCapacity; i-- > 0;) {
            for (Entry old = oldMap[i]; old != null;) {
                Entry e = old;
                old = old.next;

                int index = (e.hash & 0x7FFFFFFF) % newCapacity;
                e.next = newMap[index];
                newMap[index] = e;
            }
        }
    }

    /**
     * Maps the specified <code>key</code> to the specified <code>value</code>
     * in this Cache. Neither the key nor the value can be <code>null</code>.
     * If the cache is full to its capacity, then the least recently used entry
     * in the cache will be replaced.
     * <p>
     * 
     * 
     * The value can be retrieved by calling the <code>get</code> method with
     * a key that is equal to the original key.
     * 
     * @param key
     *            the Cache key.
     * @param value
     *            the value.
     * @return the previous value of the specified key in this Cache, or
     *         <code>null</code> if it did not have one.
     * @exception NullPointerException
     *                if the key or value is <code>null</code>.
     * @see Object#equals(Object)
     * @see #get(Object)
     */
    public Object put(Object key, Object value) {
        // Make sure the value is not null
        if (value == null) {
            throw new NullPointerException();
        }

        rwlock.writeLock().lock();

        try {
            // Makes sure the key is not already in the Cache.
            Entry tab[] = table;
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % tab.length;
            for (Entry e = tab[index]; e != null; e = e.next) {
                if ((e.hash == hash) && e.key.equals(key)) {
                    Object old = e.value;
                    e.value = value;
                    // Since the entry is already present move it to the end of
                    //the list
                    lruTracker.replaceLast(e);
                    return old;
                }
            }

            if (count >= threshold && count != maxSize) {
                // Rehash the table if the threshold is exceeded
                modCount++;
                rehash();
                tab = table;
                index = (hash & 0x7FFFFFFF) % tab.length;
            }

            Entry e = null;
            // Table is full need to replace an entry with new one
            if (count == maxSize) {
                // Get the least recently used entry
                e = lruTracker.getFirst();
                // Remove the entry from the table to accomidate new entry
                adjustEntry(e.key);
                // Modify the values of this entry to reflect new entry
                // (Avoiding the creation of a new entry object)
                lruTracker.replaceLast(e);
                e.changeValues(hash, key, value, tab[index]);
            } else {
                modCount++;
                count++;
                // Creates the new entry.
                e = new Entry(hash, key, value, tab[index]);
                lruTracker.addLast(e);
            }
            
            tab[index] = e;
        } finally {
            rwlock.writeLock().unlock();
        }

        return null;
    }

    /**
     * This method adjusts the table by removing the entry corresponding to key
     * from the table.
     */
    // NOTE: The remove() method cannot be used for this functionality as the
    // counter and modCount value should not be changed in this context
    protected void adjustEntry(Object key) {
        Entry tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (Entry e = tab[index], prev = null; e != null; prev = e, e = e.next)
        {
            if ((e.hash == hash) && e.key.equals(key)) {
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
            }
        }
    }

    /**
     * Removes the key (and its corresponding value) from this Cache. This
     * method does nothing if the key is not in the Cache.
     * 
     * @param key
     *            the key that needs to be removed.
     * @return the value to which the key had been mapped in this Cache, or
     *         <code>null</code> if the key did not have a mapping.
     */
    public Object remove(Object key) {
        rwlock.writeLock().lock();

        try {
            Entry tab[] = table;
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % tab.length;
            for (Entry e = tab[index], prev = null; e != null;
                prev = e, e = e.next) {
                if ((e.hash == hash) && e.key.equals(key)) {
                    modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }
                    count--;
                    Object oldValue = e.value;
                    e.value = null;
                    lruTracker.remove(e);
                    return oldValue;
                }
            }
        } finally {
            rwlock.writeLock().unlock();
        }
        return null;
    }

    /**
     * Copies all of the mappings from the specified Map to this Hashtable These
     * mappings will replace any mappings that this Hashtable had for any of the
     * keys currently in the specified Map.
     * 
     * @since JDK1.2
     */
    public void putAll(Map t) {
        rwlock.writeLock().lock();

        try {
            for (Iterator i = t.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry e = (Map.Entry) i.next();
                put(e.getKey(), e.getValue());
            }
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    /**
     * Clears this Cache so that it contains no keys.
     */
    public void clear() {
        rwlock.writeLock().lock();
        try {
            Entry tab[] = table;
            modCount++;
            for (int index = tab.length; --index >= 0;) {
                tab[index] = null;
            }
            // Clear the LRU Tracker
            lruTracker.clear();
            count = 0;
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    /**
     * Returns a string representation of this <tt>Cache</tt> object in the
     * form of a set of entries, enclosed in braces and separated by the ASCII
     * characters "<tt>,&nbsp;</tt>" (comma and space). Each entry is
     * rendered as the key, an equals sign <tt>=</tt>, and the associated
     * element, where the <tt>toString</tt> method is used to convert the key
     * and element to strings.
     * <p>
     * Overrides to <tt>toString</tt> method of <tt>Object</tt>.
     * 
     * @return a string representation of this Cache.
     */
    @Override
    public String toString() {
        rwlock.readLock().lock();
        
        try {
            int max = size() - 1;
            StringBuilder buf = new StringBuilder();
            buf.append("{");

            Iterator it = entrySet().iterator();
            for (int i = 0; i <= max; i++) {
                Entry e = (Entry) (it.next());
                buf.append(e.key).append("=").append(e.value);
                if (i < max) {
                    buf.append(", ");
                }
            }

            buf.append("}");
            return buf.toString();
        } finally {
            rwlock.readLock().unlock();
        }
    }

    public String audit() {
        rwlock.readLock().lock();

        // ensure LRU list length is the same as the number of elements in the
        // cache
        try {
            String retStr = "";
            int ltl = lruTracker.length();
            if (ltl != count) {
                retStr = "LRU list length (" + ltl + ") != count (" + count
                    + ")";
            }
            return retStr;
        } finally {
            rwlock.readLock().unlock();
        }
    }

    // Views

    private transient Set keySet = null;

    private transient Set entrySet = null;

    private transient Collection values = null;

    /**
     * Returns a Set view of the keys contained in this Cache. The Set is backed
     * by the Cache, so changes to the Cache are reflected in the Set, and
     * vice-versa. The Set supports element removal (which removes the
     * corresponding entry from the Cache), but not element addition.
     * 
     * @since JDK1.2
     */
    public Set keySet() {
        if (keySet == null)
            keySet = new SynchronizedSet(new KeySet(), this);
        return keySet;
    }

    private class KeySet extends AbstractSet {
        public Iterator iterator() {
            return new Enumerator(KEYS, true);
        }

        public int size() {
            return count;
        }

        @Override
        public boolean contains(Object o) {
            return containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            return Cache.this.remove(o) != null;
        }

        @Override
        public void clear() {
            Cache.this.clear();
        }
    }

    /**
     * Returns a Set view of the entries contained in this Cache. Each element
     * in this collection is a Map.Entry. The Set is backed by the Cache, so
     * changes to the Cache are reflected in the Set, and vice-versa. The Set
     * supports element removal (which removes the corresponding entry from the
     * Cache), but not element addition.
     * 
     * @see java.util.Map.Entry
     * @since JDK1.2
     */
    public Set entrySet() {
        if (entrySet == null)
            entrySet = new SynchronizedSet(new EntrySet(), this);
        return entrySet;
    }

    private class EntrySet extends AbstractSet {
        public Iterator iterator() {
            return new Enumerator(ENTRIES, true);
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry entry = (Map.Entry) o;
            Object key = entry.getKey();
            Entry tab[] = table;
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % tab.length;

            for (Entry e = tab[index]; e != null; e = e.next)
                if (e.hash == hash && e.equals(entry))
                    return true;
            return false;
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry entry = (Map.Entry) o;
            Object key = entry.getKey();
            Entry tab[] = table;
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % tab.length;

            for (Entry e = tab[index], prev = null; e != null; 
                                        prev = e, e = e.next) 
            {
                if (e.hash == hash && e.equals(entry)) {
                    modCount++;
                    if (prev != null)
                        prev.next = e.next;
                    else
                        tab[index] = e.next;

                    count--;
                    e.value = null;
                    return true;
                }
            }
            return false;
        }

        public int size() {
            return count;
        }

        @Override
        public void clear() {
            Cache.this.clear();
        }
    }

    /**
     * Returns a Collection view of the values contained in this Cache. The
     * Collection is backed by the Cache, so changes to the Cache are reflected
     * in the Collection, and vice-versa. The Collection supports element
     * removal (which removes the corresponding entry from the Cache), but not
     * element addition.
     * 
     * @since JDK1.2
     */
    public Collection values() {
        if (values == null) {
            values = new SynchronizedCollection(new ValueCollection(), this);
        }
        return values;
    }

    private class ValueCollection extends AbstractCollection {
        public Iterator iterator() {
            return new Enumerator(VALUES, true);
        }

        public int size() {
            return count;
        }

        @Override
        public boolean contains(Object o) {
            return containsValue(o);
        }

        @Override
        public void clear() {
            Cache.this.clear();
        }
    }

    // Comparison and hashing

    /**
     * Compares the specified Object with this Map for equality, as per the
     * definition in the Map interface.
     * 
     * @return true if the specified Object is equal to this Map.
     * @see Map#equals(Object)
     * @since JDK1.2
     */
    @Override
    public boolean equals(Object o) {
        rwlock.readLock().lock();

        try {
            if (o == this) {
                return true;
            }

            if (!(o instanceof Map)) {
                return false;
            }
            Map t = (Map) o;
            if (t.size() != size()) {
                return false;
            }

            for (Iterator i = entrySet().iterator(); i.hasNext(); ) {
                Entry e = (Entry) i.next();
                Object key = e.getKey();
                Object value = e.getValue();
                if (value == null) {
                    if (!(t.get(key) == null && t.containsKey(key))) {
                        return false;
                    }
                } else {
                    if (!value.equals(t.get(key))) {
                        return false;
                    }
                }
            }
            return true;
        } finally {
            rwlock.readLock().unlock();
        }
    }

    /**
     * Returns the hash code value for this Map as per the definition in the Map
     * interface.
     * 
     * @see Map#hashCode()
     * @since JDK1.2
     */
    @Override
    public int hashCode() {
        rwlock.readLock().lock();
        int h = 0;
        try {
            Iterator i = entrySet().iterator();
            while (i.hasNext()) {
                h += i.next().hashCode();
            }
        } finally {
            rwlock.readLock().unlock();
        }
        return h;
    }

    /**
     * Save the state of the Cache to a stream (i.e., serialize it).
     * 
     * @param s
     *            object output stream instance.
     * @throws IOException
     *             if object cannot be written.
     *
    private synchronized void writeObject(java.io.ObjectOutputStream s)
            throws IOException {
        // Write out the length, threshold, loadfactor
        s.defaultWriteObject();

        // Write out length, count of elements and then the key/value objects
        s.writeInt(table.length);
        s.writeInt(count);
        for (int index = table.length - 1; index >= 0; index--) {
            Entry entry = table[index];

            while (entry != null) {
                s.writeObject(entry.key);
                s.writeObject(entry.value);
                entry = entry.next;
            }
        }
    }

    /**
     * Reconstitute the Cache from a stream (i.e., deserialize it).
     *
    private synchronized void readObject(java.io.ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        // Read in the length, threshold, and loadfactor
        s.defaultReadObject();

        // Read the original length of the array and number of elements
        int origlength = s.readInt();
        int elements = s.readInt();

        // Compute new size with a bit of room 5% to grow but
        // No larger than the original size. Make the length
        // odd if it's large enough, this helps distribute the entries.
        // Guard against the length ending up zero, that's not valid.
        int length = (int) (elements * loadFactor) + (elements / 20) + 3;
        if (length > elements && (length & 1) == 0)
            length--;
        if (origlength > 0 && length > origlength)
            length = origlength;

        table = new Entry[length];
        count = 0;

        // Read the number of elements and then all the key/value objects
        for (; elements > 0; elements--) {
            Object key = s.readObject();
            Object value = s.readObject();
            put(key, value);
        }
    } */

    /**
     * Class which is used to create and maintain a circular doubly linked with
     * functionality to add and delete Entry objects.
     */
    private class LRUList {
        Entry header = new Entry(0, null, null, null);
        int size;

        protected LRUList() {
        }

        protected LRUList(Entry h) {
            header = h;
        }

        // Method to add an entry to the end (last) of the list
        protected synchronized void addLast(Entry e) {
            if (header.lruNext == null) { // LRUList empty
                header.lruNext = header.lruPrev = e;
                e.lruNext = e.lruPrev = header;
            } else {
                header.lruPrev.lruNext = e;
                e.lruNext = header;
                e.lruPrev = header.lruPrev;
                header.lruPrev = e;
            }
            size++;

        }

        // Method to remove an entry from the list
        protected synchronized void remove(Entry e) {
            if (e == null)
                return;

            e.lruPrev.lruNext = e.lruNext;
            e.lruNext.lruPrev = e.lruPrev;

            e.lruNext = e.lruPrev = null;
            size--;
        }

        // Method to get the first entry in the list
        protected Entry getFirst() {
            return header.lruNext;
        }

        // Method to get the last entry in the list
        protected Entry getLast() {
            return header.lruPrev;
        }

        // Method to remove the first entry from the list
        protected Entry removeFirst() {
            Entry first = header.lruNext;
            remove(first);
            return first;
        }

        // Method to move an entry to the end of the list
        protected synchronized void replaceLast(Entry e) {
            remove(e);
            addLast(e);
        }

        protected int length() {
            return (size);
        }
        
        protected void clear() {
            header = new Entry(0, null, null, null);
        }
    }

    /**
     * Cache collision list.
     */
    private static class Entry implements Map.Entry {
        int hash;
        Object key;
        Object value;
        Entry next;
        // Fields used to maintain a sorted list
        Entry lruNext;
        Entry lruPrev;

        protected Entry(int hash, Object key, Object value, Entry next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
            this.lruNext = null;
            this.lruPrev = null;
        }

        @Override
        protected synchronized Object clone() {
            return new Entry(hash, key, value, (next == null ? null
                    : (Entry) next.clone()));
        }

        protected void changeValues(int hash, Object key, Object value,
                Entry next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
            // Do not change the lru pointers. b'coz they can be changed
            // accordingly
        }

        // Map.Entry Ops

        public Object getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }

        public Object setValue(Object value) {
            if (value == null)
                throw new NullPointerException();

            Object oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry e = (Map.Entry) o;

            return (key == null ? e.getKey() == null : key.equals(e.getKey()))
                    && (value == null ? e.getValue() == null : value.equals(e
                            .getValue()));
        }

        @Override
        public int hashCode() {
            return hash ^ (value == null ? 0 : value.hashCode());
        }

        @Override
        public String toString() {
            return key.toString() + "=" + value.toString();
        }
    }

    // Types of Enumerations/Iterations
    private static final int KEYS = 0;

    private static final int VALUES = 1;

    private static final int ENTRIES = 2;

    /**
     * A Cache enumerator class. This class implements both the Enumeration and
     * Iterator interfaces, but individual instances can be created with the
     * Iterator methods disabled. This is necessary to avoid unintentionally
     * increasing the capabilities granted a user by passing an Enumeration.
     */
    private class Enumerator implements Enumeration, Iterator {
        Entry[] table = Cache.this.table;

        int index = table.length;

        Entry entry = null;

        Entry lastReturned = null;

        int type;

        /**
         * Indicates whether this Enumerator is serving as an Iterator or an
         * Enumeration. (true -> Iterator).
         */
        boolean iterator;

        /**
         * The modCount value that the iterator believes that the backing List
         * should have. If this expectation is violated, the iterator has
         * detected concurrent modification.
         */
        private int expectedModCount = modCount;

        Enumerator(int type, boolean iterator) {
            this.type = type;
            this.iterator = iterator;
        }

        public boolean hasMoreElements() {
            while (entry == null && index > 0)
                entry = table[--index];

            return entry != null;
        }

        public Object nextElement() {
            while (entry == null && index > 0)
                entry = table[--index];

            if (entry != null) {
                Entry e = lastReturned = entry;
                entry = e.next;
                return type == KEYS ? e.key : (type == VALUES ? e.value : e);
            }
            throw new NoSuchElementException("Cache Enumerator");
        }

        // Iterator methods
        public boolean hasNext() {
            return hasMoreElements();
        }

        public Object next() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            return nextElement();
        }

        public void remove() {
            if (!iterator)
                throw new UnsupportedOperationException();
            if (lastReturned == null)
                throw new IllegalStateException("Cache Enumerator");
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();

            synchronized (Cache.this) {
                Entry[] tab = Cache.this.table;
                int index = (lastReturned.hash & 0x7FFFFFFF) % tab.length;

                for (Entry e = tab[index], prev = null; e != null; 
                                                prev = e, e = e.next) 
                {
                    if (e == lastReturned) {
                        modCount++;
                        expectedModCount++;
                        if (prev == null)
                            tab[index] = e.next;
                        else
                            prev.next = e.next;
                        count--;
                        lastReturned = null;
                        return;
                    }
                }
                throw new ConcurrentModificationException();
            }
        }
    }

    static class SynchronizedCollection implements Collection, Serializable {
        // use serialVersionUID from JDK 1.2.2 for interoperability
        private static final long serialVersionUID = 3053995032091335093L;

        Collection c; // Backing Collection

        Object mutex; // Object on which to synchronize

        SynchronizedCollection(Collection c) {
            if (c == null) {
                throw new NullPointerException();
            }
            this.c = c;
            mutex = this;
        }

        SynchronizedCollection(Collection c, Object mutex) {
            this.c = c;
            this.mutex = mutex;
        }

        public int size() {
            synchronized (mutex) {
                return c.size();
            }
        }

        public boolean isEmpty() {
            synchronized (mutex) {
                return c.isEmpty();
            }
        }

        public boolean contains(Object o) {
            synchronized (mutex) {
                return c.contains(o);
            }
        }

        public Object[] toArray() {
            synchronized (mutex) {
                return c.toArray();
            }
        }

        public Object[] toArray(Object[] a) {
            synchronized (mutex) {
                return c.toArray(a);
            }
        }

        public Iterator iterator() {
            return c.iterator(); // Must be manually synched by user!
        }

        public boolean add(Object o) {
            synchronized (mutex) {
                return c.add(o);
            }
        }

        public boolean remove(Object o) {
            synchronized (mutex) {
                return c.remove(o);
            }
        }

        public boolean containsAll(Collection coll) {
            synchronized (mutex) {
                return c.containsAll(coll);
            }
        }

        public boolean addAll(Collection coll) {
            synchronized (mutex) {
                return c.addAll(coll);
            }
        }

        public boolean removeAll(Collection coll) {
            synchronized (mutex) {
                return c.removeAll(coll);
            }
        }

        public boolean retainAll(Collection coll) {
            synchronized (mutex) {
                return c.retainAll(coll);
            }
        }

        public void clear() {
            synchronized (mutex) {
                c.clear();
            }
        }

        @Override
        public String toString() {
            synchronized (mutex) {
                return c.toString();
            }
        }
    }

    static class SynchronizedSet extends SynchronizedCollection implements Set {
        SynchronizedSet(Set s) {
            super(s);
        }

        SynchronizedSet(Set s, Object mutex) {
            super(s, mutex);
        }

        @Override
        public boolean equals(Object o) {
            synchronized (mutex) {
                return c.equals(o);
            }
        }

        @Override
        public int hashCode() {
            synchronized (mutex) {
                return c.hashCode();
            }
        }
    }
}
