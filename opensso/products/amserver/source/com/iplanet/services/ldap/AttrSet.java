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
 * $Id: AttrSet.java,v 1.4 2009/01/28 05:34:49 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.ldap;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import com.sun.identity.shared.ldap.LDAPAttributeSet;

/**
 * Represents a set of attributes
 * @supported.api
 */
public class AttrSet implements java.io.Serializable, java.lang.Cloneable {

    private ArrayList _attrs = new ArrayList();

    /**
     * Empty Attribute Set.
     * @supported.api
     */
    public static final AttrSet EMPTY_ATTR_SET = new AttrSet();

    /**
     * No argument constructor
     * @supported.api
     */
    public AttrSet() {
    }

    /**
     * Construct attribute set given an array of attributes.
     * 
     * @param attrs
     *            array of attributes to be defined in the attribute set
     * @supported.api
     */
    public AttrSet(Attr[] attrs) {
        int size = attrs.length;
        _attrs = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            _attrs.add(attrs[i]);
        }
    }

    /**
     * Construct attribute set given an attribute
     * 
     * @param attr
     *            attribute to be defined in the attribute set
     * @supported.api
     */
    public AttrSet(Attr attr) {
        add(attr);
    }

    /**
     * Construct AttrSet from LDAPAttributeSet
     * 
     * @param ldapAttrSet
     *            LDAP attribute set
     * 
     */
    public AttrSet(LDAPAttributeSet ldapAttrSet) {
        int size = ldapAttrSet.size();
        _attrs = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            _attrs.add(new Attr(ldapAttrSet.elementAt(i)));
        }
    }

    /**
     * Add one attribute to the AttrSet The attribute
     * should have only string values
     * 
     * @param attr
     *            attribute to be added to the set
     * @supported.api
     */
    public void add(Attr attr) {
        if (attr == null)
            return;
        Attr attr1 = findAttribute(attr.getName());
        if (attr1 == null) {
            _attrs.add(attr);
        } else {
            // attribute already exists,
            // add new values to existing attribute
            attr1.addValues(attr.getStringValues());
        }
    }

    /**
     * Add one attribute to the AttrSet The attribute
     * should have only byte values
     * 
     * @param attr
     *            attribute to be added to the set
     * @supported.api
     */
    public void addBinaryAttr(Attr attr) {
        Attr attr1 = findAttribute(attr.getName());
        if (attr1 == null) {
            _attrs.add(attr);
        } else {
            // attribute already exists,
            // add new values to existing attribute
            attr1.addValues(attr.getByteValues());
        }
    }

    /**
     * Removes an exisiting attribute
     * 
     * @param name
     *            attribute to be removed
     * @supported.api
     */
    public void remove(String name) {
        int index = indexOf(name);
        if (index != -1) {
            _attrs.remove(index);
        }
    }

    /**
     * Remove a specified value for an attribute in the
     * set
     * 
     * @param attrName
     *            attribute name to be looked up
     * @param delValue
     *            value to be deleted for the specified attribute
     * @supported.api
     */
    public void remove(String attrName, String delValue) {
        int index = indexOf(attrName);
        if (index != -1) {
            Attr attr = (Attr) _attrs.get(index);
            attr.removeValue(delValue);
            if (attr.size() == 0) {
                _attrs.remove(index);
            }
        }
    }

    /**
     * Replace an existing attribute.
     * 
     * @param attr
     *            attribute to be replaced
     * @supported.api
     */
    public void replace(Attr attr) {
        int index = indexOf(attr.getName());
        if (index != -1) {
            _attrs.set(index, attr);
        } else {
            _attrs.add(attr);
        }
    }

    /**
     * Get names of attributes.
     * 
     * @return Names of attributes in the set
     * @supported.api
     */
    public String[] getAttributeNames() {
        int size = size();
        String[] names = new String[size];
        for (int i = 0; i < size; i++) {
            names[i] = ((Attr) _attrs.get(i)).getName();
        }
        return names;
    }

    /**
     * Gets the attribute contained in the set. If not
     * found returns null object
     * 
     * @param name
     *            name of the attribute to get
     * @return attribute found
     * @supported.api
     */
    public Attr getAttribute(String name) {
        // We may probably want to clone. Not cloning now.
        return findAttribute(name);
    }

    /**
     * Enumerate the attributes contained in the attribute
     * set
     * 
     * @return enmeration of attributes in the set
     * @supported.api
     */
    public Enumeration getAttributes() {
        // iterator would be preferred; returning Enumeration for backward
        // compatibility
        return new IterEnumeration(_attrs.iterator());
    }

    /**
     * Gets the first string value right from a specified
     * attribute
     * 
     * @param attrName
     *            name of the attribute to be queried in the set
     * @return the first string value found
     * @supported.api
     */
    public String getValue(String attrName) {
        String value = null;
        Attr attr = findAttribute(attrName);
        if (attr != null) {
            value = attr.getValue();
        }
        return value;
    }

    /**
     * Check if attrSet has this attribute
     * 
     * @param attrName
     *            name of the attribute to be checked against the set
     * @return true if found and false otherwise
     * @supported.api
     */
    public boolean contains(String attrName) {
        boolean containsTheValue = false;
        int index = indexOf(attrName);
        if (index != -1) {
            containsTheValue = true;
        }
        return containsTheValue;
    }

    /**
     * Check if this attrSet has the attribute with the
     * given value
     * 
     * @param attrName
     *            name of the attribute to be checked against the set
     * @param value
     *            value of the attribute the attribute should contain
     * @return true if found and false otherwise
     * @supported.api
     */
    public boolean contains(String attrName, String value) {
        boolean containsTheValue = false;
        Attr attr = findAttribute(attrName);
        if (attr != null) {
            containsTheValue = attr.contains(value);
        }
        return containsTheValue;
    }

    /**
     * Get the number of attributes in the Attribute Set
     * 
     * @return number of attributes in the set
     * @supported.api
     */
    public int size() {
        return _attrs.size();
    }

    /**
     * Get the attribute at an index that starts from 0
     * 
     * @return the attribute at the given index
     */
    public Attr elementAt(int index) {
        return (Attr) _attrs.get(index);
    }

    /**
     * Gets the index for an attribute contained in the set
     * 
     * @return index that is zero based. If attrName is not found in the set,
     *         this method returns -1.
     */
    public int indexOf(String attrName) {
        attrName = attrName.toLowerCase();
        int index = -1;
        int size = _attrs.size();
        for (int i = 0; i < size; i++) {
            if (attrName.equals(((Attr) _attrs.get(i)).getName())) {
                index = i;
                break;
            }
        }
        return index;
    }

    /**
     * Find the attribute gvien the attribute name
     * 
     * @return attribute found, returns null if no such attribute exists
     */
    private Attr findAttribute(String name) {
        name = name.toLowerCase();
        Attr attr = null;
        if (_attrs != null) {
            int size = _attrs.size();
            for (int i = 0; i < size; i++) {
                Attr attr1 = (Attr) _attrs.get(i);
                if (attr1.getName().equals(name)) {
                    attr = attr1;
                    break;
                }
            }
        }
        return attr;
    }

    /**
     * Return a copy of the object
     * 
     * @return A copy of the object
     * @supported.api
     */
    public Object clone() {
        AttrSet attrSet = new AttrSet();
        int size = _attrs.size();
        for (int i = 0; i < size; i++) {
            attrSet.add((Attr) ((Attr) _attrs.get(i)).clone());
        }
        return attrSet;
    }

    /**
     * Maps to an LDAPAttributeSet
     * 
     * @return the equivalent LDAPAttributeSet
     */
    public LDAPAttributeSet toLDAPAttributeSet() {
        LDAPAttributeSet ldapAttrSet = new LDAPAttributeSet();
        int size = size();
        for (int i = 0; i < size; i++) {
            Attr attr = (Attr) _attrs.get(i);
            if (attr.size() > 0) {
                ldapAttrSet.add(attr.toLDAPAttribute());
            }
        }
        return ldapAttrSet;
    }

    /**
     * Retrieves the string representation of an AttrSet
     * 
     * @return string representation of the AttrSet.
     * @supported.api
     */
    public String toString() {
        StringBuilder sb = new StringBuilder("AttrSet: ");
        int size = _attrs.size();
        for (int i = 0; i < size; i++) {
            sb.append(_attrs.get(i).toString()).append("\n");
        }
        return sb.toString();
    }

}

class IterEnumeration implements Enumeration {

    private Iterator _iter;

    IterEnumeration(Iterator iterator) {
        _iter = iterator;
    }

    public boolean hasMoreElements() {
        return _iter.hasNext();
    }

    public Object nextElement() {
        return _iter.next();
    }

}
