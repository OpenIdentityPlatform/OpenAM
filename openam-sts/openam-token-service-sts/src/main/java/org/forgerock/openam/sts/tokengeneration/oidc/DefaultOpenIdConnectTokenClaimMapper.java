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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.tokengeneration.oidc;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.sm.DNMapper;
import com.google.common.base.Joiner;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.utils.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @see org.forgerock.openam.sts.tokengeneration.oidc.OpenIdConnectTokenClaimMapper
 * This is the default implementation of the OpenIdConnectTokenClaimMapper, which will be used if the user has not
 * specified a custom implementation in the OpenIdConnnectTokenConfig associated with the published sts instance.
 */
@SuppressWarnings("unchecked")
public class DefaultOpenIdConnectTokenClaimMapper implements OpenIdConnectTokenClaimMapper {
    @Override
    public Map<String, String> getCustomClaims(SSOToken token, Map<String, String> claimMap) throws TokenCreationException {
        try {
            final AMIdentity amIdentity = IdUtils.getIdentity(token);
            final HashSet<String> attributeNames = new HashSet<>(claimMap.size());
            attributeNames.addAll(claimMap.values());
            Map<String, String> joinedMappings =  joinMultiValues(amIdentity.getAttributes(attributeNames));
            /*
             At this point, the key entries joinedMappings will be the attribute name, and the value will be the
             corresponding value pulled from the user data store. Because I need to return a Map where the keys are the
             claim names, as in the claimMap parameter, I need to create a new map, whose keys correspond to the
             keys in the claimMap parameter, and whose value correspond to the joinedMappings value.
             */
            Map<String, String> adjustedMap = new HashMap<>(joinedMappings.size());
            adjustedMap.put("ip", token.getProperty("Host", true));
            adjustedMap.put("realm", DNMapper.orgNameToRealmName(amIdentity.getRealm()));
            for (Map.Entry<String, String> claimMapEntry : claimMap.entrySet()) {
                if (!StringUtils.isEmpty(joinedMappings.get(claimMapEntry.getValue()))) {
                    adjustedMap.put(claimMapEntry.getKey(), joinedMappings.get(claimMapEntry.getValue()));
                }
            }
            return adjustedMap;
        } catch (IdRepoException | SSOException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception encountered in claim attribute lookup: " + e, e);
        }
    }

    private Map<String, String> joinMultiValues(Map<String, Set<String>> customClaims) {
        HashMap<String, String> claimMap = new HashMap<>(customClaims.size());
        Joiner joiner = Joiner.on(" ").skipNulls();
        for (Map.Entry<String, Set<String>> entry : customClaims.entrySet()) {
            claimMap.put(entry.getKey(), joiner.join(entry.getValue()));
        }
        return claimMap;
    }
}
