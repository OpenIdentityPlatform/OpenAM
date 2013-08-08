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

import com.sun.identity.sm.AttributeSchemaImpl;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class wraps the attribute modifications for a specific service schema 
 * and the attributes within the services schema schemas.
 * 
 * @author steve
 */
public class ServiceSchemaModificationWrapper {
    private String serviceName = null;
    private String schemaName = null;
    private Set<AttributeSchemaImpl> attributes = null;
    private Map<String, ServiceSchemaModificationWrapper> subSchemas = null;
    
    /**
     * Create a new wrapper for the specific service and schemaName, used when 
     * the service has sub schemas
     * 
     * @param serviceName The name of the service
     * @param schemaName The name of the sub schema within the service
     */
    public ServiceSchemaModificationWrapper(String serviceName, String schemaName) {
        this(serviceName, schemaName, new HashSet<AttributeSchemaImpl>());
    }

    /**
     * Create a new wrapper for a services attribute modifications
     *
     * @param serviceName The name of the service
     * @param schemaName The name of the schema
     * @param attrs Set of modified attributes
     */
    public ServiceSchemaModificationWrapper(String serviceName,
                                            String schemaName,
                                            Set<AttributeSchemaImpl> attrs) {
        this(serviceName, schemaName, attrs, new HashMap<String, ServiceSchemaModificationWrapper>());
    }
    
    /**
     * Create a new wrapper for a services attribute modifications
     * 
     * @param serviceName The name of the service
     * @param schemaName The name of the schema
     * @param attrs Set of modified attributes
     * @param subSchemas Services sub schema modification wrappers
     */
    public ServiceSchemaModificationWrapper(String serviceName,
                                            String schemaName,
                                            Set<AttributeSchemaImpl> attrs, 
                                            Map<String, ServiceSchemaModificationWrapper> subSchemas) {
        this.serviceName = serviceName;
        this.schemaName = schemaName;
        this.attributes = attrs;
        this.subSchemas = subSchemas;
    }
    
    /**
     * Returns true if this wrapper contains a populated sub schema modification
     * wrapper
     * 
     * @return True if sub schemas have been modified 
     */
    public boolean hasSubSchema() {
        if (subSchemas != null) {
            return (subSchemas.isEmpty()) ? false : true;
        } 
        
        return false;
    }
    
    /**
     * Returns true if the services attributes have been modified either that this
     * level or within a sub schema.
     * 
     * @return true if this wrapper contains a modified schema
     */
    public boolean hasBeenModified() {
        if (!hasSubSchema()) {
            return isAttributesModified();
        } else {
            boolean modified = isAttributesModified();
            
            for (Map.Entry<String, ServiceSchemaModificationWrapper> subSchema : subSchemas.entrySet()) {
                modified |= subSchema.getValue().hasBeenModified();
            }
                    
            return modified;
        }
    }
    
    /**
     * Have the attributes of this service schema been modified
     * 
     * @return true if the attributes are modified
     */
    protected boolean isAttributesModified() {
        if (attributes != null) {
            return (attributes.isEmpty()) ? false : true;
        } 
        
        return false;
    }

    /**
     * Returns the name of the service associated with this wrapper
     * 
     * @return The name of the service
     */
    public String getServiceName() {
        return serviceName;
    }
    
    /**
     * Returns the name of the schema associated with this wrapper
     * 
     * @return The name of the schema 
     */
    public String getSchemaName() {
        return schemaName;
    }
    
    /**
     * Returns the set of modified attributes associated with this wrapper
     * 
     * @return The set of modified attributes
     */
    public Set<AttributeSchemaImpl> getAttributes() {
        return attributes;
    }
    
    /**
     * Set the set of modified attributes associated with this wrapper
     * 
     * @param attributes set of modified attributes
     */
    public void setAttributes(Set<AttributeSchemaImpl> attributes) {
        this.attributes = attributes;
    }
    
    /**
     * Return the sub schema maps of modified sub schema attribute wrappers
     * 
     * @return A map keyed on sub schema name with the value of the sub schema
     */
    public Map<String, ServiceSchemaModificationWrapper> getSubSchemas() {
        return subSchemas;
    }
    
    /**
     * Add a new named modified sub schema to this wrapper
     * 
     * @param name The name of the sub schema
     * @param subSchema The modified sub schema wrapper
     */
    public void addSubSchema(String name, ServiceSchemaModificationWrapper subSchema) {
        subSchemas.put(name, subSchema);
    }
}
