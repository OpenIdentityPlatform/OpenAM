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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.sts.tokengeneration.oidc;

import com.iplanet.sso.SSOToken;
import org.forgerock.openam.sts.TokenCreationException;

import java.util.Map;

/**
 * An instance of this interface will be used to insert any custom claims into issued OpenIdConnect tokens.
 * STS instances will be published with state which will allow users to specify their own implementation of this
 * interface, and if so, an instance of the user-specified class will be consulted to perform the attribute mapping.
 *
 * @supported.all.api
 */
public interface OpenIdConnectTokenClaimMapper {
    /**
     *
     * @param token The SSOToken corresponding to the subject of the to-be-issued OpenIdConnect token
     * @param claimMap the claim mapping, as defined by the OpenIdConnectTokenConfig state associated with the published
     *                 sts instance. The map keys will be the claim names, and the LDAP datastore lookup of the attributes
     *                 provided by the map values will provide the value of the claim. Multiple attributes will be separated
     *                 by a space. If the LDAP lookup of the subject corresponding to the SSOToken of the attribute specified
     *                 in the map value does not return a result, the claim will not be inserted in the issued token.
     * @return the mapping of custom claim names to claim values to be inserted in the issued token. If one of the custom
     * claims conflicts with a standard claim name already in the jwt, then an warning will be logged but the custom claim
     * will be inserted. All entries should be non-null.
     * @throws TokenCreationException if the attribute lookup fails
     */
    Map<String, String> getCustomClaims(SSOToken token, Map<String, String> claimMap) throws TokenCreationException;
}
