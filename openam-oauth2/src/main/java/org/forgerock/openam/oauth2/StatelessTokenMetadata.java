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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.oauth2;

import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.*;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.ID;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.TOKEN_NAME;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.GRANT_TYPE;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Stateful representation of the stateless OAuth2 tokens.
 * It contains minimum information required for revoking
 * stateless OAuth2 tokens and showing them on the user's dashboard.
 */
public class StatelessTokenMetadata {

    private final String id;
    private String resourceOwnerId;
    private long expiryTime;
    private String grantId;
    private String clientId;
    private Set<String> scope;
    private String realm;
    private String name;
    private String grantType;

    public StatelessTokenMetadata(String id, String resourceOwnerId, long expiryTime, String grantId,
            String clientId, Set<String> scope, String realm, String name, String grantType) {
        this.id = id;
        this.resourceOwnerId = resourceOwnerId;
        this.expiryTime = expiryTime;
        this.grantId = grantId;
        this.clientId = clientId;
        this.scope = scope;
        this.realm = realm;
        this.name = name;
        this.grantType = grantType;
    }

    public String getId() {
        return id;
    }

    public String getResourceOwnerId() {
        return resourceOwnerId;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public String getGrantId() {
        return grantId;
    }

    public String getClientId() {
        return clientId;
    }

    public Set<String> getScope() {
        return scope;
    }

    public String getRealm() {
        return realm;
    }

    public String getName() { return name; }

    public String getGrantType() { return grantType; }

    public Map<String, Object> asMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(USERNAME, getResourceOwnerId());
        map.put(CLIENT_ID, getClientId());
        map.put(GRANT_TYPE, getGrantType());
        map.put(REALM, getRealm());
        map.put(EXPIRE_TIME, getExpiryTime());
        map.put(ID, getId());
        map.put(TOKEN_NAME, getName());
        map.put(SCOPE, getScope());
        return map;
    }
}
