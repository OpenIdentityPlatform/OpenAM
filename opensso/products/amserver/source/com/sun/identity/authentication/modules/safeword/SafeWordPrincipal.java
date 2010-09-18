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
 * $Id: SafeWordPrincipal.java,v 1.2 2008/09/04 23:37:04 bigfatrat Exp $
 *
 */


package com.sun.identity.authentication.modules.safeword;

import java.security.Principal;


/**
 * <p> This class implements the <code>Principal</code> interface
 * and represents an SafeWord user.
 *
 * <p> Principals such as this <code>SafeWordPrincipal</code>
 * may be associated with a particular <code>Subject</code>
 * to augment that <code>Subject</code> with an additional
 * identity.  Refer to the <code>Subject</code> class for more information
 * on how to achieve this.  Authorization decisions can then be based upon 
 * the Principals associated with a <code>Subject</code>.
 */
public class SafeWordPrincipal implements Principal, java.io.Serializable {

    /**
     * @serial
     */
    private String name;

    /**
     * Create a SafeWordPrincipal with a SafeWord username.
     *
     * <p>
     *
     * @param name the SafeWord username for this user.
     *
     * @exception NullPointerException if the <code>name</code>
     *                                 is <code>null</code>.
     */
    public SafeWordPrincipal(String name) {
        if (name == null) {
	    throw new NullPointerException("illegal null input");
        }

	this.name = name;
    }

    /**
     * Return the SafeWord username for this 
     * <code>SafeWordPrincipal</code>.
     *
     * <p>
     *
     * @return the SafeWord username for this 
     *         <code>SafeWordPrincipal</code>
     */
    public String getName() {
	return name;
    }

    /**
     * Return a string representation of this 
     * <code>SafeWordPrincipal</code>.
     *
     * <p>
     *
     * @return a string representation of this
     *         <code>SafeWordPrincipal</code>.
     */
    public String toString() {
	return("SafeWordPrincipal: " + name);
    }

    /**
     * Compares the specified Object with this <code>SafeWordPrincipal
     * </code> for equality.  Returns true if the given object is also a
     * <code>SafeWordPrincipal</code> and the two SafeWordPrincipals
     * have the same username.
     *
     * <p>
     *
     * @param o Object to be compared for equality with this
     *          <code>SafeWordPrincipal</code>.
     *
     * @return true if the specified Object is equal equal to this
     *         <code>SafeWordPrincipal</code>.
     */
    public boolean equals(Object o) {
        if (o == null) {
	    return false;
        }

        if (this == o) {
            return true;
        }
 
        if (!(o instanceof SafeWordPrincipal)) {
            return false;
        }
        
        SafeWordPrincipal that = (SafeWordPrincipal)o;

        if (this.getName().equals(that.getName())) {
	    return true;
        }
        
	return false;
    }
 
    /**
     * Return a hash code for this <code>SafeWordPrincipal</code>.
     *
     * <p>
     *
     * @return a hash code for this <code>SafeWordPrincipal</code>.
     */
    public int hashCode() {
	return name.hashCode();
    }
    
}
