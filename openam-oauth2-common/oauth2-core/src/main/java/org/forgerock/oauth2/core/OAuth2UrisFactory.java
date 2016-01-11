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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.services.context.Context;

/**
 * A factory for creating/retrieving OAuth2 Uris instances.
 * <br/>
 * It is up to the implementation to provide caching of OAuth2Uris instance if it wants to supported
 * multiple OAuth2 providers.
 *
 * @param <T> The realm object type.
 *
 * @since 13.0.0
 */
public interface OAuth2UrisFactory<T> {

    /**
     * Gets a OAuth2UrisFactory instance.
     *
     * @param request The OAuth2 request.
     * @return A OAuth2UrisFactory instance.
     */
    OAuth2Uris get(final OAuth2Request request) throws NotFoundException, ServerException;

    /**
     * Gets the instance of the OAuth2UrisFactory.
     *
     * @param context The context that can be used to obtain the base deployment url.
     * @param realmInfo The realm info.
     * @return The OAuth2UrisFactory instance.
     */
    OAuth2Uris get(Context context, T realmInfo) throws NotFoundException, ServerException;

    /**
     * Gets the instance of the OAuth2UrisFactory.
     *
     * @param request The request.
     * @param realmInfo The realm info.
     * @return The OAuth2UrisFactory instance.
     */
    OAuth2Uris get(HttpServletRequest request, T realmInfo) throws NotFoundException, ServerException;
}
