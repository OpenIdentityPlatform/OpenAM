/*
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
 * $Id: ModSet.java,v 1.4 2009/01/28 05:34:49 ww203982 Exp $
 *
 * Portions Copyright 2015 ForgeRock AS.
 */

package com.iplanet.services.ldap;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.Modification;
import org.forgerock.opendj.ldap.ModificationType;

/**
 * Represents a set of modification on attributes
 * @supported.api
 */
public class ModSet implements java.io.Serializable {
    // TODO: This is an incomplete implementation. Currently subclass from
    // LDAPModificationSet is used to get things going. Need internal
    // representation overhaul to move away from "extends LDAPModification"

    /**
     * Modification specifiers for ADD
     */
    public static final int ADD = ModificationType.ADD.intValue();

    /**
     * Modification specifiers for REPLACE
     */
    public static final int REPLACE = ModificationType.REPLACE.intValue();

    /**
     * Modification specifiers for DELETE
     */
    public static final int DELETE = ModificationType.DELETE.intValue();

    static final long serialVersionUID = 4650238666753391214L;
    private int current = 0;
    private final List<Modification> modifications = new ArrayList<>();

    /**
     * Default consturctor
     */
    public ModSet() {
        current = 0;
    }

    /**
     * Constructor with an attribute set defaulting all operation types to
     * ModSet.ADD
     * 
     * @param attrSet
     *            Attribute set to construct the modSet. All operations are
     *            default to ModSet.ADD
     */
    public ModSet(AttrSet attrSet) {
        this(attrSet, ModSet.ADD);
    }

    /**
     * Construct ModSet given the same operation on a set of attributes
     * 
     * @param attrSet
     *            Attribute set to construct the ModSet
     * @param op
     *            Operation type for ADD, REPLACE or DELETE
     */
    public ModSet(AttrSet attrSet, int op) {
        for (int i = 0; i < attrSet.size(); i++) {
            this.add(op, attrSet.elementAt(i).toLDAPAttribute());
        }
    }

    /**
     * Retrieves the number of <CODE>LDAPModification</CODE>
     * objects in this set.
     * @return the number of <CODE>LDAPModification</CODE>
     * objects in this set.
     */
    public int size() {
        return modifications.size();
    }

    /**
     * Retrieves a particular <CODE>LDAPModification</CODE> object at
     * the position specified by the index.
     * @param index position of the <CODE>LDAPModification</CODE>
     * object that you want to retrieve.
     * @return <CODE>LDAPModification</CODE> object representing
     * a change to make to an attribute.
     */
    public Modification elementAt(int index) {
        return modifications.get(index);
    }

    /**
     * Removes a particular <CODE>LDAPModification</CODE> object at
     * the position specified by the index.
     * @param index position of the <CODE>LDAPModification</CODE>
     * object that you want to remove
     */
    public void removeElementAt(int index) {
        modifications.remove(index);
    }

    /**
     * Specifies another modification to be added to the set of modifications.
     * @param op the type of modification to make. This can be one of the following:
     *   <P>
     *   <UL>
     *   <LI><CODE>LDAPModification.ADD</CODE> (the value should be added to the attribute)
     *   <LI><CODE>LDAPModification.DELETE</CODE> (the value should be removed from the attribute)
     *   <LI><CODE>LDAPModification.REPLACE</CODE> (the value should replace the existing value of the attribute)
     *   </UL><P>
     * If you are working with a binary value (not a string value), you need to bitwise OR (|) the
     * modification type with <CODE>LDAPModification.BVALUES</CODE>.
     * <P>
     *
     * @param attr the attribute (possibly with values) to modify
     */
    public synchronized void add(int op, Attribute attr) {
        Modification mod = new Modification(ModificationType.valueOf(op), attr);
        modifications.add(mod);
    }

    /**
     * Removes the first attribute with the specified name in the set of modifications.
     * @param name name of the attribute to remove
     */
    public synchronized void remove( String name ) {
        for (int i = 0; i < modifications.size(); i++) {
            Modification mod = modifications.get(i);
            Attribute attr = mod.getAttribute();
            if (name.equalsIgnoreCase(attr.getAttributeDescriptionAsString())) {
                modifications.remove(i);
                return;
            }
        }
    }

    /**
     * Retrieves the string representation of the
     * modification set.
     *
     * @return string representation of the modification set.
     */
    public String toString() {
        return "LDAPModificationSet: " + modifications.toString();
    }
}
