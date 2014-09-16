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
import org.forgerock.openam.authentication.modules.common.mapping.AttributeMapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.forgerock.openam.authentication.modules.oidc.OpenIdConnectConfig.BUNDLE_KEY_PRINCIPAL_MAPPING_FAILURE;
import static org.forgerock.openam.authentication.modules.oidc.OpenIdConnectConfig.RESOURCE_BUNDLE_NAME;

/**
 * An {@code AttributeMapper} that gets its values from a JWT.
 * @see org.forgerock.openam.authentication.modules.common.mapping.AttributeMapper
 */
public class JwtAttributeMapper implements AttributeMapper<JwtClaimsSet> {
    private static Debug logger = Debug.getInstance("amAuth");
    /*
    This value should always be set to one, as we are returning the first result in the Set encapsulated in the
    IdSearchResult class. See lookupPrincipal below.
     */
    private static final int SINGLE_SEARCH_RESULT = 1;

    /**
     * {@inheritDoc}
     */
    public void init(String bundleName) {
        // not needed
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Set<String>> getAttributes(Map<String, String> localToJwtAttributeMapping,
                                                                    JwtClaimsSet jwtClaimsSet) {
        Map<String, Set<String>> lookupAttributes = new HashMap<String, Set<String>>();
        /*
        Go through the localToJwtAttributeMapping entries and see if the jwt attribute is present in the jwt. If
        present, create an entry in the lookupAttributes, and insert an entry corresponding to the value in the jwt.
        As far as excluding duplicates is concerned:
        1. the JwtClaimsSet excludes duplicate entries with the same key
        2. The localToJwtAttributeMapping is ultimately derived from user-input state, but the OpenIdConnectConfig ctor
        excludes duplicate mapping entries (preserving the first). See MappingUtils.parseMappings. So
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
                    logger.error("In JwtAttributeMapper.getAttributes, the " +
                            "localToJwtAttributeMappings appears to have duplicate entries: " + localToJwtAttributeMapping +
                            "; Or possibly the JwtClaimsSet has duplicate entries: " + jwtClaimsSet +
                            ". Will preserve the following existing mappings: " + lookupAttributes);
                }
            }
        }
        return lookupAttributes;
    }

}
