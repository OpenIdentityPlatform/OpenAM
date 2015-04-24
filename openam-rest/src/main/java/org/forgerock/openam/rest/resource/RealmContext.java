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
    private String overrideRealm;

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
     * <p>If an override query realm parameter was specified on the request then that is returned,
     * otherwise the realm specified in the URI is rebased on top of the realm that the DNS
     * resolves to.</p>
     *
     * @return The resolved full realm path.
     */
    public String getResolvedRealm() {
        if (overrideRealm != null) {
            return overrideRealm;
        }
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

    /**
     * Adds a mapping from the DNS to the resolved DNS realm alias.
     *
     * @param dnsAlias The DNS.
     * @param realmPath The resolved realm.
     */
    public void addDnsAlias(String dnsAlias, String realmPath) {
        dnsAliasRealm = Pair.of(dnsAlias, realmPath);
    }

    /**
     * Adds a mapping for the URI realm path to the resolved realm, taking in consideration any
     * realm alias'.
     *
     * @param realmUri The URI realm path element.
     * @param realm The resolved realm.
     */
    public void addSubRealm(String realmUri, String realm) {
        if ("/".equals(relativeRealmPath.getSecond())) { //Could be a realm alias or a realm path
            realm = getRealm("/", realm);
            relativeRealmPath = Pair.of(realmUri, realm);
        } else { //Is not a realm alias, it must be a realm path
            String a = getRealm(relativeRealmPath.getSecond(), realm);
            relativeRealmPath = Pair.of(a, a);
        }
    }

    /**
     * Sets the realm resolved from the request realm query parameter.
     *
     * @param realm The resolved realm.
     */
    void setOverrideRealm(String realm) {
        this.overrideRealm = realm;
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

    /**
     * Gets the base resolved realm.
     *
     * <p>If a DNS alias, that resolves to a sub-realm, was used then that is returned, otherwise
     * the resolved realm from the URI realm is returned.</p>
     *
     * @return The base realm.
     */
    public String getBaseRealm() {
        if (dnsAliasRealm == null || dnsAliasRealm.getSecond().equals("/")) {
            return relativeRealmPath.getSecond();
        }
        return dnsAliasRealm.getSecond();
    }

    /**
     * Gets the relative resolved realm which is rebased on top of the base DNS realm.
     *
     * @return The relative realm.
     */
    public String getRelativeRealm() {
       return relativeRealmPath.getSecond();
    }

    /**
     * Gets the full rebased realm, including the DNS realm and the URI realm.
     *
     * @return The full rebased realm.
     */
    String getRebasedRealm() {
        if (dnsAliasRealm == null || dnsAliasRealm.getSecond().equals("/")) {
            return relativeRealmPath.getSecond();
        } else {
            return dnsAliasRealm.getSecond() + relativeRealmPath.getSecond();
        }
    }

    /**
     * Gets the realm specified by the override query parameter.
     *
     * @return The override realm.
     */
    String getOverrideRealm() {
        return overrideRealm;
    }
}
