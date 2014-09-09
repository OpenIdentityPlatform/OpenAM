/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All rights reserved.
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

package org.forgerock.openam.forgerockrest.server;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.common.configuration.MapValueParser;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.forgerockrest.ServiceConfigUtils;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.services.RestSecurity;
import org.forgerock.openam.utils.StringUtils;

import java.security.AccessController;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Represents Server Information that can be queried via a REST interface.
 *
 * This resources acts as a read only resource for the moment.
 *
 * @author alin.brici@forgerock.com
 */
public class ServerInfoResource implements CollectionResourceProvider {

    private static Debug debug = Debug.getInstance("frRest");

    private static final String SERVICE_NAME = "socialAuthNService";
    private static final String ENABLED_IMPLEMENTATIONS_ATTRIBUTE = "socialAuthNEnabled";
    private static final String DISPLAY_NAME_ATTRIBUTE = "socialAuthNDisplayName";
    private static final String CHAINS_ATTRIBUTE = "socialAuthNAuthChain";
    private static final String ICONS_ATTRIBUTE = "socialAuthNIcon";

    private static final String AUTH_CHAIN_ATTRIBUTE = "Configurations";

    private final MapValueParser nameValueParser = new MapValueParser();

    /**
     * Retrieves the cookie domains set on the server
     * @param context Current Server Context
     * @param request Request from client to retrieve id
     * @param handler Result handler which handles error or success
     */
    private void getCookieDomains(ServerContext context, String resourceId,  ReadRequest request,
            ResultHandler<Resource> handler) {
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));
        Set<String> cookieDomains;
        Resource resource;
        int rev;
        try {
            cookieDomains = AuthClientUtils.getCookieDomains();
            rev = cookieDomains.hashCode();
            result.put("domains", cookieDomains);
            resource = new Resource(resourceId, Integer.toString(rev), result);
            handler.handleResult(resource);
        } catch (Exception e) {
            debug.error("ServerInfoResource.getCookieDomains: Cannot retrieve cookie domains." + e);
            handler.handleError(new NotFoundException(e.getMessage()));
        }
    }

    /**
     * Retrieves all server info set on the server
     * @param context Current Server Context
     * @param request Request from client to retrieve id
     * @param handler Result handler which handles error or success
     * @param realm The realm
     */
    private void getAllServerInfo(ServerContext context, String resourceId,  ReadRequest request,
            ResultHandler<Resource> handler, final String realm) {
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));
        Set<String> cookieDomains;
        Set<String> protectedUserAttributes;
        Resource resource;
        RestSecurity restSecurity = new RestSecurity(realm);

        //added for the XUI to be able to understand its locale to request the appropriate translations to cache
        ISLocaleContext locale = new ISLocaleContext();
        HttpContext httpContext = context.asContext(HttpContext.class);
        locale.setLocale(httpContext); //we have nothing else to go on at this point other than their request

        try {
            cookieDomains = AuthClientUtils.getCookieDomains();
            protectedUserAttributes = restSecurity.getProtectedUserAttributes();
            result.put("domains", cookieDomains);
            result.put("protectedUserAttributes", protectedUserAttributes);
            result.put("cookieName", SystemProperties.get(Constants.AM_COOKIE_NAME, "iPlanetDirectoryPro"));
            result.put("forgotPassword", String.valueOf(restSecurity.isForgotPassword()));
            result.put("selfRegistration", String.valueOf(restSecurity.isSelfRegistration()));
            result.put("lang", locale.getLocale().getLanguage());
            result.put("successfulUserRegistrationDestination", restSecurity.getSuccessfulUserRegistrationDestination());

            addSocialAuthnImplementations(realm, result);

            resource = new Resource(resourceId, Integer.toString(result.asMap().hashCode()), result);
            handler.handleResult(resource);
        } catch (Exception e) {
            debug.error("ServerInfoResource.getAllServerInfo:: Cannot retrieve all server info. " + e);
            handler.handleError(new NotFoundException(e.getMessage()));
        }
    }

    /**
     * Read out the data whose schema is defined in the file socialAuthN.xml. The enabled implementations value gives
     * us the social authentication implementations we care about.  We use the values there to index into the display
     * names, auth chains and icons.
     *
     * @param realm The realm/organization name
     * @param result The Json we're writing into
     */
    private void addSocialAuthnImplementations(String realm, JsonValue result) {

        try {
            SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
            ServiceConfigManager mgr = new ServiceConfigManager(SERVICE_NAME, token);
            ServiceConfig serviceConfig = mgr.getOrganizationConfig(realm, null);

            Set<String> enabledImplementationSet = ServiceConfigUtils.getSetAttribute(serviceConfig,
                                                                                ENABLED_IMPLEMENTATIONS_ATTRIBUTE);
            ArrayList<SocialAuthenticationImplementation> implementations =
                                    new ArrayList<SocialAuthenticationImplementation>();

            // For each of the enabled implementations...
            if (enabledImplementationSet != null) {
                for (String name : enabledImplementationSet) {

                    SocialAuthenticationImplementation implementation = new SocialAuthenticationImplementation();
                    implementation.setDisplayName(getEntryForImplementation(serviceConfig, name,
                                                                                        DISPLAY_NAME_ATTRIBUTE));
                    implementation.setAuthnChain(getEntryForImplementation(serviceConfig, name, CHAINS_ATTRIBUTE));
                    implementation.setIconPath(getEntryForImplementation(serviceConfig, name, ICONS_ATTRIBUTE));

                    if (isValid(implementation)) {
                        implementations.add(implementation);
                    }
                }
                if (!implementations.isEmpty()) {
                    result.put("socialImplementations", implementations);
                }
            }
        } catch (SSOException sso) {
            debug.error("Caught SSO Exception while trying to get the ServiceConfigManager", sso);
        } catch (SMSException sms) {
            debug.error("Caught SMS Exception while trying to get the ServiceConfigManager", sms);
        } catch (Exception e) {
            debug.error("Caught exception while trying to get the attribute " + ENABLED_IMPLEMENTATIONS_ATTRIBUTE, e);
        }
    }

    /**
     * Check if the values in the specified implementation are valid.  Currently this just involves checking the
     * values are non null.
     *
     * @param implementation the social authentication implementation
     * @return true if the implementation has all its required values
     */
    private boolean isValid(SocialAuthenticationImplementation implementation) {

        return StringUtils.isNotBlank(implementation.getAuthnChain())
                && StringUtils.isNotBlank(implementation.getDisplayName())
                && StringUtils.isNotBlank(implementation.getIconPath());
    }

    /**
     * Given the specified name, use the serviceConfig object to retrieve the entire set of [name]=value pairs
     * specified.
     *
     * @param serviceConfig The service config
     * @param name The implementation name we're looking for, e.g. google
     * @param attributeName The attribute we're interested in
     * @return The value (as in [name]=value) corresponding to the specified name, or null if not found.
     */
    private String getEntryForImplementation(ServiceConfig serviceConfig, String name, String attributeName) {
        return nameValueParser.getValueForName(name, ServiceConfigUtils.getSetAttribute(serviceConfig, attributeName));
    }

    /**
     * {@inheritDoc}
     */
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    public void actionInstance(ServerContext context, String s, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    public void deleteInstance(ServerContext context, String s, DeleteRequest request,
            ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    public void patchInstance(ServerContext context, String s, PatchRequest request, ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    public void readInstance(ServerContext context, String s, ReadRequest request, ResultHandler<Resource> handler) {

        RealmContext realmContext = context.asContext(RealmContext.class);
        String realm = realmContext.getRealm();

        if (s.equalsIgnoreCase("cookieDomains")) {
            getCookieDomains(context, s, request, handler);
        } else if (s.equalsIgnoreCase("*")) {
            getAllServerInfo(context, s, request, handler, realm);
        } else { // for now this is the only case coming in, so fail if otherwise
            final ResourceException e = new NotSupportedException("ResourceId not supported: " + s);
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateInstance(ServerContext context, String s, UpdateRequest request,
            ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * A class to encapsulate a social authentication implementation.  This is just here so JSON will give us some
     * pretty output.
     */
    static class SocialAuthenticationImplementation {
        private String iconPath; // the path to the icon
        private String authnChain;
        private String displayName;

        public String getIconPath() {
            return iconPath;
        }

        public void setIconPath(String iconPath) {
            this.iconPath = iconPath;
        }

        public String getAuthnChain() {
            return authnChain;
        }

        public void setAuthnChain(String authnChain) {
            this.authnChain = authnChain;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
}
