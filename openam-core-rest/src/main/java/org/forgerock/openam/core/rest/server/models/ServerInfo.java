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


import java.util.List;
import java.util.Set;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;
import org.forgerock.openam.core.rest.server.SocialAuthenticationImplementation;

import com.sun.identity.authentication.client.ZeroPageLoginConfig;

/**
 * ServerInfo bean.
 */
@Title("Site")
@Description("Represents server information")
public class ServerInfo {

    @Title("Cookie domains")
    @Description("")
    public Set<String> domains;

    @Title("Protected user attributes")
    public Set<String> protectedUserAttributes;

    @Title("Cookie Name")
    public String cookieName;

    @Title("Secure Cookie")
    public boolean secureCookie;

    @Title("Forgot password")
    @Description("True if forgotten password is enabled")
    public String forgotPassword;

    @Title("Forgot username")
    @Description("True if forgotten username is enabled")
    public String forgotUsername;

    @Title("Security Question")
    @Description("True if security questions are enabled")
    public String kbaEnabled;

    @Title("User self registration")
    @Description("True if user self registration is enabled")
    public String selfRegistration;

    @Title("Locale")
    @Description("The locale")
    public String lang;

    @Title("Successful user registration destination")
    public String successfulUserRegistrationDestination;

    @Title("Social authentication implementations")
    public List<SocialAuthenticationImplementation> socialImplementations;

    @Title("Referrals enabled")
    @Description("True if referrals are enabled")
    public String referralsEnabled;

    @Title("Zero page login config")
    public ZeroPageLoginConfig zeroPageLogin;

    @Title("Realm")
    public String realm;

    @Title("XUI user session validation enabled")
    @Description("True if XUI user session validation is enabled")
    public boolean xuiUserSessionValidationEnabled;
}