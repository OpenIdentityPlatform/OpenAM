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
 * $Id: AccessRightObject.java,v 1.3 2008/06/25 05:41:43 qcheng Exp $
 *
 */

package com.iplanet.ums;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Represents the attribute access rights associated with a user or role.
 * 
 * @supported.api
 */
public class AccessRightObject {
    // readable attributes
    private HashSet readables = new HashSet();

    // writable attributes
    private HashSet writables = new HashSet();

    /**
     * Default constructor
     *
     * @supported.api
     */
    public AccessRightObject() {
    }

    /**
     * This constructor establishes collections of readable attribute names and
     * writeable attribute names.
     * 
     * @param readableAttributeNames
     *            Collection of readable attribute names
     * @param writableAttributeNames
     *            Collection of writable attribute names
     *           
     *
     * @supported.api
     */
    public AccessRightObject(Collection readableAttributeNames,
            Collection writableAttributeNames) {
        // need to convert all attribute names to lower case
        if (readableAttributeNames != null) {
            Iterator it = readableAttributeNames.iterator();
            while (it.hasNext()) {
                String temp = (String) it.next();
                readables.add(temp.toLowerCase());
            }
        }
        if (writableAttributeNames != null) {
            Iterator it = writableAttributeNames.iterator();
            while (it.hasNext()) {
                String temp = (String) it.next();
                writables.add(temp.toLowerCase());
            }
        }
    }

    /**
     * Grant read permission to attributes.
     * 
     * @param attributeNames
     *            A collection of attribute names to which read permission will
     *            be granted.
     * 
     * @supported.api
     */
    public void grantReadPermission(Collection attributeNames) {
        // need to convert all attribute names to lower case
        Iterator it = attributeNames.iterator();
        if (it != null) {
            while (it.hasNext()) {
                String temp = (String) it.next();
                readables.add(temp.toLowerCase());
            }
        }
    }

    /**
     * Grant write permission to attributes.
     * 
     * @param attributeNames
     *            A collection of attribute names to which write permission will
     *            be granted.
     * 
     * @supported.api
     */
    public void grantWritePermission(Collection attributeNames) {
        // need to convert all attribute names to lower case
        Iterator it = attributeNames.iterator();
        if (it != null) {
            while (it.hasNext()) {
                String temp = (String) it.next();
                writables.add(temp.toLowerCase());
            }
        }
    }

    /**
     * Revoke read permission on attributes.
     * 
     * @param attributeNames
     *            A collection of attribute names on which read permission will
     *            be revoked.
     * 
     * @supported.api
     */
    public void revokeReadPermission(Collection attributeNames) {
        // need to convert all attribute names to lower case
        Iterator it = attributeNames.iterator();
        if (it != null) {
            while (it.hasNext()) {
                String temp = (String) it.next();
                readables.remove(temp.toLowerCase());
            }
        }
    }

    /**
     * Revoke write permission on attributes.
     * 
     * @param attributeNames
     *            A collection of attribute names on which write permission will
     *            be revoked.
     * 
     * @supported.api
     */
    public void revokeWritePermission(Collection attributeNames) {
        // need to convert all attribute names to lower case
        Iterator it = attributeNames.iterator();
        if (it != null) {
            while (it.hasNext()) {
                String temp = (String) it.next();
                writables.remove(temp.toLowerCase());
            }
        }
    }

    /**
     * Get all the readable attribute names.
     * 
     * @return Collection of all the readable attribute names
     * 
     * @supported.api
     */
    public Collection getReadableAttributeNames() {
        return (Collection) readables.clone();
    }

    /**
     * Get all the writable attribute names.
     * 
     * @return Collection of all the writable attribute names
     * 
     * @supported.api
     */
    public Collection getWritableAttributeNames() {
        return (Collection) writables.clone();
    }

    /**
     * Check if an attribute is readable.
     * 
     * @param attributeName
     *            The attribute to be checked
     * @return <code>boolean; </code> true if this attribute is readable, false
     *         otherwise
     * 
     * @supported.api
     */
    public boolean isReadable(String attributeName) {
        if (readables.contains(attributeName.toLowerCase()))
            return true;
        else
            return false;
    }

    /**
     * Check if an attribute is writable.
     * 
     * @param attributeName
     *            The attribute to be checked.
     * @return <code>boolean;</code> true if this attribute is writable, false
     *         otherwise
     * 
     * @supported.api
     */
    public boolean isWritable(String attributeName) {
        if (writables.contains(attributeName.toLowerCase()))
            return true;
        else
            return false;
    }
}
