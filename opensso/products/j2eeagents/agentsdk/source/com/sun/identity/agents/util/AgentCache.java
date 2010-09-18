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
 * $Id: AgentCache.java,v 1.2 2008/06/25 05:51:58 qcheng Exp $
 *
 */

package com.sun.identity.agents.util;

import java.util.Enumeration;

import com.iplanet.am.util.Cache;
import com.sun.identity.agents.arch.AgentConfiguration;

/**
 * A general purpose cache used by agent filter
 */
public class AgentCache {
    
    public AgentCache(String name, int size) {
        setName(name);
        setCache(new Cache(size));
    }
    
    public AgentCache(String name) {
        this(name, 1000);
    }
    
    /**
     * Method get
     * Returns the String representation of the value to which
     * the specified key is mapped in this Cache
     *
     * @param key a String key in the cache
     * @return an Object value to which the key is mapped in this cache or null
     * if the key is not mapped to any value in this cache
     *
     * @see  com.sun.identity.agents.common.CacheManager#put
     */
        public Object get (String key) {
          return getCache().get(key);
    }

    /**
     * Method put
     * Maps the specified key to the specified value in this Cache. Neither the
     * key nor the value can be null. If the cache is full to its capacity, then
     * the least recently used entry in the cache will be replaced.
     * <br>
     * The value can be retrieved by calling the get method with a key that is
     * equal to the original key.
     * @param key  - the Cache key. Should be a String
     * @param value - the object value of the supplied key
     * @see com.sun.identity.agents.common.CacheManager#get
     */

        public void put(String key, Object value) {
            getCache().put(key, value);
    }

        /**
         * Method remove
         * Removes the key (and its corresponding value) from this Cache. This
         * method does nothing if the key is not in the Cache.
         * @param key the String key that needs to be removed
         * @return the value to which the key was mapped in this cache, or
         * null if the key did not have a mapping
         */
        public java.lang.Object remove(String key) {
            return (getCache().remove(key));
    }


        /**
         * Method isEmpty
         * Tests if this Cache maps no keys to values
         * @return true if this Cache maps no keys to values; false otherwise
         */
        public boolean isEmpty() {
                return getCache().isEmpty();
        }


        /**
         * Method contains
         * Tests if some key maps into the specified value String in this Cache.
         * This operation is more expensive than the containsKey  method
         *
         * @param value a <b>String</b> value to search for. For any other type 
         * of object it will throw AgentException
         * @return true if and only if some key maps to the value argument in
         * this Cache as determined by the equals method; false otherwise.
         * @throws NullPointerException - if the value is null
         */
    public boolean contains(java.lang.Object value) {
                return getCache().contains(value);
    }

        /**
         * Method containsKey
         * Tests if the specified String is a key in this Cache
     *
         * @param key a String object whose presence in this cache
         * is to be tested.
         * @return true if and only if the specified object is a key in this 
         * Cache, as determined by the equals method; false otherwise
         */
        public boolean containsKey (String key) {
                return getCache().containsKey(key);
        }

        /**
         * Method containsValue
         * Returns true if this Cache maps one or more keys to this value
     *
         * @param value whose presence in this Cache is to be tested.
         * @return true if this Cache maps one or more keys to this value
         */
    public boolean containsValue(java.lang.Object value) {
                return getCache().containsValue(value);
    }

    /**
     * Method keys
     * Returns an enumeration of the keys in this Cache
     * @return an enumeration of the keys in this Cache
     */
      public Enumeration keys() {
                return getCache().keys();
    }

        /**
         * Method elements
         * Returns an enumeration of the values in this Cache. Use the 
         * Enumeration methods on the returned object to fetch the elements
         * @return an enumeration of the values in this Cache
         */
    public java.util.Enumeration elements() {
            return getCache().elements();
    }


    /**
     * Method size
     * Returns the number of keys in this Cache
     * @return the number of keys in this Cache
     */
    public int size() {
        return getCache().size();
    }

    /**
     * Method keySet
     *
     * Returns a Set view of the keys contained in this Cache. The Set is backed
     * by the Cache, so changes to the Cache are reflected in the Set, and
     * vice-versa. The Set supports element removal (which removes the
     * corresponding entry from the Cache), but not element addition.
     *
     * @return  a Set view of the keys contained in this Cache
     */
        public java.util.Set keySet() {
        return getCache().keySet();
        }

        /**
         * Method getName
         * Returns the name String of this object set by setName method
         * @return java.lang.String name set by setName method
         */
        public String getName() {
                return _name;
        }

        /**
         * Method setName
         * Sets an instance variable called name in ths object to be used for
         * logging or debugging purposes
         * @param name to be assigned to an instance variable for this object
         * Must be a String; it will not do anything otherwise
         */
    private void setName(String name) {
                        _name = name;
    }

    /**
     * Method getCache
     * Returns the instance of <code>com.iplanet.am.util.Cache</code>
     * @return the instance of the <code>Cache</code> object
     * @see com.iplanet.am.util.Cache
     */
    private final Cache getCache() {
        return _cache;
    }

    /**
     * Method getCache
     * Stores the instance of Cache object to an instance variable
     * @param cache an instance of Cache object
     * @see com.iplanet.am.util.Cache
     */
    private void setCache( Cache cache ) {
        _cache = cache;
    }

    private String _name;
    private Cache _cache;    
    
    static {
        AgentConfiguration.initialize();
    }
}
