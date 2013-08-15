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
 * "Portions copyright [year] [name of copyright owner]"
 */

package org.forgerock.restlet.ext.openam.server;

import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.restlet.ext.openam.OpenAMParameters;
import org.forgerock.openam.oauth2.provider.impl.OpenAMUser;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Redirector;
import org.restlet.security.Authenticator;
import org.restlet.security.Enroler;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Used to authenticate to OpenAM and redirect to its login page.
 */
public abstract class AbstractOpenAMAuthenticator extends Authenticator {

    private final Reference openamServer;
    private String serviceName = null;
    private String moduleName = null;
    private String realm = null;
    private String locale = null;

    //ensure the prompt for login shows the login page atleast one time
    static private boolean hasRan = false;

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
                } else if (prompt.contains("none") && promptSet.size() > 1){
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

    protected void redirect(Request request, Response response) {
        if (OAuth2Utils.DEBUG.messageEnabled()){
            OAuth2Utils.DEBUG.message("Redirecting to OpenAM login page");
        }
        HttpServletRequest httpRequest = ServletUtils.getRequest(request);
        String authURL = null;
        URI authURI = null;

        authURL = getAuthURL(request);

        /**
         * TODO check for forward and do a forward vs a redirection See IDPSSOFederate.redirectAuthentication() for details
        //check for forward or request
        StringBuffer appliRootUrl = getAppliRootUrl(httpRequest);
        boolean forward = false;
        StringBuffer newURL;
        // build newUrl to auth service and test if redirect or forward

        if(FSUtils.isSameContainer(httpRequest, authURL)){
            forward = true;
            String relativePath = getRelativePath(authURL, appliRootUrl.
                    toString());
            newURL = new StringBuffer(relativePath);
        } else {
            // cannot forward so redirect
            forward = false ;
            newURL = new StringBuffer(authURL);
        }
        */

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
        //TODO investigate more options for the LOGIN servlett

        amserver.addQueryParameter(OAuth2Constants.Custom.GOTO, request.getResourceRef().toString());

        Redirector redirector =
                new Redirector(getContext(), amserver.toString(), Redirector.MODE_CLIENT_FOUND);
        redirector.handle(request, response);
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

    /* TODO will be used when forward is implemented
    private static StringBuffer getAppliRootUrl(HttpServletRequest request) {
        StringBuffer result = new StringBuffer();
        String scheme = request.getScheme();             // http
        String serverName = request.getServerName();     // hostname.com
        int serverPort = request.getServerPort();        // 80
        String contextPath = request.getContextPath();   // /mywebapp
        result.append(scheme).append("://").append(serverName).append(":").
                append(serverPort);
        result.append(contextPath);
        return result ;
    }

    private static String getRelativePath(String absUrl, String appliRootUrl) {
        return absUrl.substring(appliRootUrl.length(), absUrl.length());
    }
    */
}
