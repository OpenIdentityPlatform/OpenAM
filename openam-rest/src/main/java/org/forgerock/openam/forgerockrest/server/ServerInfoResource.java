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
import com.iplanet.services.util.CookieUtils;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.common.FQDNUtils;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.common.configuration.MapValueParser;
import com.sun.identity.policy.PolicyConfig;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
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
import org.forgerock.openam.forgerockrest.entitlements.RealmAwareResource;
import org.forgerock.openam.services.RestSecurity;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Represents Server Information that can be queried via a REST interface.
 *
 * This resources acts as a read only resource for the moment.
 */
public class ServerInfoResource extends RealmAwareResource {

    private final Debug debug;

    private final static String COOKIE_DOMAINS = "cookieDomains";
    private final static String ALL_SERVER_INFO = "*";

    @Inject
    public ServerInfoResource(@Named("frRest") Debug debug) {
        this.debug = debug;
    }

    private final MapValueParser nameValueParser = new MapValueParser();

    /**
     * Retrieves the cookie domains set on the server.
     *
     * @param handler Result handler which handles error or success
     */
    private void getCookieDomains(ResultHandler<Resource> handler) {
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));
        Set<String> cookieDomains;
        Resource resource;
        int rev;
        try {
            cookieDomains = AuthClientUtils.getCookieDomains();
            rev = cookieDomains.hashCode();
            result.put("domains", cookieDomains);
            resource = new Resource(COOKIE_DOMAINS, Integer.toString(rev), result);
            if (debug.messageEnabled()) {
                debug.message("ServerInfoResource.getCookieDomains ::" +
                        " Added resource to response: " + COOKIE_DOMAINS);
            }
            handler.handleResult(resource);
        } catch (Exception e) {
            debug.error("ServerInfoResource.getCookieDomains : Cannot retrieve cookie domains.", e);
            handler.handleError(new NotFoundException(e.getMessage()));
        }
    }

    /**
     * Retrieves all server info set on the server.
     *
     * @param context Current Server Context.
     * @param realm realm in whose security context we use.
     * @param handler Result handler which handles error or success.
     */
    private void getAllServerInfo(ServerContext context, ResultHandler<Resource> handler, String realm) {
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
            result.put("secureCookie", CookieUtils.isCookieSecure());
            result.put("forgotPassword", String.valueOf(restSecurity.isForgotPassword()));
            result.put("selfRegistration", String.valueOf(restSecurity.isSelfRegistration()));
            result.put("lang", locale.getLocale().getLanguage());
            result.put("successfulUserRegistrationDestination",
                    restSecurity.getSuccessfulUserRegistrationDestination());
            result.put("socialImplementations", getSocialAuthnImplementations(realm));
            result.put("referralsEnabled", String.valueOf(PolicyConfig.isReferralsEnabled()));
            result.put("zeroPageLogin", AuthUtils.getZeroPageLoginConfig(realm));

            String hostname = URI.create(context.asContext(HttpContext.class).getPath()).getHost();

            result.put("FQDN", getFQDN(hostname));

            if (debug.messageEnabled()) {
                debug.message("ServerInfoResource.getAllServerInfo ::" +
                        " Added resource to response: " + ALL_SERVER_INFO);
            }

            resource = new Resource(ALL_SERVER_INFO, Integer.toString(result.asMap().hashCode()), result);

            handler.handleResult(resource);
        } catch (Exception e) {
            debug.error("ServerInfoResource.getAllServerInfo : Cannot retrieve all server info domains.", e);
            handler.handleError(new NotFoundException(e.getMessage()));
        }
    }

    private String getFQDN(String hostName) {
        String fqdn = FQDNUtils.getInstance().getFullyQualifiedHostName(hostName);

        if (fqdn != null) {
            return fqdn;
        }

        return hostName;
    }

    /**
     * Read out the data whose schema is defined in the file socialAuthN.xml. The enabled implementations value gives
     * us the social authentication implementations we care about.  We use the values there to index into the display
     * names, auth chains and icons.
     *
     * @param realm The realm/organization name
     */
    private List<SocialAuthenticationImplementation> getSocialAuthnImplementations(String realm) {

        List<SocialAuthenticationImplementation> implementations =
                new ArrayList<SocialAuthenticationImplementation>();

        try {
            final ServiceConfig serviceConfig = getServiceConfig(SocialAuthenticationImplementation.SERVICE_NAME,
                    realm);
            Set<String> enabledImplementationSet = ServiceConfigUtils.getSetAttribute(serviceConfig,
                    SocialAuthenticationImplementation.ENABLED_IMPLEMENTATIONS_ATTRIBUTE);

            if (enabledImplementationSet != null) {
                for (String name : enabledImplementationSet) {

                    SocialAuthenticationImplementation implementation = new SocialAuthenticationImplementation();
                    implementation.setDisplayName(getEntryForImplementation(serviceConfig, name,
                            SocialAuthenticationImplementation.DISPLAY_NAME_ATTRIBUTE));
                    implementation.setAuthnChain(getEntryForImplementation(serviceConfig, name,
                            SocialAuthenticationImplementation.CHAINS_ATTRIBUTE));
                    implementation.setIconPath(getEntryForImplementation(serviceConfig, name,
                            SocialAuthenticationImplementation.ICONS_ATTRIBUTE));

                    if (implementation.isValid()) {
                        implementations.add(implementation);
                    }
                }

            }
        } catch (SSOException sso) {
            debug.error("Caught SSO Exception while trying to get the ServiceConfigManager", sso);
        } catch (SMSException sms) {
            debug.error("Caught SMS Exception while trying to get the ServiceConfigManager", sms);
        } catch (Exception e) {
            if (debug.errorEnabled()) {
                debug.error("Caught exception while trying to get the attribute " +
                        SocialAuthenticationImplementation.ENABLED_IMPLEMENTATIONS_ATTRIBUTE, e);
            }
        }

        return implementations;

    }

    private ServiceConfig getServiceConfig(final String serviceName, final String realm) throws SSOException,
            SMSException {
        SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
        ServiceConfigManager mgr = new ServiceConfigManager(serviceName, token);
        return mgr.getOrganizationConfig(realm, null);
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
    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
                             ResultHandler<Resource> handler) {

        final String realm = getRealm(context);

        debug.message("ServerInfoResource :: READ : in realm: " + realm);

        if (COOKIE_DOMAINS.equalsIgnoreCase(resourceId)) {
            getCookieDomains(handler);
        } else if (ALL_SERVER_INFO.equalsIgnoreCase(resourceId)) {
            getAllServerInfo(context, handler, realm);
        } else { // for now this is the only case coming in, so fail if otherwise
            final ResourceException e = new NotSupportedException("ResourceId not supported: " + resourceId);
            if (debug.errorEnabled()) {
                debug.error("ServerInfoResource :: READ : in realm: " + realm +
                        ": Cannot receive information on requested resource: " + resourceId, e);
            }
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

}
