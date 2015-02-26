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

import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.OAuth2Constants;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.provider.OAuth2ProviderSettings;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.Request;

import java.security.AccessController;
import java.util.Map;
import java.util.Set;

public class OAuth2ProviderSettingsImpl implements OAuth2ProviderSettings {
    private SSOToken token = null;
    private ServiceConfig scm = null;
    private ServiceConfigManager mgr = null;
    private Request request = null;

    public OAuth2ProviderSettingsImpl(Request request){
        this.request = request;
        token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
        try {
            mgr = new ServiceConfigManager(token, OAuth2Constants.OAuth2ProviderService.NAME, OAuth2Constants.OAuth2ProviderService.VERSION);
            scm = mgr.getOrganizationConfig(OAuth2Utils.getRealm(request), null);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider settings config", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider settings config");
        }
    }

    public OAuth2ProviderSettingsImpl(Request request, SSOToken token, ServiceConfig sc, ServiceConfigManager scm){
        this.request = request;
        this.token = token;
        this.scm = sc;
        this.mgr = scm;
    }

    /**
     * {@inheritDoc}
     */
    public long getAuthorizationCodeLifetime(){
        try{
            Map<String, Set<String>> attrs = scm.getAttributes();
            Set<String> attr = attrs.get(OAuth2Constants.OAuth2ProviderService.AUTHZ_CODE_LIFETIME_NAME);
            if (attr != null && !attr.isEmpty()){
                return Long.parseLong(attr.iterator().next());
            } else {
                OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                        OAuth2Constants.OAuth2ProviderService.AUTHZ_CODE_LIFETIME_NAME);
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                    OAuth2Constants.OAuth2ProviderService.AUTHZ_CODE_LIFETIME_NAME);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getRefreshTokenLifetime(){
        try{
            Map<String, Set<String>> attrs = scm.getAttributes();
            Set<String> attr = attrs.get(OAuth2Constants.OAuth2ProviderService.REFRESH_TOKEN_LIFETIME_NAME);
            if (attr != null && !attr.isEmpty()){
                return Long.parseLong(attr.iterator().next());
            } else {
                OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                        OAuth2Constants.OAuth2ProviderService.REFRESH_TOKEN_LIFETIME_NAME);
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                    OAuth2Constants.OAuth2ProviderService.REFRESH_TOKEN_LIFETIME_NAME);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getAccessTokenLifetime(){
        try{
            Map<String, Set<String>> attrs = scm.getAttributes();
            Set<String> attr = attrs.get(OAuth2Constants.OAuth2ProviderService.ACCESS_TOKEN_LIFETIME_NAME);
            if (attr != null && !attr.isEmpty()){
                return Long.parseLong(attr.iterator().next());
            } else {
                OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                        OAuth2Constants.OAuth2ProviderService.ACCESS_TOKEN_LIFETIME_NAME);
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                    OAuth2Constants.OAuth2ProviderService.ACCESS_TOKEN_LIFETIME_NAME);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean getRefreshTokensEnabledState(){
        try{
            Map<String, Set<String>> attrs = scm.getAttributes();
            Set<String> attr = attrs.get(OAuth2Constants.OAuth2ProviderService.ISSUE_REFRESH_TOKEN);
            if (attr != null && !attr.isEmpty()){
                return Boolean.parseBoolean(attr.iterator().next());
            } else {
                OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                        OAuth2Constants.OAuth2ProviderService.ISSUE_REFRESH_TOKEN);
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                    OAuth2Constants.OAuth2ProviderService.ISSUE_REFRESH_TOKEN);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getScopeImplementationClass(){
        try{
            Map<String, Set<String>> attrs = scm.getAttributes();
            Set<String> attr = attrs.get(OAuth2Constants.OAuth2ProviderService.SCOPE_PLUGIN_CLASS);
            if (attr != null && !attr.isEmpty()){
                return attr.iterator().next();
            } else {
                OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                        OAuth2Constants.OAuth2ProviderService.SCOPE_PLUGIN_CLASS);
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                    OAuth2Constants.OAuth2ProviderService.SCOPE_PLUGIN_CLASS);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getResponseTypes(){
        try{
            Map<String, Set<String>> attrs = scm.getAttributes();
            Set<String> attr = attrs.get(OAuth2Constants.OAuth2ProviderService.RESPONSE_TYPE_LIST);
            if (attr != null && !attr.isEmpty()){
                return attr;
            } else {
                OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                        OAuth2Constants.OAuth2ProviderService.RESPONSE_TYPE_LIST);
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                    OAuth2Constants.OAuth2ProviderService.RESPONSE_TYPE_LIST);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getListOfAttributesTheResourceOwnerIsAuthenticatedOn(){
        try{
            Map<String, Set<String>> attrs = scm.getAttributes();
            Set<String> attr = attrs.get(OAuth2Constants.OAuth2ProviderService.AUTHENITCATION_ATTRIBUTES);
            if (attr != null && !attr.isEmpty()){
                return attr;
            } else {
                OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                        OAuth2Constants.OAuth2ProviderService.AUTHENITCATION_ATTRIBUTES);
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                    OAuth2Constants.OAuth2ProviderService.AUTHENITCATION_ATTRIBUTES);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getSharedConsentAttributeName(){
        try{
            Map<String, Set<String>> attrs = scm.getAttributes();
            Set<String> attr = attrs.get(OAuth2Constants.OAuth2ProviderService.SAVED_CONSENT_ATTRIBUTE);
            if (attr != null && !attr.isEmpty()){
                return attr.iterator().next();
            } else {
                OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                        OAuth2Constants.OAuth2ProviderService.SAVED_CONSENT_ATTRIBUTE);
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                    OAuth2Constants.OAuth2ProviderService.SAVED_CONSENT_ATTRIBUTE);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getAuthorizationEndpoint(){
        return OAuth2Utils.getDeploymentURL(request)+"/oauth2/authorize";
    }

    /**
     * {@inheritDoc}
     */
    public String getTokenEndpoint(){
        return OAuth2Utils.getDeploymentURL(request)+"/oauth2/authorize";
    }

    /**
     * {@inheritDoc}
     */
    public String getUserInfoEndpoint(){
        return OAuth2Utils.getDeploymentURL(request)+"/oauth2/userinfo";
    }

    /**
     * Below Is OpenID Connect Settings
     */

    /**
     * {@inheritDoc}
     */
    public String getOpenIDConnectVersion(){
        return "3.0";
    }

    /**
     * {@inheritDoc}
     */
    public String getOpenIDConnectIssuer(){
        return OAuth2Utils.getDeploymentURL(request);
    }

    /**
     * {@inheritDoc}
     */
    public String getCheckSessionEndpoint(){
        return OAuth2Utils.getDeploymentURL(request)+"/oauth2/connect/checkSession";
    }

    /**
     * {@inheritDoc}
     */
    public String getEndSessionEndPoint(){
        return OAuth2Utils.getDeploymentURL(request)+"/oauth2/connect/endSession";
    }

    /**
     * {@inheritDoc}
     */
    public String getJWKSUri(){
        try{
            Map<String, Set<String>> attrs = scm.getAttributes();
            Set<String> attr = attrs.get(OAuth2Constants.OAuth2ProviderService.JKWS_URI);
            if (attr != null && !attr.isEmpty()){
                return attr.iterator().next();
            } else {
                OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                        OAuth2Constants.OAuth2ProviderService.JKWS_URI);
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                    OAuth2Constants.OAuth2ProviderService.JKWS_URI);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getClientRegistrationEndpoint(){
        return OAuth2Utils.getDeploymentURL(request)+"/oauth2/connect/register";
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getSubjectTypesSupported(){
        try{
            Map<String, Set<String>> attrs = scm.getAttributes();
            Set<String> attr = attrs.get(OAuth2Constants.OAuth2ProviderService.SUBJECT_TYPES_SUPPORTED);
            if (attr != null && !attr.isEmpty()){
                return attr;
            } else {
                OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                        OAuth2Constants.OAuth2ProviderService.SUBJECT_TYPES_SUPPORTED);
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                    OAuth2Constants.OAuth2ProviderService.SUBJECT_TYPES_SUPPORTED);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getTheIDTokenSigningAlgorithmsSupported(){
        try{
            Map<String, Set<String>> attrs = scm.getAttributes();
            Set<String> attr = attrs.get(OAuth2Constants.OAuth2ProviderService.ID_TOKEN_SIGNING_ALGORITHMS);
            if (attr != null && !attr.isEmpty()){
                return attr;
            } else {
                OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                        OAuth2Constants.OAuth2ProviderService.ID_TOKEN_SIGNING_ALGORITHMS);
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                    OAuth2Constants.OAuth2ProviderService.ID_TOKEN_SIGNING_ALGORITHMS);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getSupportedClaims(){
        try{
            Map<String, Set<String>> attrs = scm.getAttributes();
            Set<String> attr = attrs.get(OAuth2Constants.OAuth2ProviderService.SUPPORTED_CLAIMS);
            if (attr != null && !attr.isEmpty()){
                return attr;
            } else {
                OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                        OAuth2Constants.OAuth2ProviderService.SUPPORTED_CLAIMS);
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: "+
                    OAuth2Constants.OAuth2ProviderService.SUPPORTED_CLAIMS);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
        }
    }

}
