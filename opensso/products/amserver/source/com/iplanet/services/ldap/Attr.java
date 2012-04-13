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
 * $Id: Attr.java,v 1.4 2009/01/28 05:34:49 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.ldap;

import java.util.ArrayList;
import java.util.Locale;

import com.sun.identity.shared.ldap.LDAPAttribute;

/**
 * Represents an attribute value pair in UMS. The value of an attribute can be
 * of multiple values.
 * @supported.api
 */
public class Attr implements java.io.Serializable, java.lang.Cloneable {

    String _name;

    private ArrayList _stringValues;

    private ArrayList _byteValues;

    private LDAPAttribute _ldapAttribute;

    /**
     * Default constructor
     * @supported.api
     */
    public Attr() {
    }

    /**
     * Constructs an attribute value pair with no value.
     * 
     * @param name
     *            attribute name
     * @supported.api
     */
    public Attr(String name) {
        _name = name.toLowerCase();
    }

    /**
     * Construct an attribute value pair with a single string value.
     * 
     * @param name
     *            the name of attribute
     * @param value
     *            string value of attribute
     * @supported.api
     */
    public Attr(String name, String value) {
        _name = name.toLowerCase();
        _stringValues = new ArrayList(1);
        _stringValues.add(value);
    }

    /**
     * Construct an attribute value pair with a multiple string values.
     * 
     * @param name
     *            the name of attribute
     * @param value
     *            multiple string values of attribute
     * @supported.api
     */
    public Attr(String name, String[] value) {
        _name = name.toLowerCase();
        int size = value.length;
        _stringValues = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            _stringValues.add(value[i]);
        }
    }

    /**
     * Constructs an attribute value pair with byte array.
     * 
     * @param name
     *            attribute name
     * @param value
     *            byte array as input for value
     * @supported.api
     */
    public Attr(String name, byte[] value) {
        _name = name.toLowerCase();
        _byteValues = new ArrayList(1);
        _byteValues.add(value);
    }

    /**
     * Constructs an attribute value pair with array of byte array.
     * 
     * @param name
     *            attribute name
     * @param value
     *            array of byte array as input for value
     * @supported.api
     */
    public Attr(String name, byte[][] value) {
        _name = name.toLowerCase();
        _byteValues = new ArrayList(1);
        int size = value.length;
        for (int i = 0; i < size; i++) {
            _byteValues.add(value[i]);
        }
    }

    /**
     * Construct an attribute based on a LDAP attribute
     * 
     * @param attr
     *            ldap attribute to construct from
     */
    public Attr(LDAPAttribute attr) {
        _name = attr.getName().toLowerCase();
        _ldapAttribute = attr; // attr.clone() ?
    }

    /**
     * Map to a ldap attribute
     * 
     * @return an ldap attribute
     */
    public LDAPAttribute toLDAPAttribute() {
        int size = 0;
        LDAPAttribute ldapAttribute = null;
        if (_stringValues != null) {
            size = _stringValues.size();
            if (size == 0) {
                ldapAttribute = new LDAPAttribute(_name);
            } else if (size == 1) {
                ldapAttribute = new LDAPAttribute(_name, (String) _stringValues
                        .get(0));
            } else if (size > 1) {
                ldapAttribute = new LDAPAttribute(_name);
                for (int i = 0; i < size; i++) {
                    ldapAttribute.addValue((String) _stringValues.get(i));
                }
            }
        } else if (_byteValues != null) {
            ldapAttribute = new LDAPAttribute(_name);
            size = _byteValues.size();
            for (int i = 0; i < size; i++) {
                ldapAttribute.addValue((byte[]) _byteValues.get(i)); // clone?
            }
        } else if (_ldapAttribute != null) {
            ldapAttribute = _ldapAttribute; // clone?
        } else if (_name != null) {
            ldapAttribute = new LDAPAttribute(_name);
        }
        return ldapAttribute;
    }

    /**
     * Set value of an attribute
     * 
     * @param value
     *            the attribute value to be set
     * @supported.api
     */
    public void setValue(String value) {
        if (_stringValues == null) {
            setupStringValues();
        }
        _stringValues.clear();
        addValue(value);
    }

    /**
     * Add a string value to the attribute
     * 
     * @param value
     *            value to be added to the attribute
     * @supported.api
     */
    public void addValue(String value) {
        if (_stringValues == null) {
            setupStringValues();
        }
        if (!_stringValues.contains(value)) {
            _stringValues.add(value);
        }
    }

    /**
     * Add mulitple string values to the attribute
     * 
     * @param values
     *            string values to be added to the attribute
     * @supported.api
     */
    public void addValues(String[] values) {
        int size = values.length;
        for (int i = 0; i < size; i++) {
            addValue(values[i]);
        }
    }

    /**
     * Remove a specified string value in the attribute
     * 
     * @param value
     *            specified value to be remvoed from the value array
     * @supported.api
     */
    public void removeValue(String value) {
        if (_stringValues == null) {
            setupStringValues();
        }
        int size = _stringValues.size();
        for (int i = 0; i < size; i++) {
            if (_stringValues.get(i).equals(value)) {
                _stringValues.remove(i);
                break;
            }
        }
    }

    /**
     * Set value of an attribute
     * 
     * @param value
     *            the attribute value to be set
     * @supported.api
     */
    public void setValue(byte[] value) {
        if (_byteValues == null) {
            setupByteValues();
        }
        _byteValues.clear();
        addValue(value);
    }

    /**
     * Add a byte array value to the attribute
     * 
     * @param value
     *            byte array value to be added to the attribute
     * @supported.api
     */
    public void addValue(byte[] value) {
        if (_byteValues == null) {
            setupByteValues();
        }
        _byteValues.add(value); // clone?
    }

    /**
     * Add a list byte array values to the attribute
     * 
     * @param values
     *            of byte array values to be added to the attribute
     * @supported.api
     */
    public void addValues(byte[][] values) {
        int size = values.length;
        for (int i = 0; i < size; i++) {
            addValue(values[i]);
        }
    }

    /**
     * Remove a specified string value in the attribute
     * 
     * @param value
     *            specified value to be remvoed from the value array
     * @supported.api
     */
    public void removeValue(byte[] value) {
        if (_byteValues == null) {
            setupByteValues();
        }
        int size = _byteValues.size();
        for (int i = 0; i < size; i++) {
            // we might have to change the logic here to compare each byte
            if (_byteValues.get(i).equals(value)) {
                _byteValues.remove(i);
                break;
            }
        }
    }

    /**
     * Get name of an UMS attribute
     * 
     * @return name of an UMS attribute
     * @supported.api
     */
    public String getName() {
        return _name;
    }

    /**
     * Get name of attribute in a given Locale
     * 
     * @param locale
     *            Given locale for the attribute name to return
     * @return name of an UMS attribute
     * @supported.api
     */
    public String getName(Locale locale) {
        return Attr.getName(_name, locale);
    }

    /**
     * Get attribute name with locale input.
     * 
     * @param attrName
     *            name of the attribute
     * @param locale
     *            desired locale for the attribute
     * @return attribute name with locale attached for retrieval
     * @supported.api
     */
    static public String getName(String attrName, Locale locale) {
        String name = null;
        String baseName = getBaseName(attrName);
        if (locale == null) {
            name = baseName;
        } else {

            // TODO: ??? check if locale.toString method provides the
            // contents in locale.getLanguage, locale.getSubtype, and
            // locale.getVariant methods that match the language subtypes
            // in LDAP.
            //
            String localeStr = locale.toString();
            if (localeStr.length() > 0) {
                StringBuilder sb = new StringBuilder(baseName);
                sb.append(";lang-");
                sb.append(localeStr);
                name = sb.toString();
            }
        }
        return name;
    }

    /**
     * Get base name for the attribute. e.g, the base name
     * of "cn;lang-en" or "cn;lang-ja" is "cn"
     * 
     * @return basename of an attribute
     * @supported.api
     */
    public String getBaseName() {
        String baseName = null;
        if (_name == null) {
            baseName = null;
        } else {
            baseName = LDAPAttribute.getBaseName(_name);
        }
        return baseName;
    }

    /**
     * Get base name for an attribute name. e.g, the base
     * name of "cn;lang-en" or "cn;lang-ja" is "cn"
     * 
     * @return basename of the given attribute name
     * @supported.api
     */
    static public String getBaseName(String attrName) {
        return LDAPAttribute.getBaseName(attrName);
    }

    /**
     * Get one string value of the attribute
     * 
     * @return one value of the attribute
     * @supported.api
     */
    public String getValue() {
        String value = null;
        if (_stringValues == null) {
            setupStringValues();
        }
        if (!_stringValues.isEmpty()) {
            value = (String) _stringValues.get(0);
        }
        return value;
    }

    /**
     * Get the string values of the attribute
     * 
     * @return the values in an string array
     * @supported.api
     */
    public String[] getStringValues() {
        // Returning a colletion would be better, but would break existing
        // higher level
        // code

        // com.iplanet.ums.Pauser.pause("10", "_stringValues : " +
        // _stringValues);
        // com.iplanet.ums.Pauser.pause("10", "_stringValue : " + _stringValue);
        // System.out.println("_stringValue : " + _stringValue);
        // System.out.println("_stringValues : " + _stringValues);
        String[] stringValues = null;
        if (_stringValues == null) {
            setupStringValues();
        }
        int size = _stringValues.size();
        stringValues = new String[size];
        for (int i = 0; i < size; i++) {
            stringValues[i] = (String) _stringValues.get(i);
        }
        return stringValues;
    }

    /**
     * Checks whether the given value already exist for
     * the attribute
     * 
     * @param value
     *            the value to check for
     * @return <code>true</code> if the value already exists,
     *         <code>false</code> otherwise
     * @supported.api
     */
    public boolean contains(String value) {
        boolean contained = false;
        if (_stringValues == null) {
            setupStringValues();
        }
        int size = _stringValues.size();
        for (int i = 0; i < size; i++) {
            if (_stringValues.get(i).equals(value)) {
                contained = true;
                break;
            }
        }
        return contained;
    }

    /**
     * Get one byte[] value of the attribute Returning a
     * colletion would be better, but will not be consistent with the method
     * getStringValues()
     * 
     * @return one byte[] value
     * @supported.api
     */
    public byte[] getByteValue() {
        // Not cloning the value before returning. Do we need to clone?
        byte[] value = null;
        if (_byteValues == null) {
            setupByteValues();
        }
        if (!_byteValues.isEmpty()) {
            value = (byte[]) _byteValues.get(0);
        }
        return value;
    }

    /**
     * Get the byte[] values of the attribute
     * 
     * @return the byte[] values in array
     * @supported.api
     */
    public byte[][] getByteValues() {
        // Not cloning the values before returning. Do we need to clone?
        byte[][] byteValues = null;
        if (_byteValues == null) {
            setupByteValues();
        }
        int size = _byteValues.size();
        byteValues = new byte[size][];
        for (int i = 0; i < size; i++) {
            byteValues[i] = (byte[]) _byteValues.get(i);
        }
        return byteValues;
    }

    /**
     * Checks whether the given value already exist for
     * the attribute
     * 
     * @param value
     *            the value to check for
     * @return <code>true</code> if the value already exists,
     *         <code>false</code> otherwise
     * @supported.api
     */
    public boolean contains(byte[] value) {
        boolean contained = false;
        if (_byteValues == null) {
            setupByteValues();
        }
        int size = _byteValues.size();
        for (int i = 0; i < size; i++) {
            // we might have to change the logic here to compare each byte
            if (_byteValues.get(i).equals(value)) {
                contained = true;
                break;
            }
        }
        return contained;
    }

    /**
     * Get the number of values of the attribute
     * 
     * @return The number of values of the attribute
     * @supported.api
     */
    public int size() {
        int size = 0;
        if (_stringValues != null) {
            size = _stringValues.size();
        } else if (_byteValues != null) {
            size = _byteValues.size();
        } else if (_ldapAttribute != null) {
            size = _ldapAttribute.size();
        }
        return size;
    }

    /**
     * Return a copy of the object
     * 
     * @return A copy of the object
     * @supported.api
     */
    public Object clone() {
        /*
         * TO DO : revisit and do proper deep cloning?
         */
        Attr theClone = null;
        try {
            theClone = (Attr) super.clone();
        } catch (Exception e) {
        }
        if (_stringValues != null) {
            theClone._stringValues = (ArrayList) _stringValues.clone();
        }
        if (_byteValues != null) {
            theClone._byteValues = (ArrayList) _byteValues.clone();
        } else if (_ldapAttribute != null) {
            theClone._ldapAttribute = _ldapAttribute; // clone?
        }
        return theClone;
    }

    /**
     * Retrieves the string representation of an attribute
     * 
     * @return string representation of the attribute.
     * @supported.api
     */
    public String toString() {
        if (_stringValues == null) {
            setupStringValues();
        }
        return "Name : " + _name + _stringValues;
    }

    /**
     */
    private void setupStringValues() {
        if (_ldapAttribute != null) {
            String[] values = _ldapAttribute.getStringValueArray();
            int size = values.length;
            _stringValues = new ArrayList(size);
            for (int i = 0; i < size; i++) {
                _stringValues.add(values[i]);
            }
        } else {
            _stringValues = new ArrayList();
        }
    }

    /**
     */
    private void setupByteValues() {
        if (_ldapAttribute != null) {
            byte[][] values = _ldapAttribute.getByteValueArray();
            int size = values.length;
            _byteValues = new ArrayList(size);
            for (int i = 0; i < size; i++) {
                _byteValues.add(values[i]);
            }
        } else {
            _byteValues = new ArrayList();
        }
    }

}
