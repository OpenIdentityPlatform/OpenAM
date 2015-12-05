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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.audit.servlet;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.openam.audit.AuditConstants.Component;

/**
 * Factory interface for Guice assisted injection of {@link Auditor}.
 *
 * @since 13.0.0
 */
public interface AuditorFactory {

    /**
     * Construct a new Auditor instance with other dependencies provided by Guice.
     *
     * @param request HttpServletRequest.
     * @param response Queryable decorator over the HttpServletResponse.
     * @param component The component.
     * @return new Auditor instance.
     */
    Auditor create(HttpServletRequest request, AuditableHttpServletResponse response, Component component);
}