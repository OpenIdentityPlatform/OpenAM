/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted [2012] [Forgerock Inc]"
 */

package org.forgerock.restlet.ext.openam.client;

import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.forgerock.restlet.ext.openam.OpenAMAuthenticatorHelper;
import org.forgerock.restlet.ext.openam.OpenAMParameters;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.Reference;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Filter;

/**
 * A OpenAMProxy transparently authenticates the client before the request. This
 * is a sample class and not optimized yet. Used to Test OAuth 2.
 * 
 * @author Laszlo Hordos
 */
public class OpenAMProxy extends Filter {
    private final static String TOKEN_PREFIX = "token.id=";
    private final OpenAMParameters params;

    public OpenAMProxy(OpenAMParameters params) {
        this.params = params;
    }

    public OpenAMProxy(Context context, OpenAMParameters params) {
        super(context);
        this.params = params;
    }

    public OpenAMProxy(Context context, Restlet next, OpenAMParameters params) {
        super(context, next);
        this.params = params;
    }

    @Override
    protected int beforeHandle(Request request, Response response) {
        // String token = rest(request, response);
        String token = sdk(request, response);
        if (null != token) {
            ChallengeResponse cr = new ChallengeResponse(OpenAMAuthenticatorHelper.HTTP_OPENAM);
            OpenAMAuthenticatorHelper.saveSSOToken(cr, token);
            request.setChallengeResponse(cr);
            return Filter.CONTINUE;
        }
        return Filter.STOP;
    }

    protected String rest(Request request, Response response) {
        Reference auth =
                new Reference(params.getOpenAMServerRef().toString() + "/identity/authenticate");
        auth.addQueryParameter("username", params.getApplicationUserName());
        auth.addQueryParameter("password", params.getApplicationUserPassword());

        // &uri=realm=%2F%26module=DataStore"
        // &uri=realm=/&module=DataStore

        try {
            ClientResource client = new ClientResource(getContext(), auth);
            Representation body = client.get();
            if (body != null && body instanceof EmptyRepresentation == false) {
                // token.id=AQIC5wM2LY4Sfcwq-PN5UmgYQ--XlvB2KyZRnEtUoalHeI0.*AAJTSQACMDE.*
                String token = body.getText();
                if (token.startsWith(TOKEN_PREFIX)) {
                    return token.substring(TOKEN_PREFIX.length());
                }
            }
        } catch (ResourceException e) {
            response.setStatus(e.getStatus());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected String sdk(Request request, Response response) {
        try {
            com.sun.identity.authentication.AuthContext lc =
                    new com.sun.identity.authentication.AuthContext(params.getOrgName());
            if (params.getLocale() == null) {
                lc.login(com.sun.identity.authentication.AuthContext.IndexType.MODULE_INSTANCE,
                        params.getLoginIndexName());
            } else {
                lc.login(com.sun.identity.authentication.AuthContext.IndexType.MODULE_INSTANCE,
                        params.getLoginIndexName(), params.getLocale());
            }
            String token = null;
            Callback[] callbacks = null;
            // get information requested from module
            while (lc.hasMoreRequirements()) {
                callbacks = lc.getRequirements();
                if (callbacks != null) {
                    addLoginCallbackMessage(callbacks);
                    lc.submitRequirements(callbacks);
                }
            }

            if (lc.getStatus() == com.sun.identity.authentication.AuthContext.Status.SUCCESS) {
                System.out.println("Login succeeded.");
                try {
                    token = lc.getSSOToken().getTokenID().toString();
                    for (Map.Entry<String, javax.servlet.http.Cookie> entry : ((Map<String, javax.servlet.http.Cookie>) lc
                            .getCookieTable()).entrySet()) {
                        request.getCookies().add(entry.getKey(), entry.getValue().getValue());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (lc.getStatus() == com.sun.identity.authentication.AuthContext.Status.FAILED) {
                System.out.println("Login failed.");
            } else {
                System.out.println("Unknown status: " + lc.getStatus());
            }
            return token;
        } catch (UnsupportedCallbackException e) {
            e.printStackTrace();
        } catch (com.sun.identity.authentication.spi.AuthLoginException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void addLoginCallbackMessage(Callback[] callbacks)
            throws UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof TextOutputCallback) {
                ((TextOutputCallback) callbacks[i]).getMessage();
            } else if (callbacks[i] instanceof NameCallback) {
                ((NameCallback) callbacks[i]).setName(params.getApplicationUserName());
            } else if (callbacks[i] instanceof PasswordCallback) {
                ((PasswordCallback) callbacks[i]).setPassword(params.getApplicationUserPassword()
                        .toCharArray());
            } else if (callbacks[i] instanceof TextInputCallback) {
                ((TextInputCallback) callbacks[i]).setText("something");
            } else if (callbacks[i] instanceof ChoiceCallback) {
                ((ChoiceCallback) callbacks[i]).setSelectedIndex(0);
            }
        }
    }
}
