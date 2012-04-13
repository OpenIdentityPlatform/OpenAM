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
 * $Id: ADPrincipal.java,v 1.2 2008/06/25 05:41:55 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.ad;

import java.security.Principal;

public class ADPrincipal implements Principal, java.io.Serializable {
    private String name;
    
    public ADPrincipal(String name) {
        if (name == null) {
            throw new NullPointerException("illegal null input");
        }
        this.name = name;
    }

    /**
     * Returns the AD username for this <code>ADPrincipal</code>.
     *
     * @return the AD username for this <code>ADPrincipal</code>.
     */
    public String getName() {
        return name;
    }

    /**
     * Return a string representation of this <code>ADPrincipal</code>.
     *
     * @return a string representation of this <code>ADPrincipal</code>.
     */
    public String toString() {
        return("ADPrincipal:  " + name);
    }

    /**
     * Compares the specified Object with this <code>ADPrincipal</code>
     * for equality.  Returns true if the given object is also a
     * <code>ADPrincipal</code> and the two <code>ADPrincipal</code>s
     * have the same username.
     *
     * @param o Object to be compared for equality with this
     *          <code>ADPrincipal</code>.
     * @return <code>true</code> if the specified Object is equal equal to this
     *         <code>ADPrincipal</code>.
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        if (!(o instanceof ADPrincipal)) {
            return false;
        }

        ADPrincipal that = (ADPrincipal)o;
        return this.getName().equals(that.getName());
    }
 
    /**
     * Returns a hash code for this <code>ADPrincipal</code>.
     *
     * @return a hash code for this <code>ADPrincipal</code>.
     */
    public int hashCode() {
        return name.hashCode();
    }
}
