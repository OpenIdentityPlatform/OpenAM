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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.core;

import static org.forgerock.openam.utils.RealmUtils.cleanRealm;
import static org.forgerock.openam.utils.RealmUtils.concatenateRealmPath;
import static org.forgerock.util.Reject.checkNotNull;

import java.util.Objects;

import org.forgerock.util.Reject;

/**
 * Encapsulates information about the realm that a request is for.
 *
 * <p>Keeps track of the absolute realm, the portion of the realm that was derived from the
 * request URI and the override realm specified as a request query parameter.</p>
 */
public class RealmInfo {

    private final String absoluteRealm;
    private String realmSubPath;
    private String overrideRealm;

    public RealmInfo(String absoluteRealm, String realmSubPath, String overrideRealm) {
        this.absoluteRealm = checkNotNull(absoluteRealm);
        this.realmSubPath = checkNotNull(realmSubPath);
        this.overrideRealm = overrideRealm;
    }

    public RealmInfo(String absoluteRealm) {
        this(absoluteRealm, "", null);
    }

    public String getAbsoluteRealm() {
        return absoluteRealm;
    }

    public String getRealmSubPath() {
        return realmSubPath;
    }

    /**
     * Sets the given absolute realm and returns a new {@code RealmInfo} instance, without
     * modifying this instance.
     *
     * @param absoluteRealm The absolute realm.
     * @return A new {@code RealmInfo} instance.
     */
    public RealmInfo withAbsoluteRealm(String absoluteRealm) {
        absoluteRealm = cleanRealm(absoluteRealm);
        return new RealmInfo(absoluteRealm, realmSubPath, overrideRealm);
    }

    /**
     * Appends the given realm sub path to the absolute realm and existing realm sub path and
     * returns a new {@code RealmInfo} instance, without modifying this instance.
     *
     * @param realmSubPath The realm sub path.
     * @return A new {@code RealmInfo} instance.
     */
    public RealmInfo appendUriRealm(String realmSubPath) {
        Reject.ifNull(realmSubPath);
        return new RealmInfo(
                concatenateRealmPath(absoluteRealm, cleanRealm(realmSubPath)),
                concatenateRealmPath(this.realmSubPath, cleanRealm(realmSubPath)),
                overrideRealm);
    }

    /**
     * Sets the given override realm and returns a new {@code RealmInfo} instance, without
     * modifying this instance.
     *
     * @param overrideRealm The realm override.
     * @return A new {@code RealmInfo} instance.
     */
    public RealmInfo withOverrideRealm(String overrideRealm) {
        overrideRealm = cleanRealm(overrideRealm);
        return new RealmInfo(overrideRealm, realmSubPath, overrideRealm);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RealmInfo realmInfo = (RealmInfo) o;

        return Objects.equals(absoluteRealm, realmInfo.absoluteRealm)
                && Objects.equals(realmSubPath, realmInfo.realmSubPath)
                && Objects.equals(overrideRealm, realmInfo.overrideRealm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(absoluteRealm, realmSubPath, overrideRealm);
    }
}
