/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * "Portions Copyrighted 2009 Warren Strange <warren.strange@gmail.com>"
 */

package com.sun.identity.provider.springsecurity;

import com.iplanet.sso.SSOToken;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.providers.AbstractAuthenticationToken;

/**
 * *** THIS IS NOT USED RIGHT NOW ****. 
 *
 * TODO: Do we really need this class - or can we get by with the
 * UserNamePasswordToken ????
 *
 * We might want to overide isAuthenticated method to resepect the
 * sso token timeout, and/or re-check the authorizations.
 *
 * According to the docs, Spring will not recheck the authz as long as
 * isAuthenticated returns true;
 *
 *
 * Not used right now.....
 * 
 * @author warrenstrange
 */
public class OpenSSOAuthenticationToken extends AbstractAuthenticationToken{

    private SSOToken    token; 

    public OpenSSOAuthenticationToken(GrantedAuthority ga[])
    {
        super(ga);
    }
   
    /**
     *
     * Todo: Should this return the sso token?
     * @return null - as credentials are not passed by opensso
     */
    public Object getCredentials() {
        return token;
    }

    public Object getPrincipal() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
