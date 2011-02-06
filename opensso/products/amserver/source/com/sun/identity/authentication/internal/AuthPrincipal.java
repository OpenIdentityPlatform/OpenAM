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
 * $Id: AuthPrincipal.java,v 1.3 2008/06/25 05:41:53 qcheng Exp $
 *
 */

package com.sun.identity.authentication.internal;

import com.sun.identity.authentication.internal.server.AuthSPrincipal;

/**
 * <p>
 * This class extends the <code>AuthSPrincipal</code> class.
 * 
 * <p>
 * Principals such as this <code>AuthPrincipal</code> may be associated with a
 * particular <code>Subject</code> to augment that <code>Subject</code> with
 * an additional identity. Refer to the <code>Subject</code> class for more
 * information on how to achieve this. Authorization decisions can then be based
 * upon the Principals associated with a <code>Subject</code>.
 * 
 * @see java.security.Principal
 * @see javax.security.auth.Subject
 *
 * @supported.api
 */
public class AuthPrincipal extends AuthSPrincipal {

    /**
     * Create an <code>AuthPrincipal</code> with a user name.
     * 
     * @param name
     *            the name for this user.
     * @exception NullPointerException
     *                if the <code>name</code> is <code>null</code>.
     *
     * @supported.api
     */
    public AuthPrincipal(String name) {
        super(name);
    }

    /**
     * Return the user name for this <code>AuthPrincipal</code>.
     * 
     * @return the user name for this <code>AuthPrincipal</code>
     *
     * @supported.api
     */
    public String getName() {
        return name;
    }

    /**
     * Return the AuthMethod for this <code>AuthPrincipal</code>.
     * 
     * @return the AuthMethod for this <code>AuthPrincipal</code>
     */
    public String getAuthMethod() {
        return authMethod;
    }

    /**
     * Return the AuthLevel for this <code>AuthPrincipal</code>.
     * 
     * @return the AuthLevel for this <code>AuthPrincipal</code>
     */
    public String getAuthLevel() {
        return authLevel;
    }

    /**
     * Return a string representation of this
     * <code>AuthPrincipal</code>.
     * 
     * @return a string representation of this <code>AuthPrincipal</code>.
     *
     * @supported.api
     */
    public String toString() {
        return ("AuthPrincipal:  " + name);
    }

    /**
     * Compares the specified object with this
     * <code>AuthPrincipal</code> for equality. Returns true if the given
     * object is also an <code>AuthPrincipal</code> and the two
     * <code>AuthPrincipal</code>s have the same user name.
     * 
     * @param o
     *            Object to be compared for equality with this
     *            <code>AuthPrincipal</code>.
     * @return true if the specified Object is equal to this
     *         <code>AuthPrincipal</code>.
     *
     * @supported.api
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (this == o)
            return true;

        if (!(o instanceof AuthPrincipal))
            return false;

        AuthPrincipal that = (AuthPrincipal) o;

        if (this.getName().equals(that.getName()))
            return true;

        return false;
    }

    /**
     * Return a hash code for this
     * <code>AuthPrincipal</code>.
     * 
     * @return a hash code for this <code>AuthPrincipal</code>.
     *
     * @supported.api
     */
    public int hashCode() {
        return name.hashCode();
    }
}
