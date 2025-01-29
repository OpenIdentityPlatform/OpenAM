/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.identityplatform.openam.click.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpSession;
import org.apache.click.util.FlashAttribute;

/**
 * Provides a Map adaptor for HttpSession objects. A SessionMap instance is
 * available in each Velocity page using the name "<span class="blue">session</span>".
 * <p/>
 * For example suppose we have a User object in the session with the
 * attribute name "user" when a user is logged on.  We can display the users
 * name in the page when the are logged onto the system.
 *
 * <pre class="codeHtml">
 * <span class="red">#if</span> (<span class="blue">$session</span>.user)
 *   <span class="blue">$session</span>.user.fullname you are logged on.
 * <span class="red">#else</span>
 *   You are not logged on.
 * <span class="red">#end</span> </pre>
 *
 * The ClickServlet adds a SessionMap instance to the Velocity Context before
 * it is merged with the page template.
 * <p/>
 * The SessionMap supports {@link FlashAttribute} which when accessed via
 * {@link #get(Object)} are removed from the session.
 */
public class SessionMap implements Map<String, Object> {

    /** The internal session attribute. */
    protected HttpSession session;

    /**
     * Create a <tt>HttpSession</tt> <tt>Map</tt> adaptor.
     *
     * @param value the http session
     */
    public SessionMap(HttpSession value) {
        session = value;
    }

    /**
     * @see java.util.Map#size()
     */
    public int size() {
        if (session != null) {
            int size = 0;
            Enumeration<?> enumeration = session.getAttributeNames();
            while (enumeration.hasMoreElements()) {
                enumeration.nextElement();
                size++;
            }
            return size;
        } else {
            return 0;
        }
    }

    /**
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * @see java.util.Map#containsKey(Object)
     */
    public boolean containsKey(Object key) {
        if (session != null && key != null) {
            return session.getAttribute(key.toString()) != null;
        } else {
            return false;
        }
    }

    /**
     * This method is not supported and will throw
     * <tt>UnsupportedOperationException</tt> if invoked.
     *
     * @see java.util.Map#containsValue(Object)
     */
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * If the stored object is a FlashObject this method will return the
     * FlashObject value and then remove it from the session.
     *
     * @see java.util.Map#get(Object)
     */
    public Object get(Object key) {
        if (session != null && key != null) {
            Object object = session.getAttribute(key.toString());

            if (object instanceof FlashAttribute) {
                FlashAttribute flashObject = (FlashAttribute) object;
                object = flashObject.getValue();
                session.removeAttribute(key.toString());
            }

            return object;

        } else {
            return null;
        }
    }

    /**
     * @see java.util.Map#put(Object, Object)
     */
    public Object put(String key, Object value) {
        if (session != null && key != null) {
            Object out = session.getAttribute(key.toString());

            session.setAttribute(key.toString(), value);

            return out;

        } else {
            return null;
        }
    }

    /**
     * @see java.util.Map#remove(Object)
     */
    public Object remove(Object key) {
        if (session != null && key != null) {
            Object out = session.getAttribute(key.toString());
            session.removeAttribute(key.toString());

            return out;

        } else {
            return null;
        }
    }

    /**
     * @see java.util.Map#putAll(Map)
     */
    public void putAll(Map<? extends String, ?> map) {
        if (session != null && map != null) {
            for (Map.Entry<? extends String, ? extends Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                session.setAttribute(key, value);
            }
        }
    }

    /**
     * @see java.util.Map#clear()
     */
    public void clear() {
        if (session != null) {
            Enumeration<?> enumeration = session.getAttributeNames();
            while (enumeration.hasMoreElements()) {
                String name = enumeration.nextElement().toString();
                session.removeAttribute(name);
            }
        }
    }

    /**
     * @see java.util.Map#keySet()
     */
    public Set<String> keySet() {
        if (session != null) {
            Set<String> keySet = new HashSet<String>();

            Enumeration<?> enumeration = session.getAttributeNames();
            while (enumeration.hasMoreElements()) {
                keySet.add(enumeration.nextElement().toString());
            }

            return keySet;

        } else {
            return Collections.emptySet();
        }
    }

    /**
     * @see java.util.Map#values()
     */
    public Collection<Object> values() {
        if (session != null) {
            List<Object> values = new ArrayList<Object>();

            Enumeration<?> enumeration = session.getAttributeNames();
            while (enumeration.hasMoreElements()) {
                String name = enumeration.nextElement().toString();
                Object value = session.getAttribute(name);
                values.add(value);
            }

            return values;

        } else {
            return Collections.emptyList();
        }
    }

    /**
     * @see java.util.Map#entrySet()
     */
    public Set<Map.Entry<String, Object>> entrySet() {
        if (session != null) {
            Set<Map.Entry<String, Object>> entrySet = new HashSet<Map.Entry<String, Object>>();

            Enumeration<?> enumeration = session.getAttributeNames();
            while (enumeration.hasMoreElements()) {
                String name = enumeration.nextElement().toString();
                Object value = session.getAttribute(name);
                entrySet.add(new Entry(name, value));
            }

            return entrySet;

        } else {
            return Collections.emptySet();
        }
    }

    static class Entry implements Map.Entry<String, Object> {

        final String key;

        Object value;

        /**
         * Creates new entry.
         */
        Entry(String k, Object v) {
            value = v;
            key = k;
        }

        public final String getKey() {
            return key;
        }

        public final Object getValue() {
            return value;
        }

        public final Object setValue(Object newValue) {
            Object oldValue = value;
            value = newValue;
            return oldValue;
        }

        /**
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry e = (Entry) o;
            Object k1 = getKey();
            Object k2 = e.getKey();
            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                Object v1 = getValue();
                Object v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2))) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            String k = getKey();
            Object v = getValue();

            int hash = 17;
            hash = hash * 37 + (k == null ? 0 : k.hashCode());
            hash = hash * 37 + (v == null ? 0 : v.hashCode());

            return hash;
        }
    }
}
