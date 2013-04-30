/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted [year] [name of company]"
 */

package org.forgerock.restlet.ext.oauth2.consumer;

import java.util.Collection;
import java.util.Set;

import org.forgerock.openam.oauth2.model.CoreToken;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.Request;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.routing.Redirector;
import org.restlet.util.Series;

/**
 * A RequestCallbackHandler handles the request and gets nessesary information.
 * Used by the demo application.
 *
 */
public interface RequestCallbackHandler<T extends CoreToken> {

    public T popAccessToken(Request request);

    public void pushAccessToken(Request request, T token);

    /**
     * @return
     * @see <a href=
     *      "http://tools.ietf.org/html/draft-ietf-oauth-v2-25#section-3.1.2>Redirecti
     *      o n Endpoint</a>
     */
    public String getRedirectionEndpoint(Request request, Reference reference);

    /**
     * Get the scope for the request.
     * <p/>
     * If this returns null then the {@link OAuth2Proxy} uses it's own default
     * scope If it returns an empty set then the {@link OAuth2Proxy} won't set
     * the scope in the request
     * 
     * @return
     * @see <a href=
     *      "http://tools.ietf.org/html/draft-ietf-oauth-v2-25#section-3.3>Acces
     *      s Token Scope</a>
     */
    public Collection<String> getScope(Request request, Set<String> scope);

    /**
     * @return
     * @see <a href=
     *      "http://tools.ietf.org/html/draft-ietf-oauth-v2-25#section-3.3>Acces
     *      s Token Scope</a>
     */
    public String getState(Request request);

    /**
     * Get the final custom parameter set of the outgoing OAuth2 request.
     * <p/>
     * The input parameters are the
     * {@link org.forgerock.restlet.ext.oauth2.consumer.OAuth2Proxy#getParameters()}
     * 
     * @param parameters
     *            copy of the parameters of {@link OAuth2Proxy} it can be null
     * @return
     */
    public Series<Parameter> decorateParameters(Series<Parameter> parameters);

    public OAuth2Utils.ParameterLocation getTokenLocation(Request request);

    /**
     * Pass back a handler to redirect the User-Agent to the Authorization
     * Endpoint
     * <p/>
     * The implementation should be:
     * {@code
     * redirector.handle(getRequest(), getResponse());
     * }
     * 
     * @param redirector
     */
    public OAuth2Proxy.AuthenticationStatus authorizationRedirect(Redirector redirector);
}
