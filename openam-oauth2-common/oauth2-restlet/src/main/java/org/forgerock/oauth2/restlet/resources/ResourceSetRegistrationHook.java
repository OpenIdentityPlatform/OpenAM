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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.oauth2.restlet.resources;

import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.restlet.Context;

import com.sun.identity.entitlement.EntitlementException;

/**
 * Hook for registration events of Resource Sets.
 *
 * @since 13.0.0
 */
public interface ResourceSetRegistrationHook {

    /**
     * Fired after a Resource Set description is successfully created.
     *
     * @param realm The realm the Resource Set was created in.
     * @param resourceSet The Resource Set description.
     */
    void resourceSetCreated(String realm, ResourceSetDescription resourceSet) throws ServerException;

    /**
     * Fired before a Resource Set description is about to be deleted.
     * @param realm The realm the Resource Set will be deleted in.
     * @param resourceSet The Resource Set description.
     */
    void resourceSetDeleted(String realm, ResourceSetDescription resourceSet) throws ServerException;
}
