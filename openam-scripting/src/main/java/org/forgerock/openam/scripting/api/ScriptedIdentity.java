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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openam.scripting.api;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.openam.scripting.ScriptConstants;
import org.forgerock.openam.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * A wrapper class to limit an authentication script's exposure to a AmIdentity object
 */
public class ScriptedIdentity {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptConstants.LOGGER_NAME);

    private final AMIdentity amIdentity;

    /**
     * The constructor for the <code>ScriptedIdentity</code> object
     * @param amIdentity The amIdentity object containing the user information
     */
    public ScriptedIdentity(AMIdentity amIdentity) {
        this.amIdentity = amIdentity;
    }

    /**
     * Retrieves a particular attribute's value
     * @param attributeName The name of the attribute to be retrieved
     * @return The value of the attribute
     */
    public Set getAttribute(String attributeName) {
        try {
            return amIdentity.getAttribute(attributeName);
        } catch (IdRepoException e) {
            LOGGER.warn("Exception trying to get attribute", e);
        } catch (SSOException e) {
            LOGGER.warn("SSO Exception", e);
        }

        return null;
    }

    /**
     * Sets the attribute's values. If the attribute already exists all existing values will be overridden. If it doesn't exist, it will be created.
     * @param attributeName The name of the attribute
     * @param attributeValues The values of the attribute
     */
    public void setAttribute(String attributeName, Object[] attributeValues) {
        Set attributeValuesAsSet = CollectionUtils.asSet(attributeValues);
        HashMap<String, Set> attributes = new HashMap<String, Set>();
        attributes.put(attributeName, attributeValuesAsSet);

        try {
            amIdentity.setAttributes(attributes);
        } catch (IdRepoException e) {
            LOGGER.warn("Exception trying to set attribute", e);
        } catch (SSOException e) {
            LOGGER.warn("SSO Exception", e);
        }
    }

    public void addAttribute(String attributeName, String attributeValue) {
        Set<String> currentAttributeValues = null;
        try {
            currentAttributeValues = amIdentity.getAttribute(attributeName);
        } catch (IdRepoException e) {
            LOGGER.warn("Attribute '" + attributeName + "' doesn't currently exist. Creating new attribute..");
        } catch (SSOException e) {
            LOGGER.warn("SSO Exception", e);
        }

        if(currentAttributeValues == null) {
            currentAttributeValues = new HashSet<String>();
        }

        currentAttributeValues.add(attributeValue);
        setAttribute(attributeName, currentAttributeValues.toArray());
    }

    /**
     * Persists the current state of the user's attributes
     */
    public void store() {
        try {
            amIdentity.store();
        } catch (IdRepoException e) {
            LOGGER.warn("Exception persisting attribute", e);
        } catch (SSOException e) {
            LOGGER.warn("Exception persisting attribute", e);
        }
    }

}
