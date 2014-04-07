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
* information: "Portions copyright [year] [name of copyright owner]".
*
* Copyright 2014 ForgeRock AS.
*/

package org.forgerock.openam.authentication.modules.oidc;

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
import org.forgerock.json.jose.jwt.JwtClaimsSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.forgerock.openam.authentication.modules.oidc.OpenIdConnectConfig.BUNDLE_KEY_PRINCIPAL_MAPPING_FAILURE;
import static org.forgerock.openam.authentication.modules.oidc.OpenIdConnectConfig.RESOURCE_BUNDLE_NAME;

/**
 * @see org.forgerock.openam.authentication.modules.oidc.PrincipalMapper
 */
public class DefaultPrincipalMapper implements PrincipalMapper {
    private static Debug logger = Debug.getInstance("amAuth");
    /*
    This value should always be set to one, as we are returning the first result in the Set encapsulated in the
    IdSearchResult class. See lookupPrincipal below.
     */
    private static final int SINGLE_SEARCH_RESULT = 1;

    public Map<String, Set<String>> getAttributesForPrincipalLookup(Map<String, String> localToJwtAttributeMapping,
                                                                    JwtClaimsSet jwtClaimsSet) {
        Map<String, Set<String>> lookupAttributes = new HashMap<String, Set<String>>();
        /*
        Go through the localToJwtAttributeMapping entries and see if the jwt attribute is present in the jwt. If
        present, create an entry in the lookupAttributes, and insert an entry corresponding to the value in the jwt.
        As far as excluding duplicates is concerned:
        1. the JwtClaimsSet excludes duplicate entries with the same key
        2. The localToJwtAttributeMapping is ultimately derived from user-input state, but the OpenIdConnectConfig ctor
        excludes duplicate mapping entries (preserving the first). See OpenIdConnectConfig.parseLocalToJwkMappings. So
        when populating he lookupAttributes below, I don't need to exclude duplicate entries, as neither the JwtClaimsSet,
        nor the localToJwtAttributeMapping contains duplicate entries.
        Just to be sure, I will log an error if I encounter this situation, in case any of the above invariants are violated.
         */
        for (Map.Entry<String, String> entry : localToJwtAttributeMapping.entrySet()) {
            if (jwtClaimsSet.isDefined(entry.getValue())) {
                if (!lookupAttributes.containsKey(entry.getKey())) {
                    Set<String> value = new HashSet<String>();
                    //obtain the claim as an Object, and call toString on it, as a Set<String> needs to be populated.
                    value.add(jwtClaimsSet.getClaim(entry.getValue()).toString());
                    lookupAttributes.put(entry.getKey(), value);
                } else {
                    logger.error("In DefaultPrincipalMapper.getAttributesForPrincipalLookup, the " +
                            "localToJwtAttributeMappings appears to have duplicate entries: " + localToJwtAttributeMapping +
                            "; Or possibly the JwtClaimsSet has duplicate entries: " + jwtClaimsSet +
                            ". Will preserve the following existing mappings: " + lookupAttributes);
                }
            }
        }
        return lookupAttributes;
    }

    @Override
    public String lookupPrincipal(AMIdentityRepository idrepo, Map<String, Set<String>> searchAttributes) throws AuthLoginException {
        if (searchAttributes == null || searchAttributes.isEmpty()) {
            logger.error("Search attributes empty in lookupPrincipal!");
            return null;
        }
        try {
            final IdSearchResults searchResults = idrepo.searchIdentities(IdType.USER, "*", getSearchControl(searchAttributes));
            if ((searchResults != null) && (IdSearchResults.SUCCESS == searchResults.getErrorCode())) {
                Set<AMIdentity> resultSet = searchResults.getSearchResults();
                if (resultSet.size() == SINGLE_SEARCH_RESULT) {
                    return resultSet.iterator().next().getName();
                } else {
                    logger.warning("In lookupPrincipal, result set did not return a single result: " + resultSet.size());
                }
            } else {
                logger.warning("In lookupPrincipal, IdSearchResults returned non-success status: " + searchResults.getErrorCode());
            }
        } catch (IdRepoException ex) {
            logger.error("DefaultPrincipalMapper.lookupPrincipal: Problem while  "
                    + "searching  for the user: " + ex, ex);
        } catch (SSOException ex) {
            logger.error("DefaultPrincipalMapper.lookupPrincipal: Problem while  "
                    + "searching  for the user: " + ex, ex);
        }
        logger.error("No principal could be mapped in the DefaultPrincipalMapper.");
        throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_PRINCIPAL_MAPPING_FAILURE, null);
    }

    private IdSearchControl getSearchControl(Map<String, Set<String>> searchAttributes) {
        IdSearchControl control = new IdSearchControl();
        control.setMaxResults(SINGLE_SEARCH_RESULT);
        control.setSearchModifiers(IdSearchOpModifier.OR, searchAttributes);
        return control;
    }
}
