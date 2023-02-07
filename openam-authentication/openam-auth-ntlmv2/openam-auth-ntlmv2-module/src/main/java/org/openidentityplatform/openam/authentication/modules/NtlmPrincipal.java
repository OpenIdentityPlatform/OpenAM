/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2023 Open Identity Platform Community.
 */

package org.openidentityplatform.openam.authentication.modules;

import java.security.Principal;

public class NtlmPrincipal implements Principal, 
    java.io.Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = -65682668721667416L;
	final private String name;

    public NtlmPrincipal(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }


    public String toString() {
        return(this.getClass().getName().concat(": ").concat(name));
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        
        if (this == obj) {
            return true;
        }

        if (! (obj instanceof NtlmPrincipal)) {
            return false;
        }

        NtlmPrincipal wtp = (NtlmPrincipal)obj;
        if (name.equals(wtp.getName())) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return name.hashCode();
    }
}
