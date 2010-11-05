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
 *
 * "Portions Copyrighted 2009 Warren Strange"
 *
 * $Id: OpenSSOSimpleAuthoritiesPopulator.java,v 1.1 2009/02/26 18:18:52 wstrange Exp $
 *
 */
package com.sun.identity.provider.springsecurity;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import java.util.Iterator;
import java.util.Set;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;

/**
 * Strategy to convert OpenSSO group memberships for a principal
 * into an array of Spring GrantedAuthorities[].
 *
 * This implemenentation is very simple:
 * It converts the OpenSSO group name to upper case, and then
 * prepends the prefix "ROLE_". For example, an OpenSSO group
 * membership of staff becomes "ROLE_STAFF"
 *
 * In the future we might want to extract an interface from this class
 * and create other strategy mechanisims. For example - using
 * a map to map OpenSSO groups to Spring roles.
 *
 * Note that this implementation always adds ROLE_AUTHENTICATED
 * to the granted authorities. if the SSOToken is valid, the user
 * has authenticated.
 *
 * @author warrenstrange
 */
public class OpenSSOSimpleAuthoritiesPopulator {

    /**
     * Lookup the users group memberships and return as an array of GrantedAuthority
     * @param ssoToken users SSOTOken
     * @return Arrayo of GrantedAuthority representing the userss OpenSSO groups.
     * 
     * @throws com.sun.identity.idm.IdRepoException
     * @throws com.iplanet.sso.SSOException
     */
    public GrantedAuthority[] getGrantedAuthorities(SSOToken ssoToken) throws IdRepoException, SSOException {
        GrantedAuthority ga[] = null;
        AMIdentity id = IdUtils.getIdentity(ssoToken);
      
        Set groups = id.getMemberships(IdType.GROUP);

        if (groups != null && groups.size() > 0) {
            //leave one extra spot for ROLE_AUTHENTICATED
            ga = new GrantedAuthority[groups.size() + 1];
            int i = 0;
            for (Iterator itr = groups.iterator(); itr.hasNext();) {
                AMIdentity group = (AMIdentity) itr.next();
                String role = "ROLE_" + group.getName().toUpperCase();
                ga[i++] = new GrantedAuthorityImpl(role);
            }
        }
        else
            ga = new GrantedAuthority[1];

        // if we are at this point the user must have authenticated to OpenSSO
        ga[ga.length -1] = new  GrantedAuthorityImpl("ROLE_AUTHENTICATED");
      

        return ga;
    }
}
