/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: HOTPPrincipal.java,v 1.1 2009/03/24 23:52:12 pluo Exp $
 *
 */
/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.hotp;



import java.security.Principal;

public class HOTPPrincipal implements Principal, java.io.Serializable {

    private String name;

    /**
     * Public constructor that takes user name
     *
     */
    public HOTPPrincipal(String name) {
        if (name == null) {
            throw new NullPointerException("illegal null input");
        }

        this.name = name;
    }

    /**
     * Returns the HOTP username for this <code>HOTPPrincipal</code>.
     *
     * <p>
     *
     * @return the HOTP username for this <code>HOTPPrincipal</code>
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a string representation of this <code>HOTPPrincipal</code>.
     *
     * <p>
     *
     * @return a string representation of this <code>HOTPPrincipal</code>.
     */
    public String toString() {
        return ("HOTPPrincipal:  " + name);
    }

    /**
     * Compares the specified Object with this <code>HOTPPrincipal</code>
     * for equality.  Returns true if the given object is also a
     * <code>HOTPPrincipal</code> and the two HOTPPrincipals
     * have the same username.
     *
     * <p>
     *
     * @param o Object to be compared for equality with this
     *		<code>HOTPPrincipal</code>.
     *
     * @return true if the specified Object is equal equal to this
     *		<code>HOTPPrincipal</code>.
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        if (!(o instanceof HOTPPrincipal)) {
            return false;
        }
        HOTPPrincipal that = (HOTPPrincipal) o;

        if (this.getName().equals(that.getName())) {
            return true;
        }
        return false;
    }

    /**
     * Returns a hash code for this <code>HOTPPrincipal</code>.
     *
     * <p>
     *
     * @return a hash code for this <code>HOTPPrincipal</code>.
     */
    public int hashCode() {
        return name.hashCode();
    }
}
