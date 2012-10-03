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
 * $Id: SSOPrincipal.java,v 1.2 2008/06/25 05:41:42 qcheng Exp $
 *
 */

package com.iplanet.sso.providers.dpro;

/**
 * This class <code>SSOPrincipal</code>represents 
 * the entities such as a user or a 
 * service. It implements java.security.Principal interface.
 */

class SSOPrincipal implements java.security.Principal {
    
    /**Pricipal entity name */
     private String principal;

    /**
     * Creates a SSOPrincipal object
     * 
     * @param String The name of the principal
     */

    SSOPrincipal(String name) {
        principal = name;
    }

    /**
     * Compares this principal to the specified object.  Returns true
     * if the object passed in matches the principal.
     * @param Object The object to be compared
     * @return <code>true</code>if the principal match.
     *         <code>false</code>
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        return principal.equals(object.toString());
    }

    /**
     * Returns the principal name of this object
     * 
     * @return String The name of the principal
     */
    public String getName() {
        return principal;
    }

    /**
     * Returns the string representation of this object
     * 
     * @return String The string representation of this object
     */
    public String toString() {
        return principal;
    }
}
