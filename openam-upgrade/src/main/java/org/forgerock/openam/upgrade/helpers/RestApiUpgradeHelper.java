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
package org.forgerock.openam.upgrade.helpers;

import com.sun.identity.sm.AbstractUpgradeHelper;
import com.sun.identity.sm.AttributeSchemaImpl;
import org.forgerock.openam.upgrade.UpgradeException;

import java.util.Collections;

/**
 * This helper is used to set the default values for the REST API global configuration on upgrade of the product.
 */
public class RestApiUpgradeHelper extends AbstractUpgradeHelper {

    private static final String DEFAULT_VERSION_ATTRIBUTE = "openam-rest-apis-default-version";
    private static final String OLDEST_VERSION = "OLDEST";

    /**
     * The constructor for RestApiUpgradeHelper is used to register the attributes which will be upgraded.
     */
    public RestApiUpgradeHelper() {
        attributes.add(DEFAULT_VERSION_ATTRIBUTE);
    }

    /**
     * This is called when the service is added for the first time and it will change
     * the default install value of an attribute.
     *
     * @param newAttr The attribute schema definition to be modified.
     * @return The modified attribute schema.
     * @throws UpgradeException If there was an error while performing the attribute upgrade.
     */
    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl newAttr) throws UpgradeException {
        return updateDefaultValues(newAttr, Collections.singleton(OLDEST_VERSION));
    }

    /**
     * This implementation will always return <code>null</code> as there is currently no need to modify existing
     * schema attributes. This might change in the future in which case it should return the modified attribute.
     *
     * {@inheritDoc}
     */
    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl oldAttr, AttributeSchemaImpl newAttr)
            throws UpgradeException {
        return null;
    }
}
