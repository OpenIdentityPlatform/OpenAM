/* The contents of this file are subject to the terms
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
 * $Id: VALidPrincipal.java,v 1.1 2009/04/21 10:23:28 ja17658 Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.authentication.modules.valid;

import java.io.IOException;
import java.security.Principal;

public class VALidPrincipal implements Principal, java.io.Serializable {
    private String name;

    /**
     * TODO-JAVADOC
     */
    public VALidPrincipal(String name) {
        if (name == null) {
            throw new NullPointerException("illegal null input");
        }
        this.name = name;
    }

    /**
     * Returns the VALid user name for this <code>VALid</code>.
     *
     * @return the VALid user name for this <code>VALid</code>.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a string representation of this <code>VALid</code>.
     *
     * @return a string representation of this <code>VALid</code>.
     */
    public String toString() {
        return("VALidPrincipal:  " + name);
    }

    /**
     * Compares the specified Object with this <code>VALidPrincipal</code>
     * for equality.  Returns <code>true</code> if the given object is also a
     * <code>VALidPrincipal</code> and the two <code>VALidPrincipal</code>s
     * have the same user name.
     *
     * @param o Object to be compared for equality with this
     *        <code>VALidPrincipal</code>.
     *
     * @return <code>true</code> if the specified Object is equal equal to this
     *         <code>VALidPrincipal</code>.
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }
 
        if (!(o instanceof VALidPrincipal)) {
            return false;
        }

        VALidPrincipal that = (VALidPrincipal)o;
        return this.getName().equals(that.getName());
    }
 
    /**
     * Returns a hash code for this <code>VALid</code>.
     *
     * @return a hash code for this <code>VALid</code>.
     */
    public int hashCode() {
        return name.hashCode();
    }
}
