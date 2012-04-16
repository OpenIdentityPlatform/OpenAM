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

import org.w3c.dom.Node;

/**
 * Wraps the definition of a new sub schema
 * 
 * @author steve
 */
public class NewSubSchemaWrapper {
    private String serviceName = null;
    private String subSchemaName = null;
    private Node subSchemas = null;
    
    /**
     * Creates a new sub schema wrapper
     * 
     * @param serviceName
     * @param subSchemaName
     * @param subSchemas 
     */
    public NewSubSchemaWrapper(String serviceName, String subSchemaName, Node subSchemas) {
        this.serviceName = serviceName;
        this.subSchemaName = subSchemaName;       
        this.subSchemas = subSchemas;
    }
    
    /**
     * Get the name of the service within which this new sub schema will be
     * defined.
     * 
     * @return The service name
     */
    public String getServiceName() {
        return serviceName;
    }
    
    /**
     * Get the name of the new sub schema that will be created.
     * 
     * @return The name of the sub schema
     */
    public String getSubSchemaName() {
        return subSchemaName;
    }
    
    /**
     * Get the XML node that defines this new sub schema, sub-sub schemas are
     * supported
     * 
     * @return The XML Node of the new sub schema
     */
    public Node getSubSchemaNode() {
        return subSchemas;
    }
}
