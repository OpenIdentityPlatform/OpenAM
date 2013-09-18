/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock Inc. All rights reserved.
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

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.*;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.forgerockrest.RestUtils;

import java.security.AccessController;
import java.util.*;

public class ClientResourceManager {

    public boolean usersEqual(ServerContext context) throws SSOException, IdRepoException{
        return getUid(context).equals(getAdminIdentity());
    }

    private AMIdentity getAdminIdentity(){
        String adminUser = SystemProperties.get(Constants.AUTHENTICATION_SUPER_USER);
        AMIdentity adminUserId = null;
        if (adminUser != null) {
            return new AMIdentity(getAdminToken(),
                    adminUser, IdType.USER, "/", null);
        }
        return null;
    }

    private SSOToken getAdminToken(){
        return (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
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

    private AMIdentity getUid(ServerContext context) throws SSOException, IdRepoException{
        String cookie = getCookieFromServerContext(context);
        SSOTokenManager mgr = SSOTokenManager.getInstance();
        SSOToken token = mgr.createSSOToken(cookie);
        return IdUtils.getIdentity(token);

    }

    public void deleteIdentity(String id) throws SSOException, IdRepoException, InternalServerErrorException{
        AMIdentityRepository repo = new AMIdentityRepository(getAdminToken() , null);
        Set<AMIdentity> ids = new HashSet<AMIdentity>();
        ids.add(getIdentity(id, null));
        repo.deleteIdentities(ids);
    }
}
