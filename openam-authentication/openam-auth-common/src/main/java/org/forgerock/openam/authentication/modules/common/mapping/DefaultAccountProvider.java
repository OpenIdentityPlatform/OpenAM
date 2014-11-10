/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2014 ForgeRock AS.
 * Copyright 2011 Cybernetica AS.
 * 
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.authentication.modules.common.mapping;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.debug.Debug;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A default {@code AccountProvider} implementation.
 */
public class DefaultAccountProvider implements AccountProvider {

    private static Debug debug = Debug.getInstance("amAuth");

    private String idNameAttribute = "uid";

    /**
     * Default constructor uses default attribute for id name (uid) when creating users.
     */
    public DefaultAccountProvider() {}

    /**
     * Custom attribute name to be used for the id name.
     * @param idNameAttribute The attribute name that should be used to extract the user ID from the map of attributes
     *                        when creating users.
     */
    public DefaultAccountProvider(String idNameAttribute) {
        this.idNameAttribute = idNameAttribute;
    }

    /**
     * {@inheritDoc}
     */
    public AMIdentity searchUser(AMIdentityRepository idrepo, Map<String, Set<String>> attr) {
        AMIdentity identity = null;

        if (attr == null || attr.isEmpty()) {
            debug.warning("DefaultAccountMapper.searchUser: empty search");
            return null;
        }

        IdSearchControl ctrl = getSearchControl(IdSearchOpModifier.OR, attr);
        IdSearchResults results;
        try {
            results = idrepo.searchIdentities(IdType.USER, "*", ctrl);
            Iterator<AMIdentity> iter = results.getSearchResults().iterator();
            if (iter.hasNext()) {
                identity = iter.next();
                if (debug.messageEnabled()) {
                    debug.message("getUser: user found : " + identity.getName());
                }
            }
        } catch (IdRepoException ex) {
            debug.error("DefaultAccountMapper.searchUser: Problem while searching for the user. IdRepo", ex);
        } catch (SSOException ex) {
            debug.error("DefaultAccountMapper.searchUser: Problem while searching for the user. SSOExc", ex);
        }

        return identity;
    }

    /**
     * {@inheritDoc}
     */
    public AMIdentity provisionUser(AMIdentityRepository idrepo, Map<String, Set<String>> attributes) 
      throws AuthLoginException {

        AMIdentity identity = null;
            try {
                String userId;
                Set<String> idAttribute = attributes.get(idNameAttribute);
                if (idAttribute != null && !idAttribute.isEmpty()) {
                    userId = idAttribute.iterator().next();
                } else {
                    userId = UUID.randomUUID().toString();
                }
                identity = idrepo.createIdentity(IdType.USER, userId, attributes);
            } catch (IdRepoException ire) {
                debug.error("DefaultAccountMapper.getAccount: IRE ", ire);
                debug.error("LDAPERROR Code = " + ire.getLDAPErrorCode());
                if (ire.getLDAPErrorCode() != null && !ire.getLDAPErrorCode().equalsIgnoreCase("68")) {
                    throw new AuthLoginException("Failed to create user");
                }
            } catch (SSOException ex) {
                debug.error("DefaultAccountMapper.getAttributes: Problem while creating the user. SSOExc", ex);
                throw new AuthLoginException("Failed to create user");
            }
        
        return identity;
  }


    private IdSearchControl getSearchControl(IdSearchOpModifier modifier, Map<String, Set<String>> avMap) {
    	IdSearchControl control = new IdSearchControl();
    	control.setMaxResults(1);
        control.setSearchModifiers(modifier, avMap);
    	return control;
    }

}
