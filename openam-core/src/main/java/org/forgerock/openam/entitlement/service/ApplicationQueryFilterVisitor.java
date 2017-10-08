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
package org.forgerock.openam.entitlement.service;

import static com.sun.identity.entitlement.Application.*;
import static java.util.Collections.*;
import static org.forgerock.openam.utils.StringUtils.isNotEmpty;

import java.util.Map;
import java.util.Set;

import org.forgerock.openam.sm.ServiceConfigQueryFilterVisitor;

import com.sun.identity.sm.ServiceConfig;

/**
 * Query filter visitor for filtering on Application configuration attributes.
 *
 * @since 13.1.0
 */
public final class ApplicationQueryFilterVisitor extends ServiceConfigQueryFilterVisitor {

    private final String applicationName;

    /**
     * Create a new instance of {@link ApplicationQueryFilterVisitor} for a specific Application.
     * @param applicationName The name of the application.
     */
    public ApplicationQueryFilterVisitor(String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    protected Map<String, Set<String>> getConfigData(ServiceConfig serviceConfig) {
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> appData = serviceConfig.getAttributes();

        // Add the name as it is not stored with the attributes
        appData.put("name", singleton(applicationName));

        // Extract the meta data for accessibility by the query filter visitor
        Set<String> metaData = appData.get("meta");
        for (String entry : metaData) {
            Set<String> attributeValue = emptySet();
            if (isNotEmpty(entry)) {
                String[] tokens = entry.split("=");
                if (tokens.length == 2) {
                    attributeValue = singleton(tokens[1]);
                }
            }
            if (entry.startsWith(CREATED_BY_ATTRIBUTE)) {
                appData.put("createdBy", attributeValue);
            } else if (entry.startsWith(CREATION_DATE_ATTRIBUTE)) {
                appData.put("creationDate", attributeValue);
            } else if (entry.startsWith(LAST_MODIFIED_BY_ATTRIBUTE)) {
                appData.put("lastModifiedBy", attributeValue);
            } else  if (entry.startsWith(LAST_MODIFIED_DATE_ATTRIBUTE)) {
                appData.put("lastModifiedDate", attributeValue);
            }
        }
        return appData;
    }
}
