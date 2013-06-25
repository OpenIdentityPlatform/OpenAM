/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock Inc. All rights reserved.
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
 * "Portions copyright [year] [name of copyright owner]"
 */

package org.forgerock.openam.oauth2.provider.impl;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.OAuth2Constants;
import edu.emory.mathcs.backport.java.util.Arrays;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.provider.AbstractIdentityVerifier;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.engine.util.ChildContext;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Redirector;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;


/**
 * Verifies an OpenAMUser
 */
public class OpenAMIdentityVerifier extends AbstractIdentityVerifier<OpenAMUser> {

    private String serviceName = null;
    private String moduleName = null;
    private String realm = null;
    private String locale = null;
    private Context context = null;

    //ensure the prompt for login shows the login page atleast one time
    static private boolean hasRan = false;

    /**
     * Constructor.
     * <p/>
     *
     */
    public OpenAMIdentityVerifier(Context context) {
        this.context = context;
    }

    @Override
    protected OpenAMUser createUser(AuthContext authContext) throws Exception {
        SSOToken token = authContext.getSSOToken();
        return new OpenAMUser(token.getProperty("UserToken"), token);
    }

    /**
     * {@inheritDoc}
     */
    public int verify(Request request, Response response) {
        int result = RESULT_INVALID;
        //login
        boolean authenticated = false;
        authenticated = authenticate(request, response);
        if (authenticated) {
            result = RESULT_VALID;
        }

        return result;
    }


    protected boolean authenticate(Request request, Response response) {
        String prompt = OAuth2Utils.getRequestParameter(request, OAuth2Constants.Custom.PROMPT,String.class);
        String[] prompts = null;
        Set<String> promptSet = null;

        //put the space separated prompts into a Set collection
        if (prompt != null && !prompt.isEmpty()){
            prompts = prompt.split(" ");
        }
        if (prompts != null && prompts.length > 0){
            promptSet = new HashSet<String>(Arrays.asList(prompts));
        } else {
            promptSet = new HashSet<String>();
        }

        try {
            SSOToken token = getToken(request, response);
            if (promptSet != null && !promptSet.isEmpty() && promptSet.contains("login") && !hasRan){
                hasRan = true;
                return false;
            }
            if (null != token) {
                AMIdentity identity = IdUtils.getIdentity(token);

                OpenAMUser user = new OpenAMUser(token.getProperty("UserToken"), token);
                request.getClientInfo().setUser(user);
                return identity.isActive();
            } else {
                if (promptSet != null && !promptSet.isEmpty() && promptSet.contains("none") && promptSet.size() == 1){
                    OAuth2Utils.DEBUG.error("Not pre-authenticated and prompt parameter equals none.");
                    throw OAuthProblemException.OAuthError.ACCESS_DENIED.handle(request);
                } else if (promptSet.contains("none") && promptSet.size() > 1){
                    // prompt has more than one value with none error
                    OAuth2Utils.DEBUG.error("Prompt parameter only allows none when none is present.");
                    throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(request);
                }
                hasRan = false;
                return false;
            }
        } catch (SSOException e) {
            OAuth2Utils.DEBUG.error("Error authenticating user against OpenAM: ", e );
            redirect(request, response);
        } catch (IdRepoException e) {
            OAuth2Utils.DEBUG.error("Error authenticating user against OpenAM: ", e );
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage(), e);
        }
        return false;
    }

    protected void redirect(Request request, Response response) throws OAuthProblemException {
        if (OAuth2Utils.DEBUG.messageEnabled()){
            OAuth2Utils.DEBUG.message("Redirecting to OpenAM login page");
        }
        HttpServletRequest httpRequest = ServletUtils.getRequest(request);
        String authURL = null;
        URI authURI = null;

        authURL = getAuthURL(request);

        try {
            authURI = new URI(authURL);
        } catch (URISyntaxException e){
            OAuth2Utils.DEBUG.error("Unable to construct authURI", e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage(), e);
        }
        Reference amserver = new Reference(authURI);
        realm = OAuth2Utils.getRealm(request);
        moduleName = OAuth2Utils.getModuleName(request);
        serviceName = OAuth2Utils.getServiceName(request);
        locale = OAuth2Utils.getLocale(request);

        if (null != realm && !realm.isEmpty()) {
            amserver.addQueryParameter(OAuth2Constants.Custom.REALM, realm);
        }
        if (null != locale && !locale.isEmpty()){
            amserver.addQueryParameter(OAuth2Constants.Custom.LOCALE, locale);
        }
        if (null != moduleName && !moduleName.isEmpty()) {
            amserver.addQueryParameter(OAuth2Constants.Custom.MODULE, moduleName);
        } else if (null != serviceName && !serviceName.isEmpty()) {
            amserver.addQueryParameter(OAuth2Constants.Custom.SERVICE, serviceName);
        }
        //TODO investigate more options for the LOGIN servlet

        amserver.addQueryParameter(OAuth2Constants.Custom.GOTO, request.getResourceRef().toString());
        //response.redirectPermanent(amserver);
        Redirector redirector =
                new Redirector(new Context(), amserver.toString(), Redirector.MODE_CLIENT_FOUND);
        redirector.handle(request, response);
        throw OAuthProblemException.OAuthError.ACCESS_DENIED.handle(request).redirectUri(amserver.toUri());
    }

    private String getAuthURL(Request request){
        HttpServletRequest httpRequest = ServletUtils.getRequest(request);
        String uri = httpRequest.getRequestURI();
        String deploymentURI = uri;
        int firstSlashIndex = uri.indexOf("/");
        int secondSlashIndex = uri.indexOf("/", firstSlashIndex + 1);
        if (secondSlashIndex != -1) {
            deploymentURI = uri.substring(0, secondSlashIndex);
        }
        StringBuffer sb = new StringBuffer(100);
        sb.append(httpRequest.getScheme()).append("://")
                .append(httpRequest.getServerName()).append(":")
                .append(httpRequest.getServerPort())
                .append(deploymentURI)
                .append("/UI/Login");
        return sb.toString();
    }

    protected SSOToken getToken(Request request, Response response) throws SSOException {
        HttpServletRequest httpRequest = ServletUtils.getRequest(request);
        SSOToken token = null;
        SSOTokenManager mgr = SSOTokenManager.getInstance();
        token = mgr.createSSOToken(httpRequest);
        return token;
    }
}

