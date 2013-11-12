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
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.restlet.ext.oauth2.consumer;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Uniform;
import org.forgerock.openam.oauth2.model.BearerToken;

/**
 * A BearerOAuth2Proxy is a class used to test OAuth2 and by them demo application
 *
 */
public class BearerOAuth2Proxy extends OAuth2Proxy<BearerTokenExtractor, BearerToken> {

    private BearerTokenExtractor helper;
    private RequestCallbackHandler<BearerToken> handler;

    public BearerOAuth2Proxy(Uniform oAuth2Client) {
        super(oAuth2Client);
        this.helper = new BearerTokenExtractor();
        this.handler = null;
    }

    public BearerOAuth2Proxy(Context context, Uniform oAuth2Client) {
        super(context, oAuth2Client);
        this.helper = new BearerTokenExtractor();
        this.handler = null;
    }

    // --------

    public BearerOAuth2Proxy(BearerOAuth2Proxy oAuth2Proxy, Flow flow,
            RequestCallbackHandler<BearerToken> handler) {
        super(oAuth2Proxy, flow);
        this.helper = new BearerTokenExtractor();
        this.handler = handler;
    }

    public BearerOAuth2Proxy(BearerOAuth2Proxy oAuth2Proxy, Flow flow,
                             BearerTokenExtractor helper, RequestCallbackHandler handler) {
        super(oAuth2Proxy, flow);
        this.helper = helper;
        this.handler = handler;
    }

    public BearerOAuth2Proxy(Context context, BearerOAuth2Proxy oAuth2Proxy, Flow flow,
            RequestCallbackHandler<BearerToken> handler) {
        super(context, oAuth2Proxy, flow);
        this.helper = new BearerTokenExtractor();
        this.handler = handler;
    }

    public BearerOAuth2Proxy(Context context, BearerOAuth2Proxy oAuth2Proxy, Flow flow,
                             BearerTokenExtractor helper, RequestCallbackHandler<BearerToken> handler) {
        super(context, oAuth2Proxy, flow);
        this.helper = helper;
        this.handler = handler;
    }

    // --------

    @Override
    public RequestCallbackHandler<BearerToken> getRequestCallbackHandler(Request request,
            Response response) {
        // TODO have a Request Wrapper implementation
        return handler;
    }

    @Override
    public BearerTokenExtractor getAccessTokenExtractor() {
        return helper;
    }

    public static BearerOAuth2Proxy popOAuth2Proxy(Context context) {
        BearerOAuth2Proxy proxy = null;
        if (null != context) {
            Object o = context.getAttributes().get(BearerOAuth2Proxy.class.getName());
            if (o instanceof BearerOAuth2Proxy) {
                proxy = (BearerOAuth2Proxy) o;
            }
        }
        return proxy;
    }

    public void pushOAuth2Proxy(Context context) {
        if (null != context) {
            context.getAttributes().put(BearerOAuth2Proxy.class.getName(), this);
        }
    }
}
