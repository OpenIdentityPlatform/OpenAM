/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
 * Copyright © 2011 Cybernetica AS.
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

package org.forgerock.openam.authentication.modules.oauth2;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.BUNDLE_NAME;
import org.json.JSONException;
import org.json.JSONObject;


public class DefaultAccountMapper implements AccountMapper {

    public DefaultAccountMapper() {
    }

    @Override
    public Map<String, Set<String>> getAccount(Set<String> accountMapConfiguration, 
            String responseObtained) throws AuthLoginException {

        Map<String, Set<String>> attr = new HashMap<String, Set<String>>();
        JSONObject json;
        try {
            json = new JSONObject((String)responseObtained);
        } catch (JSONException ex) {
            OAuthUtil.debugError("OAuth.process(): JSONException: " + ex.getMessage());
                    throw new AuthLoginException(BUNDLE_NAME,
                            ex.getMessage(), null);
        }

        for (String entry : accountMapConfiguration) {
            try {
                if (entry.indexOf("=") == -1) {
                    OAuthUtil.debugMessage("DefaultAccountMapper.getAttributes: " + 
                                "Invalid entry." + entry);
                    continue;
                }
                StringTokenizer st = new StringTokenizer(entry, "=");
                String responseName = st.nextToken();
                String localName = st.nextToken();

                String data;
                if (responseName != null && responseName.indexOf(".") != -1) {
                    StringTokenizer parts = new StringTokenizer(responseName, ".");
                    data = json.getJSONObject(parts.nextToken()).getString(parts.nextToken());
                } else {
                    data = json.getString(responseName);
                }
                attr.put(localName, OAuthUtil.addToSet(new HashSet<String>(), data));        
            } catch (JSONException ex) {
                OAuthUtil.debugError("DefaultAccountMapper.getAttributes: Error when "
                        + "trying to get attributes from the response ", ex);
                throw new AuthLoginException("Configuration problem, attribute ",ex);
            }

        }
        OAuthUtil.debugMessage("DefaultAccountMapper.getAttributes: " + 
                                "Attribute Map obtained=" + attr);
        
        return attr;
    }

    @Override
    public AMIdentity searchUser(AMIdentityRepository idrepo, Map<String, Set<String>> attr) {
        AMIdentity identity = null;
        IdSearchControl ctrl = null;
        
        if (attr == null || attr.isEmpty()) {
            OAuthUtil.debugWarning("DefaultAccountMapper.searchUser: empty search");
            return null;
        }
        
        ctrl = getSearchControl(IdSearchOpModifier.OR, attr);
        IdSearchResults results;       
        try {
            results = idrepo.searchIdentities(IdType.USER, "*", ctrl);
            Iterator<AMIdentity> iter = results.getSearchResults().iterator();
            if (iter.hasNext()) {
                identity = iter.next();
                OAuthUtil.debugMessage("getUser: user found : " + identity.getName());
            }
        } catch (IdRepoException ex) {
            OAuthUtil.debugError("DefaultAccountMapper.searchUser: Problem while  "
                    + "searching  for the user. IdRepo", ex);
        } catch (SSOException ex) {
            OAuthUtil.debugError("DefaultAccountMapper.searchUser: Problem while  "
                    + "searching  for the user. SSOExc", ex);
        }

        return identity;
    }

    @Override
    public AMIdentity provisionUser(AMIdentityRepository idrepo, Map<String, Set<String>> attributes) 
      throws AuthLoginException {

        AMIdentity identity = null;
            try {
                String userId = UUID.randomUUID().toString();
                identity = idrepo.createIdentity(IdType.USER, userId, attributes);

            } catch (IdRepoException ire) {
                OAuthUtil.debugError("DefaultAccountMapper.getAccount: IRE ", ire);
                OAuthUtil.debugError("LDAPERROR Code = " + ire.getLDAPErrorCode());
                if (ire.getLDAPErrorCode() != null && !ire.getLDAPErrorCode().equalsIgnoreCase("68")) {
                    throw new AuthLoginException("Failed to create user");
                }
            } catch (SSOException ex) {
                OAuthUtil.debugError("DefaultAccountMapper.getAttributes: Problem while  "
                    + "creating the user. SSOExc", ex);
                throw new AuthLoginException("Failed to create user");
            }
        
        return identity;
  }


    private IdSearchControl getSearchControl(
            IdSearchOpModifier modifier, Map<String, Set<String>> avMap) {
        
    	IdSearchControl control = new IdSearchControl();
    	control.setMaxResults(1);
        control.setSearchModifiers(modifier, avMap);
    	return control;
    }

}
