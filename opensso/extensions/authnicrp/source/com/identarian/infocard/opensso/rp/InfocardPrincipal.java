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
 * $Id: InfocardPrincipal.java,v 1.3 2009/07/08 08:59:28 ppetitsm Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2008 Patrick Petit Consulting
 */

package com.identarian.infocard.opensso.rp;

import java.io.Serializable;
import java.security.Principal;

public class InfocardPrincipal implements Principal, Serializable {
    private String name;

    public InfocardPrincipal(String name) {
        if (name == null) {
            throw new NullPointerException("illegal null input");
        }
        this.name = name;
    }

    /**
     * Returns the LDAP username for this <code>InfocardPrincipal</code>.
     *
     * @return the LDAP username for this <code>InfocardPrincipal</code>
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a string representation of this <code>InfocardPrincipal</code>.
     *
     * @return a string representation of this <code>InfocardPrincipal</code>.
     */
    @Override
    public String toString() {
        return(this.getClass().getName() + ":" + name);
    }

    /**
     * Compares the specified Object with this <code>InfocardPrincipal</code>
     * for equality.  Returns true if the given object is also a
     * <code>InfocardPrincipal</code> and the two SamplePrincipals
     * have the same username.
     *
     * @param o Object to be compared for equality with this
     *        <code>InfocardPrincipal</code>.
     *
     * @return true if the specified Object is equal equal to this
     *         <code>InfocardPrincipal</code>.
     */
    @Override
    public boolean equals(Object o) {
        if ((o == null) || !(o instanceof InfocardPrincipal)) {
            return false;
        }
        if (this == o) {
            return true;
        }
        InfocardPrincipal that = (InfocardPrincipal)o;
        return this.getName().equals(that.getName());
    }
 
    /**
     * Return a hash code for this <code>InfocardPrincipal</code>.
     *
     * @return a hash code for this <code>InfocardPrincipal</code>.
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
