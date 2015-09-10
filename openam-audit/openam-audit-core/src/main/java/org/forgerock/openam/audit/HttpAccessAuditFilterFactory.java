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
package org.forgerock.openam.audit;

import static org.forgerock.openam.audit.AuditConstants.Component;

import javax.inject.Inject;
import java.util.Map;

import org.forgerock.http.Filter;

/**
 * Factory to assist with the creation of audit filters for restlet access.
 *
 * @since 13.0.0
 */
public final class HttpAccessAuditFilterFactory {

    private final Map<Component, AbstractHttpAccessAuditFilter> httpAccessAuditFilters;

    /**
     * Guice injected constructor for creating a <code>RestletAccessAuditFilterFactory</code> instance.
     *
     * @param httpAccessAuditFilters The map of Component HttpAccessAudit Filters.
     */
    @Inject
    public HttpAccessAuditFilterFactory(Map<Component, AbstractHttpAccessAuditFilter> httpAccessAuditFilters) {
        this.httpAccessAuditFilters = httpAccessAuditFilters;
    }

    /**
     * Create a new {@link Filter} for the given restlet and component for auditing access to the restlet.
     *
     * @param component The component represented by the restlet.
     * @return an instance of {@link Filter}
     */
    public Filter createFilter(Component component) {
        if (!httpAccessAuditFilters.containsKey(component)) {
            throw new IllegalArgumentException("Filter for " + component + " does not exist.");
        }
        return httpAccessAuditFilters.get(component);
    }

}
