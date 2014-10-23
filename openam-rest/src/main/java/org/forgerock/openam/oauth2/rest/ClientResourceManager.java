/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012-2014 ForgeRock AS.
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
 * "Portions Copyrighted [year] [name of company]"
 */

package org.forgerock.openam.oauth2.rest;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.forgerockrest.RestUtils;

import java.security.AccessController;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClientResourceManager {

    private SSOToken getAdminToken(){
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    public void createIdentity(String realm, String id, Map<String, Set<String>> attrs)
            throws IdRepoException, SSOException{
        AMIdentityRepository repo = new AMIdentityRepository(getAdminToken() , realm);
        repo.createIdentity(IdType.AGENTONLY, id, attrs);
    }

    private AMIdentity getIdentity(String uName, String realm) throws InternalServerErrorException {
        AMIdentity theID = null;
        AMIdentityRepository amIdRepo = null;
        try{
            amIdRepo = new AMIdentityRepository(getAdminToken() , realm);
        } catch (IdRepoException e){
            throw new InternalServerErrorException("Unable to get idrepo", e);
        } catch (SSOException e){
            throw new InternalServerErrorException("Unable to get idrepo", e);
        }
        IdSearchControl idsc = new IdSearchControl();
        idsc.setRecursive(true);
        idsc.setAllReturnAttributes(true);
        // search for the identity
        Set<AMIdentity> results = Collections.EMPTY_SET;
        try {
            idsc.setMaxResults(0);
            IdSearchResults searchResults =
                    amIdRepo.searchIdentities(IdType.AGENTONLY, uName, idsc);
            if (searchResults != null) {
                results = searchResults.getSearchResults();
            }

            if (results == null || results.size() != 1) {
                throw new InternalServerErrorException("Too many results or not enough");
            }

            theID = results.iterator().next();
        } catch (IdRepoException e) {
            throw new InternalServerErrorException("Unable to get search results", e);
        } catch (SSOException e) {
            throw new InternalServerErrorException("Unable to get search results", e);
        }
        return theID;
    }

    /**
     * Returns TokenID from headers
     *
     * @param context ServerContext which contains the headers.
     * @return String with TokenID
     */
    private String getCookieFromServerContext(ServerContext context) {
        return RestUtils.getCookieFromServerContext(context);
    }

    public void deleteIdentity(String id, String realm) throws SSOException, IdRepoException, InternalServerErrorException{
        AMIdentityRepository repo = new AMIdentityRepository(getAdminToken(), realm);
        Set<AMIdentity> ids = new HashSet<AMIdentity>();
        ids.add(getIdentity(id, realm));
        repo.deleteIdentities(ids);
    }
}
