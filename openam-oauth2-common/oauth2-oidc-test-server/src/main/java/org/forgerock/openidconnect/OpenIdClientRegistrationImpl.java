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

package org.forgerock.openidconnect;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @since 12.0.0
 */
public class OpenIdClientRegistrationImpl implements OpenIdConnectClientRegistration {

    private final Client client;

    public OpenIdClientRegistrationImpl(final Client client) {
        this.client = client;
    }

    public String getIDTokenSignedResponseAlgorithm() {
        return client.getIdTokenSignedResponseAlgorithm();
    }

    public Set<URI> getRedirectUris() {
        return client.getRedirectionURIs();
    }

    public Set<String> getAllowedResponseTypes() {
        return client.getResponseTypes();
    }

    public String getClientId() {
        return client.getClientID();
    }

    public String getAccessTokenType() {
        return "Bearer";
    }

    public String getDisplayName(String locale) {
        String defaultName = null;
        final String DELIMITER = "|";
        for (String name : client.getDisplayName()) {
            if (name.contains(DELIMITER)) {
                int locationOfDelimiter = name.indexOf(DELIMITER);
                if (name.substring(0, locationOfDelimiter).equalsIgnoreCase(locale)) {
                    return name.substring(locationOfDelimiter+1, name.length());
                }
            } else {
                defaultName = name;
            }
        }

        return defaultName;
    }

    public String getDisplayDescription(String locale) {
        String defaultName = null;
        final String DELIMITER = "|";
        for (String name : client.getDisplayDescription()) {
            if (name.contains(DELIMITER)) {
                int locationOfDelimiter = name.indexOf(DELIMITER);
                if (name.substring(0, locationOfDelimiter).equalsIgnoreCase(locale)) {
                    return name.substring(locationOfDelimiter+1, name.length());
                }
            } else {
                defaultName = name;
            }
        }

        return defaultName;
    }

    public Map<String, String> getAllowedScopeDescriptions(String locale) {
        final String DELIMITER = "\\|";
        final Map<String, String> scopeDescriptions = new LinkedHashMap<String, String>();
        for (final String scopeDescription : client.getAllowedGrantScopes()) {
            final String[] parts = scopeDescription.split(DELIMITER);
            if (parts != null) {
                //no description or locale
                if (parts.length == 1){
                    continue;
                } else if (parts.length == 2){
                    //no locale add description
                    scopeDescriptions.put(parts[0], parts[1]);
                } else if (parts.length == 3){
                    //locale and description
                    if (parts[1].equalsIgnoreCase(locale)){
                        scopeDescriptions.put(parts[0], parts[2]);
                    } else {
                        //not the right locale
                        continue;
                    }
                } else {
                    continue;
                }
            }
        }
        return scopeDescriptions;
    }

    public Set<String> getDefaultScopes() {
        return client.getDefaultGrantScopes();
    }

    public Set<String> getAllowedScopes() {
        return client.getAllowedGrantScopes();
    }

    public boolean isConfidential() {
        return client.getClientType().getType().equals("Confidential");
    }

    public String getClientSessionURI() {
        return client.getClientSessionURI();
    }
}
