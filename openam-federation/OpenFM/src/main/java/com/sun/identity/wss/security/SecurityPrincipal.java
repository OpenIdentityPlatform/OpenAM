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
 * $Id: SecurityPrincipal.java,v 1.6 2009/05/20 03:32:10 mallas Exp $
 *
 */

package com.sun.identity.wss.security;

import java.io.Serializable;
import  java.security.Principal;

/**
 * This class <code>SecurityPrincipal</code> exposes the authenticated
 * principal via the message level security.
 * It implements <code>Principal</code> and <code>Serializable</code>
 * interfaces.
 * 
 * @supported.all.api
 */
public class SecurityPrincipal implements Principal, Serializable {

    private String name;

    /**
     * Constructs SecurityPrincipal object.
     * @param name the name of the principal
     */
    public SecurityPrincipal(String name) {
        this.name = name;
    }

    /**
     * Compares with given object.
     * @param o the given object
     * @return false if the given object is not equal to this principal.
     */
    public boolean equals(Object o) {
        if(this == o) {
           return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        
        if(name == null) {
           return false;
        }
        return name.equals(((SecurityPrincipal)o).getName());        
       
    }

    /**
     * Returns the name of the principal.
     * @return the name of the principal.
     */
    public String getName() {
        return name;
    }

    /**
     * Converts to string.
     * @return String the principal name string.
     */
    public String toString() {
       return name;
    }
}
