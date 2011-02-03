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
 * $Id: Guid.java,v 1.4 2009/01/28 05:34:50 ww203982 Exp $
 *
 */

package com.iplanet.ums;

import com.sun.identity.shared.ldap.LDAPDN;

/**
 * This class represents an LDAP entry and it provides
 * access to the ID (dn) and GUID of the given name. Every persistent object
 * (that is, entry in the Directory server) has a GUID (Globally Unique
 * Identifier) associated with it. Upon doing a getGuid() (getGuid() is a method
 * in the PersistentObject class) on an LDAP entry, this GUID object would be
 * returned (not a DN string or a guid in the LDAP sense). Methods of the Guid
 * object could then be used to get the actual DN, etc.
 *
 * @supported.all.api
 */
public class Guid {

    // holds the LDAP dn for the LDAP entry associated with this Guid object
    private String _dn;

    // holds the unique ID for the LDAP entry associated with this Guid object
    private long _uniqueId;

    /**
     * Constructs a Guid object from the specified distinguished name.
     * 
     * @param dn
     *            string representation of the distinguished name
     */
    public Guid(String dn) {
        _dn = dn;
        _uniqueId = -1;
    }

    /**
     * Constructs a Guid object from the specified unique ID.
     * 
     * @param id
     *            unique ID
     */
    public Guid(long id) {
        _dn = "";
        _uniqueId = id;
    }

    /**
     * Constructs a Guid object from the specified distinguished name and unique
     * ID.
     * 
     * @param dn
     *            string representation of the distinguished name
     * @param id
     *            unique ID
     */
    public Guid(String dn, long id) {
        _dn = dn;
        _uniqueId = id;
    }

    /**
     * Returns the string representation of the distinguished name.
     * 
     * @return the string representation of the distinguished name
     */
    public String getDn() {
        return _dn;
    }

    /**
     * Sets the dn for this object. Note that the value is not persisted in
     * LDAP.
     * 
     * @param dn
     *            string representation of the distinguished name
     */
    protected void setDn(String dn) {
        _dn = dn;
    }

    /**
     * Returns the nsuniqueID name in the Guid object associated with an LDAP
     * entry.
     * 
     * @return the nsuniqueID name in the Guid object associated with an LDAP
     *         entry
     */
    public long getId() {
        return _uniqueId;
    }

    /**
     * Sets the nsuniqueID name in the Guid object associated with an LDAP entry
     * Note that the value is not persisted in LDAP.
     * 
     * @param id
     *            the nsuniqueID name
     */
    protected void setId(long id) {
        _uniqueId = id;
    }

    /**
     * Determines if the current Guid is equal to the specified Guid.
     * 
     * @param guid
     *            Guid to compare against the current Guid
     * @return true if the two Guids are the same
     */
    public boolean equals(Guid guid) {
        return LDAPDN.equals(_dn, guid.getDn());
    }

    /**
     * Compares two dn's for equality.
     * 
     * @param dn1
     *            the first dn to compare
     * @param dn2
     *            the second dn to compare
     * @return true if the two dn's are equal
     */
    static boolean equals(String dn1, String dn2) {
        return LDAPDN.equals(dn1, dn2);
    }

    /**
     * Returns the String form of this Guid object.
     * 
     * @return the string representation of the Guid
     */
    public String toString() {
        return _dn;
        // For future use
        // StringBuffer buff = new StringBuffer();
        // buff.append("DN : " + _dn + "\n");
        // buff.append("ID : " + _uniqueId + "\n");
        // return buff.toString();
    }
}
