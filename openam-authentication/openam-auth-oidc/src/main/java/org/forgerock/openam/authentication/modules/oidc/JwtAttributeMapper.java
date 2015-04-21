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
* Copyright 2014-2015 ForgeRock AS.
* Portions Copyrighted 2015 Nomura Research Institute, Ltd.
*/

package org.forgerock.openam.authentication.modules.oidc;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.authentication.modules.common.mapping.AttributeMapper;
import org.forgerock.openam.utils.CollectionUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An {@code AttributeMapper} that gets its values from a JWT.
 * @see org.forgerock.openam.authentication.modules.common.mapping.AttributeMapper
 */
public class JwtAttributeMapper implements AttributeMapper<JwtClaimsSet> {
    private static Debug logger = Debug.getInstance("amAuth");

    private List<String> prefixedAttributes = null;
    private String prefix = null;

    /**
     * Default constructor with no prefix
     */
    public JwtAttributeMapper() {}

    /**
     * Constructor that allows a prefix to be added to the mapped values
     * @param prefixedAttributesList Comma-separated list of attributes that need a prefix applied, or <code>*</code>.
     * @param prefix The prefix to be applied.
     */
    public JwtAttributeMapper(String prefixedAttributesList, String prefix) {
        this.prefix = prefix;
        this.prefixedAttributes = Arrays.asList(prefixedAttributesList.split(","));
    }

    /**
     * {@inheritDoc}
     */
    public void init(String bundleName) {
        // not needed
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Set<String>> getAttributes(Map<String, String> jwtToLocalAttributeMapping,
                                                                    JwtClaimsSet jwtClaimsSet) {
        Map<String, Set<String>> lookupAttributes = new HashMap<String, Set<String>>();
        if (jwtToLocalAttributeMapping == null || jwtClaimsSet == null) {
            return lookupAttributes;
        }
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
        for (Map.Entry<String, String> entry : jwtToLocalAttributeMapping.entrySet()) {
            String jwtName = entry.getKey();
            if (jwtClaimsSet.isDefined(jwtName)) {
                String localName = entry.getValue();
                if (!lookupAttributes.containsKey(localName)) {
                    Set<String> value = new HashSet<String>();
                    //obtain the claim as an Object, and call toString on it, as a Set<String> needs to be populated.
                    String data = jwtClaimsSet.getClaim(jwtName).toString();
                    if (prefix != null && (prefixedAttributes.contains(localName) || prefixedAttributes.contains("*"))) {
                        data = prefix + data;
                    }
                    lookupAttributes.put(localName, CollectionUtils.asSet(data));
                } else {
                    logger.error("In JwtAttributeMapper.getAttributes, the " +
                            "jwtToLocalAttributeMappings appears to have duplicate entries: " + jwtToLocalAttributeMapping +
                            "; Or possibly the JwtClaimsSet has duplicate entries: " + jwtClaimsSet +
                            ". Will preserve the following existing mappings: " + lookupAttributes);
                }
            }
        }
        return lookupAttributes;
    }

}
