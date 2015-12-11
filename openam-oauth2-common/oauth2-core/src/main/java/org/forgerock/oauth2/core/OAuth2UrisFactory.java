package org.forgerock.oauth2.core;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.oauth2.core.exceptions.NotFoundException;
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
    OAuth2Uris get(final OAuth2Request request) throws NotFoundException;

    /**
     * Gets the instance of the OAuth2UrisFactory.
     *
     * @param context The context that can be used to obtain the base deployment url.
     * @param realmInfo The realm info.
     * @return The OAuth2UrisFactory instance.
     */
    OAuth2Uris get(Context context, T realmInfo) throws NotFoundException;

    /**
     * Gets the instance of the OAuth2UrisFactory.
     *
     * @param request The request.
     * @param realmInfo The realm info.
     * @return The OAuth2UrisFactory instance.
     */
    OAuth2Uris get(HttpServletRequest request, T realmInfo) throws NotFoundException;
}
