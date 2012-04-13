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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper class that contains the modifications within a subschema; currently
 * only new sub schema are supported.
 * 
 * @author Steve Ferris 
 */
public class SubSchemaModificationWrapper {
    private Map<String, NewSubSchemaWrapper> newSubSchemas = null;
    private SubSchemaModificationWrapper subSchema = null;
    
    /**
     * Creates a new sub schema modification wrapper
     */
    public SubSchemaModificationWrapper() {
        newSubSchemas = new HashMap<String, NewSubSchemaWrapper>();
    }
         
    /**
     * Adds a new named sub schema to this service
     * 
     * @param key The name of the sub schema
     * @param value The sub schema wrapper defining the sub schema
     */
    public void put(String key, NewSubSchemaWrapper value) {
        newSubSchemas.put(key, value);
    }
    
    /**
     * Sets the new sub schema for this wrapper
     * 
     * @param subSchemaWrapper The modified sub schema
     */
    public void setSubSchema(SubSchemaModificationWrapper subSchemaWrapper) {
        this.subSchema = subSchemaWrapper;
    }
    
    /**
     * Returns the entry set of the new sub schema
     * 
     * @return The entry set of the modified sub schemas
     */
    public Set<Map.Entry<String, NewSubSchemaWrapper>> entrySet() {
        return newSubSchemas.entrySet();
    }
    
    /**
     * Return the child sub schema beneath this sub schema
     * 
     * @return The modified child sub schema
     */
    public SubSchemaModificationWrapper getSubSchema() {
        return subSchema;
    }
    
    /**
     * Returns true if the sub schema exists
     * 
     * @return true if this wrapper has a sub schema
     */
    public boolean hasSubSchema() {
        return (subSchema == null) ? false : true;
    }
    
    /**
     * Returns true if the service sub schema map is empty
     * 
     * @return true if the sub schema is not modified
     */
    public boolean hasNewSubSchema() {
        if (newSubSchemas == null) {
            return false;
        }
        
        return !newSubSchemas.isEmpty();
    }
    
    /**
     * Returns true if the sub schema either has a new sub schema or sub schema
     * 
     * @return true if the sub schema wrapper has been modified at some level
     */
    public boolean subSchemaChanged() {
        if (!hasSubSchema()) {
            return hasNewSubSchema();
        } else {
            boolean modified = hasNewSubSchema();
            modified |= subSchema.subSchemaChanged();
                    
            return modified;
        }
    }
}
