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
 * FvPrincipal.java
 *
 * Created on 2007/09/20, 21:11 
 * @author yasushi.iwakata@sun.com
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.authentication.modules.fvauth;

import java.io.Serializable;
import java.security.Principal;

/**
 * User Principal.
 */
public class FvPrincipal implements Principal, Serializable {
    private String name;

    public FvPrincipal(String name) {
        if (name == null) {
            throw new NullPointerException("illegal null input");
        }
        this.name = name;
    }
    
    /**
     * Returns the name of the authenticated user.
     *
     * @return Fv authenticated user for this <code>FvPrincipal</code>
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns a string representation of this <code>FvPrincipal</code>.
     *
     * @return a string representation of this <code>FvPrincipal</code>.
     */
    @Override
    public String toString() {
        return("FvPrincipal:  " + name);
    }
    
    /**
     * Compares the specified Object with this <code>FvPrincipal</code>
     * for equality.  Returns true if the given object is also a
     * <code>FvPrincipal</code> and the two FvPrincipals
     * have the same username.
     *
     * @param o Object to be compared for equality with this
     *        <code>FvPrincipal</code>.
     *
     * @return true if the specified Object is equal equal to this
     *         <code>FvPrincipal</code>.
     */
    @Override public boolean equals(Object o) {
        if ((o == null) || !(o instanceof FvPrincipal)) {
            return false;
        }
        if (this == o) {
            return true;
        }
        FvPrincipal that = (FvPrincipal)o;
        return this.getName().equals(that.getName());
    }
    
    /**
     * Return a hash code for this <code>FvPrincipal</code>.
     *
     * @return a hash code for this <code>FvPrincipal</code>.
     */
    @Override public int hashCode() {
        return name.hashCode();
    }
}
