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
 * $Id: PrincipalTokenRestriction.java,v 1.4 2008/06/25 05:43:59 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.session.util;

import com.iplanet.am.util.Misc;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.sso.SSOToken;

/**
 * This Class represents a PrincipalTokenRestriction
 * implementing the TokenRestriction interface provides methods to check and 
 * compare SSOToken restrictions for this DN
 */
public class PrincipalTokenRestriction implements TokenRestriction {
   
    /* The dn to compare to*/
     private String dn = null;

    /** 
     * Creates <code>PrincipalTokenRestriction</code> object for the specified
     *          <code>dn</code>
     * @param   dn the name of the DN.
     */
    public PrincipalTokenRestriction(String dn) {
        this.dn = Misc.canonicalize(dn);
    }

    /**
     * Returns a hash code for this object.
     * 
     * @return a hash code value for this object.
     */
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Compares this DN to the specified object. The result is true if and only
     * if the argument is not null, and <code>other</code> is the same as this
     * restriction.
     * 
     * @param other -
     *            the object to compare this restriction against.
     * @return true if the restriction object compared as string are equal;
     *         false otherwise.
     */
    public boolean equals(Object other) {
        return other != null && (other instanceof PrincipalTokenRestriction)
                && this.dn.equals(((PrincipalTokenRestriction) other).dn);
    }

    /**
     * Returns a true if the restriction matches the context for which it was
     * set, otherwise it returns false.
     * 
     * @param context
     *            The context from which the restriction needs to be checked The
     *            context can be: - the SSOToken of the Application against
     *            which the restriction is being compared
     * @return boolean True if the restriction is satisfied, false otherwise
     * @throws Exception if the there was an error.
     */
    public boolean isSatisfied(Object context) throws Exception {
        if (context instanceof SSOToken) {
            SSOToken usedBy = (SSOToken) context;
            return dn.equals(Misc.canonicalize(
                usedBy.getPrincipal().getName()));
        }
        return false;
    }
}
