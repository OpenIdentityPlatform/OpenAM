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

package org.forgerock.openam.xacml.v3;

import com.sun.identity.entitlement.EntitlementException;

/**
 * Describes import step which can be applied to the underlying data store.
 *
 * @since 13.5.0
 */
public interface PersistableImportStep<T> extends ImportStep {

    /**
     * Contents of this import step will be applied to the data store.
     *
     * @throws EntitlementException
     *         if there are any errors
     */
    void apply() throws EntitlementException;

    /**
     * Gets the instance being imported.
     *
     * @return the instance being imported.
     */
    T get();

}
