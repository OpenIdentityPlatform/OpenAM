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
 * $Id: SecurIDPrincipal.java,v 1.2 2008/09/04 23:36:24 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.securid;



import java.security.Principal;


public class SecurIDPrincipal implements Principal, java.io.Serializable {

    /**
     * @serial
     */
    private String name;

    
    public SecurIDPrincipal(String name) {
	if (name == null)
	    throw new NullPointerException("illegal null input");

	this.name = name;
    }

    /**
     * Return the SecurID username for this <code>SecurIDPrincipal</code>.
     *
     * <p>
     *
     * @return the SecurID username for this <code>SecurIDPrincipal</code>
     */
    public String getName() {
	return name;
    }

    /**
     * Return a string representation of this <code>SecurIDPrincipal</code>.
     *
     * <p>
     *
     * @return a string representation of this <code>SecurIDPrincipal</code>.
     */
    public String toString() {
	return("SecurIDPrincipal:  " + name);
    }

    /**
     * Compares the specified Object with this <code>SecurIDPrincipal</code>
     * for equality.  Returns true if the given object is also a
     * <code>SecurIDPrincipal</code> and the two SecurIDPrincipals
     * have the same username.
     *
     * <p>
     *
     * @param o Object to be compared for equality with this
     *		<code>SecurIDPrincipal</code>.
     *
     * @return true if the specified Object is equal equal to this
     *		<code>SecurIDPrincipal</code>.
     */
    public boolean equals(Object o) {
	if (o == null)
	    return false;

        if (this == o)
            return true;
 
        if (!(o instanceof SecurIDPrincipal))
            return false;
        SecurIDPrincipal that = (SecurIDPrincipal)o;

	if (this.getName().equals(that.getName()))
	    return true;
	return false;
    }
 
    /**
     * Return a hash code for this <code>SecurIDPrincipal</code>.
     *
     * <p>
     *
     * @return a hash code for this <code>SecurIDPrincipal</code>.
     */
    public int hashCode() {
	return name.hashCode();
    }
}
