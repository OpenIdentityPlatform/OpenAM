/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: VIPPrincipal.java,v 1.2 2008/08/19 23:10:18 superpat7 Exp $
 *
 */

package com.sun;

import java.security.Principal;

/**
 * Implements the methods in the Principal interface
 *
 * @author Terry J. Gardner
 */
public class VIPPrincipal implements Principal {
    
    /**
     * @serial
     */
    private String name;
    
    public VIPPrincipal(String name) {
        if(name == null) {
            throw new NullPointerException("illegal null input");
        }
        this.name = name;
    }
    
    /**
     * Return the LDAP username for this <code>VIPPrincipal</code>.
     *
     * <p>
     *
     * @return the LDAP username for this <code>VIPPrincipal</code>
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * Return a string representation of this <code>VIPPrincipal</code>.
     *
     * <p>
     *
     * @return a string representation of this <code>VIPPrincipal</code>.
     */
    public String toString() {
        return("VIPPrincipal:  " + name);
    }
    
    /**
     * Compares the specified Object with this <code>VIPPrincipal</code>
     * for equality.  Returns true if the given object is also a
     * <code>VIPPrincipal</code> and the two VIPPrincipals
     * have the same username. If the object to be compared is null
     * no action is taken and no exception id thrown.
     *
     * <p>
     *
     * @param o Object to be compared for equality with this
     *		<code>VIPPrincipal</code>.
     *
     * @return true if the specified Object is equal equal to this
     *		<code>VIPPrincipal</code>.
     */
    public boolean equals(Object o) {
        if(o == null) {
            return false;
        }
        if(this == o) {
            return true;
        }
        if(!(o instanceof VIPPrincipal)) {
            return false;
        }
        VIPPrincipal that = (VIPPrincipal)o;
        
        if(this.getName().equals(that.getName())) {
            return true;
        }
        return false;
    }
    
    /**
     * Return a hash code for this <code>VIPPrincipal</code>.
     *
     * <p>
     *
     * @return a hash code for this <code>VIPPrincipal</code>.
     */
    public int hashCode() {
        return name.hashCode();
    }
}
