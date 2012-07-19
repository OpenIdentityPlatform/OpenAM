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
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.restlet.ext.openam.server;

import org.forgerock.restlet.ext.openam.OpenAMParameters;
import org.forgerock.restlet.ext.openam.OpenAMUser;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Redirector;
import org.restlet.security.Authenticator;
import org.restlet.security.Enroler;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public abstract class AbstractOpenAMAuthenticator extends Authenticator {

    private final Reference openamServer;
    private String serviceName = null;
    private String moduleName = null;
    private String realm = null;

    /**
     * {@inheritDoc}
     */
    public AbstractOpenAMAuthenticator(Context context, OpenAMParameters parameters) {
        super(context);
        this.openamServer = parameters.getOpenAMServerRef();
        init(parameters);
    }

    /**
     * {@inheritDoc}
     */
    public AbstractOpenAMAuthenticator(Context context, OpenAMParameters parameters,
            boolean optional) {
        super(context, optional);
        this.openamServer = parameters.getOpenAMServerRef();
        init(parameters);
    }

    /**
     * {@inheritDoc}
     */
    public AbstractOpenAMAuthenticator(Context context, OpenAMParameters parameters,
            boolean multiAuthenticating, boolean optional, Enroler enroler) {
        super(context, multiAuthenticating, optional, enroler);
        this.openamServer = parameters.getOpenAMServerRef();
        init(parameters);
    }

    /**
     * {@inheritDoc}
     */
    public AbstractOpenAMAuthenticator(Context context, OpenAMParameters parameters,
            boolean optional, Enroler enroler) {
        super(context, optional, enroler);
        this.openamServer = parameters.getOpenAMServerRef();
        init(parameters);
    }

    protected void init(OpenAMParameters parameters) {
        String path = this.openamServer.getPath();
        path = path.endsWith("/") ? path + "UI/Login" : path + "/UI/Login";
        this.openamServer.setPath(path);
        realm = parameters.getOrgName();
        if (OpenAMParameters.IndexType.MODULE.equals(parameters.getLoginIndexType())) {
            moduleName = parameters.getLoginIndexName();
        } else if (OpenAMParameters.IndexType.SERVICE.equals(parameters.getLoginIndexType())) {
            serviceName = parameters.getLoginIndexName();
        }
    }

    protected abstract SSOToken getToken(Request request, Response response) throws SSOException;

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean authenticate(Request request, Response response) {
        try {
            SSOToken token = getToken(request, response);
            if (null != token) {
                AMIdentity identity = IdUtils.getIdentity(token);

                OpenAMUser user = new OpenAMUser(identity.getName(), token);
                request.getClientInfo().setUser(user);
                return identity.isActive();
            }
        } catch (SSOException e) {
            redirect(request, response);
        } catch (IdRepoException e) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage(), e);
        }
        return false;
    }

    protected void redirect(Request request, Response response) {
        Reference amserver = new Reference(openamServer);
        if (null != realm) {
            amserver.addQueryParameter("realm", realm);
        }
        if (null != moduleName) {
            amserver.addQueryParameter("module", moduleName);
        } else if (null != serviceName) {
            amserver.addQueryParameter("service", serviceName);
        }

        amserver.addQueryParameter("goto", request.getResourceRef().toString());

        Redirector redirector =
                new Redirector(getContext(), amserver.toString(), Redirector.MODE_CLIENT_FOUND);
        redirector.handle(request, response);
    }
}
