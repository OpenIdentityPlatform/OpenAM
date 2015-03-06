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

package org.forgerock.openam.uma;

import java.net.URI;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import org.forgerock.oauth2.core.exceptions.ServerException;

/**
 * Models all of the possible settings the UMA provider can have and that can be configured.
 *
 * @since 13.0.0
 */
public interface UmaSettings {

    /**
     * Gets the supported version of the UMA specification.
     *
     * @return The UMA version.
     */
    String getVersion();

    /**
     * <p>Gets the supported PAT Profiles.</p>
     *
     * <p>NOTE: The "bearer" profile MUST be supported.</p>
     *
     * @return The supported PAT profiles.
     * @throws org.forgerock.oauth2.core.exceptions.ServerException If there is a problem reading the configuration.
     */
    Set<String> getSupportedPATProfiles() throws ServerException;

    /**
     * <p>Gets the supported AAT Profiles.</p>
     *
     * <p>NOTE: The "bearer" profile MUST be supported.</p>
     *
     * @return The supported AAT profiles.
     * @throws ServerException If there is a problem reading the configuration.
     */
    Set<String> getSupportedAATProfiles() throws ServerException;

    /**
     * <p>Gets the supported RPT Profiles.</p>
     *
     * <p>NOTE: The "bearer" profile MUST be supported.</p>
     *
     * @return The supported RPT profiles.
     * @throws ServerException If there is a problem reading the configuration.
     */
    Set<String> getSupportedRPTProfiles() throws ServerException;

    /**
     * <p>Gets the config file to be used to store Uma audit</p>
     * @return
     * @throws ServerException
     */
    String getAuditLogConfig() throws ServerException, SMSException, SSOException;

    /**
     * Gets the supported PAT Grant Types.
     *
     * @return The supported PAT grant types.
     * @throws ServerException If there is a problem reading the configuration.
     */
    Set<String> getSupportedPATGrantTypes() throws ServerException;

    /**
     * Gets the supported AAT Grant Types.
     *
     * @return The supported AAT grant types.
     * @throws ServerException If there is a problem reading the configuration.
     */
    Set<String> getSupportedAATGrantTypes() throws ServerException;

    /**
     * Gets the supported claim token profiles.
     *
     * @return The supported claim token profiles.
     * @throws ServerException If there is a problem reading the configuration.
     */
    Set<String> getSupportedClaimTokenProfiles() throws ServerException;

    /**
     * Gets the supported UMA profiles.
     *
     * @return The supported UMA profiles.
     * @throws ServerException If there is a problem reading the configuration.
     */
    Set<URI> getSupportedUmaProfiles() throws ServerException;

    /**
     * Gets the lifetime an RPT will have before it expires.
     *
     * @return The lifetime of an RPT in seconds.
     * @throws ServerException If there is a problem reading the configuration.
     */
    long getRPTLifetime() throws ServerException;

    /**
     * Gets the lifetime an permission ticket will have before it expires.
     *
     * @return The lifetime of an permission ticket in seconds.
     * @throws ServerException If there is a problem reading the configuration.
     */
    long getPermissionTicketLifetime() throws ServerException;

    /**
     * Gets whether a Resource Server's policies should be deleted when the Resource Server OAuth2
     * agent entry is removed, or the "uma_protection" scope is removed.
     *
     * @return {@code true} if the policies should be deleted.
     * @throws ServerException If there is a problem reading the configuration.
     */
    boolean onDeleteResourceServerDeletePolicies() throws ServerException;

    /**
     * Gets whether a Resource Server's resource sets should be deleted when the Resource Server
     * OAuth2 agent entry is removed, or the "uma_protection" scope is removed.
     *
     * @return {@code true} if the resource sets should be deleted.
     * @throws ServerException If there is a problem reading the configuration.
     */
    boolean onDeleteResourceServerDeleteResourceSets() throws ServerException;
}
