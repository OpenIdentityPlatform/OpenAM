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

/**
 * This wrapper holds the set of modified (new) sub schemas, this class exists
 * as in future deleted sub schemas will be supported and this class will wrap
 * both add and deletes.
 * 
 * @author Steve Ferris
 */
public class SubSchemaUpgradeWrapper {
    protected SubSchemaModificationWrapper subSchemasAdded = null;
    
    /**
     * Create a new sub schema wrapper around the new sub schemas
     * 
     * @param sAdd The modified sub schema
     */
    public SubSchemaUpgradeWrapper(SubSchemaModificationWrapper sAdd) {
        subSchemasAdded = sAdd;
    }
    
    /**
     * Return the new sub schemas
     * 
     * @return The new sub schemas
     */
    public SubSchemaModificationWrapper getSubSchemasAdded() {
        return subSchemasAdded;
    }    
}
