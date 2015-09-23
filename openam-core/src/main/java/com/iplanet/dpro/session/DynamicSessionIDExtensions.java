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
 * Copyright 2015 ForgeRock AS.
 */
package com.iplanet.dpro.session;

import com.iplanet.services.naming.SessionIDCorrector;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.services.naming.WebtopNamingQuery;
import org.forgerock.util.annotations.VisibleForTesting;

import java.util.Map;

/**
 * Responsible for dynamically updating the SessionID extensions (S1/SI) based on current
 * server configuration.
 *
 * When the server/site configuration changes, this layer will be able to update the requested
 * SessionID extension dynamically using the SessionIDCorrector.
 *
 * Importantly, the logic of how the S1/SI values are arranged is maintained as per {@link SessionID#validate()}.
 */
public class DynamicSessionIDExtensions implements SessionIDExtensions {
    private final SessionIDExtensions delegate;
    private final WebtopNamingQuery query;

    /**
     * Constructor with all dependencies defined.
     * @param query Non null, required for lookup of WebtopNaming#getSessionIDCorrector.
     * @param delegate Non null, required for delegation of actual operation.
     */
    @VisibleForTesting
    DynamicSessionIDExtensions(WebtopNamingQuery query, SessionIDExtensions delegate) {
        this.query = query;
        this.delegate = delegate;
    }

    /**
     * Create a default instance.
     *
     * @param delegate The delegate to defer.
     */
    public DynamicSessionIDExtensions(SessionIDExtensions delegate) {
        this(new WebtopNamingQuery(), delegate);
    }

    /**
     * @return Possibly null PrimaryID based on current Server/Site configuration.
     */
    @Override
    public String getPrimaryID() {
        SessionIDCorrector corrector = query.getSessionIDCorrector();
        if (corrector == null) { // WebtopNaming not yet configured.
            return delegate.getPrimaryID();
        }
        return corrector.translatePrimaryID(delegate.getPrimaryID(), delegate.getSiteID());
    }

    /**
     * @return Possibly null SiteID based on current Server/Site configuration.
     */
    @Override
    public String getSiteID() {
        SessionIDCorrector corrector = query.getSessionIDCorrector();
        if (corrector == null) { // WebtopNaming not yet configured.
            return delegate.getSiteID();
        }
        return corrector.translateSiteID(delegate.getPrimaryID(), delegate.getSiteID());
    }

    /**
     * @return Defers to the delegated instance for getStorageKey call.
     */
    @Override
    public String getStorageKey() {
        return delegate.getStorageKey();
    }

    /**
     * Defers to the delegated instance for get.
     * @param key Non null
     * @return Possibly null
     */
    @Override
    public String get(String key) {
        return delegate.get(key);
    }

    /**
     * Defers to the delegated instance for get.
     * @param key Non null key.
     * @param value Non null value.
     */
    @Override
    public void add(String key, String value) {
        delegate.add(key, value);
    }

    /**
     * @return Defers to the delegated instance for asMap call.
     */
    @Override
    public Map<String, String> asMap() {
        return delegate.asMap();
    }

    /**
     * @return A string representation of the underlying SessionIDCorrector responsible for the mappings.
     */
    @Override
    public String toString() {
        return WebtopNaming.getSessionIDCorrector().toString();
    }
}
