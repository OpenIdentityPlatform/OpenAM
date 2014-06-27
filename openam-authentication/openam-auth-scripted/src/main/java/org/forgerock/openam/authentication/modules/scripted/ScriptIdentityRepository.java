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
 * Portions Copyrighted 2010-2014 ForgeRock, Inc.
 */
package org.forgerock.openam.authentication.modules.scripted;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.*;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.util.Reject;

import java.util.Collections;
import java.util.Set;

/**
 * A repository to retrieve user information within a scripting module's script
 */
public class ScriptIdentityRepository {
    private static final Debug DEBUG = Debug.getInstance("ScriptedModuleIdentityRepository");
    private AMIdentityRepository identityRepository;

    /**
     * Constructor for <code>ScriptIdentityRepository</code> object
     *
     * @param identityRepository The AmIdentityRepository object used to retrieve/persist user information
     */
    public ScriptIdentityRepository(AMIdentityRepository identityRepository) {
        Reject.ifNull(identityRepository);
        this.identityRepository = identityRepository;
    }

    /**
     * Returns a particular attribute for a particular user
     *
     * @param userName      The name of the user
     * @param attributeName The attribute name to be returned
     * @return A set of Strings containing all values of the attribute
     */
    public Set getAttribute(String userName, String attributeName) {
        ScriptedIdentity amIdentity = getIdentity(userName);
        return amIdentity.getAttribute(attributeName);
    }

    /**
     * Sets a particular attribute for a particular user. If the attribute already exists it will be overridden.
     *
     * @param userName       The name of the user
     * @param attributeName  The attribute name to be set
     * @param attributeValues The new value of the attribute
     */
    public void setAttribute(String userName, String attributeName, String[] attributeValues) {
        ScriptedIdentity amIdentity = getIdentity(userName);
        amIdentity.setAttribute(attributeName, attributeValues);
        amIdentity.store();
    }

    /**
     * Adds an attribute to the list of values already assigned to the attributeName
     * @param userName The name of the user
     * @param attributeName The attribute name to be added to
     * @param attributeValue The value to be added
     */
    public void addAttribute(String userName, String attributeName, String attributeValue) {
        ScriptedIdentity amIdentity = getIdentity(userName);
        amIdentity.addAttribute(attributeName, attributeValue);
        amIdentity.store();
    }

    /**
     * Retrieves the attributes associated with a particular user
     *
     * @param userName The name of the user
     * @return A ScriptedIdentity object containing the attributes for the specified user
     */
    private ScriptedIdentity getIdentity(String userName) {
        AMIdentity amIdentity = null;

        IdSearchControl idsc = new IdSearchControl();
        idsc.setAllReturnAttributes(true);
        idsc.setMaxResults(0);
        Set<AMIdentity> results = Collections.emptySet();

        try {
            IdSearchResults searchResults = identityRepository.searchIdentities(IdType.USER, userName, idsc);
            if (searchResults != null) {
                results = searchResults.getSearchResults();
            }

            if (results.isEmpty()) {
                throw new IdRepoException("getIdentity : User " + userName
                        + " is not found");
            } else if (results.size() > 1) {
                throw new IdRepoException(
                        "getIdentity : More than one user found for the userName "
                                + userName
                );
            }

            amIdentity = results.iterator().next();
        } catch (IdRepoException e) {
            DEBUG.error("Error searching Identities with username : " + userName, e);
        } catch (SSOException e) {
            DEBUG.error("Module exception : ", e);
        }

        return new ScriptedIdentity(amIdentity);
    }

}
