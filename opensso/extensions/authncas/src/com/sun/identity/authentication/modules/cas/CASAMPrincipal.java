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
 * $Id: CASAMPrincipal.java,v 1.1 2009/07/16 20:17:25 superpat7 Exp $
 *
 * Portions Copyrighted 2009 Qingfeng Zhang
 *
 */

package com.sun.identity.authentication.modules.cas;

import java.security.Principal;

public class CASAMPrincipal implements Principal, java.io.Serializable {

	/**
	 * @serial
	 */
	private String name;

	public CASAMPrincipal(String name) {
		if (name == null)
			throw new NullPointerException("illegal null input");

		this.name = name;
	}

	/**
	 * Return the LDAP username for this <code>SamplePrincipal</code>.
	 * 
	 * <p>
	 * 
	 * @return the LDAP username for this <code>SamplePrincipal</code>
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return a string representation of this <code>SamplePrincipal</code>.
	 * 
	 * <p>
	 * 
	 * @return a string representation of this <code>SamplePrincipal</code>.
	 */
	public String toString() {
		return ("CASAMPrincipal:  " + name);
	}

	/**
	 * Compares the specified Object with this <code>SamplePrincipal</code>
	 * for equality. Returns true if the given object is also a
	 * <code>SamplePrincipal</code> and the two SamplePrincipals have the same
	 * username.
	 * 
	 * <p>
	 * 
	 * @param o
	 *            Object to be compared for equality with this
	 *            <code>SamplePrincipal</code>.
	 * 
	 * @return true if the specified Object is equal equal to this
	 *         <code>SamplePrincipal</code>.
	 */
	public boolean equals(Object o) {
		if (o == null)
			return false;

		if (this == o)
			return true;

		if (!(o instanceof CASAMPrincipal))
			return false;
		CASAMPrincipal that = (CASAMPrincipal) o;

		if (this.getName().equals(that.getName()))
			return true;
		return false;
	}

	/**
	 * Return a hash code for this <code>SamplePrincipal</code>.
	 * 
	 * <p>
	 * 
	 * @return a hash code for this <code>SamplePrincipal</code>.
	 */
	public int hashCode() {
		return name.hashCode();
	}
}
