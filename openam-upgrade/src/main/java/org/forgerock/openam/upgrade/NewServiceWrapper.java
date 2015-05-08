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

package org.forgerock.openam.upgrade;

import org.w3c.dom.Document;

import java.util.Map;

/**
 * Wraps the new Service document and any modifications required after upgrade.
 */
public class NewServiceWrapper {
    private final String serviceName;
    private final Map<String, ServiceSchemaModificationWrapper> modifiedSchemaMap;
    private final Document document;

    /**
     * Create a new wrapper for the service to be created and the attributes modified in the service.
     *
     * @param serviceName Name of the service being created.
     * @param modifiedSchemaMap A map, keyed on the name of the schema, that holds schema modification wrappers
     *                          containing the modified attributes.
     * @param document The document describing the new service.
     */
    public NewServiceWrapper(String serviceName,
                             Map<String, ServiceSchemaModificationWrapper> modifiedSchemaMap,
                             Document document) {
        this.serviceName = serviceName;
        this.modifiedSchemaMap = modifiedSchemaMap;
        this.document = document;
    }
    
    /**
     * Get the wrapper containing the service modifications.
     * 
     * @return The schema service modification wrapper.
     */
    public Map<String, ServiceSchemaModificationWrapper> getModifiedSchemaMap() {
        return modifiedSchemaMap;
    }

    /**
     * Get the document describing the new service.
     *
     * @return The document describing the new service.
     */
    public Document getServiceDocument() {
        return document;
    }

    /**
     * Get the name of the service associated with this wrapper.
     *
     * @return The name of the service.
     */
    public String getServiceName() {
        return serviceName;
    }
}
