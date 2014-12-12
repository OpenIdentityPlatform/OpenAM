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

package org.forgerock.openam.rest.resource;

import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.util.Pair;

/**
 * A CREST Context for holding realm information from the Request.
 *
 * @since 12.0.0
 */
public class RealmContext extends ServerContext {

    private Pair<String, String> dnsAliasRealm;
    private Pair<String, String> relativeRealmPath = Pair.of("/", "/");

    /**
     * Constructs a new empty RealmContext instance.
     *
     * @param parent The parent context.
     */
    public RealmContext(Context parent) {
        super(parent);
    }

    /**
     * Gets the full resolved realm path.
     *
     * @return The resolved realm path.
     */
    public String getResolvedRealm() {//TODO rename to getAbsoluteRealm or getAbsoluteRealmPath?
        StringBuilder resolvedRealm = new StringBuilder();
        if (dnsAliasRealm != null) {
            final String dnsAlias = dnsAliasRealm.getSecond();
            if(!dnsAlias.equals("/")) {
                resolvedRealm.append(dnsAlias);
            }
        }
        resolvedRealm.append(relativeRealmPath.getSecond());
        String realmPath = resolvedRealm.toString();
        if (!"/".equals(realmPath) && realmPath.endsWith("/")) {
            realmPath = realmPath.substring(0, realmPath.length() - 1);
        }
        return realmPath;
    }

    public void addDnsAlias(String dnsAlias, String realmPath) {
        dnsAliasRealm = Pair.of(dnsAlias, realmPath);
    }

    public void addSubRealm(String realm, String realmPath) {
        if ("/".equals(relativeRealmPath.getSecond())) { //Could be a realm alias or a realm path
            if (!realmPath.startsWith("/")) {
                realmPath = "/" + realmPath;
            }
            if (realmPath.endsWith("/")) {
                realmPath = realmPath.substring(0, realmPath.length() - 1);
            }
            relativeRealmPath = Pair.of(realm, realmPath);
        } else { //Is not a realm alias, it must be a realm path
            String a = getRealm(relativeRealmPath.getSecond(), realm);
            relativeRealmPath = Pair.of(a, a);
        }
    }

    private String getRealm(String realm, String subrealm) {
        if (subrealm == null || subrealm.isEmpty()) {
            return realm;
        }
        if (subrealm.startsWith("/")) {
            subrealm = subrealm.substring(1);
        }
        if (subrealm.endsWith("/")) {
            subrealm = subrealm.substring(0, subrealm.length() - 1);
        }

        return realm.equals("/") ? realm + subrealm : realm + "/" + subrealm;
    }

    public String getBaseRealm() {
        if (dnsAliasRealm.getSecond().equals("/")) {
            return relativeRealmPath.getSecond();
        }
        return dnsAliasRealm.getSecond();
    }

    public String getRelativeRealm() {
        return relativeRealmPath.getSecond();
    }
}
