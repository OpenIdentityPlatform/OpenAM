/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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
 * This wrapper object is meant to store the changes made to service schemas, currently this is limited to new schemas,
 * but in future this could be possibly extended to track removed schemas as well. Modified schemas are currently
 * separately handled by {@link ServiceSchemaUpgradeWrapper}.
 *
 * @author Peter Major
 */
public class SchemaUpgradeWrapper {

    private ServiceSchemaModificationWrapper newSchema;

    public SchemaUpgradeWrapper(ServiceSchemaModificationWrapper addedSchemas) {
        this.newSchema = addedSchemas;
    }

    public ServiceSchemaModificationWrapper getNewSchema() {
        return newSchema;
    }
}
