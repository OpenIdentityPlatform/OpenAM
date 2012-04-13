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
import java.util.Set;

/**
 *
 * @author steve
 */
public interface UpgradeHelper {

    /**
     * Given the existing attributes in the schema and the new attribute the upgrade helper
     * can decide how to change the new attr based on the state of the existing attributes.
     * For example this could be useful, when you add a new attribute, and it's default
     * value depends on other existing attribute.
     *
     * @param existingAttrs The old attributes
     * @param newAttr The new attribute schema
     * @return The possibly updated attribute schema, this will be used in the upgrade
     * @throws UpgradeException If something bad happens, this will be used to log not stop the upgrade
     */
    public AttributeSchemaImpl addNewAttribute(Set<AttributeSchemaImpl> existingAttrs, AttributeSchemaImpl newAttr)
            throws UpgradeException;
    /**
     * Given the existing attribute schema and the new attribute schema the upgrade helper
     * can decide how to change the new attr based on the state of the existing attribute.
     * 
     * @param oldAttr The old attribute schema
     * @param newAttr The new attribute schema
     * @return The possibly updated attribute schema, this will be used in the upgrade
     * @throws UpgradeException If something bad happens, this will be used to log not stop the upgrade
     */
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl oldAttr, AttributeSchemaImpl newAttr)
            throws UpgradeException;

    /**
     * Return the Set of attributes that are to be upgrade by this service helper
     * 
     * @return The set of upgraded attributes names
     */
    public Set<String> getAttributes();
}
