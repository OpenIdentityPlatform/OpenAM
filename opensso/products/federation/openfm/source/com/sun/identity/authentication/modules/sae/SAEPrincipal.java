/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SAEPrincipal.java,v 1.2 2008/06/25 05:49:31 qcheng Exp $
 *
 */

package com.sun.identity.authentication.modules.sae;

import java.io.IOException;

import javax.security.auth.*;
import javax.security.auth.login.*;
import javax.security.auth.callback.*;

import java.security.Principal;


public class SAEPrincipal implements Principal, java.io.Serializable {

    /**
     * @serial
     */
    private String name;

    public SAEPrincipal (String name) {
	if (name == null)
	    throw new NullPointerException("illegal null input");

	this.name = name;
    }

    /**
     * Returns the username for this <code>SAEPrincipal</code>.
     *
     * <p>
     *
     * @return the username for this <code>SAEPrincipal</code>
     */
    public String getName() {
	return name;
    }

    /**
     * Returns a string representation of this <code>SAEPrincipal</code>.
     *
     * <p>
     *
     * @return a string representation of this <code>SAEPrincipal</code>.
     */
    public String toString() {
	return("SAEPrincipal:  " + name);
    }

    /**
     * Compares the specified Object with this <code>SAEPrincipal</code>
     * for equality.  Returns true if the given object is also a
     * <code>SAEPrincipal</code> and the two SAEPrincipals
     * have the same username.
     *
     * <p>
     *
     * @param o Object to be compared for equality with this
     *		<code>SAEPrincipal</code>.
     *
     * @return true if the specified Object is equal equal to this
     *		<code>SAEPrincipal</code>.
     */
    public boolean equals(Object o) {
	if (o == null)
	    return false;

        if (this == o)
            return true;
 
        if (!(o instanceof SAEPrincipal))
            return false;
        SAEPrincipal that = (SAEPrincipal)o;

	if (this.getName().equals(that.getName()))
	    return true;
	return false;
    }
 
    /**
     * Returns a hash code for this <code>SAEPrincipal</code>.
     *
     * <p>
     *
     * @return a hash code for this <code>SAEPrincipal</code>.
     */
    public int hashCode() {
	return name.hashCode();
    }
}
