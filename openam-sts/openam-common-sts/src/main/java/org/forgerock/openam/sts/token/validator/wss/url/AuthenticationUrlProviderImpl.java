/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token.validator.wss.url;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.MalformedURLException;
import java.net.URL;

/**
 */
public class AuthenticationUrlProviderImpl implements AuthenticationUrlProvider {
    private static final Character QUESTION_MARK = '?';
    private static final Character AMPERSAND = '&';
    private static final String AUTH_INDEX_TYPE_PARAM = "authIndexType=";
    private static final String AUTH_INDEX_VALUE_PARAM = "authIndexValue=";

    private final AuthTargetMapping authTargetMapping;
    private final String realm;
    private final String restAuthnUriElement;
    private final String amDeploymentUrl;
    private final String jsonRoot;
    private final UrlConstituentCatenator urlConstituentCatenator;


    @Inject
    public AuthenticationUrlProviderImpl(
            @Named(AMSTSConstants.AM_DEPLOYMENT_URL) String amDeploymentUrl,
            @Named(AMSTSConstants.REST_AUTHN_URI_ELEMENT) String restAuthnUriElement,
            AuthTargetMapping authTargetMapping,
            @Named(AMSTSConstants.REALM) String realm,
            @Named(AMSTSConstants.AM_REST_AUTHN_JSON_ROOT) String jsonRoot,
            UrlConstituentCatenator urlConstituentCatenator) {
        this.amDeploymentUrl = amDeploymentUrl;
        this.restAuthnUriElement = restAuthnUriElement;
        this.authTargetMapping = authTargetMapping;
        this.realm = realm;
        this.jsonRoot = jsonRoot;
        this.urlConstituentCatenator = urlConstituentCatenator;
    }

    @Override
    public URL authenticationUrl(Object token) throws TokenValidationException {
        StringBuilder stringBuilder =
                new StringBuilder(urlConstituentCatenator.catenateUrlConstituents(amDeploymentUrl, jsonRoot));
        if (!AMSTSConstants.ROOT_REALM.equals(realm)) {
            stringBuilder = urlConstituentCatenator.catentateUrlConstituent(stringBuilder, realm);
        }
        stringBuilder = urlConstituentCatenator.catentateUrlConstituent(stringBuilder, restAuthnUriElement);
        AuthTargetMapping.AuthTarget target = authTargetMapping.getAuthTargetMapping(token.getClass());
        if (target != null) {
            stringBuilder.append(QUESTION_MARK);
            stringBuilder.append(AUTH_INDEX_TYPE_PARAM).append(target.getAuthIndexType());
            stringBuilder.append(AMPERSAND).append(AUTH_INDEX_VALUE_PARAM).append(target.getAuthIndexValue());
        }
        try {
            return new URL(stringBuilder.toString());
        } catch (MalformedURLException e) {
            throw new TokenValidationException(ResourceException.INTERNAL_ERROR, e.getMessage(), e);
        }
    }
}
