/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * 
 * "Portions Copyrighted 2008 Robert Dale <robdale@gmail.com>"
 * "Portions Copyrighted 2009 Warren Strange <warren.strange@gmail.com>"
 *
 * $Id: OpenSSOAuthenticationProvider.java,v 1.1 2009/02/26 18:18:53 wstrange Exp $
 *
 */
package com.sun.identity.provider.springsecurity;

import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.providers.AuthenticationProvider;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetails;

/**
 * @see AuthenticationProvider
 */
public class OpenSSOAuthenticationProvider implements AuthenticationProvider {

    private static Debug debug = Debug.getInstance("amSpring");
    

    /**
     * authenticate the access request.
     *
     * Note by this point the user has already been granted an sso token
     * (i.e. they have already authenticated because they were redirected
     * to opensso).
     *
     * If the user has any group membership we turn those into
     * GrantedAuthortities (roles in Spring terminolgy).
     * @see  OpenSSOSimpleAuthoritiesPopulator
     *
     * Note that a failure to retrieve OpenSSO roles does not result in
     * an non revcoverable exception (but we should revist this decision). In theory
     * we can continue with authentication only. The user will have no
     * GrantedAuthorities.
     *
     * @param authentication
     * @return authentication token - possibly withe ROLE_*  authorities.
     * 
     * @throws org.springframework.security.AuthenticationException
     */
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OpenSSOSimpleAuthoritiesPopulator populator = new OpenSSOSimpleAuthoritiesPopulator();

        if( debug.messageEnabled())
            debug.message("Authentication: " + authentication);
        
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;
        String principal =  (String) token.getPrincipal();

        // hack alert
        // We pass in the SSOToken as the credential (.e.g the password)
        // this is probably confusing - and we should refactor to use a
        // proper OpenSSOAuthenitcationToken.
        SSOToken ssoToken = (SSOToken) token.getCredentials();

        try {
            GrantedAuthority ga[] = populator.getGrantedAuthorities(ssoToken);
            UserDetails u = new User(principal, "secret", true,  true, true, true, ga);
            authentication = new UsernamePasswordAuthenticationToken(u, "secret", ga);
        } catch (Exception ex) {
             //throw new AuthenticationServiceException("Exception trying to get AMIdentity", ex);
            // Note: We eat the exception
            // The authentication can still succeed - but there will be no
            // granted authorities (i.e. no roles granted).
            // This is arguably the right thing to do here
            debug.error("Exception Trying to get AMIdentity", ex);
        }

        return authentication;
    }

    public boolean supports(Class authentication) {
        if( debug.messageEnabled() )
            debug.message("Class: " + authentication);
        return true;
    }
}
