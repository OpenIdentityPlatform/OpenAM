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
 * $Id: QualifiedCollection.java,v 1.3 2008/06/25 05:41:38 qcheng Exp $
 *
 */

package com.iplanet.services.ldap.aci;

import java.util.Collection;

/**
 * Class that wraps a collection and a boolean flag Used to represent ACI target
 * attributes and ACI permissions. The boolean flag indicates whether the target
 * attributes ( or the permissions ) are exclusive or inclusive
 * @supported.api
 */
public class QualifiedCollection {

    Collection _collection;

    boolean _exclusive;

    /**
     * Constructor
     * 
     * @param collection
     *            the collections of values.
     * @param exclusive
     *            the boolean flag indicating whether the values are excluisive
     *            or inclusive.
     * @supported.api
     */
    public QualifiedCollection(Collection collection, boolean exclusive) {
        _collection = collection;
        _exclusive = exclusive;
    }

    /**
     * Compares whether the passed object is equal to this object semantically.
     * The objects are considered equal if they have the same collection of
     * values, and the same value of exclusive flag
     * 
     * @param object
     *            the object that is to be compared for equality
     * @return <code>true</code> if the passed object is equal to this object,
     *         <code>false</code> otherwise
     * @supported.api
     */
    public boolean equals(Object object) {
        boolean objectsEqual = false;
        if (object == this) {
            objectsEqual = true;
        } else if (object != null && object.getClass().equals(getClass())) {
            QualifiedCollection castObject = (QualifiedCollection) object;
            if ((castObject.isExclusive() == isExclusive())
                    && (castObject.getCollection().equals(getCollection()))) {
                objectsEqual = true;
            }
        }
        return objectsEqual;
    }

    /**
     * Clones this object
     * 
     * @return the cloned object
     * @supported.api
     */
    public Object clone() {
        QualifiedCollection theClone = null;
        try {
            theClone = (QualifiedCollection) super.clone();
        } catch (CloneNotSupportedException ingnored) {
        }
        if (theClone != null) {
            theClone.setCollection(getCollection());
            theClone.setExclusive(isExclusive());
        }
        return theClone;
    }

    /**
     * Sets the collection of values
     * 
     * @param collection
     *            the collection of values
     * @supported.api
     */
    public void setCollection(Collection collection) {
        _collection = collection;
    }

    /**
     * Gets the collection of values
     * 
     * @return the collection of values
     * @supported.api
     */
    public Collection getCollection() {
        return _collection;
    }

    /**
     * Sets the exclusive flag
     * 
     * @param exclusive
     *            value of exclusive flag
     * @supported.api
     */
    public void setExclusive(boolean exclusive) {
        _exclusive = exclusive;
    }

    /**
     * Gets the value of the exclusive flag
     * 
     * @return the value of the exclusive flag
     * @supported.api
     */
    public boolean isExclusive() {
        return _exclusive;
    }
}
