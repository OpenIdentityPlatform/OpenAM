/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.push;

import java.security.Principal;
import java.util.Objects;

/**
 * AuthenticatorPush-specific principal representation. Useful when this module operates in
 * First Factor module mode.
 */
public class AuthenticatorPushPrincipal implements Principal, java.io.Serializable {

    private String name;

    /**
     * Public constructor that takes user name.
     *
     * @param name name of the principal to represent
     */
    public AuthenticatorPushPrincipal(String name) {
        if (name == null) {
            throw new NullPointerException("illegal null input");
        }

        this.name = name;
    }

    /**
     * Returns the AuthenticatorPush username for this <code>AuthenticatorPushPrincipal</code>.
     * @return the AuthenticatorPush username for this <code>AuthenticatorPushPrincipal</code>
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a string representation of this <code>AuthenticatorPushPrincipal</code>.
     *
     * @return a string representation of this <code>AuthenticatorPushPrincipal</code>.
     */
    public String toString() {
        return ("AuthenticatorPushPrincipal:  " + name);
    }

    /**
     * Compares the specified Object with this <code>AuthenticatorPushPrincipal</code>
     * for equality.  Returns true if the given object is also a
     * <code>AuthenticatorPushPrincipal</code> and the two AuthenticatorPushPrincipals
     * have the same username.
     *
     * @param o Object to be compared for equality with this
     *          <code>AuthenticatorPushPrincipal</code>.
     * @return true if the specified Object is equal equal to this
     *         <code>AuthenticatorPushPrincipal</code>.
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        if (getClass() != o.getClass()) {
            return false;
        }

        AuthenticatorPushPrincipal that = (AuthenticatorPushPrincipal) o;

        return Objects.equals(this.name, that.name);
    }

    /**
     * Returns a hash code for this <code>AuthenticatorPushPrincipal</code>.
     *
     * @return a hash code for this <code>AuthenticatorPushPrincipal</code>.
     */
    public int hashCode() {
        return name.hashCode();
    }

}