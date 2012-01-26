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

/**
 * This class provides a wrapper for how the attributes within a specific service 
 * has changed and defines how said attributes will be upgraded.
 * 
 * @author steve
 */
public class ServiceSchemaUpgradeWrapper implements Serializable {
    protected ServiceSchemaModificationWrapper attributesAdded = null;
    protected ServiceSchemaModificationWrapper attributesModified = null;
    protected ServiceSchemaModificationWrapper attributesDeleted = null;
    
    /**
     * Create a new wrapper for the attributes add, modified or removed in the service
     * 
     * @param aAdded The attributes added
     * @param aModed The attributes modified
     * @param aDeleted The attributed deleted
     */
    public ServiceSchemaUpgradeWrapper(ServiceSchemaModificationWrapper aAdded, 
                                       ServiceSchemaModificationWrapper aModed, 
                                       ServiceSchemaModificationWrapper aDeleted) {
        attributesAdded = aAdded;
        attributesModified = aModed;
        attributesDeleted = aDeleted;
    }
    
    /**
     * Return the wrapper for the attributes added
     * 
     * @return The attributes added wrapper
     */
    public ServiceSchemaModificationWrapper getAttributesAdded() {
        return attributesAdded;
    }
    
    /**
     * Return the wrapper for the attributes modified
     * 
     * @return The attributes modified wrapper
     */
    public ServiceSchemaModificationWrapper getAttributesModified() {
        return attributesModified;
    }
    
    /**
     * Return the wrapper for the attributes deleted
     * 
     * @return The attributes deleted wrapper
     */
    public ServiceSchemaModificationWrapper getAttributesDeleted() {
        return attributesDeleted;
    }
}
