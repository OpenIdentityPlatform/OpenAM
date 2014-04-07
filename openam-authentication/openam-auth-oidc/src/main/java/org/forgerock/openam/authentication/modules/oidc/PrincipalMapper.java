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

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentityRepository;
import org.forgerock.json.jose.jwt.JwtClaimsSet;

import java.util.Map;
import java.util.Set;

/**
 * Interface defining the ability to map OpenID Connect ID Token jwt state to a principal account in an OpenAM id repo.
 */
public interface PrincipalMapper {
    /**
     *
     * @param localToJwtAttributeMapping A mapping of the local to jwt attributes. Defines how
     *                                   jwt attributes are to be mapped to id repo attributes in order to conduct a user
     *                                   search. Only jwt state which contains a value in this set will
     *                                   be used to search the id repo. There are no duplicate entries.
     * @param jwtClaimsSet The set of claims encapsulated in the jwt. The JwtClaimsSet class excludes duplicate entries.
     * @return The Map used to drive an id repo search. The keys are the id repo attributes pulled from
     * localToJwtAttributeMapping, and the values are the state corresponding to the jwt_attribute in the
     * localToJwtAttributeMapping corresponding to to the key, as pulled from the jwtClaimsSet.
     * The Map may be empty if none of the jwt_attributes defined in the localToJwtAttributeMapping could be found
     * in the jwtClaimsSet.
     */
    Map<String, Set<String>> getAttributesForPrincipalLookup(Map<String, String> localToJwtAttributeMapping,
                                                             JwtClaimsSet jwtClaimsSet);

    /**
     * @param idrepo The realm-specific identity repository
     * @param attr   The search attributes, as returned from getAttributesForPrincipalLookup.
     * @return  The non-null String corresponding the the name of the AMIdentity instance found in the id-repo following
     * a successful search.
     * @throws AuthLoginException if the search did not yield any results.
     */
    String lookupPrincipal(AMIdentityRepository idrepo, Map<String, Set<String>> attr) throws AuthLoginException;
}
