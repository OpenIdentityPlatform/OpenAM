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
 * $Id: CaseInsensitiveProperties.java,v 1.2 2008/06/25 05:42:25 qcheng Exp $
 *
 */

package com.sun.identity.common;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

/**
 * A case insensitive Properties with case preservation. If key is a String, a
 * case insensitive hash code is used for hashing but original case of the key
 * is preserved.
 */
public class CaseInsensitiveProperties extends Properties {

	static class CaseInsensitiveEnumeration implements Enumeration {
        Enumeration mEnum = null;

        public CaseInsensitiveEnumeration(Enumeration en) {
            mEnum = en;
        }

        public boolean hasMoreElements() {
            boolean ans = false;
            if (mEnum != null) {
                ans = mEnum.hasMoreElements();
            }
            return ans;
        }

        public Object nextElement() {
            Object ans = null;
            if (mEnum != null) {
                Object nextElem = mEnum.nextElement();
                if (nextElem instanceof CaseInsensitiveKey) {
                    ans = nextElem.toString();
                } else {
                    ans = nextElem;
                }
            }
            return ans;
        }
    }

    public CaseInsensitiveProperties() {
        super();
    }

    public CaseInsensitiveProperties(Properties defaults) {
        super(defaults);
    }

    public String getProperty(String key) {
        return (String) super.get(new CaseInsensitiveKey(key));
    }

    public Object setProperty(String key, String value) {
        return super.put(new CaseInsensitiveKey(key), value);
    }

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
     * @return a case insensitive hash set of keys.
     */
    public Set keySet() {
        Set keys = super.keySet();
        CaseInsensitiveHashSet ciSet = new CaseInsensitiveHashSet();
        Iterator iter = keys.iterator();
        while (iter.hasNext()) {
            // keys are already CaseInsensitiveKey's so we can just add it.
            ciSet.add(iter.next());
        }
        return ciSet;
    }

    /*
     * @return an Enumeration of keys as String objects even though they were
     * internally stored as case insensitive strings.
     */
    public Enumeration keys() {
        return new CaseInsensitiveEnumeration(super.keys());
    }

    /*
     * @return an Enumeration of property names as String objects even though
     * they were internally stored as case insensitive strings.
     */
    public Enumeration propertyNames() {
        return new CaseInsensitiveEnumeration(super.propertyNames());
    }

    public Object put(Object key, Object value) {
        if (key instanceof String) {
            CaseInsensitiveKey ciKey = new CaseInsensitiveKey((String) key);
            return super.put(ciKey, value);
        } else {
        	return super.put(key, value);
        }
    }

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

}
