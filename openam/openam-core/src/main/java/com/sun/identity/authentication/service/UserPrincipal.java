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
 * $Id: UserPrincipal.java,v 1.2 2008/06/25 05:42:05 qcheng Exp $
 *
 */


package com.sun.identity.authentication.service;

import java.io.Serializable;
import java.security.Principal;

/**
 * This class represents the abstract notion of a user
 * which can be used to represent any entity, such as an individual, a
 * corporation, and a login id.
 */
public class UserPrincipal implements Principal, Serializable {
    private String name;

    /**
     * Creates <code>UserPrincipal</code> object.
     * @param name representing pricipal for user.
     */
    public UserPrincipal (String name) {
        if (name == null){
            throw new NullPointerException("illegal null input");
        }
        this.name = name;
    }

    /**
     * Return the user name for this <code>UserPrincipal</code>.
     *
     * @return the user name for this <code>UserPrincipal</code>.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a string representation of this <code>UserPrincipal</code>.
     *
     * @return a string representation of this <code>UserPrincipal</code>.
     */
    public String toString() {
        return("UserPrincipal:  " + name);
    }

    /**
     * Compares the specified Object with this <code>LDAPPrincipal</code>
     * for equality.  Returns <code>true</code> if the given object is also a
     * <code>LDAPPrincipal</code> and the two <code>LDAPPrincipal</code>s
     * have the same user name.
     *
     * @param o Object to be compared for equality with this
     *        <code>UserPrincipal</code>.
     * @return <code>true</code> if the specified Object is equal equal to this
     *         <code>UserPrincipal</code>.
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        if (!(o instanceof UserPrincipal)) {
            return false;
        }

        UserPrincipal that = (UserPrincipal)o;
        return this.getName().equals(that.getName());
    }
 
    /**
     * Returns a hash code for this <code>UserPrincipal</code>.
     *
     * @return a hash code for this <code>UserPrincipal</code>.
     */
    public int hashCode() {
        return name.hashCode();
    }
}
