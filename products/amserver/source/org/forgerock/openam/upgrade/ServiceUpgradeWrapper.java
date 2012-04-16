/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.upgrade;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Document;

/**
 * This class represents the set of services that have been added, modified,
 * or deleted and sub schemas that have been added.
 * 
 * @author Steve Ferris
 */
public class ServiceUpgradeWrapper implements Serializable {
    protected Map<String, Document> servicesAdded = null;
    protected Map<String, Map<String, ServiceSchemaUpgradeWrapper>> serviceModified = null;
    protected Map<String, Map<String, SubSchemaUpgradeWrapper>> subSchemasModified = null;
    protected Set<String> servicesDeleted = null;
    
    /**
     * Create a new service upgrade wrapper
     * 
     * @param sAdd The new services
     * @param sMod The modified services
     * @param ssMod The new sub schemas
     * @param sDelete The deleted services
     */
    public ServiceUpgradeWrapper(Map<String, Document> sAdd, 
                                 Map<String, Map<String, ServiceSchemaUpgradeWrapper>> sMod,
                                 Map<String, Map<String, SubSchemaUpgradeWrapper>> ssMod,
                                 Set<String> sDelete) {
        servicesAdded = sAdd;
        serviceModified = sMod;
        subSchemasModified = ssMod;
        servicesDeleted = sDelete;
    }
    
    /**
     * Return the set of services added as a map keyed on the name of the service
     * and the value being the XML document defining the service
     * 
     * @return The new services map
     */
    public Map<String, Document> getServicesAdded() {
        return servicesAdded;
    }
    
    /**
     * Return the set of modified attributes within existing services. The Map is keyed
     * on service name with the value being another Map. The value Map is keyed on 
     * service schema attribute type (Global/Organization) with the value being
     * the wrapper of service schema modifications.
     * 
     * @return The modified attributes within the service schemas
     */
    public Map<String, Map<String, ServiceSchemaUpgradeWrapper>> getServicesModified() {
        return serviceModified;
    }
    
    /**
     * Return the set of modified sub schemas. The map is keyed on service name
     * with the value being a Map. The value Map is keyed on service schema 
     * attribute type (Global/Organization) with the value being the sub
     * schema wrapper.
     * 
     * <i> Currently only adding new sub schemas is supported.
     * 
     * @return The new sub schemas
     */
    public Map<String, Map<String, SubSchemaUpgradeWrapper>> getSubSchemasModified() {
        return subSchemasModified;
    }
    
    /**
     * The set of service name that have been deleted.
     * 
     * @return The name of deleted services.
     */
    public Set<String> getServicesDeleted() {
        return servicesDeleted;
    }
}
