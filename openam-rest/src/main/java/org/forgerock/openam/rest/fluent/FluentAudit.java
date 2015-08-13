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
package org.forgerock.openam.rest.fluent;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.resource.Filter;
import org.forgerock.openam.audit.AuditConstants.Component;
import org.forgerock.util.Reject;

/**
 * Contributes to the fluent API by ensuring each registered route has some audit definition.
 *
 * @since 13.0.0
 */
public class FluentAudit {

    private final CrestRouter router;
    private final String uriTemplate;

    public FluentAudit(CrestRouter router, String uriTemplate) {
        this.router = router;
        this.uriTemplate = uriTemplate;
    }

    /**
     * Define which component the route is associated with.
     *
     * @param component
     *         compoonent area
     *
     * @return the fluent route
     */
    public FluentRoute auditAs(Component component) {
        return auditAs(component, AuditFilter.class);
    }

    /**
     * Define which component the route is associated with and the filter used to carryout any auditing.
     *
     * @param component
     *         component area
     * @param filterClass
     *         filter used for auditing
     *
     * @return the fluent route
     */
    public FluentRoute auditAs(Component component, Class<? extends Filter> filterClass) {
        Reject.ifNull(component, filterClass);
        Filter auditFilter = getAuditFilter(filterClass);
        AuditFilterWrapper filterWrapper = new AuditFilterWrapper(auditFilter, component);
        return new FluentRoute(router, uriTemplate, filterWrapper);
    }

    private <F extends Filter> F getAuditFilter(Class<F> filterClass) {
        return InjectorHolder.getInstance(filterClass);
    }

}
