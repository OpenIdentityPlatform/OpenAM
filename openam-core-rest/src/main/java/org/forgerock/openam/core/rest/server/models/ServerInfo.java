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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.core.rest.server.models;


import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.SERVER_INFO_RESOURCE;

import java.util.List;
import java.util.Set;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;
import org.forgerock.openam.core.rest.server.SocialAuthenticationImplementation;

import com.sun.identity.authentication.client.ZeroPageLoginConfig;

/**
 * ServerInfo bean.
 */
@Title(SERVER_INFO_RESOURCE + "serverinfo.title")
@Description(SERVER_INFO_RESOURCE + "serverinfo.description")
public class ServerInfo {

    @Title(SERVER_INFO_RESOURCE + "serverinfo.domains.title")
    @Description(SERVER_INFO_RESOURCE + "serverinfo.domains.description")
    public Set<String> domains;

    @Title(SERVER_INFO_RESOURCE + "serverinfo.protectedUserAttributes.title")
    @Description(SERVER_INFO_RESOURCE + "serverinfo.protectedUserAttributes.description")
    public Set<String> protectedUserAttributes;

    @Title(SERVER_INFO_RESOURCE + "serverinfo.cookieName.title")
    @Description(SERVER_INFO_RESOURCE + "serverinfo.cookieName.description")
    public String cookieName;

    @Title(SERVER_INFO_RESOURCE + "serverinfo.secureCookie.title")
    @Description(SERVER_INFO_RESOURCE + "serverinfo.secureCookie.description")
    public boolean secureCookie;

    @Title(SERVER_INFO_RESOURCE + "serverinfo.forgotPassword.title")
    @Description(SERVER_INFO_RESOURCE + "serverinfo.forgotPassword.description")
    public String forgotPassword;

    @Title(SERVER_INFO_RESOURCE + "serverinfo.forgotUsername.title")
    @Description(SERVER_INFO_RESOURCE + "serverinfo.forgotUsername.description")
    public String forgotUsername;

    @Title(SERVER_INFO_RESOURCE + "serverinfo.kbaEnabled.title")
    @Description(SERVER_INFO_RESOURCE + "serverinfo.kbaEnabled.description")
    public String kbaEnabled;

    @Title(SERVER_INFO_RESOURCE + "serverinfo.selfRegistration.title")
    @Description(SERVER_INFO_RESOURCE + "serverinfo.selfRegistration.description")
    public String selfRegistration;

    @Title(SERVER_INFO_RESOURCE + "serverinfo.lang.title")
    @Description(SERVER_INFO_RESOURCE + "serverinfo.lang.description")
    public String lang;

    @Title(SERVER_INFO_RESOURCE + "serverinfo.successfulUserRegistrationDestination.title")
    @Description(SERVER_INFO_RESOURCE + "serverinfo.successfulUserRegistrationDestination.description")
    public String successfulUserRegistrationDestination;

    @Title(SERVER_INFO_RESOURCE + "serverinfo.socialImplementations.title")
    @Description(SERVER_INFO_RESOURCE + "serverinfo.socialImplementations.description")
    public List<SocialAuthenticationImplementation> socialImplementations;

    @Title(SERVER_INFO_RESOURCE + "serverinfo.referralsEnabled.title")
    @Description(SERVER_INFO_RESOURCE + "serverinfo.referralsEnabled.description")
    public String referralsEnabled;

    @Title(SERVER_INFO_RESOURCE + "serverinfo.zeroPageLogin.title")
    @Description(SERVER_INFO_RESOURCE + "serverinfo.zeroPageLogin.description")
    public ZeroPageLoginConfig zeroPageLogin;

    @Title(SERVER_INFO_RESOURCE + "serverinfo.realm.title")
    @Description(SERVER_INFO_RESOURCE + "serverinfo.realm.description")
    public String realm;

    @Title(SERVER_INFO_RESOURCE + "serverinfo.xuiUserSessionValidationEnabled.title")
    @Description(SERVER_INFO_RESOURCE + "serverinfo.xuiUserSessionValidationEnabled.description")
    public boolean xuiUserSessionValidationEnabled;
}