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
 * "Portions Copyrighted [2012] [ForgeRock Inc]"
 */

package org.forgerock.restlet.ext.oauth2.provider;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSEntry;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import java.security.AccessController;
import java.util.*;

/**
 * Registers an OAuth2 Client with OpenAM
 */
public class RegisterClient extends ServerResource {

    private Reference registerClientRef;
    static final String REALM = "realm";
    static final String CLIENT_ID = "client_id";
    static final String CLIENT_PASSWORD = "client_password";
    static final String CLIENT_TYPE = "client_type";
    static final String REDIRECTION_URLS = "redirection_urls";
    static final String SCOPES = "scopes";
    static final String DEFAULT_SCOPES = "default_scopes";
    static final String DISPLAY_NAMES = "display_names";
    static final String DISPLAY_DESCRIPTIONS = "display_descriptions";
    //static final String TOKEN_TYPE = "token_type";

    static final String CLIENT_PASSWORD_ = "userpassword";
    static final String CLIENT_TYPE_ = "com.forgerock.openam.oauth2provider.clientType";
    static final String REDIRECTION_URLS_ = "com.forgerock.openam.oauth2provider.redirectionURIs";
    static final String SCOPES_ = "com.forgerock.openam.oauth2provider.scopes";
    static final String DEFAULT_SCOPES_ = "com.forgerock.openam.oauth2provider.defaultScopes";
    static final String DISPLAY_NAMES_ = "com.forgerock.openam.oauth2provider.name";
    static final String DISPLAY_DESCRIPTIONS_ = "com.forgerock.openam.oauth2provider.description";
    //static final String TOKEN_TYPE_ = "com.forgerock.openam.oauth2provider.tokenType";

    public RegisterClient() {
        this.registerClientRef = null;
    }

    public RegisterClient(Context context, Reference registerClientRef) {
        this.registerClientRef = registerClientRef;
        init(context, null, null);
    }

    /**
     * Set-up method that can be overridden in order to initialize the state of
     * the resource. By default it does nothing.
     *
     * @see #init(org.restlet.Context, org.restlet.Request,
     *      org.restlet.Response)
     */
    protected void doInit() throws ResourceException {

        //authenticate the user
        try {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            SSOToken ssoToken = manager.createSSOToken(getRequest().getCookies().getValues("iPlantDirectoryPro"));
            manager.validateToken(ssoToken);

            if (!ssoToken.getPrincipal().getName().equalsIgnoreCase(
                    "id=amadmin,ou=user," + SMSEntry.getRootSuffix())
                    ) {
                getResponse().setStatus(Status.valueOf(401));
            }
        } catch (SSOException e) {
            getResponse().setStatus(Status.valueOf(401));
        }
        Form form = new Form(getRequestEntity());
        Map <String, String> parameters = form.getValuesMap();
        if (parameters.isEmpty()){
            //no parameters error
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
                    "A required request parameter was not sent");
        }
        //TODO Need to sanitize the user input.
        Map<String, Set<String>> attributes = new HashMap<String, Set<String>>();

        Set<String> temp = new HashSet<String>();
        String value;
        //value = parameters.get(TOKEN_TYPE);
        //if (value == null){
        //    throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
        //            "A required request parameter was not sent");
        //}
        //temp.add(value);
        //attributes.put(TOKEN_TYPE_, temp);

        temp = new HashSet<String>();
        value = parameters.get(CLIENT_PASSWORD);
        if (value == null){
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
                    "A required request parameter was not sent");
        }
        temp.add(value);
        attributes.put(CLIENT_PASSWORD_, temp);

        temp = new HashSet<String>();
        value = parameters.get(CLIENT_TYPE);
        if (value == null){
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
                    "A required request parameter was not sent");
        }
        temp.add(value);
        attributes.put(CLIENT_TYPE_, temp);

        temp = new HashSet<String>();
        value = parameters.get(REDIRECTION_URLS);
        if (value == null){
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
                    "A required request parameter was not sent");
        }
        temp.addAll((Collection<String>)Arrays.asList(value.split(";")));
        attributes.put(REDIRECTION_URLS_, temp);

        temp = new HashSet<String>();
        value = parameters.get(SCOPES);
        if (value == null){
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
                    "A required request parameter was not sent");
        }
        temp.addAll((Collection<String>)Arrays.asList(value.split(";")));
        attributes.put(SCOPES_, temp);

        temp = new HashSet<String>();
        value = parameters.get(DEFAULT_SCOPES);
        if (value == null){
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
                    "A required request parameter was not sent");
        }
        temp.addAll((Collection<String>)Arrays.asList(value.split(";")));
        attributes.put(DEFAULT_SCOPES_, temp);

        temp = new HashSet<String>();
        value = parameters.get(DISPLAY_NAMES);
        if (value == null){
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
                    "A required request parameter was not sent");
        }
        temp.addAll((Collection<String>)Arrays.asList(value.split(";")));
        attributes.put(DISPLAY_NAMES_, temp);

        temp = new HashSet<String>();
        value = parameters.get(DISPLAY_DESCRIPTIONS);
        if (value == null){
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
                                                                        "A required request parameter was not sent");
        }
        temp.addAll((Collection<String>)Arrays.asList(value.split(";")));
        attributes.put(DISPLAY_DESCRIPTIONS_, temp);

        temp = new HashSet<String>();
        temp.add("OAuth2Client");
        attributes.put(IdConstants.AGENT_TYPE, temp);

        temp = new HashSet<String>();
        temp.add("Active");
        attributes.put("sunIdentityServerDeviceStatus", temp);
        SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
        try {
            AMIdentityRepository repo = new AMIdentityRepository(token ,parameters.get(REALM));
            repo.createIdentity(IdType.AGENTONLY,parameters.get(CLIENT_ID),attributes);
        } catch(Exception e){
            throw new OAuthProblemException(Status.CLIENT_ERROR_BAD_REQUEST.getCode(),
                    "Create Client Failed", "Unable to create the client in the available repositories", null);
        }

    }

    @Get("json")
    public Representation validate() throws ResourceException {

        return null;
    }
}
