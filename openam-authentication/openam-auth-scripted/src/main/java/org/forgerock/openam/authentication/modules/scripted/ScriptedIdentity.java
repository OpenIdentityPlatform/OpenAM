/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: LDAP.java,v 1.17 2010/01/25 22:09:16 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2014 ForgeRock AS, Inc.
 */
package org.forgerock.openam.authentication.modules.scripted;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.utils.CollectionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * A wrapper class to limit an authentication script's exposure to a AmIdentity object
 */
class ScriptedIdentity {
    private final AMIdentity amIdentity;
    private static final Debug DEBUG = Debug.getInstance("ScriptedIdentity");

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
            DEBUG.message("Exception trying to get attribute", e);
        } catch (SSOException e) {
            DEBUG.message("SSO Exception", e);
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
            DEBUG.message("Exception trying to set attribute", e);
        } catch (SSOException e) {
            DEBUG.message("SSO Exception", e);
        }
    }

    public void addAttribute(String attributeName, String attributeValue) {
        Set<String> currentAttributeValues = null;
        try {
            currentAttributeValues = amIdentity.getAttribute(attributeName);
        } catch (IdRepoException e) {
            DEBUG.message("Attribute '" + attributeName + "' doesn't currently exist. Creating new attribute..");
        } catch (SSOException e) {
            DEBUG.message("SSO Exception", e);
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
            DEBUG.message("Exception persisting attribute", e);
        } catch (SSOException e) {
            DEBUG.message("Exception persisting attribute", e);
        }
    }
}
